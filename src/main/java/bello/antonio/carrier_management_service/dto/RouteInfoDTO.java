package bello.antonio.carrier_management_service.dto;

public class RouteInfoDTO {
    private String polyline;
    private double distanceKm;
    private double duration;

    public RouteInfoDTO(String polyline, double distanceKm, double duration) {
        this.polyline = polyline;
        this.distanceKm = distanceKm;
        this.duration = duration;
    }

    public String getPolyline() { return polyline; }
    public double getDistanceKm() { return distanceKm; }
    public double getDuration() { return duration; }
}

