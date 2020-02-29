package org.itech.datarequester.bulk.service.event;

import org.itech.datarequester.bulk.model.DataRequestAttempt.DataRequestStatus;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class DataRequestEvent extends ApplicationEvent {

	private static final long serialVersionUID = -9058748235265104418L;

	private DataRequestStatus dataRequestStatus;
	private Long dataRequestAttemptId;

	public DataRequestEvent(Object source, Long dataRequestAttemptId2, DataRequestStatus dataRequestStatus) {
		super(source);
		this.dataRequestAttemptId = dataRequestAttemptId2;
		this.dataRequestStatus = dataRequestStatus;
	}

}
