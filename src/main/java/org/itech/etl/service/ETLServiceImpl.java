package org.itech.etl.service;

import org.itech.common.service.BaseObjectServiceImpl;
import org.itech.etl.dao.ETLDAO;
import org.itech.etl.valueholder.ETLRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ETLServiceImpl extends BaseObjectServiceImpl<ETLRecord, String>
        implements ETLService {
    @Autowired
    protected ETLDAO baseObjectDAO;

    ETLServiceImpl() {
        super(ETLRecord.class);
    }

    @Override
    protected ETLDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

//    @Override
//    @Transactional(readOnly = true)
//    public List<ElectronicOrder> getAllElectronicOrdersOrderedBy(SortOrder order) {
//        return getBaseObjectDAO().getAllElectronicOrdersOrderedBy(order);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ElectronicOrder> getElectronicOrdersByExternalId(String id) {
//        return getBaseObjectDAO().getElectronicOrdersByExternalId(id);
//    }
//
//    @Override
//    public List<ElectronicOrder> getAllElectronicOrdersContainingValueOrderedBy(String searchValue, SortOrder order) {
//        return getBaseObjectDAO().getAllElectronicOrdersContainingValueOrderedBy(searchValue, order);
//    }
//
//    @Override
//    public List<ElectronicOrder> getAllElectronicOrdersContainingValuesOrderedBy(String accessionNumber,
//            String patientLastName, String patientFirstName, String gender, SortOrder order) {
//        return getBaseObjectDAO().getAllElectronicOrdersContainingValuesOrderedBy(accessionNumber, patientLastName,
//                patientFirstName, gender, order);
//    }

}
