package org.itech.datarequester.bulk.service.data.model;

import java.time.Instant;

import org.itech.datarequester.bulk.model.DataRequestAttempt;
import org.itech.fhircore.service.CrudService;

public interface DataRequestAttemptService extends CrudService<DataRequestAttempt, Long> {

	Instant getLatestSuccessDate(DataRequestAttempt dataRequestAttempt);

}
