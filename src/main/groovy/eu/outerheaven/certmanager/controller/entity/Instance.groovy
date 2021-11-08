package eu.outerheaven.certmanager.controller.entity

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.OneToOne

@Entity
class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id

    private String name

    private String hostname

    private String ip

    private Long port

    private Boolean adopted

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private User user

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "instance", orphanRemoval = true)
    private InstanceAccessData instanceAccessData

    @OneToMany
    private List<User> assignedUsers

    Long getId() {
        return id
    }

    void setId(Long id) {
        this.id = id
    }

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    String getHostname() {
        return hostname
    }

    void setHostname(String hostname) {
        this.hostname = hostname
    }

    String getIp() {
        return ip
    }

    void setIp(String ip) {
        this.ip = ip
    }

    Long getPort() {
        return port
    }

    void setPort(Long port) {
        this.port = port
    }

    Boolean getAdopted() {
        return adopted
    }

    void setAdopted(Boolean adopted) {
        this.adopted = adopted
    }

    User getUser() {
        return user
    }

    void setUser(User user) {
        this.user = user
    }

    InstanceAccessData getInstanceAccessData() {
        return instanceAccessData
    }

    void setInstanceAccessData(InstanceAccessData instanceAccessData) {
        this.instanceAccessData = instanceAccessData
    }

    List<User> getAssignedUsers() {
        return assignedUsers
    }

    void setAssignedUsers(List<User> assignedUsers) {
        this.assignedUsers = assignedUsers
    }

    String getAccessUrl(){
        return "http://" + this.ip + ":" + this.port
    }
}
