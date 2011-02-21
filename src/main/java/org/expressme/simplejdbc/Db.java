package org.expressme.simplejdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Database interface.
 * 
 * @author Liao Xuefeng
 */
public class Db implements InitializingBean {

    final Log log = LogFactory.getLog(getClass());

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    String packageName;

    Map<String, EntityOperation<?>> entityMap = new ConcurrentHashMap<String, EntityOperation<?>>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void afterPropertiesSet() throws Exception {
        // scan package:
        List<Class<?>> classes = new ClasspathScanner(packageName, new ClasspathScanner.ClassFilter() {
            public boolean accept(Class<?> clazz) {
                return clazz.isAnnotationPresent(Entity.class);
            }
        }).scan();
        for (Class<?> clazz : classes) {
            entityMap.put(clazz.getName(), new EntityOperation(clazz));
        }
    }

    EntityOperation<?> getEntityOperation(Class<?> entityClass) {
        return getEntityOperation(entityClass.getName());
    }

    EntityOperation<?> getEntityOperation(String entityClassName) {
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

    public <T> List<T> queryForList(String sql, Object... params) {
        log.info("Query for list: " + sql);
        Matcher m = SELECT_FROM.matcher(sql);
        if ( ! m.matches()) {
            throw new DbException("SQL gramma error: " + sql);
        }
        String entityClassName = this.packageName + '.' + m.group(3);
        EntityOperation<?> op = getEntityOperation(entityClassName);
        jdbcTemplate.query(sql, op.rowMapper);
        List<T> list = new LinkedList<T>();
        return list;
    }

    public int executeUpdate(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }

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

    public long queryForLong(String sql, Object... args) {
        log.info("Query for long: " + sql);
        List<Long> list = jdbcTemplate.query(sql, args, longRowMapper);
        if (list.isEmpty())
            throw new DbException("empty results.");
        if (list.size() > 1)
            throw new DbException("non-unique results.");
        return list.get(0);
    }

    public int queryForInt(String sql, Object... args) {
        log.info("Query for int: " + sql);
        List<Integer> list = jdbcTemplate.query(sql, args, intRowMapper);
        if (list.isEmpty())
            throw new DbException("empty results.");
        if (list.size() > 1)
            throw new DbException("non-unique results.");
        return list.get(0);
    }

    public <T> T queryForObject(String sql, Object... args) {
        log.info("Query for object: " + sql);
        List<T> list = queryForList(sql, args);
        if (list.isEmpty())
            return null;
        if (list.size() > 1)
            throw new DbException("non-unique results.");
        return list.get(0);
    }

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

    public <T> T getById(Class<T> clazz, Object idValue) {
        EntityOperation<?> op = getEntityOperation(clazz.getName());
        SQLOperation sqlo = op.getById(idValue);
        return queryForObject(sqlo.sql, sqlo.params);
    }

    /**
     * Create an entity in database.
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
     * @param clazz Entity class.
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
