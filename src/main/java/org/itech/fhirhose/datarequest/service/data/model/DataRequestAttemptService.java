package org.itech.fhirhose.datarequest.service.data.model;

import java.time.Instant;

import org.itech.fhirhose.common.service.CrudService;
import org.itech.fhirhose.datarequest.model.DataRequestAttempt;

public interface DataRequestAttemptService extends CrudService<DataRequestAttempt, Long> {

	Instant getLatestSuccessDate(DataRequestAttempt dataRequestAttempt);

}
