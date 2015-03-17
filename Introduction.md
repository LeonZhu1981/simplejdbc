# Why SimpleJdbc #

  * ORM is too heavy;
  * The XML configuration of ORM is too complex;
  * Cascade loading and CGLIB subclassing is not suitable for web application;
  * First / second level cache is not suitable for web application;
  * Writing DAO or RowMapper is tedious

# Design Principles #

  * Design for web, not enterprise;
  * Very simple API, but support strong type and generic list;
  * Contract is better than configuration;
  * Don't write any jdbc code, DAO class, RowMapper except your entity beans.

# Known Issues #

Only support MySQL if you use queryForLimitedList().