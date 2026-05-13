package ca.vanzyl.concord.k8s.model;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A DNS (sub)domain validator.
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
@Pattern(regexp = ValidSubdomain.PATTERN)
@Constraint(validatedBy = {})
public @interface ValidSubdomain
{

    String PATTERN = "^[A-Za-z0-9](?:[A-Za-z0-9\\-]{0,61}[A-Za-z0-9])?$";

    String MESSAGE = "Must contain only lowercase alphanumeric characters and a minus (-). " +
            "Must start with an alphanumeric character or a digit. " +
            "Must be between 1 and 64 characters in length.";

    String message() default MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
