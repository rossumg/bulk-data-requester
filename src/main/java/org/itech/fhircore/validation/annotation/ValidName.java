package org.itech.fhircore.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.itech.fhircore.validation.constraint.NameValidator;

@Documented
@Constraint(validatedBy = NameValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidName {

	String message() default "{org.itech.validation.constraints.ValidName.message}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
