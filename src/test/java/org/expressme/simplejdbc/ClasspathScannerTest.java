package org.expressme.simplejdbc;

import static org.junit.Assert.*;

import java.util.List;

import org.expressme.test.Product;
import org.expressme.test.User;
import org.expressme.test2.Employee;
import org.expressme.test2.Job;
import org.junit.Test;

public class ClasspathScannerTest {

    @Test
    public void testScan() {
        Class<?>[] classes = { Product.class, User.class };
        List<Class<?>> list = new ClasspathScanner("org.expressme.test").scan();
        assertArrayEquals(classes, list.toArray(new Class<?>[list.size()]));
    }

    @Test
    public void testScan2() {
        Class<?>[] classes = { Employee.class, Job.class };
        List<Class<?>> list = new ClasspathScanner("org.expressme.test2").scan();
        assertArrayEquals(classes, list.toArray(new Class<?>[list.size()]));
    }

}
