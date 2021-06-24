package org.itech.fhircore.dao;

import org.itech.fhircore.model.CustomFhirResourceGroup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomFhirResourceGroupDAO extends CrudRepository<CustomFhirResourceGroup, Long> {

}
