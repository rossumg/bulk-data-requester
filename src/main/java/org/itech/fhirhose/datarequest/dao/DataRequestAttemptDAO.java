package org.itech.fhirhose.datarequest.dao;

import java.util.List;

import org.itech.fhirhose.datarequest.model.DataRequestAttempt;
import org.itech.fhirhose.datarequest.model.DataRequestAttempt.DataRequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DataRequestAttemptDAO extends JpaRepository<DataRequestAttempt, Long> {

	@Query("SELECT dra FROM DataRequestAttempt dra WHERE dra.dataRequestTask.id = :dataRequestTaskId")
	List<DataRequestAttempt> findDataRequestAttemptsByDataRequestTask(
			@Param("dataRequestTaskId") Long dataRequestTaskId);

	@Query("SELECT dra FROM DataRequestAttempt dra ORDER BY dra.endTime DESC")
	List<DataRequestAttempt> findLatestDataRequestAttempts(Pageable limit);

	@Query("SELECT dra FROM DataRequestAttempt dra WHERE dra.dataRequestTask.id = :dataRequestTaskId ORDER BY dra.startTime DESC")
	List<DataRequestAttempt> findLatestDataRequestAttemptsByDataRequestTask(Pageable limit,
			@Param("dataRequestTaskId") Long dataRequestTaskId);

	@Query("SELECT dra FROM DataRequestAttempt dra WHERE dra.dataRequestTask.id = :dataRequestTaskId AND dra.dataRequestStatus = :dataRequestStatus ORDER BY dra.startTime DESC")
	List<DataRequestAttempt> findLatestDataRequestAttemptsByDataRequestTaskAndStatus(Pageable limit,
			@Param("dataRequestTaskId") Long dataRequestTaskId,
			@Param("dataRequestStatus") DataRequestStatus dataRequestStatus);
}
