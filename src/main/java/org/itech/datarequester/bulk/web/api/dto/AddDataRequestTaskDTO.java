package org.itech.datarequester.bulk.web.api.dto;

import lombok.Data;

@Data
public class AddDataRequestTaskDTO {

	private String dataRequestType;

	private Integer interval;

}
