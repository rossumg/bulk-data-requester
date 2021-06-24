package org.itech.fhircore.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.util.StringUtils;

@Converter(autoApply = true)
public class ResourceTypeConverter implements AttributeConverter<ResourceType, String> {

	@Override
	public String convertToDatabaseColumn(ResourceType attribute) {
		return (attribute == null) ? null : attribute.name();
	}

	@Override
	public ResourceType convertToEntityAttribute(String dbData) {
		return (StringUtils.hasLength(dbData) ? ResourceType.valueOf(dbData) : null);
	}

}
