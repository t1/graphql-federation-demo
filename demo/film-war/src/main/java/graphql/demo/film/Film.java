package graphql.demo.film;

import com.github.t1.graphql.federation.api.Key;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Ignore;
import org.eclipse.microprofile.graphql.NonNull;

import java.util.Map;

@Getter @Builder @ToString @AllArgsConstructor
public @Key(fields = "id") class Film {
    final @NonNull @Id String id;
    final @NonNull String title;
    final @NonNull String year;
    final @NonNull String directorId;
    @Ignore final @Singular("cast") Map<String, String> cast;
}
