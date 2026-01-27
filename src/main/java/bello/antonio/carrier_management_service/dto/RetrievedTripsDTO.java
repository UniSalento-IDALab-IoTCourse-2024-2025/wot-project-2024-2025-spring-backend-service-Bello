package bello.antonio.carrier_management_service.dto;

import java.util.List;

public class RetrievedTripsDTO {
    private List<TripDTO> tripsDTO;
    private ShipmentDTO shipmentDTO;

    public List<TripDTO> getTripsDTO() {
        return tripsDTO;
    }

    public void setTripsDTO(List<TripDTO> tripsDTO) {
        this.tripsDTO = tripsDTO;
    }

    public ShipmentDTO getShipmentDTO() {
        return shipmentDTO;
    }

    public void setShipmentDTO(ShipmentDTO shipmentDTO) {
        this.shipmentDTO = shipmentDTO;
    }
}
