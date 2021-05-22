package graphql.demo.review;

import com.github.t1.graphql.federation.api.Key;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;

@Getter @Setter @ToString @NoArgsConstructor
@Builder @AllArgsConstructor
public @Key(fields = "id") class Artist {
    @NonNull @Id String id;
    @NonNull String name;
}
