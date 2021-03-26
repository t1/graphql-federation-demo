package graphql.demo.review;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@GraphQLApi
public class Films {
    private static final Map<String, Film> FILMS = new HashMap<>();

    static {
        add(Film.builder().id("1").title("Batman").year("1989").build());
        add(Film.builder().id("2").title("Batman Returns").year("1992").build());
        add(Film.builder().id("3").title("Batman: The Animated Series").year("1992").build());
    }

    private static void add(Film film) { FILMS.put(film.id, film); }

    @Query
    public @NonNull List<Film> films() { return new ArrayList<>(FILMS.values()); }

    @Query
    public Film film(String id) { return FILMS.get(id); }
}
