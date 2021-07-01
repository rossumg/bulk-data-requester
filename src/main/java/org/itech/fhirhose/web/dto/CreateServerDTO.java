package org.itech.fhirhose.web.dto;

import javax.validation.constraints.NotBlank;

import org.itech.fhirhose.validation.annotation.ValidName;

import lombok.Data;

@Data
public class CreateServerDTO {

	@NotBlank
	@ValidName
	private String name;

	@NotBlank
	private String serverAddress;

}
