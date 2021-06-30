package org.itech.statusofsample.service;

import java.util.List;

import org.itech.common.service.BaseObjectService;
import org.itech.statusofsample.valueholder.StatusOfSample;

public interface StatusOfSampleService extends BaseObjectService<StatusOfSample, String> {
    void getData(StatusOfSample sourceOfSample);

//	void updateData(StatusOfSample sourceOfSample);

//	boolean insertData(StatusOfSample sourceOfSample);

    List<StatusOfSample> getPageOfStatusOfSamples(int startingRecNo);

    Integer getTotalStatusOfSampleCount();

    StatusOfSample getDataByStatusTypeAndStatusCode(StatusOfSample statusofsample);

    List<StatusOfSample> getAllStatusOfSamples();

}
