package org.itech.fhircore.validation.constraint;

import java.util.Map;
import java.util.Map.Entry;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.itech.fhircore.validation.annotation.ValidIdMap;

public class IdMapValidator implements ConstraintValidator<ValidIdMap, Map<String, String>> {

	public enum IdType {
		Number, AlphaNum
	}

	private IdType keyIdType;
	private IdType valueIdType;

	@Override
	public void initialize(ValidIdMap validIdMap) {
		this.keyIdType = validIdMap.keyIdType();
		this.valueIdType = validIdMap.valueIdType();
	}

	@Override
	public boolean isValid(Map<String, String> value, ConstraintValidatorContext context) {
		for (Entry<String, String> mapEntry : value.entrySet()) {
			if (!validIdValue(keyIdType, mapEntry.getKey())) {
				return false;
			} else if (!validIdValue(valueIdType, mapEntry.getValue())) {
				return false;
			}
		}
		return true;
	}

	private boolean validIdValue(IdType idType, String value) {
		switch (idType) {
		case Number:
			return StringUtils.isNumeric(value);
		case AlphaNum:
			return StringUtils.isAlphanumeric(value);
		default:
			return false;
		}
	}

}
