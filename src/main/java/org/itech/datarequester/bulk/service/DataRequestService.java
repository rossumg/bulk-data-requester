package org.itech.datarequester.bulk.service;

import java.util.List;

import org.hl7.fhir.r4.model.Observation;
import org.itech.etl.model.ETLRecord;

public interface DataRequestService {

	void runDataRequestTasksForServer(Long serverId);

	void runDataRequestTask(Long dataRequestId);
	
	 public List<ETLRecord> getLatestFhirforETL(List<Observation> observations);

}
