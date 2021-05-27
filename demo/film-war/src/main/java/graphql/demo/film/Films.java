package graphql.demo.film;

import io.smallrye.graphql.federation.api.FederatedSource;
import lombok.Value;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@GraphQLApi
public class Films {
    private static final Map<String, Film> FILMS = new HashMap<>();

    static {
        add(Film.builder().id("1").title("Batman").year("1989")
            .directorId("1")
            .cast("Bruce Wayne / Batman", "2")
            .cast("Jack Napier / the Joker", "3")
            .cast("Vicki Vale", "4")
            .build());
        add(Film.builder().id("2").title("Batman Returns").year("1992")
            .directorId("1")
            .cast("Bruce Wayne / Batman", "2")
            .build());
        add(Film.builder().id("3").title("Batman: The Animated Series").year("1992").build());
    }

    private static void add(Film film) { FILMS.put(film.id, film); }

    @Query
    public @NonNull List<@NonNull Film> films() { return new ArrayList<>(FILMS.values()); }

    @Query
    public @NonNull Film film(@NonNull String id) { return FILMS.get(id); }

    public @NonNull Artist director(@Source Film film) { return new Artist(film.directorId); }

    @SuppressWarnings("unused")
    public @NonNull List<@NonNull Film> films(@FederatedSource Artist artist) {
        return FILMS.values().stream()
            .filter(film -> film.cast.containsValue(artist.id) || Objects.equals(film.directorId, artist.id))
            .collect(toList());
    }

    public @NonNull List<@NonNull Cast> cast(@Source Film film) {
        return film.cast.entrySet().stream()
            .map(entry -> new Cast(entry.getKey(), new Artist(entry.getValue())))
            .collect(toList());
    }

    public static @Value class Cast {
        @NonNull String role;
        @NonNull Artist artist;
    }
}
