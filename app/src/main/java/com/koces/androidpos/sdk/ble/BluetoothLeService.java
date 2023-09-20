/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.koces.androidpos.sdk.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.ble.BleInterface;
import com.koces.androidpos.sdk.ble.GattAttributes;
import com.koces.androidpos.sdk.ble.bleDevice;
import com.koces.androidpos.sdk.van.Constants;


import java.util.List;
import java.util.UUID;

import static com.koces.androidpos.sdk.ble.GattAttributes.BLE_NOTIFY;
import static com.koces.androidpos.sdk.ble.GattAttributes.BLE_RX;
import static com.koces.androidpos.sdk.ble.GattAttributes.BLE_TX;
import static com.koces.androidpos.sdk.ble.GattAttributes.BLE_TRAN;
import static com.koces.androidpos.sdk.ble.GattAttributes.UUID_CLIENT_CHARACTERISTIC_CONFIG;

import androidx.annotation.NonNull;

import kotlin.OverloadResolutionByLambdaReturnType;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private static com.koces.androidpos.sdk.ble.BleInterface.ResultLinstener mResultLinstener;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    public BluetoothGatt mBluetoothGatt;
    private byte[] mBuffer;
    public int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID TRANS_TX = UUID.fromString(com.koces.androidpos.sdk.ble.GattAttributes.TRANS_TX);
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.

    /** 2020-09-15 kim.jy ble 관련 세팅 값을 저장하는 부분*/
    private static boolean isSetMtuMode = false;
    String intentAction;
    int isMtuCheckCount = 0;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                //kim.jy 20191021
                isMtuCheckCount++;
                mBluetoothGatt.discoverServices();

//                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.i(TAG, "isSetMtuMode = " + com.koces.androidpos.sdk.ble.bleDevice.Mtu);
//                        isSetMtuMode = gatt.requestMtu(com.koces.androidpos.sdk.ble.bleDevice.Mtu);
//                    }
//                },1000);


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server." + status);
                mResultLinstener.ConnectionResultLinstener(STATE_DISCONNECTED);
                broadcastUpdate(intentAction);
            }
        }
//        @Override
//        public void onMtuChanged(BluetoothGatt gatt, int mtu, int newState)
//        {
//            super.onMtuChanged(gatt,mtu,newState);
//            if(newState == BluetoothGatt.GATT_SUCCESS)
//            {
//                Log.d(TAG,"Mtu Changed");
//
//                broadcastUpdate(intentAction);
//                Log.i(TAG, "Connected to GATT server.");
//                isMtuCheckCount = 0;
//                if (mConnectionState == STATE_CONNECTED) {
//                    Log.i(TAG, "Attempting to start service discovery:" );
////                    mBluetoothGatt.discoverServices();
//                } else {
//                    Log.i(TAG, "Connected to GATT server. but Disconnected from GATT server."+ newState);
//                }
//
//            }
//            else
//            {
//                isMtuCheckCount = 0;
//                intentAction = ACTION_GATT_DISCONNECTED;
//                mConnectionState = STATE_DISCONNECTED;
//                mResultLinstener.ConnectionResultLinstener(STATE_DISCONNECTED);
//                broadcastUpdate(intentAction);
//                return;
////                if (isMtuCheckCount != 1)
////                {
////                    isMtuCheckCount = 0;
////                    intentAction = ACTION_GATT_DISCONNECTED;
////                    mConnectionState = STATE_DISCONNECTED;
////                    mResultLinstener.ConnectionResultLinstener(STATE_DISCONNECTED);
////                    broadcastUpdate(intentAction);
////                    return;
////                }
////                intentAction = ACTION_GATT_CONNECTED;
////                mConnectionState = STATE_CONNECTED;
////                //kim.jy 20191021
////                //TODO: 여기서 아래의 Mtu 에다가 다른 값을 보내주거나 해본다.  by.jiw
////                com.koces.androidpos.sdk.ble.bleDevice.Mtu = 150;
////                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
////                    @Override
////                    public void run() {
////                        Log.i(TAG, "TryisSetMtuMode = " + com.koces.androidpos.sdk.ble.bleDevice.Mtu);
////                        isSetMtuMode = gatt.requestMtu(com.koces.androidpos.sdk.ble.bleDevice.Mtu);
////                    }
////                },1000);
//            }
//
//        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                //여기서 setNotification을 설정 한다.
                List<BluetoothGattService> bSerivce = gatt.getServices();
                Log.w(TAG, "onServicesDiscovered received: " + status);
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                if(com.koces.androidpos.sdk.ble.GattAttributes.mBleModelType==40) {
                    BluetoothGattCharacteristic ch = gatt.getService(BLE_TRAN).getCharacteristic(BLE_NOTIFY);
                    gatt.setCharacteristicNotification(ch, true);

//                    return;
                }
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        for(BluetoothGattService s:bSerivce)
                        {
                            if(s.getUuid().equals(com.koces.androidpos.sdk.ble.GattAttributes.BLE_TRAN))
                            {
                                if(com.koces.androidpos.sdk.ble.GattAttributes.mBleModelType==30) {
                                    BluetoothGattCharacteristic characteristic[] = {s.getCharacteristic(BLE_NOTIFY)};
                                    setCharacteristicNotification(characteristic,true);
                                    return;
                                } else {
                                    BluetoothGattCharacteristic characteristic[] = {s.getCharacteristic(BLE_RX),s.getCharacteristic(BLE_TX)};
                                    setCharacteristicNotification(characteristic,true);
                                }


                            }

                        }

//                        //sjw 221110 추가
//                        com.koces.androidpos.sdk.ble.bleDevice.Mtu = 150;
//                        Log.i(TAG, "TryisSetMtuMode = " + com.koces.androidpos.sdk.ble.bleDevice.Mtu);
//                        isSetMtuMode = gatt.requestMtu(com.koces.androidpos.sdk.ble.bleDevice.Mtu);

                        if(Setting.getBleName().contains(Constants.C1_KRE_NEW)) {
                            com.koces.androidpos.sdk.ble.bleDevice.Mtu = 150;
                            Log.i(TAG, "TryisSetMtuMode = " + com.koces.androidpos.sdk.ble.bleDevice.Mtu);
                            isSetMtuMode = mBluetoothGatt.requestMtu(com.koces.androidpos.sdk.ble.bleDevice.Mtu);
                        }
                    }
                }, 500);



            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                mResultLinstener.ConnectionResultLinstener(STATE_DISCONNECTED);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic,int status)
        {
            if(status!=0)
            {
                Log.d("kim.jy","onCharacteristicWrite -> status :" + String.valueOf(status));
            }
            bleDevice.setDataSendingResult(status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    public void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);



    }
    private void RecvDataUpdate(byte[] recv) {
        if(mBuffer==null)
        {
            if(recv.length != 0 && recv[0] == Command.STX) {
                mBuffer = new byte[recv.length];
                System.arraycopy(recv, 0, mBuffer, 0, recv.length);
            }
            else if (recv.length != 0){
                for(int i = 0;i<recv.length;i++){
                    if(recv[i] == Command.STX){
                        mBuffer = new byte[recv.length - i];
                        System.arraycopy(recv,i,mBuffer,0,recv.length);
                        break;
                    }
                }
            }
        }
        else
        {
            byte[] buffer = new byte[mBuffer.length];
            System.arraycopy(mBuffer,0,buffer,0,mBuffer.length);
            mBuffer = new byte[recv.length+mBuffer.length];
            System.arraycopy(buffer,0,mBuffer,0,buffer.length);
            System.arraycopy(recv,0,mBuffer,buffer.length,recv.length);
        }

        if (mBuffer.length>= com.koces.androidpos.sdk.ble.GattAttributes.SPEC_MIN_SIZE) {
            if (mBuffer[0] != com.koces.androidpos.sdk.ble.GattAttributes.STX) /* 만약에 버퍼 시작값이 STX가 아닌 경우 전문 이상으로 보고 모든 버퍼를 지운다.*/ {
                mBuffer = null;
            }

            byte[] size = new byte[2];
            //inicis 의 경우 Data길이 다음에 Command
//            size[0] = mBuffer[1];
//            size[1] = mBuffer[2];
            //KOVAN의 경우에는 길이 다음에 Command
            size[0] = mBuffer[1];
            size[1] = mBuffer[2];
            int protlength = Utils.byteToInt(size);
            if (mBuffer.length >= protlength + com.koces.androidpos.sdk.ble.GattAttributes.SPEC_HEADER_SIZE)
            {
                if(mBuffer.length == protlength + com.koces.androidpos.sdk.ble.GattAttributes.SPEC_HEADER_SIZE){
                    mResultLinstener.MessageResultLinstener(mBuffer.clone());
                }else{
                    byte[] temp=new byte[protlength + com.koces.androidpos.sdk.ble.GattAttributes.SPEC_HEADER_SIZE];
                    System.arraycopy(mBuffer,0,temp,0,temp.length);
                    mResultLinstener.MessageResultLinstener(temp.clone());
                }

                mBuffer = null;
            }

        }

    }
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        /*
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (TRANS_TX.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
        */

        //2020년 9월 28일 kim.jy 장치 별로 올라오는 notify 가 다르기 때문에 분기 한다.
        if(com.koces.androidpos.sdk.ble.GattAttributes.mBleModelType==10){
            if(BLE_NOTIFY==null) {
                //ble notify uuid 가 설정 되지 않는 경우에 어떻게 처리 할지 결정 해야 한다.
            }
            else if (BLE_NOTIFY.equals(characteristic.getUuid()))
            {
                final byte[] data = characteristic.getValue();
                RecvDataUpdate(data);
            }
        }
        else if(com.koces.androidpos.sdk.ble.GattAttributes.mBleModelType==20){
            if(BLE_NOTIFY==null)
            {
                //ble notify uuid 가 설정 되지 않는 경우에 어떻게 처리 할지 결정 해야 한다.
            }
            else if (BLE_RX.equals(characteristic.getUuid()))
            {
                final byte[] data = characteristic.getValue();
                RecvDataUpdate(data);
            }
        }
        else if(com.koces.androidpos.sdk.ble.GattAttributes.mBleModelType==30){
            if(BLE_NOTIFY==null)
            {
                //ble notify uuid 가 설정 되지 않는 경우에 어떻게 처리 할지 결정 해야 한다.
            }
            else if (BLE_NOTIFY.equals(characteristic.getUuid()))
            {
                final byte[] data = characteristic.getValue();
                RecvDataUpdate(data);
            }
        }
        else{
            if(BLE_RX==null)
            {
                BLE_RX = UUID.fromString(com.koces.androidpos.sdk.ble.GattAttributes.TRANS_TX);
            }
            if (BLE_RX.equals(characteristic.getUuid()))
            {
                final byte[] data = characteristic.getValue();
                RecvDataUpdate(data);

            }
        }
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize(BleInterface.ResultLinstener _lisntener) {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        mResultLinstener = _lisntener;
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address, final boolean _pairingNotFirst) {
        /* 2020-09-15 ble 설정 및 디버깅을 위해서 함수 제작 */
        InitializeDeveloperDebuggingMode();
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            mResultLinstener.ConnectionResultLinstener(STATE_DISCONNECTED);
            return false;

        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                mResultLinstener.ConnectionResultLinstener(STATE_CONNECTING);
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBluetoothGatt = device.connectGatt(this, _pairingNotFirst, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        }
        else {
            mBluetoothGatt = device.connectGatt(this, _pairingNotFirst, mGattCallback);
        }

        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        mResultLinstener.ConnectionResultLinstener(STATE_CONNECTING);
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }


    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        //수신 버퍼 초기화
        mBuffer = null;
        if(mBluetoothAdapter ==null || mBluetoothGatt == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        boolean result = mBluetoothGatt.writeCharacteristic(characteristic);
        if(!result)
        {
            Log.d("Kim.jy","writeCharacteristic -> 실패 : " + characteristic.getValue());
        }
        return result;
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param _characteristic Characteristic to act on.
     * @param _enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic[] _characteristic,
                                              boolean _enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        final boolean enabled = _enabled;
        boolean result =true;
        BluetoothGattCharacteristic characteristic = null;
        for(int i = 0; i<_characteristic.length;i++) {
            characteristic = _characteristic[i];
            if(characteristic.getUuid() != null) {
                result = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        }


        boolean finalResult = result;
        BluetoothGattCharacteristic finalCharacteristic = characteristic;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!finalResult)
                {
                    Log.d(TAG," mBluetoothGatt.setCharacteristicNotification(characteristic, enabled) : false");
                }
                // This is specific to Heart Rate Measurement.
                if (finalCharacteristic == null){
                    Log.d(TAG," mBluetoothGatt.characteristic is null");
                    return;
                }
                if(com.koces.androidpos.sdk.ble.GattAttributes.mBleModelType==10) {  //payfun 의 경우
                    if (BLE_NOTIFY.equals(finalCharacteristic.getUuid())) {
//                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GattAttributes.BLE_CHARACTERISTIC_CONFIG);
//                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                boolean result2 = mBluetoothGatt.writeDescriptor(descriptor);
//                if (!result2) {
//                    Log.d(TAG, "mBluetoothGatt.writeDescriptor : false");
//                }

                    }
                } else if(com.koces.androidpos.sdk.ble.GattAttributes.mBleModelType==20) { //아폴로3? 의 경우
                    if (BLE_TX.equals(finalCharacteristic.getUuid())) {
                        BluetoothGattDescriptor descriptor = finalCharacteristic.getDescriptor(com.koces.androidpos.sdk.ble.GattAttributes.BLE_CHARACTERISTIC_CONFIG);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        if (descriptor == null) {
                            Log.d(TAG," descriptor is null");
                            return;
                        }
                        boolean result2 = mBluetoothGatt.writeDescriptor(descriptor);
                        if (!result2) {
                            Log.d(TAG, "mBluetoothGatt.writeDescriptor(ble_rx) : false");
                        }
                    }
                } else if(com.koces.androidpos.sdk.ble.GattAttributes.mBleModelType==30) {
                    if (BLE_RX.equals(finalCharacteristic.getUuid())) {
                        BluetoothGattDescriptor descriptor = finalCharacteristic.getDescriptor(com.koces.androidpos.sdk.ble.GattAttributes.BLE_CHARACTERISTIC_CONFIG);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        if (descriptor == null) {
                            Log.d(TAG," descriptor is null");
                            return;
                        }
                        boolean result2 = mBluetoothGatt.writeDescriptor(descriptor);
                        if (!result2) {
                            Log.d(TAG, "mBluetoothGatt.writeDescriptor(ble_rx) : false");
                        }
                    }
                }
                else{
                    if (BLE_RX.equals(finalCharacteristic.getUuid())) {
                        BluetoothGattDescriptor descriptor = finalCharacteristic.getDescriptor(GattAttributes.BLE_CHARACTERISTIC_CONFIG);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        if (descriptor == null) {
                            Log.d(TAG," descriptor is null");
                            return;
                        }
                        boolean result2 = mBluetoothGatt.writeDescriptor(descriptor);
                        if (!result2) {
                            Log.d(TAG, "mBluetoothGatt.writeDescriptor : false");
                        }
                    }
                }

                //                        //sjw 221110 추가
//                if(Setting.getBleName().contains(Constants.C1_KRE_NEW)) {
//                    com.koces.androidpos.sdk.ble.bleDevice.Mtu = 150;
//                    Log.i(TAG, "TryisSetMtuMode = " + com.koces.androidpos.sdk.ble.bleDevice.Mtu);
//                    isSetMtuMode = mBluetoothGatt.requestMtu(com.koces.androidpos.sdk.ble.bleDevice.Mtu);
//                }
            }
        }, 500);

    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }


    // 실제 배포 시에는 삭제 해야 한다.
    // 테스트를 위해서 존재 하는 함수 필요 없음.

    public void InitializeDeveloperDebuggingMode()
    {
        isSetMtuMode = false;
    }

}
