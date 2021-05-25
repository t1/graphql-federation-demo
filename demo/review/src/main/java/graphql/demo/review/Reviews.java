package graphql.demo.review;

import com.github.t1.graphql.federation.api.FederatedSource;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GraphQLApi
public class Reviews {
    private static final Map<String, List<Review>> REVIEWS = new HashMap<>();

    static {
        addReview(Review.builder().id("1").score("7.5").comment("Great movie").comment("First and unique!").filmId("1").build());
        addReview(Review.builder().id("2").score("7.0").comment("Amazing!").comment("Love this one!").filmId("2").build());
        addReview(Review.builder().id("3").score("9.0").comment("Childhood memories!").comment("Really good!").filmId("2").build());
    }

    private static void addReview(Review review) { reviewsFor(review.filmId).add(review); }

    private static List<Review> reviewsFor(@Id @NonNull String filmId) { return REVIEWS.computeIfAbsent(filmId, key -> new ArrayList<>()); }

    @SuppressWarnings("unused")
    public @NonNull List<@NonNull Review> reviews(@FederatedSource Film film) { return getReviews(film.getId()); }

    @Query public @NonNull List<@NonNull Review> getReviews(@NonNull @Id String filmId) { return reviewsFor(filmId); }
}
