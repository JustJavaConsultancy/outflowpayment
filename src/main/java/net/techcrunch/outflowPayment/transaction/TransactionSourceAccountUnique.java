package net.techcrunch.outflowPayment.transaction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import static java.lang.annotation.ElementType.*;


/**
 * Validate that the sourceAccount value isn't taken yet.
 */
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = TransactionSourceAccountUnique.TransactionSourceAccountUniqueValidator.class
)
public @interface TransactionSourceAccountUnique {

    String message() default "{Exists.transaction.sourceAccount}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class TransactionSourceAccountUniqueValidator implements ConstraintValidator<TransactionSourceAccountUnique, String> {

        private final TransactionService transactionService;
        private final HttpServletRequest request;

        public TransactionSourceAccountUniqueValidator(final TransactionService transactionService,
                final HttpServletRequest request) {
            this.transactionService = transactionService;
            this.request = request;
        }

        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext cvContext) {
            if (value == null) {
                // no value present
                return true;
            }
            @SuppressWarnings("unchecked") final Map<String, String> pathVariables =
                    ((Map<String, String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
            final String currentId = pathVariables.get("id");
            if (currentId != null && value.equalsIgnoreCase(transactionService.get(Long.parseLong(currentId)).getSourceAccount())) {
                // value hasn't changed
                return true;
            }
            return !transactionService.sourceAccountExists(value);
        }

    }

}
