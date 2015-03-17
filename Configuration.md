# Runtime Environment #

  * Java 6
  * Spring 3.0
  * Web applicaiton (not an EAR)

# Config Spring #

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
                           http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-3.0.xsd
                           http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx-3.0.xsd
                           http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-3.0.xsd"
>
    <!-- your app should use jndi datasource or c3p0 rather than demo below -->
    <bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
        <property name="driverClassName" value="com.mysql.jdbc.Driver" />
        <property name="url" value="jdbc:mysql://localhost/test" />
        <property name="username" value="test" />
        <property name="password" value="test" />
    </bean>

    <!-- Spring's jdbcTemplate MUST be configured -->
    <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
        <property name="dataSource" ref="dataSource" />
    </bean>

    <!-- OPTIONAL: if you want to manage transaction in Spring -->
    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager" >
        <property name="dataSource" ref="dataSource" />
    </bean>

    <!-- the only singleton instance of Db, that's all -->
    <bean id="db" class="org.expressme.simplejdbc.Db">
        <!-- which package your entity classes located -->
        <property name="packageName" value="org.expressme.test" />
        <!-- inject the jdbcTemplate bean -->
        <property name="jdbcTemplate" ref="jdbcTemplate" />
    </bean>

    <!-- OPTIONAL: if you want to use @Transactional to manage transaction -->
    <tx:annotation-driven />

</beans>
```

That's all!

You can specify more than one package if entity beans are located in different packages:

```
    <bean id="db" class="org.expressme.simplejdbc.Db">
        <property name="packageNames">
            <list>
                <value>org.expressme.test1</value>
                <value>org.expressme.test2</value>
            </list>
        </property>
        <property name="jdbcTemplate" ref="jdbcTemplate" />
    </bean>
```