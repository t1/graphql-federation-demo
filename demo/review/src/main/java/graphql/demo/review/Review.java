package graphql.demo.review;

import io.smallrye.graphql.federation.api.Key;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;

import java.util.List;

@Getter @Setter @ToString @NoArgsConstructor
@Builder @AllArgsConstructor
public @Key(fields = "id") class Review {
    @NonNull @Id String id;
    @NonNull @Id String filmId;
    @NonNull String score;
    @Singular @NonNull List<@NonNull String> comments;
}
