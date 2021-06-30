package org.itech.fhircore.service.impl;

import org.itech.fhircore.service.CrudService;
import org.springframework.data.repository.CrudRepository;

public abstract class CrudServiceImpl<T, ID> implements CrudService<T, ID> {

	private CrudRepository<T, ID> repository;

	public CrudServiceImpl(CrudRepository<T, ID> repository) {
		this.repository = repository;
	}

	@Override
	public CrudRepository<T, ID> getDAO() {
		return repository;
	}

}
