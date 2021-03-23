package graphql.federation;

import graphql.demo.review.Content;
import graphql.demo.review.Reviews;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import javax.inject.Inject;
import java.util.List;

import static java.util.stream.Collectors.toList;

@GraphQLApi
public class Federation {
    @Query public @NonNull _Service _service() {
        // TODO derive real schema
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

    @Query public @NonNull List<Content /*TODO _Entity*/> _entities(@NonNull List<@NonNull _Any /*TODO the actual input must be downcast*/> representations) {
        // TODO determine real query from request
        var contentList = representations.stream()
            .map(any -> new Content().withId(any.getId()))
            .collect(toList());
        return reviews.reviews(contentList);
    }
}
