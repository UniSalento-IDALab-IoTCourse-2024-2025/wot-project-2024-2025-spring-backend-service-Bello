package bello.antonio.carrier_management_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TelemetryMessageDTO {
    private String vehicleName;
    private String timestamp;
    private int rowIndex;
    private String streamStatus;

    @JsonProperty("T_amb")
    private double tAmb;
    @JsonProperty("T_set")
    private double tSet;
    @JsonProperty("T_cab_meas")
    private double tCabMeas;
    @JsonProperty("T_evap_sat")
    private double tEvapSat;
    @JsonProperty("T_cond_sat")
    private double tCondSat;
    @JsonProperty("P_suc_bar")
    private double pSucBar;
    @JsonProperty("P_dis_bar")
    private double pDisBar;
    @JsonProperty("N_comp_Hz")
    private double nCompHz;
    @JsonProperty("SH_K")
    private double shK;
    @JsonProperty("P_comp_W")
    private double pCompW;
    @JsonProperty("Q_evap_W")
    private double qEvapW;
    @JsonProperty("COP")
    private double cop;
    @JsonProperty("door_open")
    private boolean doorOpen;
    @JsonProperty("defrost_on")
    private boolean defrostOn;
    @JsonProperty("valve_open")
    private boolean valveOpen;

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public String getStreamStatus() {
        return streamStatus;
    }

    public void setStreamStatus(String streamStatus) {
        this.streamStatus = streamStatus;
    }

    public double gettAmb() {
        return tAmb;
    }

    public void settAmb(double tAmb) {
        this.tAmb = tAmb;
    }

    public double gettSet() {
        return tSet;
    }

    public void settSet(double tSet) {
        this.tSet = tSet;
    }

    public double gettCabMeas() {
        return tCabMeas;
    }

    public void settCabMeas(double tCabMeas) {
        this.tCabMeas = tCabMeas;
    }

    public double gettEvapSat() {
        return tEvapSat;
    }

    public void settEvapSat(double tEvapSat) {
        this.tEvapSat = tEvapSat;
    }

    public double gettCondSat() {
        return tCondSat;
    }

    public void settCondSat(double tCondSat) {
        this.tCondSat = tCondSat;
    }

    public double getpSucBar() {
        return pSucBar;
    }

    public void setpSucBar(double pSucBar) {
        this.pSucBar = pSucBar;
    }

    public double getpDisBar() {
        return pDisBar;
    }

    public void setpDisBar(double pDisBar) {
        this.pDisBar = pDisBar;
    }

    public double getnCompHz() {
        return nCompHz;
    }

    public void setnCompHz(double nCompHz) {
        this.nCompHz = nCompHz;
    }

    public double getShK() {
        return shK;
    }

    public void setShK(double shK) {
        this.shK = shK;
    }

    public double getpCompW() {
        return pCompW;
    }

    public void setpCompW(double pCompW) {
        this.pCompW = pCompW;
    }

    public double getqEvapW() {
        return qEvapW;
    }

    public void setqEvapW(double qEvapW) {
        this.qEvapW = qEvapW;
    }

    public double getCop() {
        return cop;
    }

    public void setCop(double cop) {
        this.cop = cop;
    }

    public boolean isDoorOpen() {
        return doorOpen;
    }

    public void setDoorOpen(boolean doorOpen) {
        this.doorOpen = doorOpen;
    }

    public boolean isDefrostOn() {
        return defrostOn;
    }

    public void setDefrostOn(boolean defrostOn) {
        this.defrostOn = defrostOn;
    }

    public boolean isValveOpen() {
        return valveOpen;
    }

    public void setValveOpen(boolean valveOpen) {
        this.valveOpen = valveOpen;
    }
}
