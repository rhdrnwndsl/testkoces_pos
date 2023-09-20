package com.koces.androidpos.sdk.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.koces.androidpos.sdk.ble.BleInterface;
import com.koces.androidpos.sdk.ble.BluetoothLeService;
import com.koces.androidpos.sdk.ble.GattAttributes;
import com.koces.androidpos.sdk.van.Constants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;


public class bleDevice {
    private final static String TAG = "JamBleDevice";
    public static bleDevice instance;
    private Context m_context;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;
    private SrvConn m_srvConn;
    public BluetoothLeService m_LeService;
    private String m_BleAddr;
    private boolean m_pairingNotFirst = false;   //처음 페어링이라면 false 이미 페어링된 기종이면 true
    private static boolean isScanning;
    private static int DataSendingResult;

    public ArrayList<BluetoothDevice> mLeDevices = new ArrayList();
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private static int BLE_SEND_MAX_BUFFER_SIZE = 20;

    BleInterface.ResultLinstener mDataLinstener;
    BleInterface.ScanResultLinstener mScanResultLinstener;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int DATA_PACKET =10001;
    private static final int REQ_PERMISSION_COARSE_LOCATION = 1;
    public static int Mtu = 150;
    private Thread Thd_Send;

    private static boolean isWoosimScanning;


    public static synchronized void setDataSendingResult(int result)
    {
        DataSendingResult = result;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    if (device.getName() != null && device.getName().contains(Constants.WSP)) {
                        if (mScanResultLinstener != null) {
                            mScanResultLinstener.onResult(1, device, 0, "스캔 데이터");
                        }
//                        if (mScanResultLinstener != null) {
//                            mScanResultLinstener.onScanFinished();
//                        }
                    } else if (device.getName() != null && device.getName().contains(Constants.WOOSIM)) {
                        if (mScanResultLinstener != null) {
                            mScanResultLinstener.onResult(1, device, 0, "스캔 데이터");
                        }
//                        if (mScanResultLinstener != null) {
//                            mScanResultLinstener.onScanFinished();
//                        }
                    }
//                    else if (device.getName() != null && device.getName().contains(Constants.KWANGWOO_KRE)) {
//                        if (mScanResultLinstener != null) {
//                            mScanResultLinstener.onResult(1, device, 0, "스캔 데이터");
//                        }
////                        if (mScanResultLinstener != null) {
////                            mScanResultLinstener.onScanFinished();
////                        }
//                    }
                } else {
                    if (device.getName() != null && device.getName().contains(Constants.WSP)) {
                        if (mScanResultLinstener != null) {
                            mScanResultLinstener.onResult(1, device, 0, "스캔 데이터");
                        }
//                        if (mScanResultLinstener != null) {
//                            mScanResultLinstener.onScanFinished();
//                        }
                    } else if (device.getName() != null && device.getName().contains(Constants.WOOSIM)) {
                        if (mScanResultLinstener != null) {
                            mScanResultLinstener.onResult(1, device, 0, "스캔 데이터");
                        }
//                        if (mScanResultLinstener != null) {
//                            mScanResultLinstener.onScanFinished();
//                        }
                    }

//                    else if (device.getName() != null && device.getName().contains(Constants.KWANGWOO_KRE)) {
//                        if (mScanResultLinstener != null) {
//                            mScanResultLinstener.onResult(1, device, 0, "스캔 데이터");
//                        }
////                        if (mScanResultLinstener != null) {
////                            mScanResultLinstener.onScanFinished();
////                        }
//                    }
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                setTitle(R.string.select_device);
//                if (mNewDevicesArrayAdapter.getCount() == 0) {
//                    String noDevices = getResources().getText(R.string.none_found).toString();
//                    mNewDevicesArrayAdapter.add(noDevices);
//                }
                mBluetoothLeScanner.stopScan(mScanCallback);
                isScanning = false;
                isWoosimScanning = false;
                if (mScanResultLinstener != null) {
                    mScanResultLinstener.onScanFinished();
                }
            }
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final int Rssi = result.getRssi();
            new Handler().post(new Runnable() {
                public void run() {
                    Log.v("err", "스캐닝된 Device의 rssi");
                    mLeDeviceListAdapter.addDevice(result.getDevice());
                    if (mScanResultLinstener != null && result.getDevice().getName() != null && result.getDevice().getName() != null) {
                        mScanResultLinstener.onResult(1,result.getDevice(),Rssi,"스캔 데이터");
                    }

                }
            });
        }
    };
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            final int Rssi = rssi;
            new Handler().post(new Runnable() {
                public void run() {
                    Log.v("err", "스캐닝된 Device의 rssi");
                    mLeDeviceListAdapter.addDevice(device);
                    if (mScanResultLinstener != null) {
                        mScanResultLinstener.onResult(1,device,Rssi,"스캔 데이터");
                    }

                }
            });
        }
    };
    public bleDevice(Context _ctx,BleInterface.ResultLinstener DataLinstener)
    {
        instance = this;
        m_context = _ctx;
        mDataLinstener = DataLinstener;
        init();
    }
    public void init()
    {
        isScanning = false;
        isWoosimScanning = false;
        if(GattAttributes.BLE_TRAN==null){
            GattAttributes.BLE_TRAN = UUID.fromString(GattAttributes.TRANS);
        }
        if(GattAttributes.BLE_TX==null){
            GattAttributes.BLE_TX = GattAttributes.UUID_TRANS_TX;
        }
        if(GattAttributes.BLE_RX==null)
        {
            GattAttributes.BLE_RX = GattAttributes.UUID_TRANS_RX;
        }
        if(GattAttributes.BLE_CHARACTERISTIC_CONFIG==null)
        {
            GattAttributes.BLE_CHARACTERISTIC_CONFIG = UUID.fromString(GattAttributes.UUID_CLIENT_CHARACTERISTIC_CONFIG);
        }
        if(GattAttributes.BLE_NOTIFY==null)
        {
            GattAttributes.BLE_NOTIFY = UUID.fromString(GattAttributes.NOTIFY);
        }

    }

    /**
     * Ble 모델 종류에 따라 값을 설정 한다.
     * @param type payfun -> 10, 아폴로 타입ㄴ -> 20;
     */
    public void setBleModelType(int type)
    {
        GattAttributes.mBleModelType = type;
    }
    public int getBleModelType()
    {
        return GattAttributes.mBleModelType;
    }
    public boolean CheckBleSupported()
    {
        if (mBluetoothAdapter == null)
        {
            //블루투스를 지원하지 않으면 장치를 끈다
            mScanResultLinstener.onResult(0,null,0,"블루투스를 지원 하지 않습니다");
            Log.d(TAG,"블루투스를 지원 하지 않습니다");
            return false;
        } else
            {
            //연결 안되었을 때
            if (!mBluetoothAdapter.isEnabled()) {
                //블루투스 연결
                mScanResultLinstener.onResult(0,null,0,"블루투스 기능이 꺼져 있습니다.");
                Log.d(TAG,"블루투스 기능이 꺼져 있습니다.");
//                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                m_context.startActivity(i);
                return false;
            }
        }
        return true;
    }
    public static bleDevice getInstance()
    {
        return instance;
    }
    /**
     * ble 연결
     */
    public void connect(String _Addr)
    {

        //kim.jy-20191018 => 장비가 초기화 되지 않았을 때 처리 방법 강구
        m_BleAddr = _Addr;
        m_pairingNotFirst = false;
        startService();
        return;
    }

    public void Disconnect()
    {
        if (m_LeService != null) {
            m_LeService.disconnect();
        }
        if (m_srvConn != null)
        {
            try {
                m_context.getApplicationContext().unbindService(m_srvConn);
            } catch (IllegalArgumentException ex) {
                // Do nothing, the connection was already unbound
            }

        }


    }

    public boolean write(byte[] _byte) {

        boolean result = false;
        DataSendingResult = 0;
        BluetoothGattCharacteristic charset = null;
        if (m_LeService != null) {
            List<BluetoothGattService> Services = m_LeService.getSupportedGattServices();

            if(Services != null)
            {
                for (BluetoothGattService s : Services) {
                    if (s != null) {
                        if (s.getUuid().equals(GattAttributes.BLE_TRAN)) {
                            charset = s.getCharacteristic(GattAttributes.BLE_TX);
                            if (charset != null) {

                                Queue<byte[]> bytesArray = new LinkedList<>();
                                int bufferSize = _byte.length - 3;
                                int location = 0;

                                byte[] tempHead = new byte[1];
                                System.arraycopy(_byte,0,tempHead,0,1);
                                bytesArray.offer(tempHead);
                                byte[] templength = new byte[2];
                                System.arraycopy(_byte,1,templength,0,2);
                                bytesArray.offer(templength);
                                while (true)
                                {
                                    if(bufferSize > BLE_SEND_MAX_BUFFER_SIZE)
                                    {
                                        byte[] temp = new byte[BLE_SEND_MAX_BUFFER_SIZE];
                                        System.arraycopy(_byte,location*BLE_SEND_MAX_BUFFER_SIZE+3,temp,0,temp.length);
                                        bytesArray.offer(temp);
                                        location++;
                                        bufferSize=bufferSize-BLE_SEND_MAX_BUFFER_SIZE;
                                    }
                                    else
                                    {
                                        byte[] temp = new byte[bufferSize];
                                        System.arraycopy(_byte,location*BLE_SEND_MAX_BUFFER_SIZE+3,temp,0,temp.length);
                                        bytesArray.offer(temp);
                                        break;
                                    }
                                }

                                result = writeCharacteristic(charset,bytesArray);
                            }
                        }
                    }
                }
            }
        } else {

        }


        return result;
    }

    public boolean writeCancel(byte[] _byte) {

        boolean result = false;
        DataSendingResult = 0;
        BluetoothGattCharacteristic charset = null;
        List<BluetoothGattService> Services = m_LeService.getSupportedGattServices();

        if(Services != null)
        {
            for (BluetoothGattService s : Services) {
                if (s != null) {
                    if (s.getUuid().equals(GattAttributes.BLE_TRAN)) {
                        charset = s.getCharacteristic(GattAttributes.BLE_TX);
                        if (charset != null) {

                            byte[] bytesArray = new byte[5];
                            bytesArray = _byte;
                            charset.setValue(bytesArray);
                            result = m_LeService.writeCharacteristic(charset);
//                            result = writeCancelCharacteristic(charset,bytesArray);
                        }
                    }
                }
            }
        }

        return result;
    }

    private boolean writeCharacteristic(BluetoothGattCharacteristic charset,Queue<byte[]> bytes){
        class runnable implements Runnable {
            private Queue<byte[]> mBytes;
            private BluetoothGattCharacteristic mCharset;
            public runnable(BluetoothGattCharacteristic charset,Queue<byte[]> bytes)
            {
                mBytes = bytes;
                mCharset = charset;
            }
            @Override
            public void run() {
                int TimeOutCount = 0;
                int SendDataCount = 0;
                //테스트 위해서 주석 처리 한다.
                //m_LeService.setCharacteristicNotification(mCharset, true);

                while (true)
                {
                    /* 5/1000 * 600 = 3 sec 데이터를 전송하는데 3초이상 걸리면 전달 실패라고 생각 하고 타임 아웃 처리 한다. */
                    /* 전체 데이터 전송 시간이 아니라 20byte 전달하는데 걸리는 시간이 3초 이상인 경우 */
                    if(TimeOutCount>600)
                    {
                        mBytes.clear();
                        byte[] tmp = new byte[]{0x01,0x02,0x44,0x41,0x54,0x41,0x20,0x53,0x45,0x4E,0x44,0x49,0x4E,0x47,0x20,0x46,0x41,0x49,0x4C,0x03};
                        mDataLinstener.MessageResultLinstener(tmp);
                        break;
                    }
                    if(mBytes.isEmpty())
                    {
                        Log.d("kim.jy","Total writeCharacteristic dataCount :" + String.valueOf(SendDataCount));
                        break;
                    }
                    if(DataSendingResult==0) {

                        mCharset.setValue(mBytes.poll());
                        //테스트 위해서 리턴값을 받는다.
                        DataSendingResult = -1;
                        TimeOutCount = 0;
                        SendDataCount++;
                        Log.d("kim.jy","1writeCharacteristic dataCount :" + String.valueOf(SendDataCount));
                        boolean result = m_LeService.writeCharacteristic(mCharset);

                        Log.d("kim.jy","1writeCharacteristic dataCount result:" + result);

                    }
                    else if(DataSendingResult > 0)  //onWriteChracteristic 에서 status가 이상한 값이 올라온 경우
                    {
                        mBytes.clear();
                        byte[] tmp = new byte[]{0x01,0x02,0x44,0x41,0x54,0x41,0x20,0x53,0x45,0x4E,0x44,0x49,0x4E,0x47,0x20,0x46,0x41,0x49,0x4C,0x03};
                        mDataLinstener.MessageResultLinstener(tmp);
                        break;
                    }
                    else
                    {
                        //현재 onWriteCharacteric 콜백을 기다리는 중
                    }
                    try {
                        Thread.sleep(10);
                        TimeOutCount++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        runnable run = new runnable(charset,bytes);
        if(Thd_Send!=null)
        {
            if(Thd_Send.isAlive())
            {
                try {
                    Thd_Send.join();
                }catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }

            Thd_Send = null;
        }

        Thd_Send = new Thread(run);
        Thd_Send.start();

        return true;
    }

    private boolean writeCancelCharacteristic(BluetoothGattCharacteristic charset,byte[] bytes){
        class runnable implements Runnable {
            private byte[] mBytes;
            private BluetoothGattCharacteristic mCharset;
            private boolean mCount = false;
            public runnable(BluetoothGattCharacteristic charset,byte[] bytes)
            {
                mBytes = bytes;
                mCharset = charset;
            }
            @Override
            public void run() {
                int TimeOutCount = 0;
                int SendDataCount = 0;
                //테스트 위해서 주석 처리 한다.
                //m_LeService.setCharacteristicNotification(mCharset, true);

                while (true)
                {
                    /* 5/1000 * 600 = 3 sec 데이터를 전송하는데 3초이상 걸리면 전달 실패라고 생각 하고 타임 아웃 처리 한다. */
                    /* 전체 데이터 전송 시간이 아니라 20byte 전달하는데 걸리는 시간이 3초 이상인 경우 */
                    if(TimeOutCount>600)
                    {
                        mBytes = null;
                        byte[] tmp = new byte[]{0x01,0x02,0x44,0x41,0x54,0x41,0x20,0x53,0x45,0x4E,0x44,0x49,0x4E,0x47,0x20,0x46,0x41,0x49,0x4C,0x03};
                        mDataLinstener.MessageResultLinstener(tmp);
                        break;
                    }
                    if(mBytes == null || mBytes.length <= 0)
                    {
                        Log.d("kim.jy","Total writeCharacteristic dataCount :" + String.valueOf(SendDataCount));
                        break;
                    }
                    if(DataSendingResult==0) {

                        if(mCount)
                        {
                            return;
                        }
                        mCharset.setValue(mBytes);
                        //테스트 위해서 리턴값을 받는다.
                        DataSendingResult = -1;
                        TimeOutCount = 0;
                        SendDataCount++;
                        Log.d("kim.jy","2writeCharacteristic dataCount :" + String.valueOf(SendDataCount));
                        boolean result = m_LeService.writeCharacteristic(mCharset);
                        Log.d("kim.jy","2writeCharacteristic dataCount result:" + result);
                        mCount = true;


                    }
                    else if(DataSendingResult > 0)  //onWriteChracteristic 에서 status가 이상한 값이 올라온 경우
                    {
                        mBytes = null;
                        byte[] tmp = new byte[]{0x01,0x02,0x44,0x41,0x54,0x41,0x20,0x53,0x45,0x4E,0x44,0x49,0x4E,0x47,0x20,0x46,0x41,0x49,0x4C,0x03};
                        mDataLinstener.MessageResultLinstener(tmp);
                        break;
                    }
                    else
                    {
                        //현재 onWriteCharacteric 콜백을 기다리는 중
                    }
                    try {
                        Thread.sleep(10);
                        TimeOutCount++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        runnable run = new runnable(charset,bytes);
        if(Thd_Send!=null)
        {
            if(Thd_Send.isAlive())
            {
                try {
                    Thd_Send.join();
                }catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }

            Thd_Send = null;
        }

        Thd_Send = new Thread(run);
        Thd_Send.start();

        return true;
    }

    public void read()
    {
        byte[] readData;
        BluetoothGattCharacteristic[] charset = null;
        List<BluetoothGattService> Services = m_LeService.getSupportedGattServices();
        for(BluetoothGattService s:Services)
        {
            if(s!=null)
            {
                if(s.getUuid().equals(GattAttributes.BLE_TRAN))
                {
                    charset[0] = s.getCharacteristic(GattAttributes.BLE_RX);
                    if(charset!=null){

                        readData = charset[0].getValue();
                        m_LeService.setCharacteristicNotification(charset,true);

                        m_LeService.readCharacteristic(charset[0]);
                    }
                }
            }
        }

    }
    /**
     * ble 연결 관련 권한 설정
     */
    private void startService()
    {
        m_srvConn = new SrvConn();
        m_context.getApplicationContext().bindService(new Intent(m_context, BluetoothLeService.class), m_srvConn, Context.BIND_AUTO_CREATE);
    }
    class SrvConn implements ServiceConnection{

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            m_LeService = ((BluetoothLeService.LocalBinder)service).getService();
            if(m_LeService.initialize(mDataLinstener))
            {
                Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        if (m_BleAddr.equals(deviceHardwareAddress)) {
                            //이미 페어링 된 기기라면
                            m_pairingNotFirst = true;
                        }
                    }
                }

                m_LeService.connect(m_BleAddr,m_pairingNotFirst);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            m_LeService.disconnect();
        }
    }


    public void setOnScanListener(BleInterface.ScanResultLinstener l)
    {
        mScanResultLinstener = l;
    }
    public void setOnResultListener(BleInterface.ResultLinstener l){mDataLinstener = l;}
    public static boolean isScanning() {
        return isScanning;
    }
    public synchronized boolean isScanned() {
        return isScanning;
    }
    public void scanLeDevice(final boolean enable) {
        if (this.checkSelfPermissionAndReqPermission()) {
            if (this.CheckSupportBluetoothLe()) {
                if (this.getBluetoothAdapter()) {
                    isScanning = true;
                    this.mLeDeviceListAdapter = new LeDeviceListAdapter();
                    if (mBluetoothLeScanner == null) mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    if (enable) {
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                mBluetoothLeScanner.stopScan(mScanCallback);
//                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                isScanning = false;
                                //TODO: 주석처리한다. 이유는 스캔시 우심리더기까지 처리가 완료되는 것을 확인해야 하기 때문이다 jiw230223
//                                if (mScanResultLinstener != null) {
//                                    mScanResultLinstener.onScanFinished();
//                                }

                            }
                        }, 5000);
                        mBluetoothLeScanner.startScan(this.mScanCallback);
//                        mBluetoothAdapter.startLeScan(this.mLeScanCallback);
                    } else {
                        isScanning = false;
                        mBluetoothLeScanner.stopScan(this.mScanCallback);
//                        mBluetoothAdapter.stopLeScan(this.mLeScanCallback);
                        isWoosimScanning = false;
                        if (this.mBluetoothAdapter.isDiscovering()) {
                            this.mBluetoothAdapter.cancelDiscovery();
                        }

                        if (mScanResultLinstener != null) {
                            mScanResultLinstener.onScanFinished();

                            //TODO: null 추가. 우심리더기 스캔완료를 위해서 null 체크를 위해 null 추가 jiw230223
                            mScanResultLinstener = null;
                        }
                    }

                }
            }
        }
    }

    //TODO: 우심리더기 스캔체크 처리 추가 jiw230223
    public synchronized boolean isWoosimScanned() {
        return isWoosimScanning;
    }
    public void scanWoosimDevice(final boolean enable) {
        if (this.getBluetoothAdapter()) {
            // Get the local Bluetooth adapter
            isWoosimScanning = true;
            if(enable) {
                // Register for broadcasts when a device is discovered
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                m_context.registerReceiver(mReceiver, filter);

                // Register for broadcasts when discovery has finished
                filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                m_context.registerReceiver(mReceiver, filter);
                if (this.mBluetoothAdapter.isDiscovering()) {
                    this.mBluetoothAdapter.cancelDiscovery();
                }
                // Request discover from BluetoothAdapter
                this.mBluetoothAdapter.startDiscovery();
            } else {
                isWoosimScanning = false;
                isScanning = false;
                mBluetoothLeScanner.stopScan(this.mScanCallback);

                if (this.mBluetoothAdapter.isDiscovering()) {
                    this.mBluetoothAdapter.cancelDiscovery();
                }
                if (mScanResultLinstener != null) {
                    mScanResultLinstener.onScanFinished();
                    mScanResultLinstener = null;
                }
            }
        }

    }

    private boolean checkSelfPermissionAndReqPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (m_context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && m_context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            else {
                //ActivityCompat.requestPermissions((Activity)m_context).requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERMISSION_COARSE_LOCATION);
                ActivityCompat.requestPermissions((Activity)m_context,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERMISSION_COARSE_LOCATION);
                //ActivityCompat.requestPermissions((Activity)m_context, new String[]{"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean CheckSupportBluetoothLe() {
        if (!m_context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            Log.v("err", "블루투스를 지원하는지 확인");
            return false;
        } else {
            return true;
        }
    }

    private boolean getBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager)m_context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = bluetoothManager.getAdapter();
        if (this.mBluetoothAdapter == null) {
            Log.v("err", "디바이스에서 블루투스를 지원 하지 않음");
            return false;
        } else {
            return true;
        }
    }


    class LeDeviceListAdapter {
        public LeDeviceListAdapter() {
            mLeDevices = new ArrayList();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                Log.v("err", "스캐닝된 Device를 리스트에 추가");
                mLeDevices.add(device);
            }

        }

        public BluetoothDevice getDevice(int position) {
            return (BluetoothDevice)mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        public int getCount() {
            return mLeDevices.size();
        }

        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        public long getItemId(int i) {
            return (long)i;
        }
    }
    public void setGattUUID(UUID _characteristic_config,UUID _tx,UUID _rx,UUID _trans,UUID _notify)
    {
        GattAttributes.BLE_CHARACTERISTIC_CONFIG = _characteristic_config;
        GattAttributes.BLE_TX = _tx;
        GattAttributes.BLE_RX = _rx;
        GattAttributes.BLE_TRAN = _trans;
        GattAttributes.BLE_NOTIFY = _notify;
    }

    public BluetoothLeService getBluetoothLeService(){return m_LeService;}

    public static void setMtu(int mtu) {
        Mtu = mtu;
    }

    //    public boolean write(String _str)
//    {
//
//        boolean result = false;
//        BluetoothGattCharacteristic charset = null;
//        List<BluetoothGattService> Services = m_LeService.getSupportedGattServices();
//
//
//
//        for(BluetoothGattService s:Services)
//        {
//            if(s!=null)
//            {
//
//                if(s.getUuid().equals(UUID.fromString(GattAttributes.TRANS)))
//                {
//                    charset = s.getCharacteristic(GattAttributes.UUID_TRANS_TX);
//                    if(charset!=null){
//
//                        charset.setValue(_str);
//                        //테스트 위해서 리턴값을 받는다.
//                        result= m_LeService.writeCharacteristic(charset);
//                        m_LeService.setCharacteristicNotification(charset,true);
//
//
//                    }
//                }
//            }
//        }
    //BluetoothGattService mTransTx = Servies.(GattAttributes.TRANS_TX);
//
//
//
//        return result;
//    }

//    public boolean write(byte[] _byte) {
//
//        boolean result = false;
//        BluetoothGattCharacteristic charset = null;
//        List<BluetoothGattService> Services = m_LeService.getSupportedGattServices();
//
//
//
//
//        for (BluetoothGattService s : Services) {
//            if (s != null) {
//                if (s.getUuid().equals(GattAttributes.BLE_TRAN)) {
//                    charset = s.getCharacteristic(GattAttributes.BLE_TX);
//                    if (charset != null) {
//
//                        charset.setValue(_byte);
//                        //테스트 위해서 리턴값을 받는다.
//                        result = m_LeService.writeCharacteristic(charset);
//                        m_LeService.setCharacteristicNotification(charset, true);
//                    }
//                }
//            }
//        }
//        //BluetoothGattService mTransTx = Servies.(GattAttributes.TRANS_TX);
//
//
//        return result;
//    }
}
