package neusta.shortly.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsConfigurationTest {

    private final MetricsConfiguration config = new MetricsConfiguration();
    @Mock
    private RedisConnectionFactory connectionFactory;
    @Mock
    private RedisConnection connection;

    @Test
    void shouldAddCommonTagsToRegistry() {
        // Given
        final MeterRegistry registry = new SimpleMeterRegistry();
        final MeterRegistryCustomizer<MeterRegistry> customizer = config.metricsCommonTags();

        // When
        customizer.customize(registry);
        registry.counter("test.metric").increment();

        // Then
        final var counter = registry.get("test.metric").counter();
        assertThat(counter.getId().getTag("service")).isEqualTo("shortly-backend");
        assertThat(counter.getId().getTag("environment")).isEqualTo("dev");
    }

    @Test
    void shouldRegisterRedisConnectionMetricWhenHealthy() {
        // Given
        final MeterRegistry registry = new SimpleMeterRegistry();
        when(connectionFactory.getConnection()).thenReturn(connection);
        final MeterBinder binder = config.redisMetrics(connectionFactory);

        // When
        binder.bindTo(registry);

        // Then
        final Double value = registry.get("redis.connections.active").gauge().value();
        assertThat(value).isEqualTo(1.0);
    }

    @Test
    void shouldReturnZeroWhenRedisConnectionFails() {
        // Given
        final MeterRegistry registry = new SimpleMeterRegistry();
        when(connectionFactory.getConnection()).thenThrow(new RuntimeException("Connection failed"));
        final MeterBinder binder = config.redisMetrics(connectionFactory);

        // When
        binder.bindTo(registry);

        // Then
        final Double value = registry.get("redis.connections.active").gauge().value();
        assertThat(value).isEqualTo(0.0);
    }
}
