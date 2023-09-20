package com.koces.androidpos.sdk.SerialPort;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.koces.androidpos.sdk.ForegroundDetector;
import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.Devices.Devices;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.log.LogFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.pax.gl.commhelper.IComm;
import com.pax.gl.commhelper.ICommUsbHost;
import com.pax.gl.commhelper.exception.CommException;
import com.pax.gl.commhelper.impl.GLCommDebug;
import com.pax.gl.commhelper.impl.PaxGLComm;

public class KocesSerial {
    private static final String TAG = KocesSerial.getClassName();
    private Context m_Ctx;
    private boolean initSet = true;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    LogFile mLogFile;
    PendingIntent mPermissionIntent;
    private KocesPosSdk mKocesSdk;
    HashMap<UsbDeviceConnection, UsbDevice> usbDeviceConnections = new HashMap<>();
    ArrayList<FTDISerial> serials = new ArrayList<>();
    ArrayList<KByteArray> mResDatas = new ArrayList<>();
    //private KByteArray mResData;

    private ICommUsbHost mCommUsbHost;

    private ICommUsbHost.IUsbDeviceInfo mUsbDeviceInfo;

    class VidPid {
        public VidPid(int vid, int pid) {
            mVid = vid;
            mPid = pid;
        }
        public int mVid;
        public int mPid;
    };
    ArrayList<VidPid> knownVidPidz = new ArrayList<>();
    private static final int mBaudRate = 38400;
    private byte ExceptionSituation;
    private static final int MAX_SERIAL_DEVICE_COUNT = 2;
    //private SerialInterface.DataListener mDataListener;
    public Handler mHandler;
    public SerialInterface.ConnectListener mConnListener;

    /** 연속해서 장치가 들어올 경우. 검사하는데 한번에 하기 힘들다. 따라서 USB 연결확인체크 요청을 딜레이를 줘야한다. */
    boolean _serialCount = false;
    /** 퍼미션을 줘야할 장비가 여러개라 큐로 관리해 본다 */
//    private UsbManager[] _queueManagers;
//    private UsbDevice[] _queueDevices;
    Queue<UsbManager> _queueUsbManager;
    Queue<UsbDevice> _queueUsbDevices;

    public KocesSerial(Context _ctx,Handler _handler, SerialInterface.ConnectListener _listener2) {
        if (_ctx != null) {
            m_Ctx = _ctx;
        }
        _queueUsbManager = new LinkedList<>();
        _queueUsbDevices = new LinkedList<>();
        mHandler = _handler;
        mConnListener = _listener2;
        knownVidPidz.add(new VidPid(1027, 24597)); //1027 = 0x0403 ,24597 = 0x6015
        knownVidPidz.add(new VidPid(1027, 24577)); //1027 = 0x0403 ,24577 = 0x6001
        if (mLogFile == null) {
            mLogFile = new LogFile(m_Ctx);
        } else {
            mLogFile = LogFile.getinstance();
        }

        mLogFile.writeLog("KocesSerial Init(초기화시작)\n");
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        m_Ctx.getApplicationContext().registerReceiver(screenOffReceiver, screenFilter);

        GLCommDebug.setDebugLevel(GLCommDebug.EDebugLevel.DEBUG_LEVEL_ALL);
        if (mCommUsbHost == null)
            mCommUsbHost = PaxGLComm.getInstance(m_Ctx).createUsbHost();

        init();
    }

    /** 화면꺼짐모드에 들어갔을 때 처리 */
    private final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("android.intent.action.SCREEN_OFF")) {

//                actFinish();
//                finishAndRemoveTask();
//                android.os.Process.killProcess(android.os.Process.myPid());
            }
            else if(intent.getAction().equals("android.intent.action.SCREEN_ON")){
                try {
                    releaseUsb(null);
                    connectUsb(null);
                }
                catch (IllegalArgumentException e)
                {
                    e.printStackTrace();
                }
            }
        }
    };
    public void SerialDataSet(Handler _handler, SerialInterface.ConnectListener _listener2)
    {
//        mHandler = _handler;
//        mConnListener = _listener2;
        if(initSet)
        {
            initSet = false;
        }
        connectUsb(null);
    }

    /** 시러얼 관련 정보를 초기화 하고 연결 있는 시리얼 포트를 닫는다 */
    public void Close()
    {
        mLogFile.writeLog("KocesSerial Close(등록된리시버제거)\n");
        try {
            _serialCount = false;
            m_Ctx.getApplicationContext().unregisterReceiver(mUsbReceiver);
            m_Ctx.getApplicationContext().unregisterReceiver(mUsbDeviceReceiver);
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 시리얼 장치에 데이터를 쓰는 함수
     * @param _b 데이터
     * @param _target 데이터를 전송할 대상
     */
    public void write(byte[] _b,String[] _target) {
        ExceptionSituation = (byte)0x00;
        if(_b[0]==Command.STX)
        {
            ExceptionSituation = _b[3];
        }
        for(String n:_target) {
            for (FTDISerial s : serials) {

                if (s.device.getDeviceName().equals(n)) {
                    Log.d("KocesPacketData", "[POS -> DEVICE]");
                    Log.d("KocesPacketData", "TARGET DEVICES ADDRESS : " + s.device.getDeviceName());
                    Log.d("KocesPacketData", Utils.bytesToHex_0xType(_b));
                    mLogFile.writeLog("[POS -> DEVICE]\n");
                    mLogFile.writeLog("Utils.bytesToHex_0xType(_b)\n");
                    s.write(_b);
                    break;
                }
            }
        }
    }

    /**
     * 시리얼 클래스 초기화 함수
     */
    public void init() {
        int _flag = 0;
        if (Build.VERSION.SDK_INT >= 31) {
            _flag = PendingIntent.FLAG_MUTABLE;
        }
        Intent intent = new Intent(ACTION_USB_PERMISSION);
        intent.putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true);
        mPermissionIntent = PendingIntent.getBroadcast(m_Ctx.getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), _flag);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        Close();

        try {
            m_Ctx.getApplicationContext().registerReceiver(mUsbReceiver, filter);
            m_Ctx.getApplicationContext().registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
            m_Ctx.getApplicationContext().registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        mLogFile.writeLog("connectUsb(null)(usb사용퍼미션체크후.usb연결초기화)\n");
        connectUsb(null);
    }

    /** 최신기종에서 장치를 붙였다 때거나 최초 실행시 인식이 안된다 */
    public void ReInit()
    {
        int _flag = 0;
        if (Build.VERSION.SDK_INT >= 31) {
            _flag = PendingIntent.FLAG_MUTABLE;
        }
        mPermissionIntent = PendingIntent.getBroadcast(m_Ctx.getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), _flag);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        Close();

        try {
            m_Ctx.getApplicationContext().registerReceiver(mUsbReceiver, filter);

            m_Ctx.getApplicationContext().registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
            m_Ctx.getApplicationContext().registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        mLogFile.writeLog("connectUsb(null)(usb사용퍼미션체크후.usb연결초기화)\n");

        UsbManager manager = (UsbManager) m_Ctx.getApplicationContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            UsbDevice dev = deviceIterator.next();
            //TODO 현재 VendorID와 ProductID가 다를 수 있기 때문에 지정 하지 않는다.
            if (IsKnownVidPid(dev.getVendorId(), dev.getProductId()))
            {
                manager.requestPermission(dev, mPermissionIntent);
                mLogFile.writeLog("ConnectUsbSerial 복수 장비 =>" + dev.getDeviceName() + "\n");
                //mConnListener.onState(true, "ConnectUsbSerial 복수 장비 초기 연결 시작",dev.getDeviceName());
                addResponseBuffer(dev.getDeviceName());
//                    for(UsbDevice n: manager.getDeviceList().values())
//                    {
//                        if(!manager.hasPermission(n))
//                        {
//                            manager.requestPermission(n,mPermissionIntent);
//                        }
//                    }
            }
            else
            {
                mConnListener.onState(false, "VID or PID is different or device not found","");
                Log.d(TAG, "VID or PID is different or device not found");
            }
        }

    }

    //2020-07-30 kim.jy 특수한 경우에만 사용하는 방법 (앱 최초실행시 이벤트 발생이 없는 경우에재 연결 구조)
    private void connectUsb2(UsbDevice device) {

        //인증을 위해서 로그 삭제
        //Log.d(TAG, "connectUsb()");

        UsbManager manager = (UsbManager) m_Ctx.getApplicationContext().getSystemService(Context.USB_SERVICE);

        if (device != null)
        {
            if (IsKnownVidPid(device.getVendorId(), device.getProductId()))
            {
                connectUsbSerial(manager, device);
                addResponseBuffer(device.getDeviceName());
            }

        }
    }

    /**
     * 시리얼 장치 연결 함수
     * @param device 연결 대상
     */
    public void connectUsb(UsbDevice device) {

        //인증을 위해서 로그 삭제
        //Log.d(TAG, "connectUsb()");

        UsbManager manager = (UsbManager) m_Ctx.getApplicationContext().getSystemService(Context.USB_SERVICE);

        if (device != null)
        {

            if (IsKnownVidPid(device.getVendorId(), device.getProductId()))
            {
                connectUsbSerial(manager, device);
                if(manager.hasPermission(device)) {
                    if (Setting.DeviceType(m_Ctx) != Setting.PayDeviceType.LINES)
                    {
                        Setting.setIsUSBConnectMainStart(false);
                        return;
                    }
                    //2020-07-31 kim.jy
                    mLogFile.writeLog("ConnectUsbSerial 단일 장비");
                    mConnListener.onState(true, "ConnectUsbSerial 단일 장비 초기 연결 시작", device.getDeviceName());
                }
                addResponseBuffer(device.getDeviceName());
            }

        }
        else
        {
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            while (deviceIterator.hasNext()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                UsbDevice dev = deviceIterator.next();
                //TODO 현재 VendorID와 ProductID가 다를 수 있기 때문에 지정 하지 않는다.
                if (IsKnownVidPid(dev.getVendorId(), dev.getProductId()))
                {
                    connectUsbSerial(manager, dev);
                    mLogFile.writeLog("ConnectUsbSerial 복수 장비 =>" + dev.getDeviceName() + "\n");
                    //mConnListener.onState(true, "ConnectUsbSerial 복수 장비 초기 연결 시작",dev.getDeviceName());
                    addResponseBuffer(dev.getDeviceName());
//                    for(UsbDevice n: manager.getDeviceList().values())
//                    {
//                        if(!manager.hasPermission(n))
//                        {
//                            manager.requestPermission(n,mPermissionIntent);
//                        }
//                    }
                }
                else
                {
                    mConnListener.onState(false, "VID or PID is different or device not found","");
                    Log.d(TAG, "VID or PID is different or device not found");
                }
            }
        }
    }

    public void connectKWANGWOO() {
        mUsbDeviceInfo = null;
        ArrayList<ICommUsbHost.IUsbDeviceInfo> arrayList = mCommUsbHost.getPeerDevice();
        for (ICommUsbHost.IUsbDeviceInfo iUsbDeviceInfo : arrayList) {
            //根据实际情况进行device过滤
            if (iUsbDeviceInfo.isPaxDevice() && iUsbDeviceInfo.getDevice()!=null&&iUsbDeviceInfo.getDevice().getDeviceId() != 1007) {
                mUsbDeviceInfo = iUsbDeviceInfo;
                UsbDevice usbDevice = mUsbDeviceInfo.getDevice();
                Log.d(TAG, "========================");
                Log.d(TAG, "deviceName:" + usbDevice.getDeviceName());
                Log.d(TAG, "deviceVid:" + usbDevice.getDeviceId());
                Log.d(TAG, "devicePid:" + usbDevice.getProductId());
//                Log.d(Constant.TAG, "deviceSN:" + usbDevice.getSerialNumber());
                Log.d(TAG, "deviceManufacturerName:" + usbDevice.getManufacturerName());
                Log.d(TAG, "========================");
//                mCommUsbHost.setPaxDaemonMode(true);
                mCommUsbHost.setUsbDevice(mUsbDeviceInfo.getDevice(), null, 0);
                mCommUsbHost.setPaxSpecialDevice(false);
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mUsbDeviceInfo != null) {
                    try {
                        Log.d(TAG, "usb conn start");
                        mCommUsbHost.connect();
                        Log.d(TAG, "usb conn success");
                    } catch (CommException e) {
                        e.printStackTrace();
                        Log.d(TAG, "usb conn error");
                    }
                }
            }
        }).start();
    }

    public void sendKWANG() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int length=16;
                    byte[] bytes = new byte[length];
                    bytes = new byte[]{0x02,0x00,0x02,0x50,0x03,0x51};
                    Log.d(TAG, "usb send start");
                    if (mCommUsbHost != null) {
                        if (mCommUsbHost.getConnectStatus() != IComm.EConnectStatus.CONNECTED) {
                            return;
                        }
                    }
                    mCommUsbHost.send(bytes);
                    Log.d(TAG, "usb send success:" + Arrays.toString(bytes));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d(TAG, "usb recv start");
                                if (mCommUsbHost != null) {
                                    if (mCommUsbHost.getConnectStatus() != IComm.EConnectStatus.CONNECTED) {
                                        return;
                                    }
                                }
                                byte[] bytes = mCommUsbHost.recvNonBlocking();
                                Log.d(TAG, "usb recv success:" + Arrays.toString(bytes));
                            } catch (CommException e) {
                                e.printStackTrace();
                                Log.d(TAG, "usb recv error");
                            }
                        }
                    }).start();
                } catch (CommException e) {
                    e.printStackTrace();
                    Log.d(TAG, "usb send error");
                }
            }
        }).start();
    }

    public void disconnectKWANG() {
        try {
            Log.d(TAG, "usb disconnect start");
            mCommUsbHost.disconnect();
            Log.d(TAG, "usb disconnect success");
        } catch (CommException e) {
            e.printStackTrace();
            Log.d(TAG, "usb disconnect error");
        }
    }

    /**
     * 시리얼 장치를 필터링 하기 위한 검사 함수
     * @param vendorId
     * @param productId
     * @return
     */
    private boolean IsKnownVidPid(int vendorId, int productId) {
        Log.d("vendorId : ", String.valueOf(vendorId));
        Log.d("productId : ", String.valueOf(productId));
        for(VidPid vp : knownVidPidz)
        {
            if(vendorId == vp.mVid && productId == vp.mPid)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 연결 상태를 체크 하기 위한 함수
     * @param _busName 시리얼 버스 주소
     * @return
     */
    public boolean reCheckConnectedUsbSerial(String _busName)
    {
        UsbManager manager = (UsbManager) m_Ctx.getApplicationContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice dev = deviceIterator.next();
            //TODO 현재 VendorID와 ProductID가 다를 수 있기 때문에 지정 하지 않는다.
            if (IsKnownVidPid(dev.getVendorId(), dev.getProductId()))
            {
                if(_busName.equals(dev.getDeviceName()))
                {
                    return true;
                }
            }
        }
        return false;       //현재 연결된 시리얼포트 주소와 파라미터로 받은 주소가 다르기 때문에 return false
    }

    /**
     * 현재 연결 되어 있는 시리얼 장치 카운트
     * @return
     */
    public int CheckConnectedUsbSerialCount()
    {
        int Count = 0;
        UsbManager manager = (UsbManager) m_Ctx.getApplicationContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice dev = deviceIterator.next();
            //TODO 현재 VendorID와 ProductID가 다를 수 있기 때문에 지정 하지 않는다.
            if (IsKnownVidPid(dev.getVendorId(), dev.getProductId()))
            {
                Count++;
            }
        }
        return Count;
    }

    /**
     * 현재 연결되어 있는 장치 전체 이름을 리턴한다.
     * @return String 배열
     */
    public String[] CheckConnectedUsbSerialDeviceName()
    {
        ArrayList<String> DeviceName = new ArrayList<>();
        UsbManager manager = (UsbManager) m_Ctx.getApplicationContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice dev = deviceIterator.next();
            //TODO 현재 VendorID와 ProductID가 다를 수 있기 때문에 지정 하지 않는다.
            if (IsKnownVidPid(dev.getVendorId(), dev.getProductId()))
            {
                DeviceName.add(dev.getDeviceName());
            }
        }

        if(DeviceName.size()>0) {
            String[] DeviceNames = new String[DeviceName.size()];
            int count = 0;
            for (String n : DeviceName) {
                DeviceNames[count++] = n;
            }

            return DeviceNames;
        }

        return null;
    }

    /**
     * 지정된 주소가 있는지 여부를 검사 하고 연결 되어 있지 않은 경우에 재연결 하는 함수
     * @param _name
     */
    public void CheckConnectState(String _name)
    {
        UsbDevice device = null;
        UsbManager manager = (UsbManager) m_Ctx.getApplicationContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice dev = deviceIterator.next();
            //TODO 현재 VendorID와 ProductID가 다를 수 있기 때문에 지정 하지 않는다.
            if (IsKnownVidPid(dev.getVendorId(), dev.getProductId()))
            {
                if(dev.getDeviceName().equals(_name))
                {
                    device = dev;
                }
            }
        }

        if(device!=null) {
            boolean bcheck = false;
            for (FTDISerial n : serials) {
                if (n.device.getDeviceName().equals(device.getDeviceName())) {
                    bcheck = true;
                }
            }
            if (!bcheck) {
                connectUsb(device);
                //mConnListener.onState(true, "ConnectUsbSerial 단일 장비 초기 연결 시작", device.getDeviceName());
            }
        }
//        if(device!=null) {
//            UsbDeviceConnection devconn = manager.openDevice(device);
//            if(!IsAlreadyConnected(devconn))
//            {
//                connectUsbSerial(manager,device);
//            }
//        }
    }
    private void connectUsbSerial(UsbManager manager, UsbDevice device) {

        try {


        Boolean permitToRead = manager.hasPermission(device);
        if (permitToRead)
        {
            if (Setting.DeviceType(m_Ctx) != Setting.PayDeviceType.LINES)
            {
                return;
            }
            Log.d("connectUsbSerial device : ", String.valueOf(device));
            UsbDeviceConnection devconn = manager.openDevice(device);
            if (devconn != null && IsAlreadyConnected(devconn) == false)
            {
                FTDISerial ser = new FTDISerial(device, devconn);
                if (ser.open())
                {
                    ser.setBaudRate(mBaudRate);
//                            serial.setDataBits(SerialBase.DATA_BITS_8);
//                            serial.setParity(SerialBase.PARITY_NONE);
//                    mConnListener.onState(true,"", device.getDeviceName());
                    ser.read_start(new SerialBase.SerialReadCallback() {
                        @Override
                        public void onReceivedData(byte[] data,String busName)
                        {
                            //String s = new String(data);
                            //String rcvd_msg = "[rcvd] " + s + "\n";
                            //mEditTextReceived.append(rcvd_msg);
                            if(busName!=null)
                            {
                                for(KByteArray n:mResDatas)
                                {
                                    if(n.getBusName().equals(busName))
                                    {
                                        n.Add(data);
                                        checkProtocol(busName);
                                    }
                                }
                            }
                        }
                    });
                    boolean bcheck = false;
                    for(FTDISerial n:serials)
                    {
                        if(n.device.getDeviceName().equals(ser.device.getDeviceName()))
                        {
                            bcheck = true;
                        }
                    }
                    if(!bcheck)
                    {
                        serials.add(ser);
                    }
                    usbDeviceConnections.put(devconn, device);
                }
                else
                {
                    mConnListener.onState(false, "Failed to open serial!","");
                }
            }
        }
        else
        {
            if (_queueUsbDevices.isEmpty()) {
                _queueUsbManager.offer(manager);
                _queueUsbDevices.offer(device);
                Log.d(TAG, "Queue empty permission start");
            } else {
                Log.d(TAG, "Queue busy permission add");
                _queueUsbManager.offer(manager);
                _queueUsbDevices.offer(device);
                return;
            }
            manager.requestPermission(device, mPermissionIntent);
            mConnListener.onState(true, "Requested Permission","");
        }
        } catch (Exception e)
        {
            mConnListener.onState(false, "Failed to open serial!","");
            Log.d(TAG, "Error : " + e.toString());
            return;
        }
    }

    /** 위에서 모두 처리하지 않고 아래로 내려온다. 장치가 연속해서 권한을 묻기 때무에 이렇게 처리한다 */
    private void connectUsbSerial2()
    {
//        if (Setting.DeviceType(m_Ctx) == Setting.PayDeviceType.LINES)
//        {
//            if (_queueUsbDevices.isEmpty()) {
//                _queueUsbManager.offer(manager);
//                _queueUsbDevices.offer(device);
//                Log.d(TAG, "Queue empty permission start");
//                Boolean permitToRead = manager.hasPermission(device);
//                if (permitToRead){
//                    _queueUsbManager.poll();
//                    _queueUsbDevices.poll();
//                } else {
//                    manager.requestPermission(device, mPermissionIntent);
//                    mConnListener.onState(true, "Requested Permission","");
//                    return;
//                }
//            } else {
//                Log.d(TAG, "Queue busy permission add");
//                _queueUsbManager.offer(manager);
//                _queueUsbDevices.offer(device);
//                return;
//            }
//        } else {
//            _queueUsbManager.clear();
//            _queueUsbDevices.clear();
//        }
    }

    private boolean IsAlreadyConnected(UsbDeviceConnection conn) {
        if(usbDeviceConnections.get(conn) != null)
        {
            return true;
        }
        return false;
    }

    /**
     * 현재 USB권한이 있는지 없는지 검사 하기 위한 콜백 함수
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {

                mConnListener.onState(true, "ACTION_USB_PERMISSION","");
                Log.d(TAG, "ACTION_USB_PERMISSION");

                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    _queueUsbDevices.poll();
                    _queueUsbManager.poll();

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            connectUsb(device);
                        }
                    } else {
                        if ( device != null ) {
                            mConnListener.onState(false, "permission denied for device " + device.getDeviceName().toString(), "");
                            Log.d(TAG, "permission denied for device " + device);
                        } else {
                            mConnListener.onState(false, "permission denied for device " + ": devece is null", "");
                            Log.d(TAG, "permission denied for device " + ": devece is null");
                        }
                    }

                    if (_queueUsbManager.isEmpty() || _queueUsbDevices.isEmpty())
                    {
                        Log.d(TAG, "Queue Clear");
                        _queueUsbDevices.clear();
                        _queueUsbManager.clear();
                    }
                    else
                    {
                        Log.d(TAG, "Queue is not empty permission start");
                        UsbManager _tmpUSb = _queueUsbManager.poll();
                        UsbDevice _tmpDev = _queueUsbDevices.poll();
                        if (_queueUsbManager.isEmpty() || _queueUsbDevices.isEmpty())
                        {
                            Log.d(TAG, "Queue Clear");
                            _queueUsbDevices.clear();
                            _queueUsbManager.clear();
                        }
                        connectUsbSerial(_tmpUSb,_tmpDev);
                    }
                }
            }
        }
    };

    /**
     * 장치가 물리적으로 연결되고 분리되는 것을 감지 하기 위한 콜백 함수
     */
    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        private ArrayList<UsbDevice> devicesFound = new ArrayList<>();
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                long currentClickTime = SystemClock.uptimeMillis();
                long elapsedTime = currentClickTime - Setting.getIsmLastClickTime();
                Setting.setIsmLastClickTime(currentClickTime);

                // 중복클릭 아닌 경우
                if (elapsedTime <= Setting.getIsMIN_CLICK_INTERVAL()) {
                    Log.d(TAG,"현재  중복클릭 " +  elapsedTime);
//                    return;
                } else {
                    if (ForegroundDetector.getInstance().isForeground()) {
                        Setting.setIsAppForeGround(1);
                    } else {
                        Setting.setIsAppForeGround(2);
//                    return;
                    }
                }


                Log.d(TAG,"현재  ForegroundDetector.getInstance().isForeground() == " +  ForegroundDetector.getInstance().isForeground());
                Log.d(TAG,"현재  ForegroundDetector.getInstance().isBackground() == " +  ForegroundDetector.getInstance().isBackground());
                UsbDevice foundDev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                devicesFound.add(foundDev);

                mConnListener.onState(true, "ACTION_USB_DEVICE_ATTACHED" + "_usb연결시작_" +foundDev.toString(),"");
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Setting.setIsUSBConnectMainStart(true);
                        connectUsb(foundDev);
                    }
                },2500);

                //인증 때문에 임시로 로그를 막는다.
                Log.d(TAG, "ACTION_USB_DEVICE_ATTACHED: \n" + foundDev.toString());
                Setting.setIsUSBConnectMainStart(true);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                mConnListener.onState(false, "ACTION_USB_DEVICE_DETACHED" +"_usb연결해제시작_" + device.toString(),"");
                //인증 때문에 임시로 로그를 막는다.
                //Log.d(TAG, "ACTION_USB_DEVICE_DETACHED: \n" + device.toString());
                if (device != null) {
                    releaseUsb(device);
//                    Setting.setIsUSBConnectMainStart(false);
                    for (UsbDevice d : devicesFound) {
                        if( d == device) {
//                            releaseUsb(d);
                            devicesFound.remove(d);
                            break;
                        }
                    }
                    long currentClickTime = SystemClock.uptimeMillis();
                    long elapsedTime = currentClickTime - Setting.getIsmLastClickTime();
                    Setting.setIsmLastClickTime(currentClickTime);
                }
            }
        }
    };

    public void Dispose() {
        try {
            releaseUsb(null);
//            m_Ctx.getApplicationContext().unregisterReceiver(mUsbReceiver);
//            m_Ctx.getApplicationContext().unregisterReceiver(mUsbDeviceReceiver);
            mResDatas.clear();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
    }

    public String[] getSerialBusAddress()
    {
        int count=0;
        if(mResDatas.size()==0)
        {
            return null;
        }
        String[] tmp = new String[mResDatas.size()];
        for(KByteArray n: mResDatas)
        {
            tmp[count] = n.getBusName();
            count++;
        }
        return tmp;
    }

    private void releaseUsb(UsbDevice device) {
        if (device == null) {
            _serialCount = false;
            for (FTDISerial s : serials) {

                s.close();
            }
            serials.clear();
        } else {
            boolean retry = true;
            while(retry) {
                retry = false;

                for (FTDISerial s : serials) {
                    if (device.equals(s.device)) {
                        s.close();
                        serials.remove(s);
                        retry = true;
                        break;
                    }
                }
                //USB HUB사용시 에러 나는 문제 처리. 차후 배열 확인 필요
                try {
                    for (KByteArray n : mResDatas) {
                        if (n.getBusName().equals(device.getDeviceName())) {
                            mResDatas.remove(n);
                        }
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
//                mResDatas.clear();
//                addResponseBuffer(device.getDeviceName());
            }
        }

        mConnListener.onState(false, "releaseUsb()","");
        Log.d(TAG, "releaseUsb()");
        if(device == null)
        {
            _serialCount = false;
            Set<Map.Entry<UsbDeviceConnection, UsbDevice>> entries = usbDeviceConnections.entrySet();
            for (Map.Entry<UsbDeviceConnection, UsbDevice> e: entries) {
                UsbDeviceConnection c = e.getKey();
                UsbDevice d = e.getValue();
                c.close();
            }
            usbDeviceConnections.clear();
        }
        else {
            boolean retry = true;
            while(retry) {
                retry = false;
                Set<Map.Entry<UsbDeviceConnection, UsbDevice>> entries = usbDeviceConnections.entrySet();
                for (Map.Entry<UsbDeviceConnection, UsbDevice> e : entries) {
                    UsbDevice d = e.getValue();
                    if (device.equals(d)) {
                        UsbDeviceConnection c = e.getKey();
                        c.close();
                        usbDeviceConnections.remove(c);
                        retry = true;
                        break;
                    }
                }
            }
        }
    }

    private void checkProtocol(String _busName) {
        byte[] buffer;
        for(KByteArray mResData:mResDatas) {
            if(mResData.getBusName().equals(_busName))
            {
                if (mResData.indexData(0) == Command.STX && mResData.getlength() > 5) {
                    if (ExceptionSituation == (byte) 0xA1) {
                        buffer = mResData.CutToSize(Command.F_SIGNPADDATA_TOTAL_OVERROLL_SIZE);
                        Message message = new Message();
                        message.what = 3001;
                        message.obj = buffer;
                        mHandler.sendMessage(message);
                    } else {
                        if(mResData.getlength()>=Command.F_SIGNPADDATA_TOTAL_OVERROLL_SIZE){
                            while (mResData.indexData(1) == Command.CMD_SIGN_RES && mResData.indexData(4) == Command.ETX) {
                                mResData.CutToSize(Command.F_SIGNPADDATA_TOTAL_OVERROLL_SIZE);
                            }
                        }

                        byte[] blength = mResData.indexRangeData(1, 2);
                        int length = Utils.byteToInt(blength);

                        buffer = new byte[length];
                        if ((length + Command.F_CONTROL_DAT_BYTE) <= mResData.getlength()) {
                            buffer = mResData.CutToSize(length + Command.F_CONTROL_DAT_BYTE);
                            final byte[] tmp = buffer;

                            if (buffer[buffer.length - 2] != (byte) 0x03) {
                                //Log.d(TAG, "ETX 값이 맞지 않습니다.");
                            }
                            if (!Utils.CheckLRC(buffer)) {
                                //Log.d(TAG, "LRC 값이 맞지 않습니다.");
                            }
                            //Log.d(TAG,"CheckProtocol 길이 = " + String.valueOf(length) + "Bytes");
                            //Log.d(TAG,"mDatas Count " + mResDatas.size());
                            //Log.d(TAG,"mResData.getBusName() " + mResData.getBusName());
                            Message message = new Message();
                            message.what = 1001;
                            Log.d("KocesPacketData","[DEVICE -> POS]");
                            Log.d("KocesPacketData",Utils.bytesToHex_0xType(buffer));
                            message.obj = buffer;

                            mKocesSdk = KocesPosSdk.getInstance();
                            if (mKocesSdk.mDevicesList == null) {

                            } else {
                                for (Devices n : mKocesSdk.mDevicesList) {
                                    if (n.getmAddr().equals(_busName)) {
                                        message.arg1 = n.getmType();    //현재 응답을 주는 장치가 어떤 장치인지 알기 위해서 설정한다.
                                    }
                                }
                            }
                            mHandler.sendMessage(message);
                            //mDataListener.onRecviced(buffer);
                        }
                    }
                }
            }
        }
    }
    private void addResponseBuffer(String _busName)
    {
        boolean isExist = false;
        if(mResDatas.size()==0)
        {
            KByteArray tmp = new KByteArray();
            tmp.setMode(_busName);
            mResDatas.add(tmp);
            return;
        }
        else
        {
            for(KByteArray n : mResDatas)
            {
                if(n.getBusName().equals(_busName))
                {
                    isExist = true;
                    break;
                }
            }

            if(!isExist)
            {
                KByteArray tmp = new KByteArray();
                tmp.setMode(_busName);
                mResDatas.add(tmp);
            }
        }
    }
    public ArrayList<FTDISerial> getSerials()
    {
        return serials;
    }
    private static String getClassName() {
        return "KocesSerial";
    }
}
