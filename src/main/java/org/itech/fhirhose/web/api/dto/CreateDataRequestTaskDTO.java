package org.itech.fhirhose.web.api.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CreateDataRequestTaskDTO {

	@NotBlank
	private String dataRequestType;

	private Integer interval;

}
