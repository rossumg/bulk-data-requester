package org.itech.fhircore.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.itech.fhircore.validation.constraint.IdMapValidator;
import org.itech.fhircore.validation.constraint.IdMapValidator.IdType;

@Documented
@Constraint(validatedBy = IdMapValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIdMap {

	String message() default "{org.itech.validation.constraints.ValidIdMap.message}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	/**
	 * @return the kind of id the key is
	 */
	IdType keyIdType() default IdType.Number;

	/**
	 * @return the kind of id the value is
	 */
	IdType valueIdType() default IdType.Number;

}
