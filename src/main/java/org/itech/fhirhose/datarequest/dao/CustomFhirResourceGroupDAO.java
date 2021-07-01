package org.itech.fhirhose.datarequest.dao;

import org.itech.fhirhose.datarequest.model.CustomFhirResourceGroup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomFhirResourceGroupDAO extends CrudRepository<CustomFhirResourceGroup, Long> {

}
