/**
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.itech.common.daoimpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Query;
import org.hibernate.Session;
import org.itech.common.dao.DatabaseChangeLogDAO;
import org.itech.common.exception.LIMSRuntimeException;
import org.itech.common.log.LogEvent;
import org.itech.common.valueholder.DatabaseChangeLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
//public class DatabaseChangeLogDAOImpl extends BaseDAOImpl<DatabaseChangeLog, String> implements DatabaseChangeLogDAO {
public class DatabaseChangeLogDAOImpl implements DatabaseChangeLogDAO {

    // public DatabaseChangeLogDAOImpl() {
    // super(DatabaseChangeLog.class);
    // }

    @PersistenceContext
    EntityManager entityManager;

    @Override
    
    @Transactional(readOnly = true)
    public DatabaseChangeLog getLastExecutedChange() throws LIMSRuntimeException {
        List<DatabaseChangeLog> results;

        try {
            String sql = "from DatabaseChangeLog dcl order by dcl.executed desc";
            Query query = entityManager.unwrap(Session.class).createQuery(sql);

            results = query.list();
            // entityManager.unwrap(Session.class).flush(); // CSL remove old
            // entityManager.unwrap(Session.class).clear(); // CSL remove old

            if (results != null && results.get(0) != null) {
                return results.get(0);
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e.toString(), e);
            throw new LIMSRuntimeException("Error in DatabaseChangeLogDAOImpl getLastExecutedChange()", e);
        }

        return null;
    }

    @Override
    public Optional<DatabaseChangeLog> get(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllMatching(String propertyName, Object propertyValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllMatching(Map<String, Object> propertyValues) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllLike(Map<String, String> propertyValues) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllLike(String propertyName, String propertyValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllOrdered(String orderProperty, boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllOrdered(List<String> orderProperties, boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllMatchingOrdered(String propertyName, Object propertyValue,
            String orderProperty, boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllMatchingOrdered(String propertyName, Object propertyValue,
            List<String> orderProperties, boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllMatchingOrdered(Map<String, Object> propertyValues, String orderProperty,
            boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllMatchingOrdered(Map<String, Object> propertyValues,
            List<String> orderProperties, boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllLikeOrdered(String propertyName, String propertyValue, String orderProperty,
            boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllLikeOrdered(String propertyName, String propertyValue,
            List<String> orderProperties, boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllLikeOrdered(Map<String, String> propertyValues, String orderProperty,
            boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getAllLikeOrdered(Map<String, String> propertyValues, List<String> orderProperties,
            boolean descending) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getPage(int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getMatchingPage(String propertyName, Object propertyValue, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getMatchingPage(Map<String, Object> propertyValues, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getOrderedPage(String orderProperty, boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getOrderedPage(List<String> orderProperties, boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getMatchingOrderedPage(String propertyName, Object propertyValue,
            String orderProperty, boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getMatchingOrderedPage(String propertyName, Object propertyValue,
            List<String> orderProperties, boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getMatchingOrderedPage(Map<String, Object> propertyValues, String orderProperty,
            boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getMatchingOrderedPage(Map<String, Object> propertyValues,
            List<String> orderProperties, boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String insert(DatabaseChangeLog object) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DatabaseChangeLog update(DatabaseChangeLog baseObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(DatabaseChangeLog object) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Integer getCount() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<DatabaseChangeLog> getNext(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<DatabaseChangeLog> getPrevious(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTableName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getLikePage(String propertyName, String propertyValue, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getLikePage(Map<String, String> propertyValues, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getLikeOrderedPage(String propertyName, String propertyValue, String orderProperty,
            boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getLikeOrderedPage(String propertyName, String propertyValue,
            List<String> orderProperties, boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getLikeOrderedPage(Map<String, String> propertyValues, String orderProperty,
            boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DatabaseChangeLog> getLikeOrderedPage(Map<String, String> propertyValues, List<String> orderProperties,
            boolean descending, int startingRecNo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void evict(DatabaseChangeLog oldObject) {
        // TODO Auto-generated method stub
        
    }

}
