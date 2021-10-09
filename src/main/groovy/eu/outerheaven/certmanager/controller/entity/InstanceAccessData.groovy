package eu.outerheaven.certmanager.controller.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MapsId
import javax.persistence.OneToOne

@Entity
class InstanceAccessData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id

    private String bearer_token

    private String xsrf_token

    private Long expires

    @OneToOne
    @MapsId
    private Instance instance

    Long getId() {
        return id
    }

    void setId(Long id) {
        this.id = id
    }

    String getBearer_token() {
        return bearer_token
    }

    void setBearer_token(String bearer_token) {
        this.bearer_token = bearer_token
    }

    String getXsrf_token() {
        return xsrf_token
    }

    void setXsrf_token(String xsrf_token) {
        this.xsrf_token = xsrf_token
    }

    Long getInstanceId() {
        return instanceId
    }

    void setInstanceId(Long instanceId) {
        this.instanceId = instanceId
    }

    Long getExpires() {
        return expires
    }

    void setExpires(Long expires) {
        this.expires = expires
    }
}
