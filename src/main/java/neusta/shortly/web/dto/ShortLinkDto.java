package neusta.shortly.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record ShortLinkDto(
        @NotBlank(message = "{validation.url.notBlank}")
        @URL(message = "{validation.url.invalid}")
        String url,
        @Future(message = "{validation.expiresAt.future}")
        Instant expiresAt
) {
}
