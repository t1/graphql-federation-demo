package graphql.demo.artist;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import java.util.HashMap;
import java.util.Map;

@GraphQLApi
public class Artists {
    private static final Map<String, Artist> ARTISTS = new HashMap<>();

    static {
        add(Artist.builder().id("1").name("Tim Burton").build());
        add(Artist.builder().id("2").name("Michael Keaton").build());
        add(Artist.builder().id("3").name("Jack Nicholson").build());
        add(Artist.builder().id("4").name("Kim Basinger").build());
    }

    private static void add(Artist artist) { ARTISTS.put(artist.getId(), artist); }

    @Query public @NonNull Artist getArtist(@NonNull @Id String id) {
        if (!ARTISTS.containsKey(id)) throw new ArtistNotFoundException(id);
        return ARTISTS.get(id);
    }

    private static class ArtistNotFoundException extends RuntimeException {
        public ArtistNotFoundException(String id) { super("artist [" + id + "] not found"); }
    }
}
