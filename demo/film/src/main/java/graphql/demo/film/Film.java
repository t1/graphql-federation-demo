package graphql.demo.film;

import com.github.t1.graphql.federation.api.Key;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;

@Getter @Setter @Builder @With @ToString @NoArgsConstructor @AllArgsConstructor
public @Key(fields = "id") class Film {
    @NonNull @Id String id;
    String title;
    String year;
}
