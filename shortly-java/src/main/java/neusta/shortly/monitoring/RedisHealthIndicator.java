package neusta.shortly.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Custom Health Indicator for Redis connectivity and performance monitoring.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        final long startTime = System.currentTimeMillis();
        try (@SuppressWarnings("LocalCanBeFinal") RedisConnection connection = redisConnectionFactory.getConnection()) {
            // Ping Redis to check connectivity
            final String pong = connection.ping();
            final long responseTime = System.currentTimeMillis() - startTime;

            if ("PONG".equalsIgnoreCase(pong)) {
                // Get Redis info
                final var info = connection.serverCommands().info();
                final Long dbSize = connection.serverCommands().dbSize();

                return Health.up()
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("ping", pong)
                        .withDetail("version", info.getProperty("redis_version"))
                        .withDetail("uptime_in_seconds", info.getProperty("uptime_in_seconds"))
                        .withDetail("connected_clients", info.getProperty("connected_clients"))
                        .withDetail("used_memory_human", info.getProperty("used_memory_human"))
                        .withDetail("db_size", dbSize)
                        .build();
            } else {
                return Health.down()
                        .withDetail("reason", "Redis ping failed")
                        .build();
            }
        } catch (final Exception ex) {
            log.error("Redis health check failed", ex);
            return Health.down()
                    .withDetail("error", ex.getClass().getName())
                    .withDetail("message", ex.getMessage())
                    .build();
        }
    }
}
