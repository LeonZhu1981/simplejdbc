package org.expressme.test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class User {

    long id;
    String name;
    String passwd;
    String cssStyleName;

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

    @Column(name="css_style_name")
    public String getCssStyleName() {
        return cssStyleName;
    }

    public void setCssStyleName(String cssStyleName) {
        this.cssStyleName = cssStyleName;
    }

}
