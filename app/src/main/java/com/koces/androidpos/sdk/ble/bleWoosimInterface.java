package com.koces.androidpos.sdk.ble;

import android.bluetooth.BluetoothDevice;

import java.util.HashMap;

public class bleWoosimInterface {
    public interface ConnectionListener {
        void onState(boolean result);
    }

    public interface ResDataListener{
        void OnResult(byte[] res);
    }

    public interface BLEKeyUpdateListener{
        void result(String result, String Code, String state, HashMap<String,String> resultData);
    }
}
