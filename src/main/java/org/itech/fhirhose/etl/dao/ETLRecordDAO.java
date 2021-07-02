package org.itech.fhirhose.etl.dao;

import org.itech.fhirhose.etl.model.ETLRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ETLRecordDAO extends JpaRepository<ETLRecord, Long> {

}
