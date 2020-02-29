package org.itech.datarequester.bulk.service;

import org.itech.datarequester.bulk.model.DataRequestAttempt.DataRequestStatus;

public interface DataRequestStatusService {

	void changeDataRequestAttemptStatus(Long dataRequestAttemptId, DataRequestStatus requestStatus);

}
