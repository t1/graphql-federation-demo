package graphql.federation;

import graphql.TypeResolutionEnvironment;
import graphql.demo.review.Content;
import graphql.demo.review.Reviews;
import graphql.scalar.GraphqlStringCoercing;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchema.Builder;
import graphql.schema.GraphQLUnionType;
import lombok.extern.java.Log;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;

import static graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION;
import static graphql.introspection.Introspection.DirectiveLocation.INTERFACE;
import static graphql.introspection.Introspection.DirectiveLocation.OBJECT;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLTypeReference.typeRef;
import static java.util.stream.Collectors.toList;

/**
 * @see <a href="https://www.apollographql.com/docs/federation/federation-spec/">GraphQL Federation Spec</a>
 */
@Log
@GraphQLApi
public class Federation {
    private static final GraphQLScalarType _Any = GraphQLScalarType.newScalar().name("_Any")
        .coercing(new GraphqlStringCoercing()).build();
    private static final GraphQLScalarType _FieldSet = GraphQLScalarType.newScalar().name("_FieldSet")
        .coercing(new GraphqlStringCoercing()).build();

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
            .argument(GraphQLArgument.newArgument().name("fields").type(nonNull(_FieldSet)))
            .description("The @key directive is used to indicate a combination of fields that can be used to uniquely identify " +
                "and fetch an object or interface.")
            .build());
        // directive @provides(fields: _FieldSet!) on FIELD_DEFINITION
        builder.additionalDirective(GraphQLDirective.newDirective().name("provides").validLocations(FIELD_DEFINITION)
            .argument(GraphQLArgument.newArgument().name("fields").type(nonNull(_FieldSet)))
            .description("The @provides directive is used to annotate the expected returned fieldset from a field on a base type " +
                "that is guaranteed to be selectable by the gateway.")
            .build());
        // directive @requires(fields: _FieldSet!) on FIELD_DEFINITION
        builder.additionalDirective(GraphQLDirective.newDirective().name("requires").validLocations(FIELD_DEFINITION)
            .argument(GraphQLArgument.newArgument().name("fields").type(nonNull(_FieldSet)))
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
        // union _Entity = Content
        _Entity = GraphQLUnionType.newUnionType().name("_Entity")
            .possibleTypes(typeRef("Content")) // TODO A: collect and register @FederatedSource parameter types
            .description("This is a union of all types that use the @key directive, " +
                "including both types native to the schema and extended types.").build();

        builder.additionalType(_Entity);
    }

    private void addQueries() {
        // _entities(representations: [_Any!]!): [_Entity]!
        _entities = GraphQLFieldDefinition.newFieldDefinition().name("_entities")
            .argument(GraphQLArgument.newArgument().name("representations").type(nonNull(list(nonNull(_Any)))))
            .type(nonNull(list(_Entity))).build();

        query.field(_entities);
    }

    private void addCode() {
        codeRegistry.typeResolver(_Entity, this::resolveEntity);
        codeRegistry.dataFetcher(FieldCoordinates.coordinates(query.build(), _entities), this::fetchEntities);
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

    @Inject Reviews reviews;

    private GraphQLObjectType resolveEntity(TypeResolutionEnvironment environment) {
        var typeName = environment.getObject().getClass().getSimpleName(); // TODO B: type renames
        return environment.getSchema().getObjectType(typeName);
    }

    private Object fetchEntities(DataFetchingEnvironment environment) {
        @SuppressWarnings("unchecked")
        var representations = (List<String>) environment.getArgument("representations");
        var contentList = representations.stream()
            .map(Federation::toExternalType)
            .collect(toList());
        // TODO A: determine real resolver from request
        // TODO C: batch resolvers
        return contentList.stream()
            .map(content -> content.withReviews(reviews.reviews(content)))
            .collect(toList());
    }

    private static Content toExternalType(String any) {
        // TODO A: determine real type and fields from request
        assert any.startsWith("{__typename=Content, id=");
        assert any.endsWith("}");
        var id = any.substring(24, any.length() - 1);
        return new Content().withId(id);
    }

    // TODO A: register extended type dynamically
    @Query public Content dummyContent() { return null; }
}
