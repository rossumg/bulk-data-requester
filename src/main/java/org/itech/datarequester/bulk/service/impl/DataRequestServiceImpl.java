package org.itech.datarequester.bulk.service.impl;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
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
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.itech.common.FhirUtil;
import org.itech.common.JSONUtils;
import org.itech.datarequester.bulk.config.FhirConfig;
import org.itech.datarequester.bulk.dao.DataRequestTaskDAO;
import org.itech.datarequester.bulk.model.DataRequestAttempt;
import org.itech.datarequester.bulk.model.DataRequestAttempt.DataRequestStatus;
import org.itech.datarequester.bulk.model.DataRequestTask;
import org.itech.datarequester.bulk.service.DataRequestService;
import org.itech.datarequester.bulk.service.DataRequestStatusService;
import org.itech.datarequester.bulk.service.data.model.DataRequestAttemptService;
import org.itech.datarequester.bulk.service.data.model.DataRequestTaskService;
import org.itech.etl.dao.ETLRecordDAO;
import org.itech.etl.model.ETLRecord;
import org.itech.etl.service.ETLRecordService;
import org.itech.fhircore.dao.ServerResourceIdMapDAO;
import org.itech.fhircore.model.ResourceSearchParam;
import org.itech.fhircore.model.Server;
import org.itech.fhircore.model.ServerResourceIdMap;
import org.itech.fhircore.service.FhirResourceGroupService;
import org.itech.fhircore.service.ServerService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataRequestServiceImpl implements DataRequestService {
    
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirConfig fhirConfig;

	private ServerService serverService;
	private DataRequestTaskService serverDataRequestTaskService;
	private DataRequestTaskDAO dataRequestTaskDAO;
	private DataRequestAttemptService dataRequestAttemptService;
	private DataRequestStatusService dataRequestStatusService;
	private FhirResourceGroupService fhirResources;
	private ServerResourceIdMapDAO remoteIdToLocalIdDAO;
	
	private ETLRecordDAO etlRecordDAO;
	private ETLRecordService etlRecordService;

	@Value("${org.itech.destination-server}")
	private String destinationServerPath;

	public DataRequestServiceImpl(ServerService serverService, DataRequestTaskService serverDataRequestTaskService,
			DataRequestTaskDAO dataRequestTaskDAO, DataRequestAttemptService dataRequestAttemptService,
			DataRequestStatusService dataRequestStatusService, FhirContext fhirContext,
			FhirResourceGroupService fhirResources, ServerResourceIdMapDAO remoteIdToLocalIdDAO,
			ETLRecordService etlRecordService) {
		this.serverService = serverService;
		this.serverDataRequestTaskService = serverDataRequestTaskService;
		this.dataRequestTaskDAO = dataRequestTaskDAO;
		this.dataRequestAttemptService = dataRequestAttemptService;
		this.dataRequestStatusService = dataRequestStatusService;
		this.fhirContext = fhirContext;
		this.fhirResources = fhirResources;
		this.remoteIdToLocalIdDAO = remoteIdToLocalIdDAO;
		this.etlRecordService = etlRecordService;
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
		
//			Bundle resultBundle = addBundleToLocalServer(transactionBundle);
//			saveRemoteIdToLocalIdMap(transactionBundle, resultBundle, dataRequestAttempt);
	}
	
	private void createETLRecord(List<Bundle> searchBundles) {
	    log.debug("createETLRecord: " );
	    List<ETLRecord> etlRecordList = new ArrayList<>();

	    List<Observation> observations = new ArrayList<>();
	    for (Bundle bundle : searchBundles) {
	        for (BundleEntryComponent entry : bundle.getEntry()) {
	            observations.add((Observation) entry.getResource());
	        }
	    }
	    log.debug("createETLRecord:observations:size: " + observations.size());
	    etlRecordList = getLatestFhirforETL(observations);
	    log.debug("createETLRecord:etlRecordList:size: " + etlRecordList.size());
	    
	    // add records to data mart
	    if (etlRecordService.saveAll(etlRecordList)) {
	        log.debug("createETLRecord:saveAll:success ");
	    } else {
	        log.debug("createETLRecord:saveAll:fail ");
	    }
	    
	}
	
	public List<ETLRecord> getLatestFhirforETL(List<Observation> observations) {
	        log.debug("getLatestFhirforETL:size: " + observations.size());

	        List<ETLRecord> etlRecordList = new ArrayList<>();
	        
	        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
	        org.hl7.fhir.r4.model.Observation fhirObservation = new org.hl7.fhir.r4.model.Observation();
	        org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest = new org.hl7.fhir.r4.model.ServiceRequest();
	        org.hl7.fhir.r4.model.Practitioner fhirPractitioner = new org.hl7.fhir.r4.model.Practitioner();
	        org.hl7.fhir.r4.model.Specimen fhirSpecimen = new org.hl7.fhir.r4.model.Specimen();
	        IGenericClient localFhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
	        org.json.simple.JSONObject code = null;
	        org.json.simple.JSONArray coding = null;
	        org.json.simple.JSONObject jCoding = null;
	        
	        // gnr

	        for (Observation observation : observations) {
	            fhirObservation = (Observation) observation;
	            System.out.println("observation: " +  fhirContext.newJsonParser().encodeResourceToString(fhirObservation));
//	            log.debug( "glfe: " +   fhirObservation.getBasedOnFirstRep().getReference().toString());
	            String srString = fhirObservation.getBasedOnFirstRep().getReference().toString();
	            String srUuidString = srString.substring(srString.lastIndexOf("/") + 1);

	            //sr, prac
	            Bundle srBundle = (Bundle) localFhirClient.search().forResource(ServiceRequest.class)
	                    .where(new TokenClientParam("_id").exactly().code(srUuidString))
	                    .prettyPrint()
	                    .execute();

	            if (srBundle.hasEntry()) {
	                fhirServiceRequest = (ServiceRequest) srBundle.getEntryFirstRep().getResource();
	                // log.debug( "glfe:fhirServiceRequest: " +   fhirContext.newJsonParser().encodeResourceToString(fhirServiceRequest));
	                //  get Practitioner
	                String pracString = fhirServiceRequest.getRequester().getReference().toString();
	                String pracUuidString = pracString.substring(pracString.lastIndexOf("/") + 1);
	                // log.debug( "glfe:fhirServiceRequest: " + pracString + " " + pracUuidString);
	                Bundle pracBundle = (Bundle) localFhirClient.search().forResource(Practitioner.class)
	                        .where(new TokenClientParam("_id").exactly().code(pracUuidString))
	                        .prettyPrint()
	                        .execute();

	                if (pracBundle.hasEntry()) {
	                    fhirPractitioner = (Practitioner) pracBundle.getEntryFirstRep().getResource();
	                    // log.debug( "glfe:fhirPractitioner: " +   fhirContext.newJsonParser().encodeResourceToString(fhirPractitioner));
	                } else {
	                    // log.debug( "glfe: NO PRACTITIONER ");
	                }
	            }
	            
	            String patString = fhirObservation.getSubject().getReference().toString();
	            String patUuidString = patString.substring(patString.lastIndexOf("/") + 1);

	            Bundle patBundle = (Bundle) localFhirClient.search().forResource(org.hl7.fhir.r4.model.Patient.class)
	                    .where(new TokenClientParam("_id").exactly().code(patUuidString))
	                    .prettyPrint()
	                    .execute();

	            if (patBundle.hasEntry()) {
	                fhirPatient = (org.hl7.fhir.r4.model.Patient) patBundle.getEntryFirstRep().getResource();
	            }
	            
	            //sp
	            String spString = fhirObservation.getSpecimen().getReference().toString();
	            String spUuidString = spString.substring(spString.lastIndexOf("/") + 1);

	            Bundle spBundle = (Bundle) localFhirClient.search().forResource(Specimen.class)
	                    .where(new TokenClientParam("_id").exactly().code(spUuidString))
	                    .prettyPrint()
	                    .execute();

	            if (spBundle.hasEntry()) {
	                fhirSpecimen = (Specimen) spBundle.getEntryFirstRep().getResource();
	            }

	            JSONObject jResultUUID = null;
	            JSONObject jSRRef = null;
	            JSONObject reqRef = null;
	           
	            int j = 0;

	            ETLRecord etlRecord = new ETLRecord();
	            try {
	                String observationStr = fhirContext.newJsonParser().encodeResourceToString(fhirObservation);
	                // log.debug( "glfe: " + observationStr);
	                JSONObject observationJson = null;
	                observationJson = JSONUtils.getAsObject(observationStr);
	                // log.debug( "glfe: " + observationJson.toString());
	                if (!JSONUtils.isEmpty(observationJson)) {

	                    org.json.simple.JSONArray identifier = JSONUtils.getAsArray(observationJson.get("identifier"));
	                    for (j = 0; j < identifier.size(); ++j) {
	                        // log.debug( "glfe: " + identifier.get(j).toString());
	                        jResultUUID = JSONUtils.getAsObject(identifier.get(j));
	                        // log.debug( "glfe: " + jResultUUID.get("system").toString());
	                        // log.debug( "glfe: " + jResultUUID.get("value").toString());
	                    }
	                    try {
	                        code = JSONUtils.getAsObject(observationJson.get("valueCodeableConcept"));
	                        coding = JSONUtils.getAsArray(code.get("coding"));
	                        for (j = 0; j < coding.size(); ++j) {
	                            // log.debug( "glfe: " + coding.get(0).toString());
	                            jCoding = JSONUtils.getAsObject(coding.get(0));
	                            // log.debug( "glfe: " + jCoding.get("system").toString());
	                            // log.debug( "glfe: " + jCoding.get("code").toString());
	                            // log.debug( "glfe: " + jCoding.get("display").toString());
	                        }
	                    } catch(Exception e) { 
	                        e.printStackTrace();
	                    }
	                    etlRecord.setResult(jCoding.get("display").toString());
	                    // log.debug( "glfe: " + observationJson.get("subject").toString());

	                    JSONObject subjectRef = null;
	                    subjectRef = JSONUtils.getAsObject(observationJson.get("subject"));
	                    // log.debug( "glfe: " + subjectRef.get("reference").toString());

	                    JSONObject specimenRef = null;
	                    specimenRef = JSONUtils.getAsObject(observationJson.get("specimen"));
	                    // log.debug( "glfe: " + specimenRef.get("reference").toString());

	                    org.json.simple.JSONArray serviceRequestRef = null;
	                    serviceRequestRef = JSONUtils.getAsArray(observationJson.get("basedOn"));
	                    for (j = 0; j < serviceRequestRef.size(); ++j) {
	                        // log.debug( "glfe: " + serviceRequestRef.get(j).toString());
	                        jSRRef = JSONUtils.getAsObject(serviceRequestRef.get(j));
	                        // log.debug( "glfe: " + jSRRef.get("reference").toString());
	                    }
	                    etlRecord.setOrder_status(observationJson.get("status").toString());
	                    etlRecord.setData(observationStr);
	                }

	                String patientStr = fhirContext.newJsonParser().encodeResourceToString(fhirPatient);
	                JSONObject patientJson = null;
	                patientJson = JSONUtils.getAsObject(patientStr);
	                // log.debug( "glfe: " + patientJson.toString());
	                if (!JSONUtils.isEmpty(patientJson)) {

	                    org.json.simple.JSONArray identifier = JSONUtils.getAsArray(patientJson.get("identifier"));
	                    for (j = 0; j < identifier.size(); ++j) {
	                        // log.debug( "glfe: " + identifier.get(j).toString());
	                        JSONObject patIds = JSONUtils.getAsObject(identifier.get(j));
	                        // log.debug( "glfe: " + patIds.get("system").toString());
	                        // log.debug( "glfe: " + patIds.get("value").toString());
	                        if (patIds.get("system").toString()
	                                .equalsIgnoreCase("http://openelis-global.org/pat_nationalId")) {
	                            etlRecord.setIdentifier(patIds.get("value").toString());
	                        }
	                        etlRecord.setSex(patientJson.get("gender").toString());
	                        //       1994
	                        try {
	                            //                            String timestampToDate = patientJson.get("birthDate").toString().substring(0,10);
	                            String timestampToDate = patientJson.get("birthDate").toString().substring(0,4);
	                            timestampToDate = timestampToDate +"-01-01";
	                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	                            Date parsedDate = dateFormat.parse(timestampToDate);
	                            Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
	                            etlRecord.setBirthdate(timestamp);
	                        } catch (Exception e) {
	                            e.printStackTrace();
	                        }
	                    }

	                    org.json.simple.JSONArray name = JSONUtils.getAsArray(patientJson.get("name"));
	                    for (j = 0; j < name.size(); ++j) {
	                        // log.debug( "glfe: " + name.get(j).toString());
	                        JSONObject jName = JSONUtils.getAsObject(name.get(j));
	                        // log.debug( "glfe: " + jName.get("family").toString());
	                        // log.debug( "glfe: " + jName.get("given").toString());
	                        etlRecord.setLast_name(jName.get("family").toString());
	                        org.json.simple.JSONArray givenName = JSONUtils.getAsArray(jName.get("given"));
	                        etlRecord.setFirst_name(givenName.get(0).toString());
	                    }

	                    org.json.simple.JSONArray address = JSONUtils.getAsArray(patientJson.get("address"));
	                    for (j = 0; j < address.size(); ++j) {
	                        JSONObject jAddress = JSONUtils.getAsObject(address.get(j));
	                        org.json.simple.JSONArray jLines = JSONUtils.getAsArray(jAddress.get("line"));
	                        etlRecord.setAddress_street(jLines.get(0).toString());
	                        etlRecord.setAddress_city(jAddress.get("city").toString());
//	                        etlRecord.setAddress_country(jAddress.get("country").toString());
	                    }

	                    org.json.simple.JSONArray telecom = JSONUtils.getAsArray(patientJson.get("telecom"));
	                    for (j = 0; j < telecom.size(); ++j) {
	                        // log.debug( "glfe: " + telecom.get(j).toString());
	                        JSONObject jTelecom = JSONUtils.getAsObject(telecom.get(j));

	                        if (jTelecom.get("system").toString().equalsIgnoreCase("other")) {
	                            // log.debug( "glfe: " + jTelecom.get("system").toString());
	                            // log.debug( "glfe: " + jTelecom.get("value").toString());
	                            etlRecord.setHome_phone(jTelecom.get("value").toString());
	                        } else if (jTelecom.get("system").toString().equalsIgnoreCase("sms")) {
	                            // log.debug( "glfe: " + jTelecom.get("system").toString());
	                            // log.debug( "glfe: " + jTelecom.get("value").toString());
	                            etlRecord.setWork_phone(jTelecom.get("value").toString());
	                        }
	                    }
	                    etlRecord.setPatientId(fhirPatient.getId());
	                }

	                String serviceRequestStr = fhirContext.newJsonParser().encodeResourceToString(fhirServiceRequest);
	                // log.debug( "glfe: " + serviceRequestStr);
	                JSONObject srJson = null;
	                srJson = JSONUtils.getAsObject(serviceRequestStr);
	                // log.debug( "glfe: " + srJson.toString());

	                if (!JSONUtils.isEmpty(srJson)) {

	                    org.json.simple.JSONArray identifier = JSONUtils.getAsArray(srJson.get("identifier"));
	                    for (j = 0; j < identifier.size(); ++j) {
	                        // log.debug( "glfe: " + identifier.get(j).toString());
	                        JSONObject srIds = JSONUtils.getAsObject(identifier.get(j));
	                        // log.debug( "glfe: " + srIds.get("system").toString());
	                        // log.debug( "glfe: " + srIds.get("value").toString());
	                    }

	                    reqRef = JSONUtils.getAsObject(srJson.get("requisition"));
	                    // log.debug( "glfe: " + reqRef.get("system").toString());
	                    // log.debug( "glfe: " + reqRef.get("value").toString());
	                    etlRecord.setLabno(reqRef.get("value").toString());

	                    code = JSONUtils.getAsObject(srJson.get("code"));
	                    coding = JSONUtils.getAsArray(code.get("coding"));
	                    
	                    org.json.simple.JSONArray jCategoryArray = JSONUtils.getAsArray(srJson.get("category"));
	                    // log.debug( "glfe: " + jCategoryArray.get(0).toString());
	                    JSONObject jCatJson = (JSONObject) jCategoryArray.get(0);
	                    coding = JSONUtils.getAsArray(jCatJson.get("coding"));
	                    for (j = 0; j < coding.size(); ++j) {
	                        jCoding = (JSONObject) coding.get(j);
	                        // log.debug( "glfe: " + jCoding.get("system").toString());
	                        // log.debug( "glfe: " + jCoding.get("code").toString());
	                        // log.debug( "glfe: " + jCoding.get("display").toString());
	                    }
	                    etlRecord.setProgram(jCoding.get("display").toString());

//	                    reqRef = JSONUtils.getAsObject(srJson.get("locationReference"));
//	                    System.out.println("srReq:" + reqRef.get("system").toString());
//	                    System.out.println("srReq:" + reqRef.get("value").toString());
//	                    etlRecord.setCode_referer(reqRef.get("value").toString());
	                    
//	                    etlRecord.setReferer(fhirPractitioner.getName().get(0).getGivenAsSingleString() + 
//	                            fhirPractitioner.getName().get(0).getFamily());

	                    code = JSONUtils.getAsObject(srJson.get("code"));
	                    coding = JSONUtils.getAsArray(code.get("coding"));
	                    for (j = 0; j < coding.size(); ++j) {
	                        // log.debug( "glfe: " + coding.get(0).toString());
	                        jCoding = JSONUtils.getAsObject(coding.get(0));
	                        // log.debug( "glfe: " + jCoding.get("system").toString());
//	                        log.debug( "glfe: " + jCoding.get("code").toString());
	                        // log.debug( "glfe: " + jCoding.get("display").toString());
	                    }
	                    
	                    etlRecord.setTest(jCoding.get("display").toString()); //test description

	                    //                    2021-05-06T12:51:58-07:00
	                    try {
	                        String timestampToDate = srJson.get("authoredOn").toString().substring(0,10);
	                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	                        Date parsedDate = dateFormat.parse(timestampToDate);
	                        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
	                        etlRecord.setDate_entered(timestamp);
	                    } catch(Exception e) { 
	                        e.printStackTrace();
	                    }
	                    
	                    // done here because data_entered is used for age
	                    LocalDate birthdate = LocalDate.parse(etlRecord.getBirthdate().toString().substring(0,10));
	                    LocalDate date_entered = LocalDate.parse(etlRecord.getDate_entered().toString().substring(0,10));
	                    if ((etlRecord.getBirthdate() != null) && (etlRecord.getDate_entered() != null)) {
	                        int age_days = Period.between(birthdate, date_entered).getDays();
	                        int age_years = Period.between(birthdate, date_entered).getYears();
	                        int age_months = Period.between(birthdate, date_entered).getMonths();
	                        int age_weeks = Math.round(age_days)/7;

	                        if (age_days > 3) etlRecord.setAge_weeks(age_weeks + 1);
	                        if (age_weeks > 2) etlRecord.setAge_months(age_months + 1);
	                        etlRecord.setAge_years((age_months > 5) ? age_years + 1 : age_years);
	                        etlRecord.setAge_months((12*age_years) + age_months); 
	                        etlRecord.setAge_weeks((52*age_years) + (4*age_months) + age_weeks); 
	                    }
	                }

	                String specimenStr = fhirContext.newJsonParser().encodeResourceToString(fhirSpecimen);
	                // log.debug( "glfe: " + specimenStr);
	                JSONObject specimenJson = null;
	                specimenJson = JSONUtils.getAsObject(specimenStr);
	                // log.debug( "glfe: " + specimenJson.toString());
	                if (!JSONUtils.isEmpty(specimenJson)) {

	                    org.json.simple.JSONArray identifier = JSONUtils.getAsArray(specimenJson.get("identifier"));
	                    for (j = 0; j < identifier.size(); ++j) {
	                        // log.debug( "glfe: " + identifier.get(j).toString());
	                        JSONObject specimenId = JSONUtils.getAsObject(identifier.get(j));
	                        // log.debug( "glfe: " + specimenId.get("system").toString());
	                        // log.debug( "glfe: " + specimenId.get("value").toString());
	                    }

	                    code = JSONUtils.getAsObject(specimenJson.get("type"));
	                    coding = JSONUtils.getAsArray(code.get("coding"));
	                    for (j = 0; j < coding.size(); ++j) {
	                        // log.debug( "glfe: " + coding.get(0).toString());
	                        jCoding = JSONUtils.getAsObject(coding.get(0));
	                        // log.debug( "glfe: " + jCoding.get("system").toString());
	                        // log.debug( "glfe: " + jCoding.get("code").toString());
	                        // log.debug( "glfe: " + jCoding.get("display").toString());
	                    }
	                    //                  2021-04-29T16:58:51-07:00
	                    try {
	                        String timestampToDate = specimenJson.get("receivedTime").toString().substring(0,10);
	                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	                        Date parsedDate = dateFormat.parse(timestampToDate);
	                        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
	                        etlRecord.setDate_recpt(timestamp);
	                    } catch(Exception e) { 
	                        e.printStackTrace();
	                    }
	                    
	                    JSONObject jCollection = JSONUtils.getAsObject(specimenJson.get("collection"));
//	                    JSONObject jCollectedDateTime = JSONUtils.getAsObject(jCollection.get("collectedDateTime"));
	                    // log.debug( "glfe: " + jCollection.get("collectedDateTime").toString());
	                    try {
	                        String timestampToDate = jCollection.get("collectedDateTime").toString().substring(0,10);
	                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	                        Date parsedDate = dateFormat.parse(timestampToDate);
	                        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
	                        etlRecord.setDate_collect(timestamp);
	                    } catch(Exception e) { 
	                        e.printStackTrace();
	                    }
	                }

	                String practitionerStr = fhirContext.newJsonParser().encodeResourceToString(fhirPractitioner);
	                // log.debug( "glfe:practitionerStr: " + practitionerStr);
	                JSONObject practitionerJson = null;
	                practitionerJson = JSONUtils.getAsObject(practitionerStr);
	                // log.debug( "glfe: " + practitionerJson.toString());
	                if (!JSONUtils.isEmpty(practitionerJson)) {
	                    org.json.simple.JSONArray name = JSONUtils.getAsArray(practitionerJson.get("name"));
	                    for (j = 0; j < name.size(); ++j) {
	                        // log.debug( "glfe: " + name.get(j).toString());
	                        JSONObject jName = JSONUtils.getAsObject(name.get(j));
	                        // log.debug( "glfe: " + jName.get("family").toString());
	                        // log.debug( "glfe: " + jName.get("given").toString());
	                        org.json.simple.JSONArray givenName = JSONUtils.getAsArray(jName.get("given"));
	                        etlRecord.setReferer(givenName.get(0).toString() + " " + 
	                                jName.get("family").toString());
	                    }
	                }
	            } catch (org.json.simple.parser.ParseException e) {
	                e.printStackTrace();
	            }
	            
	            etlRecordList.add(etlRecord);
	        }
	        
	        return etlRecordList;
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
			
			IGenericClient sourceFhirClient = fhirContext.newRestfulGenericClient(
					dataRequestAttempt.getDataRequestTask().getRemoteServer().getServerUrl().toString());
			
			Bundle searchBundle = sourceFhirClient//
					.search()//
					.forResource(resourceSearchParamsSet.getKey().name())//
					.whereMap(searchParameters)//
					.lastUpdated(dateRange)//
					.returnBundle(Bundle.class).execute();
			log.trace("received json " + fhirContext.newJsonParser().encodeResourceToString(searchBundle));
			log.debug("received " + searchBundle.getTotal() + " entries of " + resourceSearchParamsSet.getKey());
			searchBundles.add(searchBundle);
			
			do {
			    if (searchBundle.getLink(Bundle.LINK_NEXT) != null) {
			        searchBundle = sourceFhirClient.loadPage().next(searchBundle).execute();
			        searchBundles.add(searchBundle);
			    }
			    else
			        searchBundle = null;
			}
			while (searchBundle != null);
			
			// TODO add a check for timeout and if it happens, process in smaller chunks. Or
			// just process in smaller chunks anyways
		}
		
		this.createETLRecord(searchBundles);
		searchBundles.clear(); // turn off other checking
		return searchBundles;
	}

	private Map<String, List<String>> createSearchParams(
			ResourceType resourceType, Set<ResourceSearchParam> resourceSearchParams) {
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
				ServerResourceIdMap remoteIdToLocalId = remoteIdToLocalIdDAO
						.findByServerIdAndResourceType(remoteServer.getId(),
								transactionBundleEntryComponent.getResource().getResourceType())
						.orElse(new ServerResourceIdMap(remoteServer,
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
