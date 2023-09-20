package com.koces.androidpos.sdk.Devices;

import android.bluetooth.BluetoothClass;
import android.view.ViewGroup;

/**
 * 장치 관련 정보를 저장 하는 클래스
 */
public class Devices {
    /**
     * Log를 위한 TAG 설정
     */
    private final static String TAG = "Devices";
    /**
     * 장치 이름
     */
    String mName;       //장치 이름
    /**
     * 장치 주소
     */
    String mAddr;       //주소
    /**
     * 약식 포트 넘버(화면에 표시 하기 위해서 필요한 정보
     */
    String mPortName;   //약식 포트 넘버(화면에 표시 하기 위해서 필요한 정보
    /**
     * 장치에서 가져온 장비 이름
     */
    String mDeviceName; //장치에서 가져온 장비 이름
    /**
     * //장치 시리얼 넘버
     */
    String mDeviceSerial;   //장치 시리얼 넘버

    /** 장치 버전 */
    String mDeviceVersion;  //장치 버전정보
    /**
     * 장치 연결 상태<br>
     * (사용하지 않음)
     */
    boolean isConnected;
    /**
     * 장치 타입
     */
    int mType;

    /**
     * 장치 설장 타입 <br>
     * 0:장비 없음, 1-IC카드 ,2- 서명패드,3- 멀티서명패드,4- 멀티패드,5-CAT ,6- BLE
     */
    private final static int NONE = 0,ICCARDREADER = 1,SIGNPAD = 2,MULTIPAD = 3,MULTIREADER = 4,CAT=5,BLE = 6;
    public Devices()
    {
        mName = "";
        mAddr = "";
        mPortName = "";
        mDeviceVersion = "";
        mType = NONE;
    }

    /**
     * 초기화 함수
     * @param _name 장치 이름
     * @param _Addr 장치 주소
     * @param _isConnected 연결 상태(현재는 체크 하지 않음)
     */
    public Devices(String _name,String _Addr,boolean _isConnected){
//        if(!isNullOrEmpty(_name)){ mName = _name;}
        if(!isNullOrEmpty(_Addr)){
            mAddr = _Addr;
            mPortName = PortName(_Addr);
        }
        mType = 0;
        isConnected = _isConnected;
    }
    /**
     * 초기화 함수 (BLE와 CAT을 받기 위해서 만든다.)
     * @param _name 장치 이름
     * @param _Addr 장치 주소
     * @param _isConnected 연결 상태(현재는 체크 하지 않음)
     */
    public Devices(String _name,String _Addr,int DeviceType,boolean _isConnected){
//        if(!isNullOrEmpty(_name)){ mName = _name;}
        if(!isNullOrEmpty(_Addr)){
            mAddr = _Addr;
            mPortName = PortName(_Addr);
        }
        mType = DeviceType;
        isConnected = _isConnected;
    }

    public boolean getConnected() { return isConnected; }

    /**
     * 연결 상태 설정<br>
     * (사용하지 않음)
     * @param _isconnected 연결 상태
     */
    public void setConnected(boolean _isconnected){isConnected = _isconnected;}

    /**
     * get 장치 버전
     */
    public String getVersion() { return mDeviceVersion; }

    /**
     * set 장치 버전
     */
    public void setVersion(String _version) {mDeviceVersion = _version;}


    /**
     * get 장치 이름
     * @return
     */
    public String getName()
    {
        return mName;
    }
    /**
     * set 장치 이름
     * @return
     */
    public void setName(String _name)
    {
        mName = _name;
    }
    /**
     * get 장치 주소
     * * @return
     */
    public String getmAddr() {
        return mAddr;
    }
    /**
     * set 장치 주소
     * * @return
     */
    public void setmAddr(String mAddr) {
        this.mAddr = mAddr;
        mPortName = PortName(mAddr);
    }
    /**
     * get 장치 타입
     * * @return
     */
    public int getmType() {
        return mType;
    }
    /**
     * set 장치 타입
     * * @return
     */
    public void setmType(int mType) {
        this.mType = mType;
    }

    public String getDeviceName(){return mDeviceName;}
    /**
     * get serial device
     * * @return
     */
    public String getDeviceSerial(){return mDeviceSerial;}

    /**
     * set 장치 이름
     * * @return
     */
    public void setDeviceName(String _deviceName){this.mDeviceName = _deviceName;}
    /**
     * set serial device
     * * @return
     */
    public void setDeviceSerial(String _deviceSerial){this.mDeviceSerial = _deviceSerial;}
    /**
     * 장비 타입을 설정 한다.<br>
     * @param _DeviceType 0:장비 없음, 1-IC카드 ,2- 서명패드,3- 멀티서명패드,4- 멀티패드,5-CAT ,6- BLE
     * @return
     */
    public boolean setType(int _DeviceType)
    {
        switch (_DeviceType){
            case 0:
                mType = NONE;
                break;
            case 1:
                mType = ICCARDREADER;
                break;
            case 2:
                mType = SIGNPAD;
                break;
            case 3:
                mType = MULTIPAD;
                break;
            case 4:
                mType = MULTIREADER;
                break;
            case 5:
                mType = CAT;
                break;
            case 6:
                mType = BLE;
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * NUll 또는 Empty체크 함수
     * @param _str 문자열
     * @return boolean
     */
    private boolean isNullOrEmpty(String _str){
        if(_str==null){
            return true;
        }
        if(_str.equals("")){
            return true;
        }
        return false;
    }

    /**
     * 장치가 연결된 주소의 마지막 포트 정보
     * @param _Port 장치 주소 마지막 포트 넘버
     * @return
     */
    private String PortName(String _Port)
    {
        String portHeader = "";
        String[] DevicesHeader = _Port.split("/");
        for (int i = 0; i < DevicesHeader.length - 1; i++) {
            if(!DevicesHeader[i].equals("")) {
                portHeader += "/" + DevicesHeader[i];
            }
        }
        portHeader += "/";

        return portHeader;
    }
}
