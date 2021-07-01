package org.itech.fhirhose.etl.service;

import java.util.List;

import org.itech.fhirhose.common.service.CrudService;
import org.itech.fhirhose.etl.model.ETLRecord;

public interface ETLRecordService extends CrudService<ETLRecord, Long> {

    boolean saveAll(List<ETLRecord> etlRecords);

}