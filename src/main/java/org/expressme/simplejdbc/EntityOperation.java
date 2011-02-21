package org.expressme.simplejdbc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.springframework.jdbc.core.RowMapper;

class EntityOperation<T> {

    final String tableName;
    final Class<T> entityClass;
    final String idProperty;
    final Map<String, PropertyMapping> mappings;
    final RowMapper<T> rowMapper;

    public EntityOperation(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.tableName = getTableName(entityClass);
        Map<String, Method> getters = Utils.findPublicGetters(entityClass);
        Map<String, Method> setters = Utils.findPublicSetters(entityClass);
        this.idProperty = findIdProperty(getters);
        this.mappings = getPropertyMappings(getters, setters);
        this.rowMapper = createRowMapper();
    }

    RowMapper<T> createRowMapper() {
        return new RowMapper<T>() {
            public T mapRow(ResultSet rs, int rowNum) throws SQLException {
                try {
                    T t = entityClass.newInstance();
                    ResultSetMetaData meta = rs.getMetaData();
                    int columns = meta.getColumnCount();
                    for (int i=1; i<=columns; i++) {
                        Object value = rs.getObject(i);
                        if (value!=null) {
                            PropertyMapping pm = mappings.get(meta.getColumnName(i));
                            if (pm!=null) {
                                pm.set(t, value);
                            }
                        }
                    }
                    return t;
                }
                catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getCause());
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    String getTableName(Class<?> entityClass) {
        String name = entityClass.getSimpleName();
        Table table = entityClass.getAnnotation(Table.class);
        if (table!=null && ! "".equals(table.name()))
            name = table.name();
        return name;
    }

    String findIdProperty(Map<String, Method> getters) {
        String idProperty = null;
        for (String property : getters.keySet()) {
            Method getter = getters.get(property);
            Id id = getter.getAnnotation(Id.class);
            if (id!=null) {
                if (idProperty!=null)
                    throw new DbException("Duplicate @Id detected.");
                idProperty = property;
            }
        }
        if (idProperty==null)
            throw new DbException("Missing @Id.");
        return idProperty;
    }

    Map<String, PropertyMapping> getPropertyMappings(Map<String, Method> getters, Map<String, Method> setters) {
        Map<String, PropertyMapping> mappings = new HashMap<String, PropertyMapping>();
        for (String property : getters.keySet()) {
            Method getter = getters.get(property);
            if (getter.isAnnotationPresent(Transient.class))
                continue;
            Method setter = setters.get(property);
            if (setter==null)
                throw new DbException("Missing setter while getter " + getter.getName() + " found.");
            mappings.put(property, new PropertyMapping(getter, setter));
        }
        return mappings;
    }

    //-- select * from TABLE where id=? ---------------------------------------

    String SQL_SELECT_BY_ID = null;

    SQLOperation getById(Object idValue) {
        if (SQL_SELECT_BY_ID==null) {
            SQL_SELECT_BY_ID = "select * from " + this.tableName + " where " + mappings.get(this.idProperty).columnName + "=?";
        }
        return new SQLOperation(SQL_SELECT_BY_ID, idValue);
    }

    //-- delete from TABLE where id=? -----------------------------------------

    String SQL_DELETE_BY_ID = null;

    SQLOperation deleteEntity(Object entity) throws Exception {
        return deleteById(mappings.get(this.idProperty).get(entity));
    }

    SQLOperation deleteById(Object idValue) {
        if (SQL_DELETE_BY_ID==null) {
            SQL_DELETE_BY_ID = "delete from " + this.tableName + " where " + mappings.get(this.idProperty).columnName + "=?";
        }
        return new SQLOperation(SQL_DELETE_BY_ID, idValue);
    }

    //-- insert into TABLE (a,b,c) values (?,?,?) -----------------------------

    String SQL_INSERT = null;
    String[] INSERT_PROPERTIES = null;

    SQLOperation insertEntity(Object entity) throws Exception {
        if (SQL_INSERT==null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("insert into ").append(this.tableName).append(" (");
            String[] properties = this.mappings.keySet().toArray(new String[0]);
            Arrays.sort(properties);
            List<String> insertableProperties = new LinkedList<String>();
            for (String property : properties) {
                PropertyMapping pm = mappings.get(property);
                if (pm.insertable) {
                    insertableProperties.add(property);
                    sb.append(pm.columnName).append(',');
                }
            }
            // set last ',' to ')':
            sb.setCharAt(sb.length()-1, ')');
            sb.append(" values (");
            for (int i=0; i<insertableProperties.size(); i++) {
                sb.append("?,");
            }
            // set last ',' to ')':
            sb.setCharAt(sb.length()-1, ')');
            SQL_INSERT = sb.toString();
            INSERT_PROPERTIES = insertableProperties.toArray(new String[insertableProperties.size()]);
        }
        Object[] params = new Object[INSERT_PROPERTIES.length];
        for (int i=0; i<INSERT_PROPERTIES.length; i++) {
            params[i] = mappings.get(INSERT_PROPERTIES[i]).get(entity);
        }
        return new SQLOperation(SQL_INSERT, params);
    }

    //-- update TABLE set a=?,b=?,c=? where id=? ------------------------------

    String SQL_UPDATE_BY_ID = null;
    String[] UPDATE_PROPERTIES = null;

    SQLOperation updateEntity(Object entity) throws Exception {
        if (SQL_UPDATE_BY_ID==null) {
            StringBuilder sb = new StringBuilder(64);
            sb.append("update ").append(this.tableName).append(" set ");
            String[] properties = this.mappings.keySet().toArray(new String[0]);
            Arrays.sort(properties);
            List<String> updatableProperties = new LinkedList<String>();
            for (String property : properties) {
                if ( ! property.equals(idProperty)) {
                    PropertyMapping pm = mappings.get(property);
                    if (pm.updatable) {
                        updatableProperties.add(property);
                        sb.append(pm.columnName).append("=?,");
                    }
                }
            }
            // delete last ',':
            sb.deleteCharAt(sb.length()-1);
            sb.append(" where ").append(this.mappings.get(this.idProperty).columnName).append("=?");
            SQL_UPDATE_BY_ID = sb.toString();
            UPDATE_PROPERTIES = updatableProperties.toArray(new String[updatableProperties.size()]);
        }
        Object[] params = new Object[UPDATE_PROPERTIES.length+1];
        for (int i=0; i<UPDATE_PROPERTIES.length; i++) {
            params[i] = this.mappings.get(UPDATE_PROPERTIES[i]).get(entity);
        }
        params[UPDATE_PROPERTIES.length] = this.mappings.get(idProperty).get(entity);
        return new SQLOperation(SQL_UPDATE_BY_ID, params);
    }

    SQLOperation updateProperties(Object entity, String... properties) throws Exception {
        StringBuilder sb = new StringBuilder(64);
        sb.append("update ").append(this.tableName).append(" set ");
        for (String property : properties) {
            PropertyMapping pm = mappings.get(property);
            if ( ! pm.updatable)
                throw new DbException("Could not update property " + property + " because its updatable=false.");
            sb.append(pm.columnName).append("=?,");
        }
        // delete last ',':
        sb.deleteCharAt(sb.length()-1);
        sb.append(" where ").append(this.mappings.get(this.idProperty).columnName).append("=?");
        Object[] params = new Object[properties.length+1];
        for (int i=0; i<properties.length; i++) {
            params[i] = mappings.get(properties[i]).get(entity);
        }
        params[properties.length] = mappings.get(idProperty).get(entity);
        return new SQLOperation(sb.toString(), params);
    }
}

class SQLOperation {

    final String sql;
    final Object[] params;

    public SQLOperation(String sql, Object... params) {
        this.sql = sql;
        this.params = params;
    }
}

class PropertyMapping {

    final boolean insertable;
    final boolean updatable;
    final String columnName;
    final boolean id;
    final Method getter;
    final Method setter;
    @SuppressWarnings("rawtypes")
    final Class enumClass;

    public PropertyMapping(Method getter, Method setter) {
        this.getter = getter;
        this.setter = setter;
        this.enumClass = getter.getReturnType().isEnum() ? getter.getReturnType() : null;
        Column column = getter.getAnnotation(Column.class);
        this.insertable = column==null ? true : column.insertable();
        this.updatable = column==null ? true : column.updatable();
        this.columnName = column==null ? Utils.getGetterName(getter) : ("".equals(column.name()) ? Utils.getGetterName(getter) : column.name());
        this.id = getter.isAnnotationPresent(Id.class);
    }

    @SuppressWarnings("unchecked")
    Object get(Object target) throws Exception {
        Object r = getter.invoke(target);
        return enumClass==null ? r : Enum.valueOf(enumClass, (String) r);
    }

    @SuppressWarnings("unchecked")
    void set(Object target, Object value) throws Exception {
        if (enumClass!=null && value!=null) {
            value = Enum.valueOf(enumClass, (String) value);
        }
        setter.invoke(target, value);
    }
}
