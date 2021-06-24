package org.itech.fhircore.dao;

import java.util.Optional;

import org.itech.fhircore.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerDAO extends JpaRepository<Server, Long> {

	Optional<Server> findByName(String name);

}
