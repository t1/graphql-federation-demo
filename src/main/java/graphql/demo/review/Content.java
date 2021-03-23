package graphql.demo.review;

import graphql.federation.Extends;
import graphql.federation.External;
import graphql.federation.Key;
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
public @Extends @Key(fields = "id") class Content {
    @External @NonNull @Id String id;
    @NonNull List<@NonNull Review> reviews;
}
