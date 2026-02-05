package neusta.shortly.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Configuration for custom application metrics and monitoring.
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfiguration {

    /**
     * Add common tags to all metrics for better filtering in Grafana/Prometheus.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                        "service", "shortly-backend",
                        "environment", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev")
                );
    }

    /**
     * Custom metrics for Redis connection pool monitoring.
     */
    @Bean
    public MeterBinder redisMetrics(final RedisConnectionFactory connectionFactory) {
        return registry -> {
            // Monitor Redis connection pool
            registry.gauge("redis.connections.active", connectionFactory,
                    factory -> {
                        try {
                            final var connection = factory.getConnection();
                            connection.close();
                            return 1;
                        } catch (final Exception e) {
                            return 0;
                        }
                    });
        };
    }
}
