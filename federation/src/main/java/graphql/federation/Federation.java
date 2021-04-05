package graphql.federation;

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
import graphql.schema.GraphQLSchema.Builder;
import graphql.schema.GraphQLUnionType;
import io.smallrye.graphql.bootstrap.Config;
import io.smallrye.graphql.execution.SchemaPrinter;
import io.smallrye.graphql.schema.ScanningContext;
import io.smallrye.graphql.schema.model.Schema;
import lombok.extern.java.Log;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
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


    @Inject Schema schema;

    private GraphQLUnionType _Entity;
    private GraphQLFieldDefinition _entities;
    private String _service;
    private GraphQLObjectType.Builder query;
    private GraphQLCodeRegistry.Builder codeRegistry;

    private final Map<Class<?>, Function<Object, Object>> federatedResolvers = new LinkedHashMap<>();

    public GraphQLSchema.Builder beforeSchemaBuild(@Observes GraphQLSchema.Builder builder) {
        _service = _service(builder.build());

        // TODO C: make the query builder available from SmallRye
        this.query = GraphQLObjectType.newObject(builder.build().getQueryType());
        // TODO C: make the GraphQLCodeRegistry available from SmallRye
        this.codeRegistry = GraphQLCodeRegistry.newCodeRegistry(builder.build().getCodeRegistry());

        addScalars(builder);
        addUnions(builder);
        addQueries();
        addCode();
        addFederatedResolvers();

        builder.query(query);
        builder.codeRegistry(codeRegistry.build());
        return builder;
    }

    private String _service(GraphQLSchema schema) {
        // TODO A: derive real schema
        if (schema == null) // disabled
            return new SchemaPrinter(PRINTER_CONFIG).print(schema);
        try (var stream = getClass().getResourceAsStream("/_service.graphql")) {
            return new Scanner(stream).useDelimiter("\\Z").next();
        } catch (IOException e) {
            throw new RuntimeException("could not load _service.graphql", e);
        }
    }

    private void addScalars(Builder builder) {
        builder.additionalType(_Any);
        builder.additionalType(_FieldSet);
    }

    private void addUnions(Builder builder) {
        var index = ScanningContext.getIndex();
        var typesWithKey = index.getAnnotations(KEY).stream()
            .map(AnnotationInstance::target)
            .map(AnnotationTarget::asClass)
            .map(typeInfo -> toObjectType(typeInfo, builder))
            .toArray(GraphQLObjectType[]::new);
        // union _Entity = ...
        _Entity = GraphQLUnionType.newUnionType().name("_Entity")
            .possibleTypes(typesWithKey)
            .description("This is a union of all types that use the @key directive, " +
                "including both types native to the schema and extended types.").build();
        builder.additionalType(_Entity);
    }

    private GraphQLObjectType toObjectType(ClassInfo typeInfo, Builder builder) {
        var typeName = typeInfo.name().local();
        var objectType = builder.build().getObjectType(typeName);
        if (objectType == null) throw new IllegalStateException("no class registered in schema for " + typeName);
        return objectType;
    }

    private void addQueries() {
        // _entities(representations: [_Any!]!): [_Entity]!
        _entities = newFieldDefinition().name("_entities")
            .argument(newArgument().name("representations").type(nonNull(list(nonNull(_Any)))))
            .type(nonNull(list(_Entity))).build();

        query.field(_entities);
    }

    private void addCode() {
        codeRegistry.typeResolver(_Entity, this::resolveEntity);
        codeRegistry.dataFetcher(FieldCoordinates.coordinates(query.build(), _entities), this::fetchEntities);
    }


    public GraphQLObjectType resolveEntity(TypeResolutionEnvironment environment) {
        var typeName = environment.getObject().getClass().getSimpleName(); // TODO B: type renames
        return environment.getSchema().getObjectType(typeName);
    }

    public List<Object> fetchEntities(DataFetchingEnvironment environment) {
        @SuppressWarnings("unchecked")
        var representations = (List<Map<String, Object>>) environment.getArgument("representations");
        return representations.stream()
            .map(this::toRepresentationInstance)
            .map(this::resolve)
            .collect(toList());
    }

    private Object resolve(Object source) {
        return federatedResolvers.get(source.getClass()).apply(source);
    }

    /** Create a prefilled instance of the type going into the federated resolver */
    private Object toRepresentationInstance(Map<String, Object> any) {
        var typeName = (String) any.get("__typename");
        try {
            var type = schema.getTypes().get(typeName);
            if (type == null) throw new IllegalStateException("no class registered in schema for " + typeName);
            var cls = Class.forName(type.getClassName());
            Object instance = cls.getConstructor().newInstance();
            // TODO B: field renames
            for (String fieldName : type.getFields().keySet()) {
                if ("__typename".equals(fieldName)) continue;
                var value = (String) any.get(fieldName);
                var field = cls.getDeclaredField(fieldName);
                if (field.isAnnotationPresent(External.class)) {
                    field.setAccessible(true);
                    field.set(instance, value);
                }
            }
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("can't create extended type instance " + typeName, e);
        }
    }

    private void addFederatedResolvers() {
        ScanningContext.getIndex()
            .getAnnotations(FEDERATED_SOURCE).stream()
            .map(AnnotationInstance::target)
            .map(AnnotationTarget::asMethodParameter)
            .map(MethodParameterInfo::method)
            .map(Federation::toReflectionMethod)
            .forEach(method -> federatedResolvers.put(method.getParameterTypes()[0], new ResolverHandler(method)));
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

    public static class ResolverHandler implements Function<Object, Object> {
        private final Method method;

        private ResolverHandler(Method method) { this.method = method; }

        @Override public Object apply(Object source) {
            // TODO C: batch resolvers
            Object value = invoke(source);
            set(source, value);
            return source;
        }

        private Object invoke(Object source) {
            var reviews = CDI.current().select(method.getDeclaringClass()).get();
            try {
                return method.invoke(reviews, source);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("invocation of federated resolver method failed: " + method, e);
            }
        }

        private void set(Object source, Object value) {
            var fieldName = method.getName(); // TODO B: method renames
            try {
                var field = source.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(source, value);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("setting of federated resolver field failed: " + fieldName, e);
            }
        }
    }


    @Query public @NonNull _Service _service() { return new _Service(_service); }
}
