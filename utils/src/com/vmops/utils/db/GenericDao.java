/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.  
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.vmops.utils.db;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

/**
 * GenericDao should be the interface that every DAO implements.  This gives
 * a uniform method for persisting and finding db entities.
 **/
public interface GenericDao<T, ID extends Serializable> {
 
    /**
     * This column can be used if the table wants to delay deletion.
     */
    static final String REMOVED_COLUMN = "removed";
    
    /**
     * This column can be used if the table wants to track creation time.
     */
    static final String CREATED_COLUMN = "created";
    
    /**
     * Look for an entity bean using the database id.  Does not lock the row.
     * @param id database unique id for the entity bean.
     * @return entity bean.
     **/
    T findById(ID id);
    
    T findById(ID id, boolean fresh);
    
    /**
     * @return VO object ready to be used for update.  It won't have any fields filled in.
     */
    T createForUpdate();
    
    SearchBuilder<T> createSearchBuilder();
    
    T createForUpdate(ID id);
    
    /**
     * Returns a SearchCriteria object that can be used to build search conditions.
     * 
     * @return SearchCriteria
     */
    SearchCriteria createSearchCriteria();
    
    /**
     * lock the rows that matched the search criteria and filter.  This method needs
     * to be called within a transaction.
     * 
     * @param sc SearchCriteria containing the different search conditions
     * @param filter Filter containing limits etc
     * @param exclusive exclusive or share lock
     * @return List<T> list of entity beans
     */
    List<T> lock(SearchCriteria sc, Filter filter, boolean exclusive);
    
    /**
     * lock 1 of the return set.  This method needs to be run within a
     * transaction or else it's useless.
     * @param sc
     * @param exclusive
     * @return T if found and locked.  null if not.
     */
    T lock(SearchCriteria sc, boolean exclusive);
    
    /**
     * Find and lock the row for update.
     * @param id id
     * @param exclusive is this a read share lock or exclusive lock?
     * @return T
     */
    T lock(ID id, Boolean exclusive);

    /**
     * Acquires a database wide lock on the id of the entity.  This ensures
     * that only one is being used.  The timeout is the configured default.
     * 
     * @param id id of the entity to acquire an lock on.
     * @return object if acquired; null if not.  If null, you need to call findById to see if it is actually not found.
     */
    T acquire(ID id);
   
    /**
     * Acquires a database wide lock on the id of the entity.  This ensures
     * that only one is being used.  The timeout is the configured default.
     * 
     * @param id id of the entity to acquire an lock on.
     * @param seconds time to wait for the lock.
     * @return entity if the lock is acquired; null if not.
     */
    T acquire(ID id, int seconds);
    
    /**
     * releases the lock acquired in the acquire method call.
     * @param id id of the entity to release the lock on.
     * @return true if it is released.  false if not or not found.
     */
    boolean release(final ID id);
    
    boolean update(ID id, T entity);
    
    /**
     * Look for all active rows.
     * @return list of entity beans.
     */
    List<T> listAllActive();

    /**
     * Look for all active rows.
     * @param filter filter to limit the results
     * @return list of entity beans.
     */
    List<T> listAllActive(Filter filter);
    
    
    /**
     * Search for the entity beans
     * @param sc
     * @param filter
     * @return list of entity beans.
     */
    List<T> search(SearchCriteria sc, Filter filter);
    
    /**
     * Retrieves the entire table.
     * @return collection of entity beans.
     **/
    List<T> listAll();

    /**
     * Retrieves the entire table.
     * @param filter filter to limit the returns.
     * @return collection of entity beans.
     **/
    List<T> listAll(Filter filter);
    
    /**
     * Persist the entity bean.  The id field of the entity is updated with
     * the new id.
     * @param entity the bean to persist.
     **/
    ID persist(T entity);
    
    /**
     * remove the entity bean.  This will call delete automatically if
     * the entity bean does not have a removed field.
     * @param id
     * @return true if removed.
     */
    boolean remove(ID id);
    
    /**
     * remove the entity bean.
     * @param id
     * @return true if removed.
     */
    boolean delete(ID id);
    
    /**
     * expunge the removed rows.
     */
    void expunge();
    
    public <K> K getNextInSequence(Class<K> clazz, String name);
    
    /**
     * Configure.
     * @param name name of the dao.
     * @param params params if any are specified.
     * @return true if config is good.  false if not.
     */
    boolean configure(String name, Map<String, Object> params) throws ConfigurationException;
}