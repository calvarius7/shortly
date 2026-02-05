package neusta.shortly.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisHealthIndicatorTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    @Mock
    private RedisServerCommands serverCommands;

    @InjectMocks
    private RedisHealthIndicator healthIndicator;

    @Test
    void shouldReturnUpWhenRedisIsHealthy() {
        // Given
        final Properties info = new Properties();
        info.setProperty("redis_version", "8.0.0");
        info.setProperty("uptime_in_seconds", "3600");
        info.setProperty("connected_clients", "5");
        info.setProperty("used_memory_human", "1.5M");

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(info);
        when(serverCommands.dbSize()).thenReturn(42L);

        // When
        final Health health = healthIndicator.health();

        // Then
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("ping", "PONG");
        assertThat(health.getDetails()).containsEntry("version", "8.0.0");
        assertThat(health.getDetails()).containsEntry("db_size", 42L);
        assertThat(health.getDetails()).containsKey("responseTime");

        verify(connection).close();
    }

    @Test
    void shouldReturnDownWhenRedisConnectionFails() {
        // Given
        when(connectionFactory.getConnection()).thenThrow(new RuntimeException("Connection refused"));

        // When
        final Health health = healthIndicator.health();

        // Then
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", RuntimeException.class.getName());
        assertThat(health.getDetails()).containsEntry("message", "Connection refused");
    }

    @Test
    void shouldReturnDownWhenPingFails() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("ERROR");


        // When
        final Health health = healthIndicator.health();

        // Then
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "Redis ping failed");
    }
}
