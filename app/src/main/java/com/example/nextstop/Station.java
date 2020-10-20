package com.example.nextstop;


public class Station implements Comparable {

    private String StationDesc;
    private String StationAlias;
    private float StationLatitude;
    private float StationLongitude;
    private String StationCode;
    private int StationId;

    public Station(String stationDesc, String stationAlias, float stationLatitude, float stationLongitude, String stationCode, int stationId) {
        StationDesc = stationDesc;
        StationAlias = stationAlias;
        StationLatitude = stationLatitude;
        StationLongitude = stationLongitude;
        StationCode = stationCode;
        StationId = stationId;
    }

    public String getStationDesc() {
        return StationDesc;
    }

    public String getStationAlias() {
        return StationAlias;
    }

    public float getStationLatitude() {
        return StationLatitude;
    }

    public float getStationLongitude() {
        return StationLongitude;
    }

    public String getStationCode() {
        return StationCode;
    }

    public int getStationId() {
        return StationId;
    }

    public void setStationDesc(String stationDesc) {
        StationDesc = stationDesc;
    }

    public void setStationAlias(String stationAlias) {
        StationAlias = stationAlias;
    }

    public void setStationLatitude(float stationLatitude) {
        StationLatitude = stationLatitude;
    }

    public void setStationLongitude(float stationLongitude) {
        StationLongitude = stationLongitude;
    }

    public void setStationCode(String stationCode) {
        StationCode = stationCode;
    }

    public void setStationId(int stationId) {
        StationId = stationId;
    }

    @Override
    public int compareTo(Object o) {
        int compareId = ((Station) o).getStationId();
        return this.StationId - compareId;
    }
}
