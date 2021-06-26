package org.itech.etl.service;

import java.util.List;

import org.itech.etl.dao.ETLRecordDAO;
import org.itech.etl.model.ETLRecord;
import org.itech.fhircore.service.impl.CrudServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class ETLRecordServiceImpl extends CrudServiceImpl<ETLRecord, Long>
implements ETLRecordService {

private ETLRecordDAO etlRecordRepository;

public ETLRecordServiceImpl(ETLRecordDAO etlRecordRepository) {
super(etlRecordRepository);
this.etlRecordRepository = etlRecordRepository;
}


    @Override
    public boolean saveAll(List<ETLRecord> etlRecords) {
        log.debug("ETLRecordServiceImpl:saveAll: " + etlRecords.size());
        List<ETLRecord> savedRecords = etlRecordRepository.saveAll(etlRecords);
        log.debug("ETLRecordServiceImpl:savedRecords: " + savedRecords.size());
        if (etlRecords.size() == savedRecords.size())
            return true;
        else
            return false;
    }
}

