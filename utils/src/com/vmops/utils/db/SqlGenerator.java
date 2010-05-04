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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeOverride;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.TableGenerator;

import com.vmops.utils.Pair;
import com.vmops.utils.Ternary;
import com.vmops.utils.db.Attribute.Flag;

public class SqlGenerator {
	Class<?> _clazz;
    ArrayList<Attribute> _attributes;
    ArrayList<Field> _embeddeds;
    ArrayList<Class<?>> _tables;
    LinkedHashMap<String, List<Attribute>> _ids;
    HashMap<String, TableGenerator> _generators;
    
    public SqlGenerator(Class<?> clazz) {
    	_clazz = clazz;
        _tables = new ArrayList<Class<?>>();
        _attributes = new ArrayList<Attribute>();
        _embeddeds = new ArrayList<Field>();
        _ids = new LinkedHashMap<String, List<Attribute>>();
        _generators = new HashMap<String, TableGenerator>();
        
        buildAttributes(clazz, DbUtil.getTableName(clazz), DbUtil.getAttributeOverrides(clazz), false, false);
        assert (_tables.size() > 0) : "Did you forget to put @Entity on " + clazz.getName();
        handleDaoAttributes(clazz);
    }
    
    protected boolean checkMethods(Class<?> clazz, Map<String, Attribute> attrs) {
    	Method[] methods = clazz.getMethods();
    	for (Method method : methods) {
    		String name = method.getName();
            if (name.startsWith("get")) {
                String fieldName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                assert !attrs.containsKey(fieldName) : "Mismatch in " + clazz.getSimpleName() + " for " + name;
            } else if (name.startsWith("is")) {
                String fieldName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                assert !attrs.containsKey(fieldName) : "Mismatch in " + clazz.getSimpleName() + " for " + name;
            } else if (name.startsWith("set")) {
                String fieldName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                assert !attrs.containsKey(fieldName) : "Mismatch in " + clazz.getSimpleName() + " for " + name;
            }
    	}
    	return true;
    	
    }
    
    protected void buildAttributes(Class<?> clazz, String tableName, AttributeOverride[] overrides, boolean embedded, boolean isId) {
        if (!embedded && clazz.getAnnotation(Entity.class) == null) {
            return;
        }
        
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            buildAttributes(parent, DbUtil.getTableName(parent), DbUtil.getAttributeOverrides(parent), false, false);
        }
        
        if (!embedded) {
            _tables.add(clazz);
            _ids.put(tableName, new ArrayList<Attribute>());
        }
        
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            
            if (!DbUtil.isPersistable(field)) {
                continue;
            }
            
            if (field.getAnnotation(Embedded.class) != null) {
                _embeddeds.add(field);
                Class<?> embeddedClass = field.getType();
                assert (embeddedClass.getAnnotation(Embeddable.class) != null) : "Class is not annotated with Embeddable: " + embeddedClass.getName();
                buildAttributes(embeddedClass, tableName, DbUtil.getAttributeOverrides(field), true, false);
                continue;
            }
            
            if (field.getAnnotation(EmbeddedId.class) != null) {
                _embeddeds.add(field);
                Class<?> embeddedClass = field.getType();
                assert (embeddedClass.getAnnotation(Embeddable.class) != null) : "Class is not annotated with Embeddable: " + embeddedClass.getName();
                buildAttributes(embeddedClass, tableName, DbUtil.getAttributeOverrides(field), true, true);
                continue;
            }
            
            TableGenerator tg = field.getAnnotation(TableGenerator.class);
            if (tg != null) {
                _generators.put(field.getName(), tg);
            }
            
            Attribute attr = new Attribute(clazz, overrides, field, tableName, embedded, isId);
            
            if (attr.isId()) {
                List<Attribute> attrs = _ids.get(tableName);
                attrs.add(attr);
            }
            
            _attributes.add(attr);
        }
    }
    
    public Map<String, TableGenerator> getTableGenerators() {
        return _generators;
    }
    
    protected void handleDaoAttributes(Class<?> clazz) {
        Attribute attr;
        Class<?> current = clazz;
        while (current != null && current.getAnnotation(Entity.class) != null) {
            DiscriminatorColumn column = current.getAnnotation(DiscriminatorColumn.class);
            if (column != null) {
                String columnName = column.name();
                attr = findAttribute(columnName);
                if (attr != null) {
                    attr.setTrue(Attribute.Flag.DaoGenerated);
                    attr.setTrue(Attribute.Flag.Insertable);
                    attr.setTrue(Attribute.Flag.Updatable);
                    attr.setFalse(Attribute.Flag.Nullable);
                    attr.setTrue(Attribute.Flag.DC);
                } else {
                    attr = new Attribute(DbUtil.getTableName(current), column.name());
                    attr.setFalse(Flag.Selectable);
                    attr.setTrue(Flag.Insertable);
                    attr.setTrue(Flag.DaoGenerated);
                    attr.setTrue(Flag.DC);
                    _attributes.add(attr);
                }
                if (column.discriminatorType() == DiscriminatorType.CHAR) {
                    attr.setTrue(Attribute.Flag.CharDT);
                } else if (column.discriminatorType() == DiscriminatorType.STRING) {
                    attr.setTrue(Attribute.Flag.StringDT);
                } else if (column.discriminatorType() == DiscriminatorType.INTEGER) {
                    attr.setTrue(Attribute.Flag.IntegerDT);
                }
            }
            
            PrimaryKeyJoinColumn[] pkjcs = DbUtil.getPrimaryKeyJoinColumns(current);
            if (pkjcs != null) {
                for (PrimaryKeyJoinColumn pkjc : pkjcs) {
                    String tableName = DbUtil.getTableName(current);
                    attr = findAttribute(pkjc.name());
                    if (attr == null || !tableName.equals(attr.table)) {
                        Attribute id = new Attribute(DbUtil.getTableName(current), pkjc.name());
                        if (pkjc.referencedColumnName().length() > 0) {
                            attr = findAttribute(pkjc.referencedColumnName());
                            assert (attr != null) : "Couldn't find referenced column name " + pkjc.referencedColumnName();
                        }
                        id.field = attr.field;
                        id.setTrue(Flag.Id);
                        id.setTrue(Flag.Insertable);
                        id.setFalse(Flag.Updatable);
                        id.setFalse(Flag.Nullable);
                        id.setFalse(Flag.Selectable);
                        _attributes.add(id);
                        List<Attribute> attrs = _ids.get(id.table);
                        attrs.add(id);
                    }
                }
            }
            current = current.getSuperclass();
        }
        
        attr = findAttribute(GenericDao.CREATED_COLUMN);
        if (attr != null && attr.field.getType() == Date.class) {
            attr.setTrue(Attribute.Flag.DaoGenerated);
            attr.setTrue(Attribute.Flag.Insertable);
            attr.setFalse(Attribute.Flag.Updatable);
            attr.setFalse(Attribute.Flag.Date);
            attr.setFalse(Attribute.Flag.Time);
            attr.setTrue(Attribute.Flag.TimeStamp);
            attr.setFalse(Attribute.Flag.Nullable);
            attr.setTrue(Attribute.Flag.Created);
        }
        
        attr = findAttribute(GenericDao.REMOVED_COLUMN);
        if (attr != null && attr.field.getType() == Date.class) {
            attr.setTrue(Attribute.Flag.DaoGenerated);
            attr.setFalse(Attribute.Flag.Insertable);
            attr.setFalse(Attribute.Flag.Updatable);
            attr.setTrue(Attribute.Flag.TimeStamp);
            attr.setFalse(Attribute.Flag.Time);
            attr.setFalse(Attribute.Flag.Date);
            attr.setTrue(Attribute.Flag.Nullable);
            attr.setTrue(Attribute.Flag.Removed);
        }
    }
    
    public Attribute findAttribute(String name) {
        for (Attribute attr : _attributes) {
            if (attr.columnName == name || attr.columnName.equals(name)) {
                return attr;
            }
        }
        
        return null;
    }
    
    public static StringBuilder buildUpdateSql(String tableName, List<Attribute> attrs) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tableName).append(" SET ");
        for (Attribute attr : attrs) {
            sql.append(attr.columnName).append(" = ?, ");
        }
        sql.delete(sql.length() - 2, sql.length());
        sql.append(" WHERE ");
        
        return sql;
    }
    
    public List<Pair<StringBuilder, Attribute[]>> buildUpdateSqls() {
        ArrayList<Pair<StringBuilder, Attribute[]>> sqls = new ArrayList<Pair<StringBuilder, Attribute[]>>(_tables.size());
        for (Class<?> table : _tables) {
            ArrayList<Attribute> attrs = new ArrayList<Attribute>();
            String tableName = DbUtil.getTableName(table);
            for (Attribute attr : _attributes) {
                if (attr.isUpdatable() && tableName.equals(attr.table)) {
                    attrs.add(attr);
                }
            }
            if (attrs.size() != 0) {
                Pair<StringBuilder, Attribute[]> pair =
                    new Pair<StringBuilder, Attribute[]>(buildUpdateSql(tableName, attrs), attrs.toArray(new Attribute[attrs.size()]));
                sqls.add(pair);
            }
        }
        return sqls;
    }
    
    public static StringBuilder buildMysqlUpdateSql(String joins, Collection<Ternary<Attribute, Boolean, Object>> setters) {
    	if (setters.size() == 0) {
    		return null;
    	}
    	
        StringBuilder sql = new StringBuilder("UPDATE ");
        
        sql.append(joins);
        
    	sql.append(" SET ");
    	
        for (Ternary<Attribute, Boolean, Object> setter : setters) {
        	Attribute attr = setter.first();
            sql.append(attr.table).append(".").append(attr.columnName).append("=");
            if (setter.second() != null) {
            	sql.append(attr.table).append(".").append(attr.columnName).append(setter.second() ? "+" : "-");
            }
            sql.append("?, ");
        }

        sql.delete(sql.length() - 2, sql.length());
        
        sql.append(" WHERE ");
        
        return sql;
    }
    
    public List<Pair<String, Attribute[]>> buildInsertSqls() {
        LinkedHashMap<String, ArrayList<Attribute>> map = new LinkedHashMap<String, ArrayList<Attribute>>();
        for (Class<?> table : _tables) {
            map.put(DbUtil.getTableName(table), new ArrayList<Attribute>());
        }
        
        for (Attribute attr : _attributes) {
            if (attr.isInsertable()) {
                ArrayList<Attribute> attrs = map.get(attr.table);
                assert (attrs != null) : "Null set of attributes for " + attr.table;
                attrs.add(attr);
            }
        }
        
        List<Pair<String, Attribute[]>> sqls = new ArrayList<Pair<String, Attribute[]>>(map.size());
        for (Map.Entry<String, ArrayList<Attribute>> entry : map.entrySet()) {
            ArrayList<Attribute> attrs = entry.getValue();
            StringBuilder sql = buildInsertSql(entry.getKey(), attrs);
            Pair<String, Attribute[]> pair = new Pair<String, Attribute[]>(sql.toString(), attrs.toArray(new Attribute[attrs.size()]));
            sqls.add(pair);
        }
        
        return sqls;
    }
    
    protected StringBuilder buildInsertSql(String table, ArrayList<Attribute> attrs) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(table).append(" (");
        for (Attribute attr : attrs) {
            sql.append(table).append(".").append(attr.columnName).append(", ");
        }
        if (attrs.size() > 0) {
            sql.delete(sql.length() - 2, sql.length());
        }
        
        sql.append(") VALUES (");
        for (Attribute attr : attrs) {
            sql.append("?, ");
        }
        
        if (attrs.size() > 0) {
            sql.delete(sql.length() - 2, sql.length());
        }
        
        sql.append(")");
        
        return sql;
    }
    
    protected List<Pair<String, Attribute[]>> buildDeleteSqls() {
        LinkedHashMap<String, ArrayList<Attribute>> map = new LinkedHashMap<String, ArrayList<Attribute>>();
        for (Class<?> table : _tables) {
            map.put(DbUtil.getTableName(table), new ArrayList<Attribute>());
        }
        
        for (Attribute attr : _attributes) {
            if (attr.isId()) {
                ArrayList<Attribute> attrs = map.get(attr.table);
                assert (attrs != null) : "Null set of attributes for " + attr.table;
                attrs.add(attr);
            }
        }
        
        List<Pair<String, Attribute[]>> sqls = new ArrayList<Pair<String, Attribute[]>>(map.size());
        for (Map.Entry<String, ArrayList<Attribute>> entry : map.entrySet()) {
            ArrayList<Attribute> attrs = entry.getValue();
            String sql = buildDeleteSql(entry.getKey(), attrs);
            Pair<String, Attribute[]> pair = new Pair<String, Attribute[]>(sql, attrs.toArray(new Attribute[attrs.size()]));
            sqls.add(pair);
        }
        
        Collections.reverse(sqls);
        return sqls;
    }
    
    protected String buildDeleteSql(String table, ArrayList<Attribute> attrs) {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(table).append(" WHERE ");
        for (Attribute attr : attrs) {
            sql.append(table).append(".").append(attr.columnName).append("= ? AND ");
        }
        sql.delete(sql.length() - 5, sql.length());
        return sql.toString();
    }
    
    public Pair<String, Attribute[]> buildRemoveSql() {
        Attribute attribute = findAttribute(GenericDao.REMOVED_COLUMN);
        if (attribute == null) {
            return null;
        }
        
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(attribute.table).append(" SET ");
        sql.append(attribute.columnName).append(" = ? WHERE ");
        
        List<Attribute> ids = _ids.get(attribute.table);
        
        // if ids == null, that means the removed column was added as a JOIN
        // value to another table.  We ignore it here.
        if (ids == null) {
        	return null;
        }
        if (ids.size() == 0) {
            return null;
        }
        
        for (Attribute id : ids) {
            sql.append(id.table).append(".").append(id.columnName).append(" = ? AND ");
        }
        
        sql.delete(sql.length() - 5, sql.length());
        
        Attribute[] attrs = ids.toArray(new Attribute[ids.size() + 1]);
        attrs[attrs.length - 1] = attribute;
        
        return new Pair<String, Attribute[]>(sql.toString(), attrs);
    }
    
    public Map<String, Attribute[]> getIdAttributes() {
        LinkedHashMap<String, Attribute[]> ids = new LinkedHashMap<String, Attribute[]>(_ids.size());
     
        for (Map.Entry<String, List<Attribute>> entry : _ids.entrySet()) {
            ids.put(entry.getKey(), entry.getValue().toArray(new Attribute[entry.getValue().size()]));
        }
        
        return ids;
    }
    
    /**
     * @return a map of tables and maps of field names to attributes.
     */
    public Map<String, Attribute> getAllAttributes() {
        Map<String, Attribute> attrs = new LinkedHashMap<String, Attribute>(_attributes.size());
        for (Attribute attr : _attributes) {
            if (attr.field != null) {
                attrs.put(attr.field.getName(), attr);
            }
        }
        
        return attrs;
    }
    
    public Map<Pair<String, String>, Attribute> getAllColumns() {
        Map<Pair<String, String>, Attribute> attrs = new LinkedHashMap<Pair<String, String>, Attribute>(_attributes.size());
        for (Attribute attr : _attributes) {
            if (attr.columnName != null) {
                attrs.put(new Pair<String, String>(attr.table, attr.columnName), attr);
            }
        }
        
        return attrs;
    }
    
    protected static void addPrimaryKeyJoinColumns(StringBuilder sql, String fromTable, String toTable, PrimaryKeyJoinColumn[] pkjcs) {
        sql.append(" INNER JOIN ").append(toTable).append(" ON ");
        for (PrimaryKeyJoinColumn pkjc : pkjcs) {
            sql.append(fromTable).append(".").append(pkjc.name());
            String refColumn = DbUtil.getReferenceColumn(pkjc);
            sql.append("=").append(toTable).append(".").append(refColumn).append(" ");
        }
    }
    
    public Pair<String, Attribute> getRemovedAttribute() {
        Attribute removed = findAttribute(GenericDao.REMOVED_COLUMN);
        if (removed == null) {
            return null;
        }
        
        if (removed.field.getType() != Date.class) {
            return null;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append(removed.table).append(".").append(removed.columnName).append(" IS NULL ");
        
        return new Pair<String, Attribute>(sql.toString(), removed);
    }
    
    protected static void buildJoins(StringBuilder innerJoin, Class<?> clazz) {
        String tableName = DbUtil.getTableName(clazz);
        
        SecondaryTable[] sts = DbUtil.getSecondaryTables(clazz);
        ArrayList<String> secondaryTables = new ArrayList<String>();
        for (SecondaryTable st : sts) {
            addPrimaryKeyJoinColumns(innerJoin, tableName, st.name(), st.pkJoinColumns());
            secondaryTables.add(st.name());
        }
        
        Class<?> parent = clazz.getSuperclass();
        if (parent.getAnnotation(Entity.class) != null) {
            String table = DbUtil.getTableName(parent);
            PrimaryKeyJoinColumn[] pkjcs = DbUtil.getPrimaryKeyJoinColumns(clazz);
            assert (pkjcs != null) : "No Join columns specified for the super class";
            addPrimaryKeyJoinColumns(innerJoin, tableName, table, pkjcs);
        }
    }

    
    public String buildTableReferences() {
    	StringBuilder sql = new StringBuilder();
        sql.append(DbUtil.getTableName(_tables.get(_tables.size() - 1)));
        
        for (Class<?> table : _tables) {
            buildJoins(sql, table);
        }
        
        return sql.toString();
    }
    
    public Pair<StringBuilder, Attribute[]> buildSelectSql() {
        StringBuilder sql = new StringBuilder("SELECT ");
        
        ArrayList<Attribute> attrs = new ArrayList<Attribute>();
        
        for (Attribute attr : _attributes) {
            if (attr.isSelectable()) {
                attrs.add(attr);
                sql.append(attr.table).append(".").append(attr.columnName).append(", ");
            }
        }
        
        if (attrs.size() > 0) {
            sql.delete(sql.length() - 2, sql.length());
        }
        
        sql.append(" FROM ").append(buildTableReferences());
        
        sql.append(" WHERE ");
        
        sql.append(buildDiscriminatorClause().first());
        
        return new Pair<StringBuilder, Attribute[]>(sql, attrs.toArray(new Attribute[attrs.size()]));
    }
    
    public Pair<StringBuilder, Attribute[]> buildSelectSql(Attribute[] attrs) {
        StringBuilder sql = new StringBuilder("SELECT ");
        
        for (Attribute attr : attrs) {
            sql.append(attr.table).append(".").append(attr.columnName).append(", ");
        }
        
        if (attrs.length > 0) {
            sql.delete(sql.length() - 2, sql.length());
        }
        
        sql.append(" FROM ").append(buildTableReferences());
        
        sql.append(" WHERE ");
        
        sql.append(buildDiscriminatorClause().first());
        
        return new Pair<StringBuilder, Attribute[]>(sql, attrs);
    }
    
    /**
     * buildDiscriminatorClause builds the join clause when there are multiple tables.
     * 
     * @return
     */
    public Pair<StringBuilder, Map<String, Object>> buildDiscriminatorClause() {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> values = new HashMap<String, Object>();
        
        for (Class<?> table : _tables) {
            DiscriminatorValue dv = table.getAnnotation(DiscriminatorValue.class);
            if (dv != null) {
                Class<?> parent = table.getSuperclass();
                DiscriminatorColumn dc = parent.getAnnotation(DiscriminatorColumn.class);
                assert(dc != null) : "Parent does not have discrminator column: " + parent.getName();
                sql.append(dc.name()).append("=");
                Object value = null;
                if (dc.discriminatorType() == DiscriminatorType.INTEGER) {
                    sql.append(dv.value());
                    value = Integer.parseInt(dv.value());
                } else if (dc.discriminatorType() == DiscriminatorType.CHAR) {
                    sql.append(dv.value());
                    value = dv.value().charAt(0);
                } else if (dc.discriminatorType() == DiscriminatorType.STRING) {
                    String v = dv.value();
                    v = v.substring(0, v.length() < dc.length() ? v.length() : dc.length());
                    sql.append("'").append(v).append("'");
                    value = v;
                }
                values.put(dc.name(), value);
                sql.append(" AND ");
                
            }
        }
        
        return new Pair<StringBuilder, Map<String, Object>>(sql, values);
    }
    
    public Field[] getEmbeddedFields() {
        return _embeddeds.toArray(new Field[_embeddeds.size()]);
    }
}
