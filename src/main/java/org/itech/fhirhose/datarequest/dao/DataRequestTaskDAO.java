package org.itech.fhirhose.datarequest.dao;

import java.util.List;

import org.itech.fhirhose.datarequest.model.DataRequestTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DataRequestTaskDAO extends JpaRepository<DataRequestTask, Long> {

	@Query("SELECT drt FROM DataRequestTask drt WHERE drt.remoteServer.id = :serverId")
	List<DataRequestTask> findDataRequestTasksFromServer(@Param("serverId") Long id);

}
