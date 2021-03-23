package graphql.demo.review;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@GraphQLApi
public class Reviews {
    private static final Map<String, List<Review>> REVIEWS = new HashMap<>();

    static {
        addReview(Review.builder().id("1").score("7.5").comment("Great movie").comment("First and unique!").contentId("1").build());
        addReview(Review.builder().id("2").score("7.0").comment("Amazing!").comment("Love this one!").contentId("2").build());
        addReview(Review.builder().id("3").score("9.0").comment("Childhood memories!").comment("Really good!").contentId("3").build());
    }

    private static void addReview(Review review) {
        REVIEWS.computeIfAbsent(review.contentId, key -> new ArrayList<>()).add(review);
    }


    /** This is a federated resolver that resolves the reviews for the externally provided content objects */
    public @NonNull List<@NonNull Content> reviews(List<Content> contents) {
        return contents.stream().map(content -> content.withReviews(reviews(content.getId()))).collect(toList());
    }

    @Query public @NonNull List<@NonNull Review> reviews(@NonNull @Id String contentId) { return REVIEWS.get(contentId); }
}
