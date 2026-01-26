package bello.antonio.carrier_management_service.dto;

import java.util.Date;

public class TripDTO {

    private String id;
    private String vehicleName;
    private String pathPolyline;
    private boolean started;
    private Date arrivalDate;
    private float price;
    private boolean scheduled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getPathPolyline() {
        return pathPolyline;
    }

    public void setPathPolyline(String pathPolyline) {
        this.pathPolyline = pathPolyline;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public Date getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(Date arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }
}
