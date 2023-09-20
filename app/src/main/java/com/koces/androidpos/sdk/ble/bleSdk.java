package com.koces.androidpos.sdk.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.koces.androidpos.AppToAppActivity;
import com.koces.androidpos.BaseActivity;
import com.koces.androidpos.Main2Activity;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.ble.BleInterface;
import com.koces.androidpos.sdk.ble.bleDevice;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.koces.androidpos.sdk.ble.BluetoothLeService;
import com.koces.androidpos.sdk.van.Constants;

import static com.koces.androidpos.sdk.ble.bleDevice.STATE_CONNECTED;

public class bleSdk {
    private final static String TAG = "BLESDK";
    private boolean binit = false;          //라이브러리 초기화 여부를 설정 한다.
    private Context m_ctx;
    private bleDevice m_device;
    private BluetoothLeService m_LeService;
    private static bleSdk instance = null;
    public bleSdkInterface.ConnectionListener mConnectResult;
    public bleSdkInterface.ScanResultListener mScanResult;
    private BleTimer m_bletimer;
    private BleTimer2 m_bletimer2;
    private Handler mHandler;
    //private CountDownTimer ConnectTimer;
    /* 수치 관련 정의 */
    private int BleConnectTimeOut;
    private boolean WhileFinish = false;
    public String addr = "";
    public String name = "";
    private int isConnectCount = 0;

    private UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private UUID RX_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    private UUID RX_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
    private UUID TX_CHAR_UUID = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
    private UUID RX_NOTIFY = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");

    private BleRepository mBleRepository;

    public bleSdk(Context _ctx, bleSdkInterface.ConnectionListener ConnectLinstener, Handler _handler) {
        m_ctx = _ctx;
        initialize();
        mConnectResult = ConnectLinstener;
        mHandler = _handler;
    }

    /**
     * 기본 함수 초기화
     */
    private void initialize() {
        instance = this;
        m_device = new bleDevice(m_ctx, mResultLinstener);

        //다른 장비 테스트 코드
//            //DS-2000
//        final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//        final UUID RX_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
//        final UUID RX_CHAR_UUID = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
//        final UUID TX_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//        final UUID RX_NOTIFY = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
        m_device.setGattUUID(CCCD, RX_CHAR_UUID, TX_CHAR_UUID, RX_SERVICE_UUID, RX_NOTIFY);
        m_device.setBleModelType(20);   //DS-2000의 경우

        /**
         * 여기부터 광우ble 리더기 연결처리부
         */
        mBleRepository = new BleRepository();
    }

    /**
     * 라이브러리 초기화 코드
     *
     * @return
     */
    public boolean init() {
        //안드로이드 버전 체크
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }
        //블루투스 권한 설정
        if (ContextCompat.checkSelfPermission(m_ctx, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(m_ctx, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED) {
            return false;
        }
        return true;

        //사용할 장비 설정 만약 사용할 장비가 특별 하지 않다면 설정 하지 않는다.
    }

    public static bleSdk getInstance() {
        if (instance != null) {
            return instance;
        }
        return null;
    }

    public boolean isConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected", (Class[]) null);
            boolean connected = (boolean) m.invoke(device, (Object[]) null);
            return connected;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean getConnected()
    {
        boolean _connected = false;
        _connected = Setting.getBleIsConnected();
        return _connected;
    }

    public void unRegisterReceiver(Context _ctx)
    {
//        m_ctx = _ctx;
        unregister(mBluetoothSearchReceiver,_ctx);
    }

    public void registerReceiver(Context _ctx)
    {
//        m_ctx.unregisterReceiver(mBluetoothSearchReceiver);
//        unregister(mBluetoothSearchReceiver);
        unRegisterReceiver(m_ctx);
        IntentFilter stateFilter = new IntentFilter();
        stateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //BluetoothAdapter.ACTION_STATE_CHANGED : 블루투스 상태변화 액션
        stateFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        stateFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED); //연결 확인
        stateFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); //연결 끊김 확인
        stateFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        stateFilter.addAction(BluetoothDevice.ACTION_FOUND);    //기기 검색됨
        stateFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);   //기기 검색 시작
        stateFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);  //기기 검색 종료
        stateFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        m_ctx = _ctx;
//        unregister(mBluetoothSearchReceiver);
        try {
//            m_ctx.registerReceiver(mBluetoothSearchReceiver, stateFilter);
            register(mBluetoothSearchReceiver,stateFilter);
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
//        register(mBluetoothSearchReceiver,stateFilter);

    }

    public void Connect(Context _ctx,String _Addr, String _Name) {
//        Toast.makeText(m_ctx, "잠시만 기다려 주세요", Toast.LENGTH_SHORT).show();
        addr = _Addr;
        name = _Name;
        BleConnectTimeOut = 20;
//        unRegisterReceiver(m_ctx);
        WhileFinish = false;
        if (m_bletimer != null) {
            m_bletimer.cancel();
            m_bletimer = null;
        }

        //TODO: 연결시도를 할 때 프리페어런스에 있는 기존 데이터는 제거한다. 상황은 아래내용참조  //jiw 230727
        // ble 연결도중(무결성검사에서 안넘어가서 앱을 강제종료) 하였더니 다음에 기존에 마지막으로 성공했던 다른 ble 장비에 연결시도.
        // 이 후 설정에서 ble를 페어링 모두 해제하고 나니 다시 ble 무결성검사에서 안넘어가던 ble에 연결시도함
        Setting.setPreference(m_ctx, Constants.BLE_DEVICE_NAME, "");
        Setting.setPreference(m_ctx, Constants.BLE_DEVICE_ADDR, "");

        //만일 장비가 신형인 경우KREC 구형인경우KRE-
        if (name.contains(Constants.C1_KRE_NEW)) {
//            mBleRepository.connectDevice(addr, m_ctx, Constants.C1_KRE_NEW);
//            return;
            CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
            RX_SERVICE_UUID = UUID.fromString("49324541-5211-FA30-4301-48AFD205E400");
            RX_CHAR_UUID = UUID.fromString("49324541-5211-FA30-4301-48AFD205E401");
            TX_CHAR_UUID = UUID.fromString("49324541-5211-FA30-4301-48AFD205E402");
            RX_NOTIFY = UUID.fromString("49324541-5211-FA30-4301-48AFD205E401");
            m_device.setBleModelType(20);   //DS-2000의 경우
        } else if(name.contains(Constants.C1_KRE_OLD_NOT_PRINT)) {
            registerReceiver(m_ctx);
            mBleRepository.connectDevice(addr, m_ctx, Constants.C1_KRE_OLD_NOT_PRINT);
            return;
//            CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//            RX_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
//            RX_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//            TX_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//            RX_NOTIFY = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//            m_device.setBleModelType(20);   //DS-2000의 경우
        } else if(name.contains(Constants.C1_KRE_OLD_USE_PRINT)) {
            registerReceiver(m_ctx);
            mBleRepository.connectDevice(addr, m_ctx, Constants.C1_KRE_OLD_USE_PRINT);
            return;
//            CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//            RX_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
//            RX_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//            TX_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//            RX_NOTIFY = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//            m_device.setBleModelType(20);   //DS-2000의 경우
        } else if(name.contains(Constants.ZOA_KRE)) {
            registerReceiver(m_ctx);
            mBleRepository.connectDevice(addr, m_ctx, Constants.ZOA_KRE);
            return;
//            CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//            RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
//            RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
//            TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
//            RX_NOTIFY = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
//            m_device.setBleModelType(30);   //DS-2000의 경우
        } else if(name.contains(Constants.KWANGWOO_KRE)) {
            registerReceiver(m_ctx);
            mBleRepository.connectDevice(addr, m_ctx, Constants.KWANGWOO_KRE);
            return;
//            CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//            RX_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
////            RX_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//            RX_CHAR_UUID = UUID.fromString("49535343-ACA3-481C-91EC-D85E28A60318");
//            TX_CHAR_UUID = UUID.fromString("49535343-ACA3-481C-91EC-D85E28A60318");
////            RX_NOTIFY = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
//            RX_NOTIFY = UUID.fromString("49535343-ACA3-481C-91EC-D85E28A60318");
//            m_device.setBleModelType(40);
        } else {
            //우심?
            //우심이 여기로 들어오면 안된다
            return;
        }

        m_device.setGattUUID(CCCD, RX_CHAR_UUID, TX_CHAR_UUID, RX_SERVICE_UUID, RX_NOTIFY);
        Setting.setIsWoosim(false);
        m_bletimer = new BleTimer(BleConnectTimeOut * 1000, 1000);
        m_bletimer.start();
        registerReceiver(m_ctx);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                m_device.connect(addr);
            }
        },500);
    }

    public void writeDevice(byte[] _bData) {
        Log.d(TAG, "APP -> DEVICE: " + Utils.bytesToHex(_bData));
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!Setting.getBleName().contains("KREC")) {
                    mBleRepository.writeData(_bData);
                    //TODO: 인증용  jiw230223
                    Random rand = new Random();
                    if(_bData!=null)
                    {
                        for(int i=0;i<_bData.length;i++)
                        {
                            _bData[i] = (byte)rand.nextInt(255);
                        }
                        Arrays.fill(_bData,(byte)0x01);
                        Arrays.fill(_bData,(byte)0x00);
                    }
                    return;
                }




                if (m_device.m_LeService == null) {
                    DisConnect();
                    Message msg = mHandler.obtainMessage();
                    msg.arg1 = Setting.HANDLER_MSGCODE_SENDING_FAIL;
                    msg.obj = "비정상적인 연결입니다. ble단말기의 전원을 껐다가 다시 켜주십시오";
                    mHandler.sendMessage(msg);
                    return;
                }
                boolean r = m_device.write(_bData);
                Log.d(TAG, r == true ? "true" : "false");
                if (!r) {
                    Message msg = mHandler.obtainMessage();
                    msg.arg1 = Setting.HANDLER_MSGCODE_SENDING_FAIL;
                    msg.obj = "Data transfer failure to ble device";
                    mHandler.sendMessage(msg);
//                    mConnectResult.onState(false);
//                    DisConnect();
                    return;
//                    Handler handler = new Handler(Looper.getMainLooper());
//                    Message msg = new Message();
//                    msg.arg1 = Setting.HANDLER_MSGCODE_SENDING_FAIL;
//                    msg.obj = "Data transfer failure to ble device";
//                    handler.sendMessage(msg);
                }

                //TODO: 인증용  jiw230223
                Random rand = new Random();
                if(_bData!=null)
                {
                    for(int i=0;i<_bData.length;i++)
                    {
                        _bData[i] = (byte)rand.nextInt(255);
                    }
                    Arrays.fill(_bData,(byte)0x01);
                    Arrays.fill(_bData,(byte)0x00);
                }
            }
        },500);

    }

    public void writeCancelDevice(byte[] _bData) {
        Log.d(TAG, "APP -> DEVICE: " + Utils.bytesToHex(_bData));
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!Setting.getBleName().contains("KREC")) {
                    mBleRepository.writeData(_bData);
                    //TODO: 인증용  jiw230223
                    Random rand = new Random();
                    if(_bData!=null)
                    {
                        for(int i=0;i<_bData.length;i++)
                        {
                            _bData[i] = (byte)rand.nextInt(255);
                        }
                        Arrays.fill(_bData,(byte)0x01);
                        Arrays.fill(_bData,(byte)0x00);
                    }
                    return;
                }


                boolean r = m_device.writeCancel(_bData);
                Log.d(TAG, r == true ? "true" : "false");
                if (!r) {
                    Message msg = mHandler.obtainMessage();
                    msg.arg1 = Setting.HANDLER_MSGCODE_SENDING_FAIL;
                    msg.obj = "Data transfer failure to ble device";
                    mHandler.sendMessage(msg);
//                    mConnectResult.onState(false);
//                    DisConnect();
                    return;
                }

                //TODO: 인증용  jiw230223
                Random rand = new Random();
                if(_bData!=null)
                {
                    for(int i=0;i<_bData.length;i++)
                    {
                        _bData[i] = (byte)rand.nextInt(255);
                    }
                    Arrays.fill(_bData,(byte)0x01);
                    Arrays.fill(_bData,(byte)0x00);
                }
            }
        }, 500);

    }

    public void DisConnect() {
//        unRegisterReceiver(m_ctx);
//        unRegisterReceiver(Setting.getTopContext());
        isConnectCount = 0;
        m_device.Disconnect();
        mBleRepository.disconnectDevice();
        Setting.setBleIsConnected(false);
        Setting.setBleName("");
//        Setting.setPreference(m_ctx, Constants.BLE_DEVICE_NAME, "");
//        Setting.setPreference(m_ctx, Constants.BLE_DEVICE_ADDR, "");

        if (bleWoosimSdk.getInstance() != null) {
            bleWoosimSdk.getInstance().Disconnect();
        }
    }

    private byte[] mBuffer;
    public void RecvDataUpdate(byte[] recv) {
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

    public void RecvData(byte[] _data) {
//        String tmp = new String(_data);
        //2020-09-07 데이터가 규칙 없이 사이즈 없이 올라 오는 경우에 대한 쳐리를 해야 한다.
        Log.d("kim.jy:RecvData", "Device -> App : " + Utils.bytesToHex(_data));
        Message msg = mHandler.obtainMessage();
        msg.arg1 = Setting.HANDLER_MSGCODE_RECVDATA;
        msg.obj = _data;
        mHandler.sendMessage(msg);
    }


    private BleInterface.ResultLinstener mResultLinstener = new BleInterface.ResultLinstener() {
        @Override
        public void ConnectionResultLinstener(int i) {
            switch (i) {
                case STATE_CONNECTED:
                    if (m_bletimer != null) {
                        m_bletimer.cancel();
                        m_bletimer = null;
                    }
                    if (m_bletimer2 != null) {
                        m_bletimer2.cancel();
                        m_bletimer2 = null;
                    }
                    Log.d(TAG,"STATE_CONNECTED");
                    mConnectResult.onState(true);
                    WhileFinish = true;
                    Setting.setBleIsConnected(true);
//                    BaseActivity _base = (BaseActivity) m_ctx;
//                    _base.ReadyDialogHide();
                    break;
                case bleDevice.STATE_CONNECTING:
                    Log.d(TAG,"STATE_CONNECTING");
                    isConnectCount++;
                    break;
                case bleDevice.STATE_DISCONNECTED:
                    if(isConnectCount > 0) {
                        Log.d(TAG,"STATE_DISCONNECTED But isConnectCount = " + isConnectCount);
                        isConnectCount = 0;
                        return;
                    }
                    if (m_bletimer != null) {
                        m_bletimer.cancel();
                        m_bletimer = null;
                    }
                    if (m_bletimer2 != null) {
                        m_bletimer2.cancel();
                        m_bletimer2 = null;
                    }
                    Log.d(TAG,"STATE_DISCONNECTED");
//                    unregister(mBluetoothSearchReceiver);
//                    m_ctx.unregisterReceiver(mBluetoothSearchReceiver);
                    mConnectResult.onState(false);
                    WhileFinish = true;
                    Setting.setBleIsConnected(false);
                    if (m_ctx instanceof AppToAppActivity)
                    {
//                        DisConnect();
                    }
                    DisConnect();
                    break;
            }
        }

        @Override
        public void MessageResultLinstener(byte[] bytes) {
            RecvData(bytes);
        }
    };

    public void setOnScanResultListener(BleInterface.ScanResultLinstener l) {
        m_device.setOnScanListener(l);
    }

    public boolean isScanning() {
        return m_device.isScanned();
    }

    //TODO: 우심리더기 스캔체크 추가 jiw230223
    public boolean isWoosimScanning() {
        return m_device.isWoosimScanned();
    }
    public void BleWoosimScan(boolean b) {
        if (b) {
            m_device.scanWoosimDevice(b);
        } else {
            if (isWoosimScanning()) {
                m_device.scanWoosimDevice(b);
            }
        }

    }

    public void BleScan(boolean b) {
        if (b) {
            m_device.scanLeDevice(b);
        } else {
            if (isScanning()) {
                m_device.scanLeDevice(b);
            }
        }
    }

    public BluetoothAdapter GetBlueToothAdapter()
    {
        return BluetoothAdapter.getDefaultAdapter();
    }

    boolean _bondChange = false;
    BroadcastReceiver mBluetoothSearchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();   //입력된 action
            Log.d("Bluetooth action", action);
            final BluetoothDevice device =   intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String name = null;
            if (device != null) {
                name = device.getName();    //broadcast를 보낸 기기의 이름을 가져온다.
            }
            //TODO:우심리더기라면 이곳에서 받지 않는다 jiw230223
            if(name != null) {
//                if (!name.contains("WOOSIM")) {
//                if (!name.contains("KRE")) {
                if (!name.contains("KRE") && !name.contains("KMR")) {
                    Log.d("Bluetooth name", name);
                    return;
                }
            }

            if(Setting.getIsWoosim()) {
                Log.d("Bluetooth id ", " = Woosim");
                return;
            } else {
                if(name == null) {
                    Log.d("Bluetooth name is :", "null");
                    return;
                }
            }

            String finalName = name;
            String finalName1 = name;
            String finalName2 = name;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if(finalName != null)
                        Log.d("Bluetooth name", finalName);
                    switch (action) {
                        case BluetoothAdapter.ACTION_STATE_CHANGED: //블루투스의 연결 상태 변경
                            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                            switch(state) {
                                case BluetoothAdapter.STATE_OFF:
                                case BluetoothAdapter.STATE_TURNING_OFF:
                                    Log.d(TAG, "STATE_OFF");
                                    if (m_bletimer != null) {
                                        m_bletimer.cancel();
                                        m_bletimer = null;
                                    }
                                    if (m_bletimer2 != null) {
                                        m_bletimer2.cancel();
                                        m_bletimer2 = null;
                                    }
//                            unregister(mBluetoothSearchReceiver);
//                            m_ctx.unregisterReceiver(mBluetoothSearchReceiver);
                                    mConnectResult.onState(false);
                                    WhileFinish = true;
                                    Setting.setBleIsConnected(false);
                                    DisConnect();
                                    break;
                                case BluetoothAdapter.STATE_DISCONNECTED:
                                case BluetoothAdapter.STATE_DISCONNECTING:
                                    Log.d(TAG, "STATE_DISCONNECTED");
                                    if (m_bletimer != null) {
                                        m_bletimer.cancel();
                                        m_bletimer = null;
                                    }
                                    if (m_bletimer2 != null) {
                                        m_bletimer2.cancel();
                                        m_bletimer2 = null;
                                    }
//                    unregister(mBluetoothSearchReceiver);
//                    m_ctx.unregisterReceiver(mBluetoothSearchReceiver);
                                    mConnectResult.onState(false);
                                    WhileFinish = true;
                                    Setting.setBleIsConnected(false);
                                    if (m_ctx instanceof AppToAppActivity)
                                    {
//                                        DisConnect();
                                    }
                                    DisConnect();
                                    break;
                                case BluetoothAdapter.STATE_ON:
                                    Log.d(TAG, "STATE_ON");
//                            registerReceiver(m_ctx);
                                    Activity _ac = (Activity) m_ctx;
                                    if(_ac.isFinishing())
                                    {
                                        return;
                                    }
                                    /**
                                     * STATE_OFF = 10;
                                     * STATE_TURNING_ON = 11;
                                     * STATE_ON = 12;
                                     * STATE_TURNING_OFF = 13;
                                     * STATE_BLE_TURNING_ON = 14;
                                     * STATE_BLE_ON = 15;
                                     * STATE_BLE_TURNING_OFF = 16;
                                     */

                                    switch (GetBlueToothAdapter().getState())
                                    {
                                        case 10:
                                        case 13:
                                        case 16:
                                            Toast.makeText(m_ctx,"블루투스, 위치 설정을 사용으로 해 주세요", Toast.LENGTH_SHORT).show();
                                            mConnectResult.onState(false);
                                            WhileFinish = true;
                                            Setting.setBleIsConnected(false);
                                            DisConnect();
                                            return;
                                        case 11:
                                            break;
                                        case 12:
                                            break;
                                        case 14:
                                            break;
                                        case 15:
                                            break;
                                    }
                                    MyBleListDialog myBleListDialog = new MyBleListDialog(m_ctx) {
                                        @Override
                                        protected void onSelectedBleDevice(String bleName, String bleAddr) {
                                            if (bleName.equals("") || bleAddr.equals(""))
                                            {
                                                return;
                                            }
//                                    BaseActivity _base = (BaseActivity) m_ctx;
//                                    _base.ReadyDialogShow(m_ctx,"장치연결중입니다",0);
//                    ShowDialog(); //여기서 장치연결중이라는 메세지박스를 띄워주어야 한다. 만들지 않아서 일단 주석처리    21-11-24.진우
                                            Connect(m_ctx,bleAddr, bleName);
                                            Setting.setBleName(bleName);
                                            Setting.setBleAddr(bleAddr);
//                                    Setting.setPreference(m_ctx, Constants.BLE_DEVICE_NAME, bleName);
//                                    Setting.setPreference(m_ctx, Constants.BLE_DEVICE_ADDR, bleAddr);
                                        }

                                        @Override
                                        protected void onFindLastBleDevice(String bleName, String bleAddr) {
                                            Connect(m_ctx,bleAddr, bleName);
                                            Setting.setBleName(bleName);
                                            Setting.setBleAddr(bleAddr);
                                        }
                                    };

                                    myBleListDialog.show();
//                           Connect(m_ctx,Setting.getPreference(m_ctx,Constants.BLE_DEVICE_ADDR),Setting.getPreference(m_ctx,Constants.BLE_DEVICE_NAME));
                                    break;
                                case BluetoothAdapter.STATE_TURNING_ON:
                                    Log.d(TAG, "STATE_TURNING_ON");
                                    break;

                            }

                            break;
                        case BluetoothDevice.ACTION_ACL_CONNECTED:  //블루투스 기기 연결
                            Log.d(TAG, "ACTION_ACL_CONNECTED");
                            if (m_bletimer != null) {
                                m_bletimer.cancel();
                                m_bletimer = null;
                            }

                            if (m_bletimer2 != null) {
                                m_bletimer2.cancel();
                                m_bletimer2 = null;
                            }

                            if (finalName2.contains("KREC")) {
                                m_bletimer2 = new BleTimer2(2 * 1000, 1000);
                                m_bletimer2.start();
                            }

                            break;
                        case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                            if(_bondChange)
                            {
                                return;
                            }
//                            mBleRepository.disconnectDevice();
                            _bondChange = true;
                            if (m_bletimer != null) {
                                m_bletimer.cancel();
                                m_bletimer = null;
                            }
                            if (m_bletimer2 != null) {
                                m_bletimer2.cancel();
                                m_bletimer2 = null;
                            }
                            BluetoothDevice paired = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            Log.d(TAG, BluetoothDevice.ACTION_BOND_STATE_CHANGED + " : "+paired.getBondState());

                            if (paired.getBondState() == BluetoothDevice.BOND_BONDED) {
                                //Setting.isConnected = true;
                                _bondChange = false;
                                //TODO 여기에 작업 추가 한다.
                                if (finalName2.contains("KREC")) {
                                    if( m_device != null) {
                                        if (m_device.m_LeService != null) {
                                            if (m_device.m_LeService.mBluetoothGatt != null) {
                                                m_device.m_LeService.mBluetoothGatt.discoverServices();
                                            }
                                        }
                                    }

                                    m_bletimer2 = new BleTimer2(2 * 1000, 1000);
                                    m_bletimer2.start();
                                }
                                else {
                                    Connect(m_ctx,addr, finalName2);
                                }

                            } else if (paired.getBondState() == BluetoothDevice.BOND_NONE) {
                                _bondChange = false;
                                if (!finalName2.contains("KREC")) {
                                    DisConnect();
                                }
                                if (finalName2.contains("KREC")) {
                                    paired.createBond();
                                }


//                                mConnectResult.onState(false);
//                                WhileFinish = true;
//                                Setting.setBleIsConnected(false);
//                                DisConnect();
                            } else
                            {
                                _bondChange = false;
//                                try { Thread.sleep(5000); } catch (InterruptedException e) {e.printStackTrace();}
//                                mBleRepository.disconnectDevice();
//                                DisConnect();
//                                paired.createBond();
                            }
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:   //블루투스 기기 끊어짐
                            if(isConnectCount > 0) {
                                Log.d(TAG,"ACTION_ACL_DISCONNECTED But isConnectCount = " + isConnectCount);
                                isConnectCount = 0;
                                return;
                            }
                            if (m_bletimer != null) {
                                m_bletimer.cancel();
                                m_bletimer = null;
                            }
                            if (m_bletimer2 != null) {
                                m_bletimer2.cancel();
                                m_bletimer2 = null;
                            }
                            mConnectResult.onState(false);
                            WhileFinish = true;
                            Setting.setBleIsConnected(false);
                            DisConnect();
//                    unregister(mBluetoothSearchReceiver);
//                    mConnectResult.onState(false);
//                    WhileFinish = true;
//                    DisConnect();
                            Log.d(TAG, "ACTION_ACL_DISCONNECTED");
                            break;

                        case BluetoothAdapter.ACTION_DISCOVERY_STARTED: //블루투스 기기 검색 시작
                            Log.d(TAG, "ACTION_DISCOVERY_STARTED");
                            break;
                        case BluetoothDevice.ACTION_FOUND:  //블루투스 기기 검색 됨, 블루투스 기기가 근처에서 검색될 때마다 수행됨
                            String device_name = device.getName();
                            String device_Address = device.getAddress();
                            //본 함수는 블루투스 기기 이름의 앞글자가 "GSM"으로 시작하는 기기만을 검색하는 코드이다
                            if(device_name != null && device_name.length() > 4){
                                Log.d("Bluetooth Name: ", device_name);
                                Log.d("Bluetooth Mac Address: ", device_Address);
                            }
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:    //블루투스 기기 검색 종료
                            Log.d(TAG, "Call Discovery finished");
                            break;
                        case BluetoothDevice.ACTION_PAIRING_REQUEST:
                            if (m_bletimer != null) {
                                m_bletimer.cancel();
                                m_bletimer = null;
                            }
                            if (m_bletimer2 != null) {
                                m_bletimer2.cancel();
                                m_bletimer2 = null;
                            }
                            Log.d(TAG, "ACTION_PAIRING_REQUEST");
//                            if (finalName2.contains("KMR-K")) {
//                                Connect(m_ctx,addr, finalName2);
//                            }
                            break;
                    }
                }
            });

//            String action = intent.getAction();


        }
    };

    public void setConnectionListener(bleSdkInterface.ConnectionListener I)
    {
        mConnectResult = I;
    }

    class BleTimer extends CountDownTimer
    {
        public BleTimer(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d(TAG,"BleTimeOutCount1 : " + millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {

            Log.d(TAG,"BleTimeOutCount1 : onFinish");
            mConnectResult.onState(false);
            DisConnect();
        }
    }

    class BleTimer2 extends CountDownTimer
    {
        public BleTimer2(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
            Toast.makeText(m_ctx,"잠시만 기다려 주세요", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d(TAG,"BleTimeOutCount2 : " + millisUntilFinished/1000);
//            Toast.makeText(m_ctx,"잠시만 기다려 주세요", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFinish() {
            isConnectCount = 0;
            Log.d(TAG,"BleTimeOutCount2 : onFinish");
            m_LeService = m_device.m_LeService;
            m_LeService.broadcastUpdate(m_LeService.ACTION_GATT_SERVICES_DISCOVERED);
            mResultLinstener.ConnectionResultLinstener(STATE_CONNECTED);
        }
    }

    public boolean isRegistered;
    public Intent register(android.content.BroadcastReceiver receiver, IntentFilter filter) {
        try {
            // ceph3us note:
            // here I propose to create
            // a isRegistered(Contex) method
            // as you can register receiver on different context
            // so you need to match against the same one :)
            // example  by storing a list of weak references
            // see LoadedApk.class - receiver dispatcher
            // its and ArrayMap there for example
            return !isRegistered
                    ? m_ctx.registerReceiver(receiver, filter)
                    : null;
        } finally {
            isRegistered = true;
        }
    }

    public boolean unregister(android.content.BroadcastReceiver receiver, Context _ctx) {
        // additional work match on context before unregister
        // eg store weak ref in register then compare in unregister
        // if match same instance
        return unregisterInternal(receiver,_ctx);
    }

    private boolean unregisterInternal(android.content.BroadcastReceiver receiver, Context _ctx) {
        try {
            _ctx.unregisterReceiver(receiver);
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }

        isRegistered = false;
        return true;
    }

    /**
     * 여기부터는 광우ble 리더기 연결 및 쓰기 읽기처리
     */


}