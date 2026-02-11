package bello.antonio.carrier_management_service.dto;

import com.google.maps.model.LatLng;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

import java.util.Date;

public class ShipmentDTO {

    private String id;
    private String idTrip;
    private String vehicleName;
    private String departureAddress;
    private String arrivalAddress;
    private double distanceKm;
    @GeoSpatialIndexed
    private LatLng departureLatLng;
    @GeoSpatialIndexed
    private LatLng arrivalLatLng;
    private Date arrivalDate;
    private int width;
    private int height;
    private int length;
    private int weight;
    private boolean refrigerated;
    private float price;
    private double duration;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdTrip() {
        return idTrip;
    }

    public void setIdTrip(String idTrip) {
        this.idTrip = idTrip;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getDepartureAddress() {
        return departureAddress;
    }

    public void setDepartureAddress(String departureAddress) {
        this.departureAddress = departureAddress;
    }

    public String getArrivalAddress() {
        return arrivalAddress;
    }

    public void setArrivalAddress(String arrivalAddress) {
        this.arrivalAddress = arrivalAddress;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
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

    public Date getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(Date arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isRefrigerated() {
        return refrigerated;
    }

    public void setRefrigerated(boolean refrigerated) {
        this.refrigerated = refrigerated;
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
}
