package com.koces.androidpos.sdk.ble;


import android.bluetooth.BluetoothDevice;

import java.util.HashMap;

public class bleSdkInterface {
    public interface ConnectionListener {
        void onState(boolean result);
    }
    public interface ScanResultListener {
        void onResult(int state, BluetoothDevice _device, int rssi, String name, String Addr, String Message);

    }
    public interface ResDataListener{
        void OnResult(byte[] res);
    }

    public interface BLEKeyUpdateListener{
        void result(String result, String Code, String state, HashMap<String,String> resultData);
    }
}


