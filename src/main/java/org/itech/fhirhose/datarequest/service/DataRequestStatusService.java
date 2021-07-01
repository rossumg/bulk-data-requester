package org.itech.fhirhose.datarequest.service;

import org.itech.fhirhose.datarequest.model.DataRequestAttempt.DataRequestStatus;

public interface DataRequestStatusService {

	void changeDataRequestAttemptStatus(Long dataRequestAttemptId, DataRequestStatus requestStatus);

}
