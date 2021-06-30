package org.itech.fhircore.service;

import org.springframework.data.repository.CrudRepository;

public interface CrudService<T, ID> {

	CrudRepository<T, ID> getDAO();

}
