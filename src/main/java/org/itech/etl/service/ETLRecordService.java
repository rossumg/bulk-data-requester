package org.itech.etl.service;

import java.util.List;

import org.itech.etl.model.ETLRecord;
import org.itech.fhircore.service.CrudService;

public interface ETLRecordService extends CrudService<ETLRecord, Long> {
    
    boolean saveAll(List<ETLRecord> etlRecords);
}