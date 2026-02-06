package neusta.shortly.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import neusta.shortly.web.validation.FlexibleUrl;

import java.time.Instant;

public record ShortLinkDto(
        @NotBlank(message = "{validation.url.notBlank}")
        @FlexibleUrl
        String url,
        @Future(message = "{validation.expiresAt.future}")
        Instant expiresAt
) {
}
