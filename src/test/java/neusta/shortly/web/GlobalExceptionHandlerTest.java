package neusta.shortly.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("handleBodyValidationErrors")
    class BodyValidation {
        @Test
        @DisplayName("aggregiert Fehlermeldungen und setzt BAD_REQUEST")
        void aggregatesMessages() {
            var binding = new BeanPropertyBindingResult(new Object(), "shortLinkDto");
            binding.addError(new FieldError("shortLinkDto", "url", null, false, null, null, "URL darf nicht leer sein"));
            binding.addError(new FieldError("shortLinkDto", "expiresAt", null, false, null, null, "Das Verfallsdatum muss in der Zukunft liegen"));
            var ex = mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(binding);

            ProblemDetail pd = handler.handleBodyValidationErrors(ex);

            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getDetail()).contains("URL darf nicht leer sein");
            assertThat(pd.getDetail()).contains("Das Verfallsdatum muss in der Zukunft liegen");
        }
    }

    @Nested
    @DisplayName("handleConstraintViolations")
    class ConstraintViolations {
        @Test
        @DisplayName("aggregiert ConstraintViolation-Messages")
        void aggregatesConstraintViolationMessages() {
            @SuppressWarnings("unchecked")
            ConstraintViolation<Object> v1 = (ConstraintViolation<Object>) mock(ConstraintViolation.class);
            when(v1.getMessage()).thenReturn("Ungültiger ShortCode (falsche Länge)");
            ConstraintViolation<Object> v2 = (ConstraintViolation<Object>) mock(ConstraintViolation.class);
            when(v2.getMessage()).thenReturn("Noch eine Fehlermeldung");

            var ex = new ConstraintViolationException(Set.of(v1, v2));

            ProblemDetail pd = handler.handleConstraintViolations(ex);
            assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(pd.getDetail()).contains("Ungültiger ShortCode");
            assertThat(pd.getDetail()).contains("Noch eine Fehlermeldung");
        }
    }
}
