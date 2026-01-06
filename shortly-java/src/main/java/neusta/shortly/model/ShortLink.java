package neusta.shortly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Data
@Builder(builderMethodName = "of")
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("links")
public class ShortLink {

    @Id
    private String shortCode;
    private String originalUrl;
    private int clicks;
    @TimeToLive
    private Long ttl;

    public static String getRedisKey(final String shortCode) {
        return ShortLink.class.getAnnotation(RedisHash.class).value() + ":" + shortCode;
    }
}
