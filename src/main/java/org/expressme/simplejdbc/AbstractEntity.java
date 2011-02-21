package org.expressme.simplejdbc;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * Base entity class containing id, version, creationTime and modifiedTime.
 * 
 * @author Michael Liao
 */
public abstract class AbstractEntity implements Serializable {

    protected String id;
    protected int version;
    protected long creationTime;
    protected long modifiedTime;

    @Id
    @Column(nullable=false, updatable=false, length=32)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Version
    @Column(nullable=false)
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Column(nullable=false, updatable=false)
    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    @Column(nullable=false)
    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

}
