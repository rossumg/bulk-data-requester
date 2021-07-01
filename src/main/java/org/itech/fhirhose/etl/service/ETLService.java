package org.itech.fhirhose.etl.service;

import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.itech.fhirhose.etl.model.ETLRecord;

public interface ETLService {

	void createPersistETLRecords(List<Bundle> searchBundles);

	List<ETLRecord> getLatestFhirforETL(List<Observation> observations);
}