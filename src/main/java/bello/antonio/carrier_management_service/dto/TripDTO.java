package bello.antonio.carrier_management_service.dto;

import com.google.maps.model.LatLng;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

import java.util.Date;

public class TripDTO {

    private String id;
    private String vehicleName;
    @GeoSpatialIndexed
    private LatLng departureLatLng;
    @GeoSpatialIndexed
    private LatLng arrivalLatLng;
    private String departureAddress;
    private String arrivalAddress;
    private String pathPolyline;
    private double distanceKm;
    private boolean started;
    private Date arrivalDate;
    private float price;
    private boolean scheduled;
    private double duration;
    private int remainingVolume;
    private boolean refrigerated;

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

    public LatLng getDepartureLatLng() {
        return departureLatLng;
    }

    public void setDepartureLatLng(LatLng departureLatLng) {
        this.departureLatLng = departureLatLng;
    }

    public LatLng getArrivalLatLng() {
        return arrivalLatLng;
    }

    public void setArrivalLatLng(LatLng arrivalLatLng) {
        this.arrivalLatLng = arrivalLatLng;
    }

    public String getArrivalAddress() {
        return arrivalAddress;
    }

    public void setArrivalAddress(String arrivalAddress) {
        this.arrivalAddress = arrivalAddress;
    }

    public String getDepartureAddress() {
        return departureAddress;
    }

    public void setDepartureAddress(String departureAddress) {
        this.departureAddress = departureAddress;
    }

    public String getPathPolyline() {
        return pathPolyline;
    }

    public void setPathPolyline(String pathPolyline) {
        this.pathPolyline = pathPolyline;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
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

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getRemainingVolume() {
        return remainingVolume;
    }

    public void setRemainingVolume(int remainingVolume) {
        this.remainingVolume = remainingVolume;
    }

    public boolean isRefrigerated() {
        return refrigerated;
    }

    public void setRefrigerated(boolean refrigerated) {
        this.refrigerated = refrigerated;
    }


}
