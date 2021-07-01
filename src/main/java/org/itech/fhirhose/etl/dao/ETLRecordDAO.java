package org.itech.fhirhose.etl.dao;

import java.util.List;

import org.itech.fhirhose.etl.model.ETLRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ETLRecordDAO extends JpaRepository<ETLRecord, Long> {
    
    @Query("SELECT etl FROM ETLRecord etl")
    List<ETLRecord> findAll();
    
    ETLRecordDAO findById(long id);

}
