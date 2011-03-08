package org.expressme.simplejdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Database interface.
 * 
 * @author Michael Liao
 */
public class Db implements InitializingBean {

    final Log log = LogFactory.getLog(getClass());

    JdbcTemplate jdbcTemplate;

    String packageName;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    final Map<String, EntityOperation<?>> entityMap = new HashMap<String, EntityOperation<?>>();

    final Map<String, EntityOperation<?>> tableMap = new HashMap<String, EntityOperation<?>>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void afterPropertiesSet() throws Exception {
        log.info("Init db...");
        // scan package:
        Set<Class<?>> classes = new ClasspathScanner(packageName, new ClasspathScanner.ClassFilter() {
            public boolean accept(Class<?> clazz) {
                return clazz.isAnnotationPresent(Entity.class);
            }
        }).scan();
        for (Class<?> clazz : classes) {
            log.info("Found entity class: " + clazz.getName());
            EntityOperation<?> op = new EntityOperation(clazz);
            entityMap.put(clazz.getName(), op);
            tableMap.put(op.tableName, op);
        }
    }

    EntityOperation<?> getEntityOperation(Class<?> entityClass) {
        return getEntityOperationByEntityName(entityClass.getName());
    }

    EntityOperation<?> getEntityOperationByTableName(String tableName) {
        EntityOperation<?> op = tableMap.get(tableName);
        if (op==null) {
            throw new DbException("Unknown table: " + tableName);
        }
        return op;
    }

    EntityOperation<?> getEntityOperationByEntityName(String entityClassName) {
        EntityOperation<?> op = entityMap.get(entityClassName);
        if (op==null) {
            throw new DbException("Unknown entity: " + entityClassName);
        }
        return op;
    }

    final static RowMapper<Long> longRowMapper = new RowMapper<Long>() {
        public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getLong(1);
        }
    };

    final static RowMapper<Integer> intRowMapper = new RowMapper<Integer>() {
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt(1);
        }
    };

    final Pattern SELECT_FROM = Pattern.compile("^(select|SELECT) .* (from|FROM) +(\\w+) ?.*$");

    /**
     * Execute any update SQL statement.
     * 
     * @param sql SQL query.
     * @param params SQL parameters.
     * @return Number of affected rows.
     */
    public int executeUpdate(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }

    /**
     * Delete an entity by its id property. For example:
     * 
     * User user = new User(12300); // id=12300
     * db.deleteEntity(user);
     * 
     * @param entity Entity object instance.
     */
    public void deleteEntity(Object entity) {
        EntityOperation<?> op = getEntityOperation(entity.getClass());
        try {
            SQLOperation sqlo = op.deleteEntity(entity);
            jdbcTemplate.update(sqlo.sql, sqlo.params);
        }
        catch (Exception e) {
            throw new DbException(e);
        }
    }

    /**
     * Update the entity with all updatable properties.
     * 
     * @param entity Entity object instance.
     */
    public void updateEntity(Object entity) {
        EntityOperation<?> op = getEntityOperation(entity.getClass());
        try {
            SQLOperation sqlo = op.updateEntity(entity);
            jdbcTemplate.update(sqlo.sql, sqlo.params);
        }
        catch (Exception e) {
            throw new DbException(e);
        }
    }

    /**
     * Update the entity with specified properties.
     * 
     * @param entity Entity object instance.
     * @param properties Properties that are about to update.
     */
    public void updateProperties(Object entity, String... properties) {
        if (properties.length == 0)
            throw new DbException("Update properties required.");
        EntityOperation<?> op = getEntityOperation(entity.getClass());
        SQLOperation sqlo = null;
        try {
            sqlo = op.updateProperties(entity, properties);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        jdbcTemplate.update(sqlo.sql, sqlo.params);
    }

    /**
     * Query for long result. For example:
     * <code>
     * long count = db.queryForLong("select count(*) from User where age>?", 20);
     * </code>
     * 
     * @param sql SQL query statement.
     * @param args SQL query parameters.
     * @return Long result.
     */
    public long queryForLong(String sql, Object... args) {
        log.info("Query for long: " + sql);
        List<Long> list = jdbcTemplate.query(sql, args, longRowMapper);
        if (list.isEmpty())
            throw new DbException("empty results.");
        if (list.size() > 1)
            throw new DbException("non-unique results.");
        return list.get(0);
    }

    /**
     * Query for int result. For example:
     * <code>
     * int count = db.queryForLong("select count(*) from User where age>?", 20);
     * </code>
     * 
     * @param sql SQL query statement.
     * @param args SQL query parameters.
     * @return Int result.
     */
    public int queryForInt(String sql, Object... args) {
        log.info("Query for int: " + sql);
        List<Integer> list = jdbcTemplate.query(sql, args, intRowMapper);
        if (list.isEmpty())
            throw new DbException("empty results.");
        if (list.size() > 1)
            throw new DbException("non-unique results.");
        return list.get(0);
    }

    /**
     * Query for one single object. For example:
     * <code>
     * User user = db.queryForObject("select * from User where name=?", "Michael");
     * </code>
     * 
     * @param sql SQL query statement.
     * @param args SQL query parameters.
     * @return The only one single result, or null if no result.
     */
    public <T> T queryForObject(String sql, Object... args) {
        log.info("Query for object: " + sql);
        List<T> list = queryForList(sql, args);
        if (list.isEmpty())
            return null;
        if (list.size() > 1)
            throw new DbException("non-unique results.");
        return list.get(0);
    }

    /**
     * Query for list. For example:
     * <code>
     * List&lt;User&gt; users = db.queryForList("select * from User where age>?", 20);
     * </code>
     * 
     * @param <T> Return type of list element.
     * @param sql SQL query.
     * @param params SQL parameters.
     * @return List of query result.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList(String sql, Object... params) {
        log.info("Query for list: " + sql);
        Matcher m = SELECT_FROM.matcher(sql);
        if ( ! m.matches()) {
            throw new DbException("SQL grammar error: " + sql);
        }
        EntityOperation<?> op = getEntityOperationByTableName(m.group(3));
        return (List<T>) jdbcTemplate.query(sql, params, op.rowMapper);
    }

    /**
     * Query for limited list. For example:
     * <code>
     * // first 5 users:
     * List&lt;User&gt; users = db.queryForList("select * from User where age>?", 0, 5, 20);
     * </code>
     * 
     * @param <T> Return type of list element.
     * @param sql SQL query.
     * @param first First result index.
     * @param max Max results.
     * @param params SQL parameters.
     * @return List of query result.
     */
    public <T> List<T> queryForLimitedList(String sql, int first, int max, Object... args) {
        log.info("Query for limited list (first=" + first + ", max=" + max + "): " + sql);
        Object[] newArgs = new Object[args.length + 2];
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = args[i];
        }
        newArgs[newArgs.length - 2] = first;
        newArgs[newArgs.length - 1] = max;
        return queryForList(buildLimitedSelect(sql), newArgs);
    }

    /**
     * Get entity by its id.
     * 
     * @param <T> Entity class type.
     * @param clazz Entity class type.
     * @param idValue Id value.
     * @return Entity instance, or null if no such entity.
     */
    @SuppressWarnings("unchecked")
    public <T> T getById(Class<T> clazz, Object idValue) {
        EntityOperation<?> op = getEntityOperationByEntityName(clazz.getName());
        SQLOperation sqlo = op.getById(idValue);
        List<T> list = (List<T>) jdbcTemplate.query(sqlo.sql, sqlo.params, op.rowMapper);
        if (list.isEmpty())
            return null;
        if (list.size()>1)
            throw new DbException("non-unique results.");
        return list.get(0);
    }

    /**
     * Create an entity in database, writing all insertable properties.
     * 
     * @param entity Entity object instance.
     */
    public void create(Object entity) {
        EntityOperation<?> op = getEntityOperation(entity.getClass());
        SQLOperation sqlo = null;
        try {
            sqlo = op.insertEntity(entity);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        jdbcTemplate.update(sqlo.sql, sqlo.params);
    }

    /**
     * Delete an entity by its id value.
     * 
     * @param clazz Entity class type.
     * @param idValue Id value.
     */
    public void deleteById(Class<?> clazz, Object idValue) {
        EntityOperation<?> op = getEntityOperation(clazz);
        SQLOperation sqlo = op.deleteById(idValue);
        jdbcTemplate.update(sqlo.sql, sqlo.params);
    }

    String buildLimitedSelect(String select) {
        StringBuilder sb = new StringBuilder(select.length() + 20);
        boolean forUpdate = select.toLowerCase().endsWith(" for update");
        if (forUpdate) {
            sb.append(select.substring(0, select.length() - 11));
        }
        else {
            sb.append(select);
        }
        sb.append(" limit ?,?");
        if (forUpdate) {
            sb.append(" for update");
        }
        return sb.toString();
    }

}
