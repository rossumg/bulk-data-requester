package org.itech.fhircore.dao;

import java.util.Set;

import org.hl7.fhir.r4.model.ResourceType;
import org.itech.fhircore.model.ResourceSearchParam;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceSearchParamDAO extends CrudRepository<ResourceSearchParam, Long> {

	@Query("SELECT rsp FROM ResourceSearchParam rsp WHERE rsp.customFhirResourceGroup.id = :resourceGroupId")
	Set<ResourceSearchParam> findAllForCustomFhirResourceGroup(@Param("resourceGroupId") Long resourceGroupId);

	@Query("SELECT rsp FROM ResourceSearchParam rsp WHERE rsp.resourceType = :resourceType AND rsp.customFhirResourceGroup.id = :resourceGroupId")
	Set<ResourceSearchParam> findAllOfResourceTypeForCustomFhirResourceGroup(
			@Param("resourceType") ResourceType resourceType,
			@Param("resourceGroupId") Long resourceGroupId);

}
