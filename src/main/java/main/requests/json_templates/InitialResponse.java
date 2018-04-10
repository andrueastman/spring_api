package main.requests.json_templates;

public class InitialResponse extends StandardResponse{

    private String requestId;
    private String driverName;
    private String vehicleRegistration;
    private String driverPhone;
    private String driverLat;
    private String driverLong;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getVehicleRegistration() {
        return vehicleRegistration;
    }

    public void setVehicleRegistration(String vehicleRegistration) {
        this.vehicleRegistration = vehicleRegistration;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public String getDriverLat() {
        return driverLat;
    }

    public void setDriverLat(String driverLat) {
        this.driverLat = driverLat;
    }

    public String getDriverLong() {
        return driverLong;
    }

    public void setDriverLong(String driverLong) {
        this.driverLong = driverLong;
    }

}
