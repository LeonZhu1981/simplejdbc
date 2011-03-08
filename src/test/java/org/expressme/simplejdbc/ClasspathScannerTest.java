package org.expressme.simplejdbc;

import static org.junit.Assert.*;

import java.util.Set;

import org.expressme.test.Product;
import org.expressme.test.User;
import org.expressme.test2.Employee;
import org.expressme.test2.Job;
import org.junit.Test;

public class ClasspathScannerTest {

    @Test
    public void testScan() {
        Class<?>[] classes = { Product.class, User.class };
        Set<Class<?>> set = new ClasspathScanner("org.expressme.test").scan();
        for (Class<?> cls : classes)
            assertTrue(set.contains(cls));
    }

    @Test
    public void testScan2() {
        Class<?>[] classes = { Employee.class, Job.class };
        Set<Class<?>> set = new ClasspathScanner("org.expressme.test2").scan();
        for (Class<?> cls : classes)
            assertTrue(set.contains(cls));
    }

}
