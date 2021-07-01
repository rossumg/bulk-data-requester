package org.itech.fhirhose.datarequest.dao;

import java.util.Optional;

import org.itech.fhirhose.datarequest.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerDAO extends JpaRepository<Server, Long> {

	Optional<Server> findByName(String name);

}
