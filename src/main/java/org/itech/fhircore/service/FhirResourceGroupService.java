package org.itech.fhircore.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.ResourceType;
import org.itech.fhircore.model.CustomFhirResourceGroup;
import org.itech.fhircore.model.ResourceSearchParam;

public interface FhirResourceGroupService {

	public enum FhirResourceCategories {
		All,
		// base resources
		Base, Individuals, Entities_1, Entities_2, Workflow, Management, Results,
		// clinical resources
		Clinical, Summary, Diagnostics, Medications, Care_Provision, Request_Response
	}

	Map<FhirResourceCategories, Set<ResourceSearchParam>> getFhirCategoriesToResourceTypes();

	Map<String, Set<ResourceSearchParam>> getDefaultFhirGroupsToResourceTypes();

	Map<String, Set<ResourceSearchParam>> getCustomFhirGroupsToResourceTypes();

	Map<String, Set<ResourceSearchParam>> getAllFhirGroupsToResourceTypes();

	Map<String, Map<ResourceType, Set<ResourceSearchParam>>> getAllFhirGroupsToResourceTypesGrouped();

	CustomFhirResourceGroup createFhirResourceGroupWithNoSearchParams(String resourceGroupName,
			Set<ResourceType> resourceTypes);

	CustomFhirResourceGroup createFhirResourceGroup(String resourceGroupName,
			Map<ResourceType, Map<String, List<String>>> resourceTypes);

	CustomFhirResourceGroup createFhirResourceGroup(String resourceGroupName,
			Set<ResourceSearchParam> resourceTypes);

	Iterable<ResourceSearchParam> updateResourceSearchParameters(Long resourceGroupId, ResourceType resourceType,
			Map<String, List<String>> searchParams);

}
