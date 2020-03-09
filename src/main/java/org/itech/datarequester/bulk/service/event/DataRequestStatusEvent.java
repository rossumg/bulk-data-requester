package org.itech.datarequester.bulk.service.event;

import org.itech.datarequester.bulk.model.DataRequestAttempt.DataRequestStatus;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class DataRequestStatusEvent extends ApplicationEvent {

	private static final long serialVersionUID = -9058748235265104418L;

	private DataRequestStatus dataRequestStatus;
	private Long dataRequestAttemptId;

	public DataRequestStatusEvent(Object source, Long dataRequestAttemptId, DataRequestStatus dataRequestStatus) {
		super(source);
		this.dataRequestAttemptId = dataRequestAttemptId;
		this.dataRequestStatus = dataRequestStatus;
	}

}
