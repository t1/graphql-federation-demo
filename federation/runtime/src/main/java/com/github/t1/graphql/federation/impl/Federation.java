package com.github.t1.graphql.federation.impl;

import com.github.t1.graphql.federation.api.External;
import com.github.t1.graphql.federation.api.FederatedSource;
import com.github.t1.graphql.federation.api.Key;
import graphql.TypeResolutionEnvironment;
import graphql.scalar.GraphqlStringCoercing;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLUnionType;
import io.smallrye.graphql.bootstrap.Config;
import io.smallrye.graphql.execution.SchemaPrinter;
import io.smallrye.graphql.schema.ScanningContext;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.schema.model.Schema;
import lombok.EqualsAndHashCode;
import lombok.extern.java.Log;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;
import static java.util.stream.Collectors.toList;

/**
 * @see <a href="https://www.apollographql.com/docs/federation/federation-spec/">GraphQL Federation Spec</a>
 */
@Log
@GraphQLApi
@ApplicationScoped
public class Federation {
    private static final DotName KEY = DotName.createSimple(Key.class.getName());
    private static final DotName FEDERATED_SOURCE = DotName.createSimple(FederatedSource.class.getName());

    private static final Config PRINTER_CONFIG = new Config() {
        @Override public boolean isIncludeDirectivesInSchema() { return true; }

        @Override public boolean isIncludeScalarsInSchema() { return true; }
    };

    private static final GraphQLScalarType _FieldSet = GraphQLScalarType.newScalar().name("_FieldSet")
        .coercing(new GraphqlStringCoercing()).build();

    private static final GraphQLScalarType _Any = GraphQLScalarType.newScalar().name("_Any")
        .coercing(new AnyCoercing()).build();

    public static class AnyCoercing implements Coercing<Object, Object> {
        @Override public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
            return dataFetcherResult;
        }

        @Override public Object parseValue(Object input) throws CoercingParseValueException {
            return input;
        }

        @Override public Object parseLiteral(Object input) throws CoercingParseLiteralException {
            return input;
        }
    }


    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject Schema schema;

    private GraphQLUnionType _Entity;
    private GraphQLFieldDefinition _entities;

    private GraphQLFieldDefinition _service;
    private GraphQLObjectType _Service;
    private GraphQLFieldDefinition _Service_sdl;
    private String rawServiceSchema;

    private GraphQLObjectType.Builder query;
    private GraphQLCodeRegistry.Builder codeRegistry;

    private final Map<Class<?>, FederatedEntityResolver> federatedEntityResolvers = new LinkedHashMap<>();
    private final Map<Class<?>, MainEntityResolver> mainEntityResolvers = new LinkedHashMap<>();

    public GraphQLSchema.Builder beforeSchemaBuild(@Observes GraphQLSchema.Builder builder) {
        var original = builder.build();
        this.rawServiceSchema = rawServiceSchema(original);

        // TODO C: make the query builder available from SmallRye
        this.query = GraphQLObjectType.newObject(original.getQueryType());
        // TODO C: make the GraphQLCodeRegistry available from SmallRye
        this.codeRegistry = GraphQLCodeRegistry.newCodeRegistry(original.getCodeRegistry());

        addScalars(builder);
        addEntityResolvers();
        addUnions(builder);
        addQueries();
        addCode();

        builder.query(query);
        builder.codeRegistry(codeRegistry.build());
        return builder;
    }

    /** The SDL without the federation extras (but with the directives) */
    private String rawServiceSchema(GraphQLSchema original) {
        try (var stream = getClass().getResourceAsStream("/_service.graphql")) {
            if (stream != null)
                return new Scanner(stream).useDelimiter("\\Z").next();
        } catch (IOException e) {
            throw new RuntimeException("could not load _service.graphql", e);
        }

        var builder = GraphQLSchema.newSchema(original);
        builder.clearDirectives();
        builder.clearSchemaDirectives();
        builder.clearAdditionalTypes();
        return new SchemaPrinter(PRINTER_CONFIG).print(builder.build())
            // TODO C: remove standard directive declarations
            .replace("\"Marks the field or enum value as deprecated\"\n" +
                "directive @deprecated(\n" +
                "    \"The reason for the deprecation\"\n" +
                "    reason: String = \"No longer supported\"\n" +
                "  ) on FIELD_DEFINITION | ENUM_VALUE\n" +
                "\n" +
                "\"Exposes a URL that specifies the behaviour of this scalar.\"\n" +
                "directive @specifiedBy(\n" +
                "    \"The URL that specifies the behaviour of this scalar.\"\n" +
                "    url: String!\n" +
                "  ) on SCALAR\n", "")
            // Apollo doesn't like a single `key` field to be wrapped in square brackets
            .replaceAll("@key\\(fields ?: ?\\[([^,\\]]*)]\\)", "@key(fields: $1)");
    }

    private void addScalars(GraphQLSchema.Builder builder) {
        builder.additionalType(_Any);
        builder.additionalType(_FieldSet);
    }

    private void addUnions(GraphQLSchema.Builder builder) {
        var graphQLSchema = builder.build();
        var entityUnionTypes = ScanningContext.getIndex()
            .getAnnotations(KEY).stream()
            .map(AnnotationInstance::target)
            .map(AnnotationTarget::asClass)
            .map(typeInfo -> toObjectType(typeInfo, graphQLSchema))
            .toArray(GraphQLObjectType[]::new);
        if (entityUnionTypes.length == 0) return;
        // union _Entity = ...
        _Entity = GraphQLUnionType.newUnionType().name("_Entity")
            .possibleTypes(entityUnionTypes)
            .description("This is a union of all types that use the @key directive, " +
                "including both types native to the schema and extended types.").build();
        builder.additionalType(_Entity);
    }

    private GraphQLObjectType toObjectType(ClassInfo typeInfo, GraphQLSchema graphQLSchema) {
        var typeName = typeInfo.name().local();
        var objectType = graphQLSchema.getObjectType(typeName);
        if (objectType == null) throw new IllegalStateException("no class registered in schema for " + typeName);
        return objectType;
    }

    private void addQueries() {
        // _entities(representations: [_Any!]!): [_Entity]!
        this._entities = newFieldDefinition().name("_entities")
            .argument(newArgument().name("representations").type(nonNull(list(nonNull(_Any)))))
            .type(nonNull(list(_Entity))).build();
        query.field(_entities);

        // _service: _Service!
        this._Service_sdl = newFieldDefinition().name("sdl").type(nonNull(GraphQLString))
            .description("The sdl representing the federated service capabilities. Includes federation directives, " +
                "removes federation types, and includes rest of full schema after schema directives have been applied")
            .build();
        this._Service = GraphQLObjectType.newObject()
            .name("_Service")
            .field(_Service_sdl)
            .build();
        this._service = newFieldDefinition().name("_service").type(nonNull(_Service)).build();
        query.field(_service);
    }

    private void addCode() {
        codeRegistry.typeResolver(_Entity, this::resolveEntity);
        codeRegistry.dataFetcher(FieldCoordinates.coordinates(query.build(), _entities), this::fetchEntities);
        codeRegistry.dataFetcher(FieldCoordinates.coordinates(query.build(), _service), this::fetchService);
        codeRegistry.dataFetcher(FieldCoordinates.coordinates(_Service, _Service_sdl), this::fetchServiceSDL);
    }


    public GraphQLObjectType resolveEntity(TypeResolutionEnvironment environment) {
        var typeName = environment.getObject().getClass().getSimpleName(); // TODO B: type renames
        return environment.getSchema().getObjectType(typeName);
    }

    public List<Object> fetchEntities(DataFetchingEnvironment environment) {
        @SuppressWarnings("unchecked")
        var representations = (List<Map<String, Object>>) environment.getArgument("representations");
        return representations.stream()
            .map(this::resolve)
            .collect(toList());
    }

    private Object resolve(Map<String, Object> representation) {
        var type = type(representation);
        var federatedEntityResolver = federatedEntityResolvers.get(type);
        if (federatedEntityResolver != null)
            return federatedEntityResolver.apply(representation);
        var mainEntityResolver = mainEntityResolvers.get(type);
        if (mainEntityResolver != null)
            return mainEntityResolver.apply(representation);
        throw new IllegalStateException("No federated or main entity resolver found for " + type + ". " +
            "Add a @Query with the @Key fields as parameters.");
    }

    private Class<?> type(Map<String, Object> representation) {
        var typeName = (String) representation.get("__typename");
        try {
            var type = schema.getTypes().get(typeName);
            if (type == null) throw new IllegalStateException("no class registered in schema for " + typeName);
            return Class.forName(type.getClassName());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("can't create extended type instance " + typeName, e);
        }
    }

    public Object fetchService(DataFetchingEnvironment env) { return _service; }

    public String fetchServiceSDL(DataFetchingEnvironment env) { return rawServiceSchema; }

    private void addEntityResolvers() {
        addMainEntityResolvers();
        addFederatedEntityResolvers();
    }

    private void addMainEntityResolvers() {
        this.schema.getQueries().stream()
            .filter(this::isMainEntityResolver)
            .map(MainEntityResolver::new)
            .distinct()
            .forEach(mainEntityResolver -> {
                log.fine("add main entity resolver method " + mainEntityResolver.method);
                mainEntityResolvers.put(mainEntityResolver.getType(), mainEntityResolver);
            });
    }

    private boolean isMainEntityResolver(Operation operation) {
        // TODO C: check type NOT @extends
        // TODO B: check (non-optional) method params match @id
        return operation.hasArguments();
    }

    private void addFederatedEntityResolvers() {
        ScanningContext.getIndex()
            .getAnnotations(FEDERATED_SOURCE).stream()
            .map(AnnotationInstance::target)
            .map(AnnotationTarget::asMethodParameter)
            .map(MethodParameterInfo::method)
            .map(Federation::toReflectionMethod)
            .distinct()
            .forEach(method -> {
                log.fine("add federated entity resolver method " + method);
                // TODO C: check that the target type IS @extends
                federatedEntityResolvers.put(method.getParameterTypes()[0], new FederatedEntityResolver(schema, method));
            });
    }

    private static Method toReflectionMethod(MethodInfo methodInfo) {
        try {
            var declaringClass = Class.forName(methodInfo.declaringClass().name().toString());
            var parameterTypes = methodInfo.parameters().stream()
                .map(Type::asClassType)
                .map(Type::name)
                .map(DotName::toString)
                .map(Federation::toClass)
                .toArray(Class[]::new);
            return declaringClass.getDeclaredMethod(methodInfo.name(), parameterTypes);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("can't find reflection method for " + methodInfo, e);
        }
    }

    private static Class<?> toClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class not found: " + className, e);
        }
    }

    /** A federated query for a type that non-extends type by using its key fields */
    @EqualsAndHashCode(of = "method")
    public static class MainEntityResolver implements Function<Map<String, Object>, Object> {
        private final Operation operation;
        private final Method method;

        private MainEntityResolver(Operation operation) {
            this.operation = operation;
            this.method = toMethod(operation);
        }

        private static Method toMethod(Operation operation) {
            return toReflectionMethod(ScanningContext.getIndex()
                .getClassByName(DotName.createSimple(operation.getClassName()))
                .firstMethod(operation.getMethodName()));
        }

        @Override public Object apply(Map<String, Object> representation) {
            var declaringBean = CDI.current().select(method.getDeclaringClass()).get();
            var args = new Object[method.getParameterCount()];
            for (int i = 0; i < method.getParameterCount(); i++) {
                var argument = operation.getArguments().get(i);
                args[i] = representation.get(argument.getName());
            }
            try {
                return method.invoke(declaringBean, args);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("invocation of federated entity resolver method failed: " + method, e);
            }
        }

        public Class<?> getType() { return method.getReturnType(); }
    }

    /** A method annotated as {@link FederatedSource} */
    public static class FederatedEntityResolver implements Function<Map<String, Object>, Object> {
        private final Schema schema;
        private final Method method;

        private FederatedEntityResolver(Schema schema, Method method) {
            this.schema = schema;
            this.method = method;
        }

        @Override public Object apply(Map<String, Object> representation) {
            // TODO C: batch entity resolvers
            var source = instantiate(representation);
            Object value = invoke(source);
            set(source, value);
            return source;
        }

        /** Create a prefilled instance of the type going into the federated entity resolver */
        private Object instantiate(Map<String, Object> representation) {
            var typeName = (String) representation.get("__typename");
            try {
                var type = schema.getTypes().get(typeName);
                if (type == null) throw new IllegalStateException("no class registered in schema for " + typeName);
                var cls = Class.forName(type.getClassName());
                Object instance = cls.getConstructor().newInstance();
                // TODO B: field renames
                for (String fieldName : type.getFields().keySet()) {
                    if ("__typename".equals(fieldName)) continue;
                    var value = (String) representation.get(fieldName);
                    var field = cls.getDeclaredField(fieldName);
                    if (field.isAnnotationPresent(External.class))
                        log.fine("non-external field " + fieldName + " on " + typeName);

                    field.setAccessible(true);
                    field.set(instance, value);
                }
                return instance;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("can't create extended type instance " + typeName, e);
            }
        }

        private Object invoke(Object source) {
            var declaringBean = CDI.current().select(method.getDeclaringClass()).get();
            try {
                return method.invoke(declaringBean, source);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("invocation of federated entity resolver method failed: " + method, e);
            }
        }

        private void set(Object source, Object value) {
            var fieldName = method.getName(); // TODO B: method renames
            try {
                var field = source.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(source, value);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("setting of federated entity resolver field failed: "
                    + source.getClass().getName() + "#" + fieldName, e);
            }
        }
    }
}
