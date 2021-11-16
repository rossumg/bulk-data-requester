package org.itech.fhirhose.datarequest.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ResourceType;
import org.itech.fhirhose.datarequest.dao.CustomFhirResourceGroupDAO;
import org.itech.fhirhose.datarequest.dao.ResourceSearchParamDAO;
import org.itech.fhirhose.datarequest.model.CustomFhirResourceGroup;
import org.itech.fhirhose.datarequest.model.ResourceSearchParam;
import org.itech.fhirhose.datarequest.service.FhirResourceGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FhirResourceGroupServiceImpl implements FhirResourceGroupService {

	private final Map<FhirResourceCategories, Set<ResourceSearchParam>> fhirCategoriesToResourceTypes;
	private final Map<String, Set<ResourceSearchParam>> defaultFhirGroupsToResourceTypes;

	private CustomFhirResourceGroupDAO customFhirResourceGroupDAO;
	private ResourceSearchParamDAO resourceSearchParamsDAO;

	public FhirResourceGroupServiceImpl(CustomFhirResourceGroupDAO customFhirResourceGroupDAO,
			ResourceSearchParamDAO resourceSearchParamsDAO) {
		this.customFhirResourceGroupDAO = customFhirResourceGroupDAO;
		this.resourceSearchParamsDAO = resourceSearchParamsDAO;

		fhirCategoriesToResourceTypes = new HashMap<>();
		
		Set<ResourceSearchParam> resultEntries = new HashSet<>();
		resultEntries.add(new ResourceSearchParam(ResourceType.Observation));
		resultEntries.add(new ResourceSearchParam(ResourceType.ServiceRequest));
//		resultEntries.add(new ResourceSearchParam(ResourceType.Patient));
//        resultEntries.add(new ResourceSearchParam(ResourceType.Specimen));
//        resultEntries.add(new ResourceSearchParam(ResourceType.Practitioner));
        fhirCategoriesToResourceTypes.put(FhirResourceCategories.Results, resultEntries);

//		Set<ResourceSearchParam> entity1Entries = new HashSet<>();
//		entity1Entries.add(new ResourceSearchParam(ResourceType.Organization));
//		entity1Entries.add(new ResourceSearchParam(ResourceType.OrganizationAffiliation));
//		entity1Entries.add(new ResourceSearchParam(ResourceType.HealthcareService));
//		entity1Entries.add(new ResourceSearchParam(ResourceType.Endpoint));
//		entity1Entries.add(new ResourceSearchParam(ResourceType.Location));
//		fhirCategoriesToResourceTypes.put(FhirResourceCategories.Entities_1, entity1Entries);
//
//		Set<ResourceSearchParam> workflowEntries = new HashSet<>();
//		workflowEntries.add(new ResourceSearchParam(ResourceType.Task));
//		workflowEntries.add(new ResourceSearchParam(ResourceType.Appointment));
//		workflowEntries.add(new ResourceSearchParam(ResourceType.AppointmentResponse));
//		workflowEntries.add(new ResourceSearchParam(ResourceType.Schedule));
//		workflowEntries.add(new ResourceSearchParam(ResourceType.VerificationResult));
//		fhirCategoriesToResourceTypes.put(FhirResourceCategories.Workflow, workflowEntries);

//		Set<ResourceSearchParam> baseEntries = new HashSet<>();
//		baseEntries.addAll(entity1Entries);
//		baseEntries.addAll(workflowEntries);
//		fhirCategoriesToResourceTypes.put(FhirResourceCategories.Base, baseEntries);

		// put all fhirCategoriesToResourceTypes into the all type
		Set<ResourceSearchParam> allEntries = new HashSet<>();
		for (Set<ResourceSearchParam> resourceTypeList : fhirCategoriesToResourceTypes.values()) {
			allEntries.addAll(resourceTypeList);
		}
		fhirCategoriesToResourceTypes.put(FhirResourceCategories.All, allEntries);

		// get map that uses strings instead of FhirResourceCategories
		defaultFhirGroupsToResourceTypes = fhirCategoriesToResourceTypes.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().name(), e -> e.getValue()));

	}

	@Override
	public Map<FhirResourceCategories, Set<ResourceSearchParam>> getFhirCategoriesToResourceTypes() {
		return fhirCategoriesToResourceTypes.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> Set.copyOf(e.getValue())));
	}

	@Override
	public Map<String, Set<ResourceSearchParam>> getDefaultFhirGroupsToResourceTypes() {
		return defaultFhirGroupsToResourceTypes.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> Set.copyOf(e.getValue())));
	}

	@Override
	public Map<String, Set<ResourceSearchParam>> getCustomFhirGroupsToResourceTypes() {
		Map<String, Set<ResourceSearchParam>> customFhirGroupsToResourceTypes = new HashMap<>();
		for (CustomFhirResourceGroup customFhirResourceGroup : customFhirResourceGroupDAO.findAll()) {
			customFhirGroupsToResourceTypes.put(customFhirResourceGroup.getResourceGroupName(),
					resourceSearchParamsDAO.findAllForCustomFhirResourceGroup(customFhirResourceGroup.getId()));
		}
		return customFhirGroupsToResourceTypes.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> Set.copyOf(e.getValue())));
	}

	@Override
	public Map<String, Set<ResourceSearchParam>> getAllFhirGroupsToResourceTypes() {
		Map<String, Set<ResourceSearchParam>> allFhirGroupsToResourceTypes = new HashMap<>();
		allFhirGroupsToResourceTypes.putAll(getDefaultFhirGroupsToResourceTypes());
		allFhirGroupsToResourceTypes.putAll(getCustomFhirGroupsToResourceTypes());
		return allFhirGroupsToResourceTypes;
	}

	@Override
	public Map<String, Map<ResourceType, Set<ResourceSearchParam>>> getAllFhirGroupsToResourceTypesGrouped() {
		Map<String, Map<ResourceType, Set<ResourceSearchParam>>> allFhirGroupsToResourceTypesGrouped = new HashMap<>();
		log.debug("getAllFhirGroupsToResourceTypesGrouped");
		allFhirGroupsToResourceTypesGrouped.putAll(groupResourceSearchParams(getDefaultFhirGroupsToResourceTypes()));
		allFhirGroupsToResourceTypesGrouped.putAll(groupResourceSearchParams(getCustomFhirGroupsToResourceTypes()));
		return allFhirGroupsToResourceTypesGrouped;
	}

	private Map<String, Map<ResourceType, Set<ResourceSearchParam>>> groupResourceSearchParams(
			Map<String, Set<ResourceSearchParam>> fhirGroupsToResourceTypes) {
		Map<String, Map<ResourceType, Set<ResourceSearchParam>>> fhirGroupsToResourceTypesGrouped = new HashMap<>();
		// for each FhirResourceGroup entry
		for (Entry<String, Set<ResourceSearchParam>> entrySet : fhirGroupsToResourceTypes.entrySet()) {
			// create a map of ResourceType to ResourceSearchParams
			Map<ResourceType, Set<ResourceSearchParam>> groupedResourceTypes = new HashMap<>();
			// for each ResourceSearchParams
			for (ResourceSearchParam resourceSearchParams : entrySet.getValue()) {
				// find the current set of ResourceSearchParams
				Set<ResourceSearchParam> groupedSearchParams = groupedResourceTypes
						.get(resourceSearchParams.getResourceType());
				// if the set is null, instantiate it and store it in the map of resourceType to
				// ResourceSearchParams
				if (groupedSearchParams == null) {
					groupedSearchParams = new HashSet<>();
					groupedResourceTypes.put(resourceSearchParams.getResourceType(), groupedSearchParams);
				}
				// add the current ResourceSearchParams to the set of ResourceSearchParams for
				// this ResourceType
				groupedSearchParams.add(resourceSearchParams);
			}
			// add the Set of ResourceSearchParams grouped by ResourceType for the current
			// FhirResourceGroup
			fhirGroupsToResourceTypesGrouped.put(entrySet.getKey(), groupedResourceTypes);
		}
		// return all
		return fhirGroupsToResourceTypesGrouped;
	}

	@Override
	public CustomFhirResourceGroup createFhirResourceGroupWithNoSearchParams(String resourceGroupName,
			Set<ResourceType> resourceTypes) {
		CustomFhirResourceGroup newResourceGroup = new CustomFhirResourceGroup(resourceGroupName);
		newResourceGroup = customFhirResourceGroupDAO.save(newResourceGroup);
		for (ResourceType resourceType : resourceTypes) {
			resourceSearchParamsDAO.save(new ResourceSearchParam(newResourceGroup, resourceType));
		}
		return newResourceGroup;
	}

	@Override
	public CustomFhirResourceGroup createFhirResourceGroup(String resourceGroupName,
			Map<ResourceType, Map<String, List<String>>> resourceTypesSearchParams) {
		CustomFhirResourceGroup newResourceGroup = new CustomFhirResourceGroup(resourceGroupName);
		newResourceGroup = customFhirResourceGroupDAO.save(newResourceGroup);
		for (Entry<ResourceType, Map<String, List<String>>> resourceTypeSearchParams : resourceTypesSearchParams
				.entrySet()) {
			for (Entry<String, List<String>> searchParam : resourceTypeSearchParams.getValue().entrySet()) {
				resourceSearchParamsDAO.save(
						new ResourceSearchParam(newResourceGroup, resourceTypeSearchParams.getKey(),
								searchParam.getKey(), searchParam.getValue()));
			}
		}
		return newResourceGroup;
	}

	@Override
	public CustomFhirResourceGroup createFhirResourceGroup(String resourceGroupName,
			Set<ResourceSearchParam> resourceTypes) {
		CustomFhirResourceGroup newResourceGroup = new CustomFhirResourceGroup(resourceGroupName);
		newResourceGroup = customFhirResourceGroupDAO.save(newResourceGroup);
		for (ResourceSearchParam resourceType : resourceTypes) {
			resourceType.setCustomFhirResourceGroup(newResourceGroup);
		}
		resourceSearchParamsDAO.saveAll(resourceTypes);
		return newResourceGroup;
	}

	@Override
	@Transactional
	public Iterable<ResourceSearchParam> updateResourceSearchParameters(Long resourceGroupId,
			ResourceType resourceType,
			Map<String, List<String>> searchParams) {
		CustomFhirResourceGroup customFhirResourceGroup = customFhirResourceGroupDAO.findById(resourceGroupId).get();
		Set<ResourceSearchParam> oldSearchParams = resourceSearchParamsDAO
				.findAllOfResourceTypeForCustomFhirResourceGroup(resourceType, resourceGroupId);
		resourceSearchParamsDAO.deleteAll(oldSearchParams);
		Set<ResourceSearchParam> resourceSearchParams = new HashSet<>();
		for (Entry<String, List<String>> searchParamNameToValues : searchParams.entrySet()) {
			ResourceSearchParam newResourceSearchParams = new ResourceSearchParam(customFhirResourceGroup,
					resourceType);
			newResourceSearchParams.setParamName(searchParamNameToValues.getKey());
			newResourceSearchParams.setParamValues(searchParamNameToValues.getValue());
			resourceSearchParams.add(newResourceSearchParams);
		}
		return resourceSearchParamsDAO.saveAll(resourceSearchParams);

	}

}
