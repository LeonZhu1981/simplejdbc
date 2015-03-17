# Define Entity Bean #

```
package org.expressme.test;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class User {

    long id;
    String name;
    String passwd;

    public User() {}

    public User(long id, String name, String passwd) {
        this.id = id;
        this.name = name;
        this.passwd = passwd;
    }

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

NOTES:

  * Use annotation of javax.persistence;
  * MUST have @Entity on class declaration;
  * MUST have one and only one @Id;
  * @Column is optional, but @Column(name="field\_name") is required if filed name is not the same to property name;
  * Non-persist properties MUST have @Transient.

# Logic Code #

@Component
public class MyController {
> @Autowired Db db;
}

Logic code usually is Controller or Action in web applications.

# Query #

Query by id:

```
User user = db.getById(User.class, 12300);
if (user==null)
    throw new RuntimeException("user is not exist");
```

Query single object:

```
User user = db.queryForObject("select * from User where name=? and passwd=?", "Michael", "passwd");
if (user==null)
    throw new RuntimeException("invalid name or password");
```

Query for list:

```
List<User> users = db.queryForList("select * from User where id>?", 56700);
```

Query for list with pagination:

```
int firstResult = 0;
int maxResults = 5;
List<User> users = db.queryForList("select * from User where id>?", firstResult, maxResults, 56700);
```

Query for int or long type:

```
int i_count = db.queryForInt("select count(id) from User where id>?", 5000);
long l_count = db.queryForLong("select count(id) from User where id>?", 5000);
```

SimpleJdbc knows how to convert the result set of query to your entity bean or generic list. Class cast is not need. But the query must be:

  * simple query on single table;
  * not a join query.

These are good for your web application if you want to your web application can be scale out (such as database sharding).

SimpleJdbc will return original, non-proxy entity bean.

# Create #

```
User user = new User();
user.setId(123);
user.setName("Michael");
user.setPasswd("passwd");
db.create(user);
```

# Update #

Update all properties of bean except @Id and @Column(updatable=false):

```
User user = ...
db.updateEntity(user);
```

Update some properties of entity bean:

```
User user = ...
user.setName("Bob");
user.setPasswd("new-passwd");
db.updateProperties(user, "name", "passwd");
```

It is useful to update some properties of entity and it always generate short, fast SQL.

# Delete #

Delete by id:

```
db.deleteById(User.class, 12300);
```

Delete an entity:

```
User user = ...
db.deleteEntity(user);
```

# Execute Any SQL #

```
db.executeUpdate("update User set name=? where id in (?,?,?)", "Bob", 1, 2, 3);
db.executeUpdate("delete User where id>? and id<?", 2000, 3000);
```

# NOTES #

SimpleJdbc DO NOT have any cache. Each method invocation is a database operation. You should build your caching system using appropriate cache such as memcached.