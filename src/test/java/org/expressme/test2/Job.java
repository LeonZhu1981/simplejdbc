package org.expressme.test2;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Job {

    int id;
    String title;

    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
