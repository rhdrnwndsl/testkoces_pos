package com.koces.androidpos.sdk.ble;

import android.bluetooth.BluetoothDevice;

public class BleInterface {
    public interface ResultLinstener {
        void ConnectionResultLinstener(int _res);
        void MessageResultLinstener(byte[] var1);
    }
    public interface ScanResultLinstener{
        void onResult(int state, BluetoothDevice device, int rssi, String Message);
        void onScanFinished();
    }
}




