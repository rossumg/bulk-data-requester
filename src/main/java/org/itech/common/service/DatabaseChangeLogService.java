package org.itech.common.service;

import org.itech.common.valueholder.DatabaseChangeLog;

//public interface DatabaseChangeLogService extends BaseObjectService<DatabaseChangeLog, String> {

public interface DatabaseChangeLogService {
    DatabaseChangeLog getLastExecutedChange();
}
