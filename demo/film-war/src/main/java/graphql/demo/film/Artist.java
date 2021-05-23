package graphql.demo.film;

import com.github.t1.graphql.federation.api.Extends;
import com.github.t1.graphql.federation.api.External;
import com.github.t1.graphql.federation.api.Key;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;

import java.util.List;

@Getter @Setter @With @ToString @NoArgsConstructor @AllArgsConstructor
public @Extends @Key(fields = "id") class Artist {
    @External @NonNull @Id String id;

    // TODO B: add this field from the return type of the federated resolver method
    @NonNull List<@NonNull Film> films;

    public Artist(String id) { this.id = id; }
}
