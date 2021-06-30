package org.itech.fhircore.validation.constraint;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.itech.fhircore.validation.annotation.ValidName;

public class NameValidator implements ConstraintValidator<ValidName, String> {

	private static final Map<Locale, String> LOCALE_WHITE_LISTS = new HashMap<>();

	private static final String ENGLISH_WHITE_LIST = "^[\\w\\s\\-\\'\\.\\,]*$";
	private static final String FRENCH_WHITE_LIST = "^[\\w\\s\\-\\'\\.\\,àâçéèêëîïôûùüÿñæœÀÂÇÉÈÊËÎÏÔÛÙÜŸÑÆŒ]*$";

	static {
		LOCALE_WHITE_LISTS.put(Locale.ENGLISH, ENGLISH_WHITE_LIST);
		LOCALE_WHITE_LISTS.put(Locale.FRENCH, FRENCH_WHITE_LIST);
	}

	@Override
	public void initialize(ValidName name) {
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		return Pattern.compile(LOCALE_WHITE_LISTS.get(Locale.FRENCH)).matcher(value).matches();
	}

}
