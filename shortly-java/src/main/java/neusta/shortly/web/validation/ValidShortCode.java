package neusta.shortly.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import neusta.shortly.service.ShortCodeGenerator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidShortCode.ShortCodeValidator.class)
public @interface ValidShortCode {

    String message() default "{validation.shortCode.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ShortCodeValidator implements ConstraintValidator<ValidShortCode, String> {
        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext context) {
            return value != null && value.length() == ShortCodeGenerator.LENGTH;
        }
    }
}
