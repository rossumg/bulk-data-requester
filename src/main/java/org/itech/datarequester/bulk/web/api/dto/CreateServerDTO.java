package org.itech.datarequester.bulk.web.api.dto;

import javax.validation.constraints.NotBlank;

import org.itech.fhircore.validation.annotation.ValidName;

import lombok.Data;

@Data
public class CreateServerDTO {

	@NotBlank
	@ValidName
	private String name;

	@NotBlank
	private String serverAddress;

}
