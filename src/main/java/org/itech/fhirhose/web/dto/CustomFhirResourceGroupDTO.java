package org.itech.fhirhose.web.dto;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hl7.fhir.r4.model.ResourceType;
import org.itech.fhirhose.validation.annotation.ValidName;

import lombok.Data;

@Data
public class CustomFhirResourceGroupDTO {

	@NotBlank
	@ValidName
	private String resourceGroupName;

	@NotNull
	private Map<ResourceType, Map<String, List<String>>> resourceTypesSearchParams;

}
