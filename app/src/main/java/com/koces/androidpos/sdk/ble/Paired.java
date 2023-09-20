package com.koces.androidpos.sdk.ble;

public class Paired {
    private String device;
    private String address;

    public Paired() {
        super();
    }

    public Paired(String device, String address) {
        super();
        this.device = device;
        this.address = address;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
