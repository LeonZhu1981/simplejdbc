package org.expressme.simplejdbc;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.expressme.test.User;
import org.expressme.test2.Job;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DbTest2 {

    static long id = System.currentTimeMillis();
    Db db = null;

    @Before
    public void setUp() throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("DbTest2.xml");
        DataSource dataSource = context.getBean(DataSource.class);
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("drop table if exists User");
        stmt.execute("drop table if exists Job");
        stmt.execute("create table User (id bigint not null primary key, name varchar(50) not null, passwd varchar(50) not null, css_style_name varchar(50) null)");
        stmt.execute("create table Job (id int not null primary key, title varchar(50) not null)");
        stmt.close();
        conn.close();
        db = context.getBean(Db.class);
    }

    @Test
    public void testQueryForList() {
        List<User> us = db.queryForList("select * from User where name=?", "name-not-exist!");
        assertTrue(us.isEmpty());
        List<Job> js = db.queryForList("select * from Job where title=?", "software-architect");
        assertTrue(js.isEmpty());
    }

}
