package org.expressme.simplejdbc;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.expressme.test.User;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DbTest {

    static long id = System.currentTimeMillis();
    Db db = null;

    @Before
    public void setUp() throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("DbTest.xml");
        DataSource dataSource = context.getBean(DataSource.class);
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("drop table if exists User");
        stmt.execute("create table User (id bigint not null primary key, name varchar(50) not null, passwd varchar(50) not null, css_style_name varchar(50) null)");
        stmt.close();
        conn.close();
        db = context.getBean(Db.class);
    }

    @Test
    public void testQueryForList() {
        final long ID = id++;
        User[] users = {
                new User(ID, "query_for_list", "password+"),
                new User(id++, "query_for_list", "password+"),
                new User(id++, "query_for_list", "password+"),
        };
        for (User user : users) {
            db.create(user);
        }
        List<User> us = db.queryForList("select * from User where name=? and id>=?", "query_for_list", ID);
        assertEquals(users.length, us.size());
        for (User u : us) {
            assertEquals("query_for_list", u.getName());
            assertEquals("password+", u.getPasswd());
        }
    }

    @Test
    public void testExecuteUpdate() {
        final long ID = id++;
        User user = new User(ID, "execute_update", "old-password");
        db.create(user);
        db.executeUpdate("update User set passwd=?", "new-password");
        User u = db.getById(User.class, ID);
        assertNotNull(u);
        assertEquals("new-password", u.getPasswd());
    }

    @Test
    public void testDeleteEntity() {
        final long ID = id++;
        User user = new User(ID, "delete_entity", "password_d");
        db.create(user);
        User u = new User(ID, "", "");
        db.deleteEntity(u);
        assertNull(db.getById(User.class, ID));
    }

    @Test
    public void testUpdateEntity() {
        final long ID = id++;
        User user = new User(ID, "to-be-update-entity", "old-password");
        db.create(user);
        User u = new User(ID, "updated-entity", "updated-password");
        db.updateEntity(u);
        User ug = db.getById(User.class, ID);
        assertNotNull(ug);
        assertEquals(ug.getName(), u.getName());
        assertEquals(ug.getPasswd(), u.getPasswd());
    }

    @Test
    public void testUpdateProperties() {
        final long ID = id++;
        User user = new User(ID, "to-be-update-entity", "old-password");
        db.create(user);
        User u = new User(ID, "updated-entity", "updated-password");
        db.updateProperties(u, "name");
        User ug = db.getById(User.class, ID);
        assertNotNull(ug);
        assertEquals(ug.getName(), u.getName());
        assertEquals(ug.getPasswd(), user.getPasswd());
    }

    @Test
    public void testQueryForLong() {
        final long ID = id++;
        User[] users = {
                new User(ID, "query_for_long", "password+"),
                new User(id++, "query_for_long", "password+"),
                new User(id++, "query_for_long", "password+"),
                new User(id++, "query_for_long", "password+"),
        };
        for (User user : users) {
            db.create(user);
        }
        assertEquals(users.length, db.queryForLong("select count(*) from User where name=? and id>=?", "query_for_long", ID));
    }

    @Test
    public void testQueryForInt() {
        final long ID = id++;
        User[] users = {
                new User(ID, "query_for_int", "password+"),
                new User(id++, "query_for_int", "password+"),
                new User(id++, "query_for_int", "password+"),
                new User(id++, "query_for_int", "password+"),
                new User(id++, "query_for_int", "password+"),
        };
        for (User user : users) {
            db.create(user);
        }
        assertEquals(users.length, db.queryForLong("select count(*) from User where name=? and id>=?", "query_for_int", ID));
    }

    @Test
    public void testQueryForObject() {
        final long ID = id++;
        User user = new User(ID, "query_for_object", "password_abc");
        db.create(user);
        User u = db.queryForObject("select * from User where name=? and id>=?", "query_for_object", ID);
        assertNotNull(u);
        assertEquals(user.getId(), u.getId());
        assertEquals(user.getName(), u.getName());
        assertEquals(user.getPasswd(), u.getPasswd());
    }

    @Test
    public void testQueryForLimitedList() {
        final long ID = id++;
        User[] users = {
                new User(ID, "query_for_limited_list", "password-0"),
                new User(id++, "query_for_limited_list", "password-1"),
                new User(id++, "query_for_limited_list", "password-2"),
                new User(id++, "query_for_limited_list", "password-3"),
                new User(id++, "query_for_limited_list", "password-4"),
        };
        for (User user : users) {
            db.create(user);
        }
        List<User> users_02 = db.queryForLimitedList("select * from User where id>=?", 0, 2, ID);
        assertEquals(2, users_02.size());
        assertEquals("password-0", users_02.get(0).getPasswd());
        assertEquals("password-1", users_02.get(1).getPasswd());

        List<User> users_22 = db.queryForLimitedList("select * from User where id>=?", 2, 2, ID);
        assertEquals(2, users_22.size());
        assertEquals("password-2", users_22.get(0).getPasswd());
        assertEquals("password-3", users_22.get(1).getPasswd());

        List<User> users_42 = db.queryForLimitedList("select * from User where id>=?", 4, 2, ID);
        assertEquals(1, users_42.size());
        assertEquals("password-4", users_42.get(0).getPasswd());
    }

    @Test
    public void testGetById() {
        final long ID = id++;
        User user = new User(ID, "get_by_id", "password_get_by_id");
        db.create(user);
        User u = db.getById(User.class, ID);
        assertNotNull(u);
        assertEquals(ID, u.getId());
        assertEquals(user.getName(), u.getName());
        assertEquals(user.getPasswd(), u.getPasswd());
    }

    @Test
    public void testCreate() {
        User user = new User(id++, "dbtest", "password");
        db.create(user);
    }

    @Test
    public void testDeleteById() {
        final long ID = id++;
        User user = new User(ID, "dbtest", "password");
        db.create(user);
        db.deleteById(User.class, ID);
    }

}
