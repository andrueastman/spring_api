package main.requests;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity // This tells Hibernate to make a table out of this class
public class RequestRejection {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @NotNull
    private Long requestID;

    @NotNull
    private String driverPhone;

    @NotNull
    private Date rejectionTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestID() {
        return requestID;
    }

    public void setRequestID(Long requestID) {
        this.requestID = requestID;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public Date getRejectionTime() {
        return rejectionTime;
    }

    public void setRejectionTime(Date rejectionTime) {
        this.rejectionTime = rejectionTime;
    }
}
