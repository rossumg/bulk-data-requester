package org.itech.fhircore.converter;

import java.net.URI;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.springframework.util.StringUtils;

@Converter(autoApply = true)
public class URIConvertor implements AttributeConverter<URI, String> {

	@Override
	public String convertToDatabaseColumn(URI attribute) {
		return (attribute == null) ? null : attribute.toString();
	}

	@Override
	public URI convertToEntityAttribute(String dbData) {
		return (StringUtils.hasLength(dbData) ? URI.create(dbData) : null);
	}

}
