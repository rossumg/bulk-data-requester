package org.itech.datarequester.bulk.web.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class AddDataRequestTaskDTO {

	@NotBlank
	private String dataRequestType;

	@NotNull
	private Integer interval;

}
