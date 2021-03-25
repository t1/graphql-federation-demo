package graphql.federation;

import graphql.TypeResolutionEnvironment;
import graphql.demo.review.Content;
import graphql.demo.review.Reviews;
import graphql.scalar.GraphqlStringCoercing;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchema.Builder;
import graphql.schema.GraphQLUnionType;
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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION;
import static graphql.introspection.Introspection.DirectiveLocation.INTERFACE;
import static graphql.introspection.Introspection.DirectiveLocation.OBJECT;
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
public class Federation {
    private static final DotName KEY = DotName.createSimple(Key.class.getName());

    private static final GraphQLScalarType _FieldSet = GraphQLScalarType.newScalar().name("_FieldSet")
        .coercing(new GraphqlStringCoercing()).build();

    private static final GraphQLScalarType _Any = GraphQLScalarType.newScalar().name("_Any")
        .coercing(new AnyCoercing()).build();

    private static class AnyCoercing implements Coercing<Object, Object> {
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
    private GraphQLObjectType.Builder query;
    private GraphQLCodeRegistry.Builder codeRegistry;

    public GraphQLSchema.Builder beforeSchemaBuild(@Observes GraphQLSchema.Builder builder) {
        // TODO C: make the query builder available from SmallRye
        this.query = GraphQLObjectType.newObject(builder.build().getQueryType());
        // TODO C: make the GraphQLCodeRegistry available from SmallRye
        this.codeRegistry = GraphQLCodeRegistry.newCodeRegistry(builder.build().getCodeRegistry());

        addScalars(builder);
        addDirectives(builder);
        addUnions(builder);
        addQueries();
        addCode();

        builder.query(query);
        builder.codeRegistry(codeRegistry.build());
        return builder;
    }

    private void addScalars(Builder builder) {
        builder.additionalType(_Any);
        builder.additionalType(_FieldSet);
    }

    private void addDirectives(GraphQLSchema.Builder builder) {
        // TODO C: custom directives
        // directive @external on FIELD_DEFINITION
        builder.additionalDirective(GraphQLDirective.newDirective().name("external").validLocation(FIELD_DEFINITION)
            .description("The @external directive is used to mark a field as owned by another service. " +
                "This allows service A to use fields from service B while also knowing at runtime the types of that field.")
            .build());
        // directive @key(fields: _FieldSet!) on OBJECT | INTERFACE
        builder.additionalDirective(GraphQLDirective.newDirective().name("key").validLocations(OBJECT, INTERFACE)
            .argument(newArgument().name("fields").type(nonNull(_FieldSet)))
            .description("The @key directive is used to indicate a combination of fields that can be used to uniquely identify " +
                "and fetch an object or interface.")
            .build());
        // directive @provides(fields: _FieldSet!) on FIELD_DEFINITION
        builder.additionalDirective(GraphQLDirective.newDirective().name("provides").validLocations(FIELD_DEFINITION)
            .argument(newArgument().name("fields").type(nonNull(_FieldSet)))
            .description("The @provides directive is used to annotate the expected returned fieldset from a field on a base type " +
                "that is guaranteed to be selectable by the gateway.")
            .build());
        // directive @requires(fields: _FieldSet!) on FIELD_DEFINITION
        builder.additionalDirective(GraphQLDirective.newDirective().name("requires").validLocations(FIELD_DEFINITION)
            .argument(newArgument().name("fields").type(nonNull(_FieldSet)))
            .description("The @requires directive is used to annotate the required input fieldset from a base type for a resolver. " +
                "It is used to develop a query plan where the required fields may not be needed by the client, " +
                "but the service may need additional information from other services.")
            .build());
        // directive @extends on OBJECT | INTERFACE
        builder.additionalDirective(GraphQLDirective.newDirective().name("extends").validLocations(OBJECT, INTERFACE)
            .description("Some libraries such as graphql-java don't have native support for type extensions in their printer. " +
                "Apollo Federation supports using an @extends directive in place of extend type to annotate type references.")
            .build());
    }

    private void addUnions(Builder builder) {
        var index = ScanningContext.getIndex();
        var typesWithKey = index.getAnnotations(KEY).stream()
            .map(AnnotationInstance::target)
            .map(AnnotationTarget::asClass)
            .map(typeInfo -> toObjectType(typeInfo, builder))
            .toArray(GraphQLObjectType[]::new);
        // union _Entity = Content
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


    private GraphQLObjectType resolveEntity(TypeResolutionEnvironment environment) {
        var typeName = environment.getObject().getClass().getSimpleName(); // TODO B: type renames
        return environment.getSchema().getObjectType(typeName);
    }

    private List<Object> fetchEntities(DataFetchingEnvironment environment) {
        @SuppressWarnings("unchecked")
        var representations = (List<Map<String, Object>>) environment.getArgument("representations");
        var contentList = representations.stream()
            .map(this::toRepresentationInstance)
            .collect(toList());
        // TODO C: batch resolvers
        return contentList.stream()
            .map(this::resolve)
            .collect(toList());
    }

    private Content resolve(Object target) {
        var content = (Content) target;
        // TODO A: determine real resolver from request
        var reviews = CDI.current().select(Reviews.class).get();
        return content.withReviews(reviews.reviews(content));
    }

    /** Create a prefilled instance of the type going into the federated resolver */
    private Object toRepresentationInstance(Map<String, Object> any) {
        var typeName = (String) any.get("__typename");
        try {
            var type = schema.getTypes().get(typeName);
            if (type == null) throw new IllegalStateException("no class registered in schema for " + typeName);
            var cls = Class.forName(type.getClassName());
            Object instance = cls.getConstructor().newInstance();
            // TODO B: be smarter about renames, etc.
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

    @Query public @NonNull _Service _service() {
        // TODO A: derive real schema
        return new _Service("\n" +
            "\"Query root\"\n" +
            "type Query {\n" +
            "  ratings(contentId: String!): [Review!]!\n" +
            "}\n" +
            "\n" +
            "type Review @key(fields: \"id\") {\n" +
            "    id: ID!\n" +
            "    contentId: ID!\n" +
            "    score: String!\n" +
            "    comments: [String!]!\n" +
            "}\n" +
            "\n" +
            "type Content @key(fields: \"id\") @extends {\n" +
            "    id: ID! @external\n" +
            "    reviews: [Review!]!\n" +
            "}\n");
    }

    // TODO A: register extended type dynamically
    @Query public Content dummyContent() { return null; }
}
