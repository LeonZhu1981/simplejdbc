package org.expressme.test;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Product {

    long id;
    String name;
    float price;
    Date expires;

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

}
