package neusta.shortly.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import neusta.shortly.service.UrlNormalizer;

import java.lang.annotation.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

@Documented
@Constraint(validatedBy = FlexibleUrl.UrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface FlexibleUrl {

    String message() default "{validation.url.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class UrlValidator implements ConstraintValidator<FlexibleUrl, String> {

        @Override
        public boolean isValid(final String url, final ConstraintValidatorContext context) {
            if (url == null || url.isBlank()) {
                return true; // @NotBlank
            }

            final var normalizedUrl = UrlNormalizer.normalize(url);
            try {
                final var ignored = new URI(normalizedUrl).toURL();
            } catch (final MalformedURLException | URISyntaxException | IllegalArgumentException e) {
                return false;
            }
            return true;
        }
    }
}
