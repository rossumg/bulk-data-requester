package org.itech.fhirhose.datarequest.service.queue;

import java.util.HashMap;
import java.util.Map;

import org.itech.fhirhose.datarequest.model.DataRequestAttempt;
import org.springframework.stereotype.Component;

@Component
public class DataRequestAttemptWaitQueue {

	private final Map<Long, DataRequestAttempt> dataRequestAttempts = new HashMap<>();

	public void addDataRequestAttempt(DataRequestAttempt dataRequestAttempt) {
		dataRequestAttempts.put(dataRequestAttempt.getId(), dataRequestAttempt);
	}

	public void removeDataRequestAttempt(DataRequestAttempt dataRequestAttempt) {
		dataRequestAttempts.remove(dataRequestAttempt.getId());
	}

	public DataRequestAttempt removeDataRequestAttempt(Long dataRequestAttemptId) {
		return dataRequestAttempts.remove(dataRequestAttemptId);
	}

	public boolean contains(Long dataRequestAttemptId) {
		return dataRequestAttempts.containsKey(dataRequestAttemptId);
	}

	public DataRequestAttempt getDataRequestAttempt(Long dataRequestAttemptId) {
		return dataRequestAttempts.get(dataRequestAttemptId);
	}

}
