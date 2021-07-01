package org.itech.fhirhose.etl.service;

import java.util.List;

import org.hl7.fhir.r4.model.Bundle;

public interface ETLService {

	void createPersistETLRecords(List<Bundle> searchBundles);

}