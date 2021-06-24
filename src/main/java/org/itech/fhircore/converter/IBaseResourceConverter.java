package org.itech.fhircore.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;

@Converter
public class IBaseResourceConverter implements AttributeConverter<IBaseResource, String> {

	private FhirContext fhirContext;

	public IBaseResourceConverter(FhirContext fhirContext) {
		this.fhirContext = fhirContext;
	}

	@Override
	public String convertToDatabaseColumn(IBaseResource attribute) {
		return fhirContext.newJsonParser().encodeResourceToString(attribute);
	}

	@Override
	public IBaseResource convertToEntityAttribute(String dbData) {
		return fhirContext.newJsonParser().parseResource(dbData);
	}


}
