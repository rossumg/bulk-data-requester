package org.itech.datarequester.bulk.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.ResourceType;
import org.itech.datarequester.bulk.dao.DataRequestTaskDAO;
import org.itech.datarequester.bulk.model.DataRequestAttempt;
import org.itech.datarequester.bulk.model.DataRequestAttempt.DataRequestStatus;
import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.datarequester.bulk.service.DataRequestServerService;
import org.itech.datarequester.bulk.service.DataRequestService;
import org.itech.datarequester.bulk.service.DataRequestStatusService;
import org.itech.datarequester.bulk.service.data.model.DataRequestAttemptService;
import org.itech.fhircore.dao.RemoteIdToLocalIdDAO;
import org.itech.fhircore.model.RemoteIdToLocalId;
import org.itech.fhircore.model.Server;
import org.itech.fhircore.service.FhirResourceGroupService;
import org.itech.fhircore.service.ServerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateRangeParam;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataRequestServiceImpl implements DataRequestService {

	private ServerService serverService;
	private DataRequestServerService dataRequestServerService;
	private DataRequestTaskDAO dataRequestTaskDAO;
	private DataRequestAttemptService dataRequestAttemptService;
	private DataRequestStatusService dataRequestStatusService;
	private FhirContext fhirContext;
	private FhirResourceGroupService fhirResources;
	private RemoteIdToLocalIdDAO remoteIdToLocalIdDAO;

	@Value("${org.itech.destination-server}")
	private String destinationServerPath;

	public DataRequestServiceImpl(ServerService serverService, DataRequestServerService dataRequestServerService,
			DataRequestTaskDAO dataRequestTaskDAO, DataRequestAttemptService dataRequestAttemptService,
			DataRequestStatusService dataRequestStatusService, FhirContext fhirContext,
			FhirResourceGroupService fhirResources, RemoteIdToLocalIdDAO remoteIdToLocalIdDAO) {
		this.serverService = serverService;
		this.dataRequestServerService = dataRequestServerService;
		this.dataRequestTaskDAO = dataRequestTaskDAO;
		this.dataRequestAttemptService = dataRequestAttemptService;
		this.dataRequestStatusService = dataRequestStatusService;
		this.fhirContext = fhirContext;
		this.fhirResources = fhirResources;
		this.remoteIdToLocalIdDAO = remoteIdToLocalIdDAO;
	}

	@SuppressWarnings("unused")
	private Map<String, Set<ResourceType>> requestTypeToResourceType;

	@PostConstruct
	private void getFhirResources() {
		requestTypeToResourceType = fhirResources.getAllFhirGroupsToResourceTypes();
	}

	@Override
	@Async
	@Transactional
	public synchronized void runDataRequestTasksForServer(Long serverId) {
			Server server = serverService.getDAO().findById(serverId).get();
			for (DataRequestTask dataRequestTask : dataRequestServerService
					.getDataRequestTasksForServer(server.getId())) {
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
		List<Bundle> searchResults = getResourceBundlesFromRemoteServer(dataRequestAttempt);
		Bundle transactionBundle = createTransactionBundleFromSearchResults(dataRequestAttempt, searchResults);
		if (transactionBundle.getTotal() > 0) {
			Bundle resultBundle = addBundleToLocalServer(transactionBundle);
			saveRemoteIdToLocalIdMap(transactionBundle, resultBundle, dataRequestAttempt);
		}
	}

	private List<Bundle> getResourceBundlesFromRemoteServer(DataRequestAttempt dataRequestAttempt) {
		Map<String, Set<ResourceType>> fhirResourcesMap = fhirResources.getAllFhirGroupsToResourceTypes();
		log.trace("fhir resource map is: " + fhirResourcesMap);
		String dataRequestType = dataRequestAttempt.getDataRequestTask().getDataRequestType();
		log.debug("data request type is: " + dataRequestType);

		DateRangeParam dateRange = new DateRangeParam()
				.setLowerBoundInclusive(Date.from(dataRequestAttemptService.getLatestSuccessDate(dataRequestAttempt)))
				.setUpperBoundInclusive(Date.from(dataRequestAttempt.getStartTime()));
		log.debug("using date range from: " + dateRange.getLowerBoundAsInstant() + " to "
				+ dateRange.getUpperBoundAsInstant());

		List<Bundle> searchBundles = new ArrayList<>();
		dataRequestStatusService.changeDataRequestAttemptStatus(dataRequestAttempt.getId(),
				DataRequestStatus.REQUESTED);
		for (ResourceType resourceType : fhirResourcesMap.get(dataRequestType)) {

			IGenericClient sourceFhirClient = fhirContext.newRestfulGenericClient(
					dataRequestAttempt.getDataRequestTask().getRemoteServer().getServerUrl().toString());
			Bundle searchBundle = sourceFhirClient//
					.search()//
					.forResource(resourceType.name())//
					.lastUpdated(dateRange)//
					.returnBundle(Bundle.class).execute();
			log.trace("received json " + fhirContext.newJsonParser().encodeResourceToString(searchBundle));
			log.debug("received " + searchBundle.getTotal() + " entries of " + resourceType);
			searchBundles.add(searchBundle);

			// TODO add a check for timeout and if it happens, process in smaller chunks. Or
			// just process in smaller chunks anyways
		}
		return searchBundles;
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
		Optional<RemoteIdToLocalId> remoteIdToLocalId = remoteIdToLocalIdDAO
				.findByServerIdAndResourceType(remoteServerId, resourceType);
		if (remoteIdToLocalId.isEmpty()
				|| !remoteIdToLocalId.get().getRemoteIdToLocalIdMap().containsKey(remoteResourceId)) {
			return Optional.empty();
		} else {
			return Optional.of(remoteIdToLocalId.get().getRemoteIdToLocalIdMap().get(remoteResourceId));
		}
	}

	private Bundle addBundleToLocalServer(Bundle transactionBundle) {
		log.debug("sending json " + fhirContext.newJsonParser().encodeResourceToString(transactionBundle));
		IGenericClient destinationFhirClient = fhirContext.newRestfulGenericClient(destinationServerPath);
		Bundle resultBundle = destinationFhirClient//
				.transaction()//
				.withBundle(transactionBundle)//
				.encodedJson().execute();
		log.debug("result json " + fhirContext.newJsonParser().encodeResourceToString(resultBundle));
		return resultBundle;
	}

	private void saveRemoteIdToLocalIdMap(Bundle transactionBundle, Bundle resultBundle,
			DataRequestAttempt dataRequestAttempt) {
		Server remoteServer = dataRequestAttempt.getDataRequestTask().getRemoteServer();
		for (int i = 0; i < transactionBundle.getTotal(); ++i) {
			BundleEntryComponent transactionBundleEntryComponent = transactionBundle.getEntry().get(i);
			BundleEntryComponent resultBundleEntryComponent = resultBundle.getEntry().get(i);
			if (resultBundleEntryComponent.getResponse().getStatus().contains("201")) {
				log.debug(transactionBundleEntryComponent.getResource().getResourceType() + " with remote id "
						+ transactionBundleEntryComponent.getResource().getIdElement().getIdPart() + " and local id "
						+ getIdFromLocation(resultBundleEntryComponent.getResponse().getLocation()) + " created");
				RemoteIdToLocalId remoteIdToLocalId = remoteIdToLocalIdDAO
						.findByServerIdAndResourceType(remoteServer.getId(),
								transactionBundleEntryComponent.getResource().getResourceType())
						.orElse(new RemoteIdToLocalId(remoteServer,
								transactionBundleEntryComponent.getResource().getResourceType()));
				Map<String, String> remoteToLocalMap = remoteIdToLocalId.getRemoteIdToLocalIdMap();
				// TODO assuming these are sorted in same order, please confirm
				remoteToLocalMap.put(transactionBundleEntryComponent.getResource().getIdElement().getIdPart(),
						getIdFromLocation(resultBundleEntryComponent.getResponse().getLocation()));
				remoteIdToLocalIdDAO.save(remoteIdToLocalId);
			} else {
				log.debug("resource wan't created");
			}
		}
	}

	private String getIdFromLocation(String location) {
		int firstSlashIndex = location.indexOf('/');
		return location.substring(firstSlashIndex + 1, location.indexOf('/', firstSlashIndex + 1));
	}

}
