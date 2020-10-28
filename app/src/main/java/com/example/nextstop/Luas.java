package com.example.nextstop;

public class Luas {

    private String _abrev;
    private String _isParkRide;
    private String _isCycleRide;
    private double _lat;
    private double _long;
    private String _pronunciation;
    private String __text;

    public Luas(String _abrev, String _isParkRide, String _isCycleRide, double _lat, double _long, String _pronunciation, String __text) {
        this._abrev = _abrev;
        this._isParkRide = _isParkRide;
        this._isCycleRide = _isCycleRide;
        this._lat = _lat;
        this._long = _long;
        this._pronunciation = _pronunciation;
        this.__text = __text;
    }


    public String get_abrev() {
        return _abrev;
    }

    public String get_isParkRide() {
        return _isParkRide;
    }

    public String get_isCycleRide() {
        return _isCycleRide;
    }

    public double get_lat() {
        return _lat;
    }

    public double get_long() {
        return _long;
    }

    public String get_pronunciation() {
        return _pronunciation;
    }

    public String get__text() {
        return __text;
    }

    public void set_abrev(String _abrev) {
        this._abrev = _abrev;
    }

    public void set_isParkRide(String _isParkRide) {
        this._isParkRide = _isParkRide;
    }

    public void set_isCycleRide(String _isCycleRide) {
        this._isCycleRide = _isCycleRide;
    }

    public void set_lat(double _lat) {
        this._lat = _lat;
    }

    public void set_long(double _long) {
        this._long = _long;
    }

    public void set_pronunciation(String _pronunciation) {
        this._pronunciation = _pronunciation;
    }

    public void set__text(String __text) {
        this.__text = __text;
    }
}
