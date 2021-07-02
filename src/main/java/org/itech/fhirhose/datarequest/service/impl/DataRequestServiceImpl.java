package org.itech.fhirhose.datarequest.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.ResourceType;
import org.itech.fhirhose.datarequest.dao.DataRequestTaskDAO;
import org.itech.fhirhose.datarequest.dao.ServerResourceIdMapDAO;
import org.itech.fhirhose.datarequest.model.DataRequestAttempt;
import org.itech.fhirhose.datarequest.model.DataRequestAttempt.DataRequestStatus;
import org.itech.fhirhose.datarequest.model.DataRequestTask;
import org.itech.fhirhose.datarequest.model.ResourceSearchParam;
import org.itech.fhirhose.datarequest.model.Server;
import org.itech.fhirhose.datarequest.model.ServerResourceIdMap;
import org.itech.fhirhose.datarequest.service.DataRequestService;
import org.itech.fhirhose.datarequest.service.DataRequestStatusService;
import org.itech.fhirhose.datarequest.service.FhirResourceGroupService;
import org.itech.fhirhose.datarequest.service.ServerService;
import org.itech.fhirhose.datarequest.service.data.model.DataRequestAttemptService;
import org.itech.fhirhose.datarequest.service.data.model.DataRequestTaskService;
import org.itech.fhirhose.datarequest.service.queue.ActiveDataRequestTaskHolder;
import org.itech.fhirhose.etl.service.ETLService;
import org.itech.fhirhose.fhir.FhirUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateRangeParam;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataRequestServiceImpl implements DataRequestService {

	private FhirUtil fhirUtil;

	private ServerService serverService;
	private DataRequestTaskService serverDataRequestTaskService;
	private DataRequestTaskDAO dataRequestTaskDAO;
	private DataRequestAttemptService dataRequestAttemptService;
	private DataRequestStatusService dataRequestStatusService;
	private FhirResourceGroupService fhirResources;
	private ServerResourceIdMapDAO remoteIdToLocalIdDAO;
	private ETLService etlService;
	private ActiveDataRequestTaskHolder activeDataRequestTaskHolder;

	public DataRequestServiceImpl(FhirUtil fhirUtil, ServerService serverService,
			DataRequestTaskService serverDataRequestTaskService, DataRequestTaskDAO dataRequestTaskDAO,
			DataRequestAttemptService dataRequestAttemptService, DataRequestStatusService dataRequestStatusService,
			FhirResourceGroupService fhirResources, ServerResourceIdMapDAO remoteIdToLocalIdDAO, ETLService etlService,
			ActiveDataRequestTaskHolder activeDataRequestTaskHolder) {
		this.fhirUtil = fhirUtil;

		this.serverService = serverService;
		this.serverDataRequestTaskService = serverDataRequestTaskService;
		this.dataRequestTaskDAO = dataRequestTaskDAO;
		this.dataRequestAttemptService = dataRequestAttemptService;
		this.dataRequestStatusService = dataRequestStatusService;
		this.fhirResources = fhirResources;
		this.remoteIdToLocalIdDAO = remoteIdToLocalIdDAO;
		this.etlService = etlService;
		this.activeDataRequestTaskHolder = activeDataRequestTaskHolder;
	}

	@Override
	@Async
	@Transactional
	public synchronized void runDataRequestTasksForServer(Long serverId) {
		Server server = serverService.getDAO().findById(serverId).get();
		for (DataRequestTask dataRequestTask : serverDataRequestTaskService.getDAO()
				.findDataRequestTasksFromServer(server.getId())) {
			runDataRequestTask(dataRequestTask);
			log.debug("finished sending request for dataRequestTask " + dataRequestTask.getId() + " for server "
					+ serverId);
		}
		log.debug("finished sending requests for dataRequestTasks for server " + serverId);
	}

	@Override
	@Async
	@Transactional
	public synchronized void runDataRequestTask(Long dataRequestTaskId) {
		DataRequestTask dataRequestTask = dataRequestTaskDAO.findById(dataRequestTaskId).get();
		runDataRequestTask(dataRequestTask);
		log.debug("finished sending request for dataRequestTask " + dataRequestTask.getId());
	}

	private void runDataRequestTask(DataRequestTask dataRequestTask) {
		log.debug("running dataRequest task " + dataRequestTask.getId());
		synchronized (activeDataRequestTaskHolder) {
			if (activeDataRequestTaskHolder.contains(dataRequestTask.getId())) {
				log.debug("task " + dataRequestTask.getId()
						+ " already running. Aborting runDataRequestTask until it completes.");
				return;
			} else {
				activeDataRequestTaskHolder.addDataRequestTask(dataRequestTask);
			}
		}

		DataRequestAttempt dataRequestAttempt = new DataRequestAttempt(dataRequestTask);
		dataRequestAttempt = dataRequestAttemptService.getDAO().save(dataRequestAttempt);
		for (int i = 0; i < dataRequestAttempt.getDataRequestTask().getDataRequestAttemptRetries(); ++i) {
			try {
				runDataRequestAttempt(dataRequestAttempt);
				dataRequestStatusService.changeDataRequestAttemptStatus(dataRequestAttempt.getId(),
						DataRequestStatus.COMPLETE);
				return;
			} catch (RuntimeException e) {
				log.warn("exception occured while running dataRequest task", e);
			}
		}
		log.error("could not complete a dataRequest task");
		dataRequestStatusService.changeDataRequestAttemptStatus(dataRequestAttempt.getId(), DataRequestStatus.FAILED);

	}

	private void runDataRequestAttempt(DataRequestAttempt dataRequestAttempt) {

		List<Bundle> searchBundles = getResourceBundlesFromRemoteServer(dataRequestAttempt);
		etlService.createPersistETLRecords(searchBundles);
	}

	private List<Bundle> getResourceBundlesFromRemoteServer(DataRequestAttempt dataRequestAttempt) {
		Map<String, Map<ResourceType, Set<ResourceSearchParam>>> fhirResourcesMap = fhirResources
				.getAllFhirGroupsToResourceTypesGrouped();
		log.trace("fhir resource map is: " + fhirResourcesMap);
		String dataRequestType = dataRequestAttempt.getDataRequestTask().getDataRequestType();
		log.debug("data request type is: " + dataRequestType);

		DateRangeParam dateRange = new DateRangeParam()
				.setLowerBoundInclusive(Date.from(dataRequestAttemptService.getLatestSuccessDate(dataRequestAttempt)))
				.setUpperBoundInclusive(Date.from(dataRequestAttempt.getStartTime()));
		log.debug("using date range from: " + dateRange.getLowerBoundAsInstant() + " to "
				+ dateRange.getUpperBoundAsInstant());

		List<Bundle> searchBundles = new ArrayList<>();
		log.debug("dataRequestAttempt ID = " + dataRequestAttempt.getId());
		dataRequestStatusService.changeDataRequestAttemptStatus(dataRequestAttempt.getId(),
				DataRequestStatus.REQUESTED);

		for (Entry<ResourceType, Set<ResourceSearchParam>> resourceSearchParamsSet : fhirResourcesMap
				.get(dataRequestType).entrySet()) {
			Map<String, List<String>> searchParameters = createSearchParams(resourceSearchParamsSet.getKey(),
					resourceSearchParamsSet.getValue());

			IGenericClient sourceFhirClient = fhirUtil
					.getFhirClient(dataRequestAttempt.getDataRequestTask().getRemoteServer().getServerUrl().toString());

			Bundle searchBundle = sourceFhirClient//
					.search()//
					.forResource(resourceSearchParamsSet.getKey().name())//
					.whereMap(searchParameters)//
					.lastUpdated(dateRange)//
					.totalMode(SearchTotalModeEnum.ACCURATE)//
					.returnBundle(Bundle.class).execute();
			log.trace("received json " + fhirUtil.getFhirParser().encodeResourceToString(searchBundle));
			if (searchBundle.hasTotal()) {
				log.debug("received " + searchBundle.getTotal() + " entries of " + resourceSearchParamsSet.getKey());
			}
			searchBundles.add(searchBundle);

			do {
				if (searchBundle.getLink(Bundle.LINK_NEXT) != null) {
					log.debug("getting next bundle");
					searchBundle = sourceFhirClient.loadPage().next(searchBundle).execute();
					searchBundles.add(searchBundle);
				} else {
					searchBundle = null;
				}
			} while (searchBundle != null);

			// TODO add a check for timeout and if it happens, process in smaller chunks. Or
			// just process in smaller chunks anyways
		}
		return searchBundles;
	}

	private Map<String, List<String>> createSearchParams(ResourceType resourceType,
			Set<ResourceSearchParam> resourceSearchParams) {
		Map<String, List<String>> searchParameters = new HashMap<>();

		for (ResourceSearchParam resourceSearchParam : resourceSearchParams) {
			if (resourceSearchParam.getParamName() != null) {
				// here we are 'OR' ing
				searchParameters.put(resourceSearchParam.getParamName(),
						Arrays.asList(String.join(",", resourceSearchParam.getParamValues())));
			}
		}
		log.debug("search parameters for " + resourceType + " are: " + searchParameters);
		return searchParameters;
	}

	private Bundle createTransactionBundleFromSearchResults(DataRequestAttempt dataRequestAttempt,
			List<Bundle> searchResults) {
		Bundle transactionBundle = new Bundle();
		transactionBundle.setType(BundleType.TRANSACTION);
		for (Bundle searchBundle : searchResults) {
			for (BundleEntryComponent searchComponent : searchBundle.getEntry()) {
				if (searchComponent.hasResource()) {
					BundleEntryComponent transactionComponent = createTransactionComponentFromSearchComponent(
							searchComponent, dataRequestAttempt.getDataRequestTask().getRemoteServer().getId());
					transactionBundle.addEntry(transactionComponent);
					transactionBundle.setTotal(transactionBundle.getTotal() + 1);
				}
			}
		}
		return transactionBundle;
	}

	private BundleEntryComponent createTransactionComponentFromSearchComponent(BundleEntryComponent searchComponent,
			Long remoteServerId) {
		ResourceType resourceType = searchComponent.getResource().getResourceType();
		String remoteResourceId = searchComponent.getResource().getIdElement().getIdPart();

		BundleEntryComponent transactionComponent = new BundleEntryComponent();
		transactionComponent.setResource(searchComponent.getResource());

		Optional<String> localId = getResourceLocalId(remoteServerId, remoteResourceId, resourceType);
		if (localId.isEmpty()) {
			// resource does not exists on local server, create it with POST
			transactionComponent.getRequest().setMethod(HTTPVerb.POST);
			transactionComponent.getRequest().setUrl(resourceType.name());
		} else {
			// resource already exists on local server, update it with PUT
			transactionComponent.getRequest().setMethod(HTTPVerb.PUT);
			transactionComponent.getRequest().setUrl(resourceType + "/" + localId.get());
		}
		return transactionComponent;
	}

	private Optional<String> getResourceLocalId(Long remoteServerId, String remoteResourceId,
			ResourceType resourceType) {
		log.debug("checking if " + resourceType + " with id " + remoteResourceId + " from remote server "
				+ remoteServerId + " already created on the local  server");

		Optional<ServerResourceIdMap> serverResourceIdMap = remoteIdToLocalIdDAO
				.findByServerIdAndResourceType(remoteServerId, resourceType);
		if (serverResourceIdMap.isEmpty()
				|| !serverResourceIdMap.get().getRemoteIdToLocalIdMap().containsKey(remoteResourceId)) {
			return Optional.empty();
		} else {
			return Optional.of(serverResourceIdMap.get().getRemoteIdToLocalIdMap().get(remoteResourceId));
		}
	}

}
