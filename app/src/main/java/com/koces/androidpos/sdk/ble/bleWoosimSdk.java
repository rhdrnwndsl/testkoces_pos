package com.koces.androidpos.sdk.ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.van.Constants;
import com.woosim.printer.WoosimCmd;
import com.woosim.printer.WoosimService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Set;

public class bleWoosimSdk {
    private final static String TAG = "bleWoosimSdk";

    private Context m_ctx;
    private static bleWoosimSdk instance = null;

    private bleWoosimInterface.ConnectionListener mConnectResult;

    private Handler mHandler;

    private int BleConnectTimeOut = 30;

    // Member object for the print services
    public bleWoosimDevice mPrintService = null;
    private WoosimService mWoosim = null;

    // Message types sent from the BluetoothPrintService Handler
    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_READ = 3;
    public static final int MESSAGE_CONNECTED_FAIL = 4;
    public static final int MESSAGE_CONNECTED_TIMEOUT = 5;

    // Key names received from the BluetoothPrintService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private static final int PERMISSION_DEVICE_SCAN_SECURE = 11;
    private static final int PERMISSION_DEVICE_SCAN_INSECURE = 12;

    public String addr = "";
    public String name = "";
    public static int mBTConnectResult = 0;

    public bleWoosimSdk(Context _ctx, bleWoosimInterface.ConnectionListener ConnectLinstener, Handler _handler) {
        m_ctx = _ctx;
        mConnectResult = ConnectLinstener;
        mHandler = _handler;
        initialize();

    }

    public static bleWoosimSdk getInstance() {
        if (instance != null) {
            return instance;
        }
        return null;
    }

    /**
     * 기본 함수 초기화
     */
    private void initialize() {
        instance = this;
        //다른 장비 테스트 코드
//            //DS-2000
//        final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//        final UUID RX_SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
//        final UUID RX_CHAR_UUID = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
//        final UUID TX_CHAR_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//        final UUID RX_NOTIFY = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
//        m_device.setGattUUID(CCCD, RX_CHAR_UUID, TX_CHAR_UUID, RX_SERVICE_UUID, RX_NOTIFY);
//        m_device.setBleModelType(20);   //DS-2000의 경우

        mPrintService = new bleWoosimDevice(mHandler);
        mWoosim = new WoosimService(mHandler);

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

        try{
            Set<BluetoothDevice> bluetoothDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                if(bluetoothDevice.getName() != null)
                {
                    if (bluetoothDevice.getName().contains(Constants.WSP) || bluetoothDevice.getName().contains(Constants.WOOSIM)   || bluetoothDevice.getName().contains(Constants.KWANGWOO_KRE))
                    {
                        if (isConnected(bluetoothDevice)) {
                            //TODO : 연결중인상태
                            _connected = true;
                        }else{
                            //TODO : 연결중이 아닌상태
//                    _connected = false;
                        }
                    }
                }


            }
        }catch(Exception e){
            //블루투스 서비스 사용불가인 경우
//            _connected = false;
        }

        return _connected;
    }

    public void Connect(String _Addr, String _Name)
    {
        addr = _Addr;
        name = _Name;
        mBTConnectResult = 0;
        mPrintService = new bleWoosimDevice(mHandler);

        if (mPrintService.getState() == bleWoosimDevice.STATE_CONNECTED) {
            return ;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not available.");
            mPrintService = null;
            return ;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is not active.");
            mPrintService = null;
            return ;
        }
        Setting.setIsWoosim(true);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(_Addr);

        //TODO: 연결시도를 할 때 프리페어런스에 있는 기존 데이터는 제거한다. 상황은 아래내용참조  //jiw 230727
        // ble 연결도중(무결성검사에서 안넘어가서 앱을 강제종료) 하였더니 다음에 기존에 마지막으로 성공했던 다른 ble 장비에 연결시도.
        // 이 후 설정에서 ble를 페어링 모두 해제하고 나니 다시 ble 무결성검사에서 안넘어가던 ble에 연결시도함
        Setting.setPreference(m_ctx, Constants.BLE_DEVICE_NAME, "");
        Setting.setPreference(m_ctx, Constants.BLE_DEVICE_ADDR, "");

        mPrintService.connect(device,false);

        // BT device connection 결과 대기
        mBTConnectResult = 0;
//        while (mBTConnectResult == 0) {
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        if (mBTConnectResult == 1) {
//            Log.d(TAG, "Bluetooth connection is succeed.");
//        } else {
//            Log.e(TAG, "Bluetooth connection is failed.");
//            Disconnect();
//        }

    }

    public void Disconnect()
    {
        if (mPrintService != null) {
            mPrintService.stop();
            mPrintService = null;
        }
        mBTConnectResult = 0;
//        bleSdk.getInstance().DisConnect();
    }




    public void printText() throws IOException {
        String string = "샘플입니다.가나다라마바사1234567";
        byte[] text = null;

        if (string == null)
            return;
        else {
            try {
//                text = string.getBytes("US-ASCII");
                text = string.getBytes("EUC-KR");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(WoosimCmd.setTextStyle(true, true, false, 1, 1));
        byteStream.write(WoosimCmd.setTextAlign(0));
        if (text != null) byteStream.write(text);
        byteStream.write(WoosimCmd.printData());

        mPrintService.write(WoosimCmd.initPrinter());
        mPrintService.write(byteStream.toByteArray());
    }


}