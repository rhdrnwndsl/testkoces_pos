package com.koces.androidpos.sdk;

import static com.koces.androidpos.sdk.ble.bleWoosimSdk.MESSAGE_CONNECTED_FAIL;
import static com.koces.androidpos.sdk.ble.bleWoosimSdk.MESSAGE_CONNECTED_TIMEOUT;
import static com.koces.androidpos.sdk.ble.bleWoosimSdk.MESSAGE_DEVICE_NAME;
import static com.koces.androidpos.sdk.ble.bleWoosimSdk.MESSAGE_READ;
import static com.koces.androidpos.sdk.ble.bleWoosimSdk.MESSAGE_TOAST;
import static com.koces.androidpos.sdk.ble.bleWoosimSdk.TOAST;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.koces.androidpos.AppToAppActivity;
import com.koces.androidpos.BaseActivity;
import com.koces.androidpos.Main2Activity;
import com.koces.androidpos.PopupActivity;
import com.koces.androidpos.sdk.Devices.AutoDetectDevices;
import com.koces.androidpos.sdk.Devices.Devices;
import com.koces.androidpos.sdk.SerialPort.FTDISerial;
import com.koces.androidpos.sdk.SerialPort.KocesSerial;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.ble.BleInterface;
import com.koces.androidpos.sdk.ble.GattAttributes;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.ble.bleSdkInterface;
import com.koces.androidpos.sdk.ble.bleWoosimInterface;
import com.koces.androidpos.sdk.ble.bleWoosimSdk;
import com.koces.androidpos.sdk.db.sqliteDbSdk;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.KocesTcpClient;
import com.koces.androidpos.sdk.van.NetworkInterface;
import com.koces.androidpos.sdk.van.TcpInterface;
import com.koces.androidpos.R;
import com.koces.androidpos.sdk.van.CatNetworkInterface;
import com.koces.androidpos.sdk.db.sqliteDbSdk.DBTradeResult;
import com.koces.androidpos.sdk.van.TmpNetworkInterface;
import com.woosim.printer.WoosimCmd;
import com.woosim.printer.WoosimService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스
 */
public class KocesPosSdk {
    private final static String TAG = "KocesPosSdk";
    private static KocesPosSdk Instance;
    private Context m_Context;
    private Activity m_Activity;
    public KocesSerial mSerial;
    public KocesTcpClient mTcpClient;
    private SerialInterface.DataListener mSerialDatalistener;
    private SerialInterface.DataListenerCash mSerialDatalistenerCash;
    private TcpInterface.DataListener mTcpDatalistener;
    private String mSignPad_Addr = "";
    private String mICReader_Addr = "";
    private String mMultiReader_Addr = "";
    private String mMultiPad_Addr = "";
    private sqliteDbSdk mSqlite;
    public BaseActivity mFocusActivity;
    public bleSdk mblsSdk;
    private TaxSdk mTaxSdk;     //22.02.08 kim.jy 세금 설정, 기록, 계산을 위해서 추가함.
    public static ArrayList<Devices> mDevicesList;         //디바이스 정보를 기록할 리스트
    BaseActivity.AppToAppUsbSerialStateListener mAToAUsbStatelistner;
    private bleSdkInterface.ResDataListener mResDataListener;
    private bleSdkInterface.ConnectionListener mResultConnectlistener;
    public bleWoosimSdk mbleWoosimSdk;
    private bleWoosimInterface.ConnectionListener mResultWoosimConnectlistener;

    //현재 tcp 진행중인지, pos 진행준인지 체크 0: 아직 둘다 아님, 1: pos로 데이터보냄 2:tcp로 데이터보냄
    private int mSendStatus = 0;

    //현재 현금영수증에서 서로다른 usb에 전문을 날려야 하는 상황인지를 체크(현금일 때 하나는 카드읽기 하나는 번호입력으로 보내야 하는 경우가 있다
    private boolean mSendUSBCashType = false;   //false = 그럴 일 없다. true = 그래야 하는 경 __PadRequestInputPasswd_no_encypt_Cash2 에서 올라온다

    // 우심데이터처리
    private ByteBuffer mRcvBuffer;
    private ReceiveTread mRcvTread;
    public static int mStatusWaiting = 0;
    public static int mPrinterStatus = 0;

    public static void setInstance(KocesPosSdk instance) {
        Instance = instance;
    }

    /**
     * 초기화 함수
     * @param _ctx Context
     */
    public KocesPosSdk(Context _ctx) {
        m_Context = _ctx;
        m_Activity = (Activity) m_Context;
        Instance = this;
        Setting.g_WaitingSerialDeviceAddr = new LinkedList<>();
        //mSerial = Setting.mUsbService.getSerial();
        //Setting.setSerialDataListener(mHandler,connectListener);
        mSerial = new KocesSerial(m_Context, mHandler, connectListener);
        mTcpClient = new KocesTcpClient(Setting.getIPAddress(this.getActivity()),Integer.parseInt(Setting.getVanPORT(this.getActivity())) ,
                Setting.getCatIPAddress(this.getActivity()),Integer.parseInt(Setting.getCatVanPORT(this.getActivity())) ,Integer.parseInt(Setting.getTMPVanPORT(this.getActivity())),
                mHandler, mnetworkConnectionListener);
        mSqlite = new sqliteDbSdk(m_Context);
        mSendStatus = 0;

        /** blesdk 설정 */
        mblsSdk = new bleSdk(m_Context, mbleConnectionListener, mbleHandler); //BLE 연결을 위한 클래스 생성
        mRcvBuffer = ByteBuffer.allocate(2048);
        mbleWoosimSdk = new bleWoosimSdk(m_Context, mbleWoosimConnectionListener, mbleHandler); //BLE 연결을 위한 클래스 생성

        /**세금 설정 */
        mTaxSdk = TaxSdk.getInstance();

        /** 최초 BLE,CAT,USB 타임아웃설정 */
        if (Setting.getPreference(m_Context,Constants.USB_TIME_OUT).equals(""))
        {
            Setting.setPreference(m_Context,Constants.USB_TIME_OUT,"30");
        }
        if (Setting.getPreference(m_Context,Constants.CAT_TIME_OUT).equals(""))
        {
            Setting.setPreference(m_Context,Constants.CAT_TIME_OUT,"30");
        }
        if (Setting.getPreference(m_Context,Constants.BLE_TIME_OUT).equals(""))
        {
            Setting.setPreference(m_Context,Constants.BLE_TIME_OUT,"20");
        }

        /** Cat Qr리더기 설정과 USB Qr리더기 설정 */
        if (Setting.getPreference(m_Context,Constants.LINE_QR_READER).equals(""))
        {
            Setting.setPreference(m_Context,Constants.LINE_QR_READER,Constants.LineQrReader.Camera.toString());
        }
        if (Setting.getPreference(m_Context,Constants.CAT_QR_READER).equals(""))
        {
            Setting.setPreference(m_Context,Constants.CAT_QR_READER,Constants.CatQrReader.Camera.toString());
        }

        /** 터치서명 크기를 어떻게 설정할지 정한다 */
        if (Setting.getPreference(m_Context,Constants.SIGNPAD_SIZE).equals(""))
        {
            Setting.setPreference(m_Context, Constants.SIGNPAD_SIZE, Constants.TouchSignPadSize.FullScreen.toString());
        }

        /** 결제서명 값 저장 */
        if (Setting.getPreference(m_Context,Constants.UNSIGNED_SETMONEY).equals(""))
        {
            Setting.setPreference(m_Context,Constants.UNSIGNED_SETMONEY,"50000");
        }

        /** 폴백사용 유무 값 저장 */
        if (Setting.getPreference(m_Context,Constants.FALLBACK_USE).equals(""))
        {
            Setting.setPreference(m_Context,Constants.FALLBACK_USE,"0");    //0=사용
        }
    }

    /** 최초실행 시 시리얼 셋팅이 한번에 완료되지 않아서 문제가 생길 수 있다(퍼미션문제) */
    public void ResetSerial()
    {
        mSerial.ReInit();
    }

    /**
     * 현재 설정된 Activity를 리턴한다.
     * @return
     */
    public Activity getActivity() {
        return (Activity) m_Context;
    }

    /**
     * 현재 설정된 Context를 리턴한다.
     * @return
     */
    public Context getContext() {
        return m_Context;
    }

    /**
     * 현재 포커스를 가져야 하는 Context를 설정한다.
     * @param _Activity 현재 포커스 가지고 있는 Activity
     * @param _mAToAUsbStatelistner 포커스에 관련 이벤트를 발생하기 위한 callback 함수
     */
    public void setFocusActivity(BaseActivity _Activity,BaseActivity.AppToAppUsbSerialStateListener _mAToAUsbStatelistner)
    {
        mFocusActivity = _Activity;
        mAToAUsbStatelistner = _mAToAUsbStatelistner;
    }

    /**
     * TCP의 ip, port를 다시 설정하는 함수
     * @param _ip
     * @param _port
     */
    public void resetTcpServerIPPort(String _ip,int _port)
    {
        mTcpClient.reSetIP(_ip,_port);
    }

    public void resetCatServerIPPort(String _ip,int _port)
    {
        mTcpClient.reSetCatIP(_ip,_port);
    }

    public static KocesPosSdk getInstance() {
        if (Instance != null) {
//            Instance.mSerial = Setting.mSerial;
//            Setting.setSerialDataListener(Instance.mHandler,Instance.connectListener);
            return Instance;
        }
        return null;
    }

    /**
     * Sqlite 에 저장된 디바이스 무결성 검사 결과를 얻어 오는 함수
     * @return String Array
     */
    public String[] getSqliteDB_IntegrityTableInfo() {
        return mSqlite.SelectData(Command.DB_IntegrityTableName);
    }

    /**
     * sqlite 데이터 기록
     * @param date
     * @param result Unicode 문제로 결과는 파라미터를 숫자로 받는다. 1인 경우는 정상 0인 경우는 실패 처리 한다
     * @param etc Unicode 문제로 결과는 파라미터를 숫자로 받는다. 1인 경우는 구동시 0인 경우는 수동으로 처리 한다
     */
    public void setSqliteDB_IntegrityTable(String date,int result,int etc)
    {
        mSqlite.InserIntegrityData(date,result==1?"1":"0",etc==1?"1":"0");
    }
    public void setSqliteDB_StoreTable(String CreditConnA1200,String CreditConnB1200,String EtcConnA1200,String EtcConnB1200,String CreditConnA2400,
                                       String CreditConnB2400,String EtcConnA2400,String EtcConnB2400,String AsNum,String ShpNm,String Tid,String BsnNo,
                                       String PreNm,String ShpAdr,String ShpTel,String WorkingKeyIndex,String WorkingKey,String TMK,String PointCount,String PointInfo,String MchData)
    {
        mSqlite.InsertStoreData(CreditConnA1200,CreditConnB1200,EtcConnA1200,EtcConnB1200,CreditConnA2400,CreditConnB2400,EtcConnA2400,EtcConnB2400,AsNum,ShpNm,Tid,BsnNo,
                PreNm,ShpAdr,ShpTel,WorkingKeyIndex,WorkingKey,TMK,PointCount,PointInfo,MchData);
    }
    /** 가맹정 정보를 가져 온다. */
    public HashMap<String,String> getSqliteDB_StoreTable(String Tid, String Bsn){
        return mSqlite.SelectStoreData(Tid,Bsn);
    }

    /**가맹점 세금 설정을 기록한다. */
     public void setSqliteDB_SettingTax(String Tid,boolean UseVAT,int AutoVAT,int IncludeVAT,int VATRate,boolean UseSVC,
                                        int AutoSVC,int IncludeSVC,int SVCRate,int minInstallMentAmount,int NoSignAmount){
         mSqlite.UpdateTaxSettingData(Tid,UseVAT,AutoVAT,IncludeVAT,VATRate,UseSVC,AutoSVC,IncludeSVC,SVCRate,minInstallMentAmount,NoSignAmount);
     }

    /**
     *
     * @param _Tid
     * @param _storeName
     * @param _storeAddr
     * @param _storeNumber
     * @param _storePhone
     * @param _storeOwner
     * @param _Trade
     * @param _cancel
     * @param _money
     * @param _giftamt
     * @param _tax
     * @param _Scv
     * @param _Txf
     * @param _InstallMent
     * @param _CashTarget
     * @param _CashInputType
     * @param _CashNum
     * @param _CardNum
     * @param _CardType
     * @param _CardInpNm
     * @param _CardIssuer
     * @param _MchNo
     * @param _AuDate
     * @param _OriAuDate
     * @param _AuNum
     * @param _OriAuNum
     * @param _TradeNo
     * @param _Message
     * @param _KakaoMessage
     * @param _PayType
     * @param _KakaoAuMoney
     * @param _KakaoSaleMoney
     * @param _KakaoMemberCd
     * @param _KakaoMemberNo
     * @param _Otc
     * @param _Pem
     * @param _Trid
     * @param _CardBin
     * @param _SearchNo
     * @param _PrintBarcd
     * @param _PrintUse
     * @param _PrintNm
     * @param _MchFee
     * @param _MchRefund
     */
     public void setSqliteDB_InsertTradeData(String _Tid,String _storeName, String _storeAddr, String _storeNumber, String _storePhone, String _storeOwner,
                                             String _Trade,String _cancel,int _money,String _giftamt,int _tax,int _Scv,int _Txf, //비과세
                                             int _InstallMent,String _CashTarget,String _CashInputType,String _CashNum,String _CardNum, //  카드번호
                                             String _CardType,String _CardInpNm,String _CardIssuer,String _MchNo,String _AuDate,String _OriAuDate, // 원거래일자
                                             String _AuNum,String _OriAuNum,String _TradeNo,String _Message,String _KakaoMessage,String _PayType, // PayType
                                             String _KakaoAuMoney,String _KakaoSaleMoney,String _KakaoMemberCd,String _KakaoMemberNo,String _Otc,// Otc
                                             String _Pem,String _Trid,String _CardBin,String _SearchNo,String _PrintBarcd,String _PrintUse, // PrintUse
                                             String _PrintNm,String _MchFee,String _MchRefund){
         mSqlite.InsertTrade( _Tid, _storeName,_storeAddr,_storeNumber,_storePhone,_storeOwner, _Trade, _cancel,_money, _giftamt, _tax, _Scv, _Txf,_InstallMent, _CashTarget, _CashInputType, _CashNum, _CardNum,
                 _CardType, _CardInpNm, _CardIssuer, _MchNo, _AuDate, _OriAuDate,_AuNum, _OriAuNum, _TradeNo, _Message, _KakaoMessage, _PayType,
                 _KakaoAuMoney, _KakaoSaleMoney, _KakaoMemberCd, _KakaoMemberNo, _Otc,_Pem, _Trid, _CardBin, _SearchNo, _PrintBarcd, _PrintUse,
                 _PrintNm, _MchFee, _MchRefund);
     }

     /** 거래내역 전체를 가져 온다. */
     public ArrayList<DBTradeResult> getSqliteDB_SelectTradeData(){
         return mSqlite.getTradeAllList();
     }

    /** 특정TID 거래내역 전체를 가져 온다. */
     public ArrayList<DBTradeResult> getSqliteDB_SelectTradeData(String _tid){
         if (_tid.equals(""))
         {
             return mSqlite.getTradeAllList();
         }
         return mSqlite.getTradeTIDAllList(_tid);
     }

    /** 특정TID 거래내역 중 신용,현금,간편,현금IC 로 분류별로 가져 온다. */
    public ArrayList<DBTradeResult> getSqliteDB_SelectTradeData(String _tid, String _tradeType){
        if (_tid.equals(""))
        {
            return mSqlite.getTradeTIDTradeTypeList(_tid, _tradeType);
        }
        return mSqlite.getTradeTIDTradeTypeList(_tid, _tradeType);
    }

    /** 특정TID 거래내역 중 신용,현금,간편,현금IC 로 분류별로 날짜 지정해서 가져 온다. */
    public ArrayList<DBTradeResult> getSqliteDB_SelectTradeData
    (String _tid, String _tradeType, String _fromDate, String _toDate){
        if (_tid.equals(""))
        {
            return mSqlite.getTradeTIDTradeTypeDateList(_tid, _tradeType, _fromDate, _toDate);
        }
        return mSqlite.getTradeTIDTradeTypeDateList(_tid, _tradeType, _fromDate, _toDate);
    }

     public DBTradeResult getSqliteDB_SelectTradeData(int index){
         return mSqlite.getTradeList(index);
     }
     public DBTradeResult getSqliteDB_SelectTradeLastData(String _Tid){
         return mSqlite.getTradeLastData(_Tid);
     }
    public ArrayList<DBTradeResult> getSqliteDB_SelectTradeListParsingData(String _tid, String _trade, String _fromdate, String _todate){
        return mSqlite.getTradeListParsingData(_tid,_trade,_fromdate,_todate);
    }
    public ArrayList<DBTradeResult> getSqliteDB_SelectTradeListPeriod(String _tid,String _fdate,String _tdate){
        return mSqlite.getTradeListPeriod(_tid,_fdate,_tdate);
    }
    public int getSqliteDB_SelectCountTradeTable(){
         return mSqlite.getTradeListCount();
    }
    //
    /**가맹점 세금 설정을 가져 온다. */
    public HashMap<String,String> getSqliteDB_TaxSettingInfo(String Tid){
        return mSqlite.SelectSettingTaxData(Tid);
    }


    /**
     * 앱투앱으로 거래를 진행 시 결과값을 저장해둔다
     * @param resultData
     */
    public void setAppToAppTradeResult(HashMap<String,String> resultData)
    {
        mSqlite.InsertAppToAppData(resultData);
    }

    public HashMap<String,String> getAppToAppTradeResult(String _tid, String _audate, String _billno)
    {
        return mSqlite.getAppToAppTrade(_tid,_audate,_billno);
    }

    public boolean checkAppToAppTradeList(String _tid, String _audate, String _billno)
    {
        return mSqlite.CheckAppToAppList(_tid,_billno,_audate);
    }

    /**
     * 현재 연결 되어 있는 USB Serial정보를 가져 온다.
     * @return
     */
    public ArrayList<FTDISerial> getUsbDevice()
    {
        return mSerial.getSerials();
    }
    public boolean CheckConnectedUsbSerialState(String _busName)
    {
        return mSerial.reCheckConnectedUsbSerial(_busName);
    }


    /**
     * 현재 안드로이드에 연결 되어 있는 USB 장치 카운트를 가져온다.<br>
     * 앱에서 설정 하지 않은 USB 장치 카운트를 리턴한다.
     * @return
     */
    public int ConnectedUsbSerialCount()    //현재 안드로이드에 연결 되어 있는 USB 장치 카운트를 가져온다. 앱에서 설정 하지 않은 USB 장치 카운트
    {
        return mSerial.CheckConnectedUsbSerialCount();
    }
    /**
     * 현재 안드로이드에 연결 되어 있는 USB 장치 주소를 가져온다.<br>
     * 앱에서 설정 하지 않은 USB 장치 주소를 리턴한다.
     * @return String Array
     */
    public String[] ConnectedUsbSerialDevicesAddr()
    {
        return mSerial.CheckConnectedUsbSerialDeviceName();
    }

    /**
     * 시리얼 데이터를 리턴 interface callback 변수를 설정한다.
     */
    public void setSerialDataLinstener(SerialInterface.DataListener _datalistener)
    {
        mSerialDatalistener = _datalistener;
    }

    /**
     * 주소를 이용하여 현재 연결 상태를 체크 한다.<br>
     * 만약 연결 되어 있지 않다면 연결을 시도 한다.
     */
    public void CheckConnectState(String _busName)
    {
        mSerial.CheckConnectState(_busName);
    }

    /**
     * 서명패드 주소를 설정 한다.
     * @param _busName
     */
    public void setSignPadAddr(String _busName)
    {
        mSignPad_Addr = _busName;
    }

    /**
     * IC Reader 주소를 설정한다.
     * @param _busName
     */
    public void setICReaderAddr(String _busName)
    {
        mICReader_Addr = _busName;
    }

    /**
     * 멀티 패드 주소를 설정한다.
     * @param _busName
     */
    public void setMultiReaderAddr(String _busName)
    {
        mMultiReader_Addr = _busName;
    }

    /**
     * 멀티 서명 패드 주소를 설정 한다.
     * @param _busName
     */
    public void setMultiPadAddr(String _busName)
    {
        mMultiPad_Addr = _busName;
    }

    /**
     * IC Reader 장치 주소를 문자열 배열 형태로 리턴한다.
     * @return
     */
    public String[] getICReaderAddr2()
    {
        String[] tmp = new String[1];
        tmp[0] = getICReaderAddr();
        return tmp;
    }
    /**
     * multi Reader 장치 주소를 문자열 배열 형태로 리턴한다.
     * @return
     */
    public String[] getMultiReaderAddr2()
    {
        String[] tmp = new String[1];
        tmp[0] = getMultiReaderAddr();
        return tmp;
    }
    /**
     * multi Reader 장치 주소를 문자열 형태로 리턴한다.
     * @return
     */
    public String getMultiReaderAddr()
    {
        String SelectedMultiReader = "";
        if(mMultiReader_Addr.equals(""))
        {
            SelectedMultiReader = Setting.getPreference(this.getActivity(),Constants.SELECTED_DEVICE_MULTI_READER);
            if(SelectedMultiReader.equals("") || SelectedMultiReader.equals("사용안함") )
            {
//                if(Setting.ICResponseDeviceType ==3)
//                {
//                    SelectedMultiReader = Setting.getPreference(this.getActivity(),Constants.SELECTED_DEVICE_MULTI_SIGN_PAD);
//                    if(SelectedMultiReader.equals("") || SelectedMultiReader.equals("사용안함"))
//                    {
                        return "";
//                    }
//                    else
//                    {
//                        setMultiPadAddr(SelectedMultiReader);
//                    }
//                }
            }
            else
            {
                setMultiReaderAddr(SelectedMultiReader);
            }
        }
        if(mDevicesList != null)
        {
            for (Devices n:mDevicesList) {
                if(n.getmType()==4)
                {
                    SelectedMultiReader =  n.getmAddr();
                }
            }
//            if(SelectedMultiReader.equals("") && Setting.ICResponseDeviceType ==3)
//            {
//                for (Devices n:mDevicesList) {
//                    if(n.getmType()==3)
//                    {
//                        SelectedMultiReader =  n.getmAddr();
//                    }
//                }
//            }
        }
        return SelectedMultiReader;
    }
    /**
     * IC Reader 장치 주소를 문자열 형태로 리턴한다.
     * @return
     */
    public String getICReaderAddr()
    {
        String SelectedICReader = "";
        if(mICReader_Addr.equals(""))
        {
            SelectedICReader = Setting.getPreference(this.getActivity(),Constants.SELECTED_DEVICE_CARD_READER);
            if(SelectedICReader.equals("") || SelectedICReader.equals("사용안함") )
            {
                return "";
            }
            else
            {
                setICReaderAddr(SelectedICReader);
            }
        }
        if(mDevicesList != null)
        {
            for (Devices n:mDevicesList) {
                if(n.getmType()==1)
                {
                    SelectedICReader =  n.getmAddr();
                }
            }
        }
        return SelectedICReader;
    }
    /**
     * 서명 장치 주소를 문자열 형태로 리턴한다.
     * @return
     */
    public String getSignPadAddr()
    {
        String SelectedSignPad ="";
        if(mSignPad_Addr.equals(""))
        {
            SelectedSignPad = Setting.getPreference(this.getActivity(),Constants.SELECTED_DEVICE_SIGN_PAD);
            if(SelectedSignPad.equals("") || SelectedSignPad.equals("사용안함"))
            {
                return "";
            }
            else
            {
                setSignPadAddr(SelectedSignPad);
            }
        }
        if(mDevicesList != null)
        {
            for (Devices n:mDevicesList) {
                if(n.getmType()==2)
                {
                    SelectedSignPad = n.getmAddr();
                }
            }
        }
        return SelectedSignPad;
    }
    /**
     * 멀티 서명 장치 주소를 문자열 형태로 리턴한다.
     * @return
     */
    public String getMultiAddr()
    {
        String SelectedMulti = "";
//        if(Setting.getSignPadType(this.getActivity()) == 0 || Setting.getSignPadType(this.getActivity()) == 3)
//        {
//            return "";
//        }
        if(mMultiPad_Addr.equals(""))
        {
            SelectedMulti = Setting.getPreference(this.getActivity(),Constants.SELECTED_DEVICE_MULTI_SIGN_PAD);
            if(SelectedMulti.equals("") || SelectedMulti.equals("사용안함"))
            {
//                if(Setting.ICResponseDeviceType ==4)
//                {
//                    SelectedMulti = Setting.getPreference(this.getActivity(),Constants.SELECTED_DEVICE_MULTI_READER);
//                    if(SelectedMulti.equals("") || SelectedMulti.equals("사용안함") )
//                    {
                        return "";
//                    }
//                    else
//                    {
//                        setMultiReaderAddr(SelectedMulti);
//                    }
//                }
            }
            else
            {
                setMultiPadAddr(SelectedMulti);
            }
        }
        if(mDevicesList != null)
        {
            for (Devices n:mDevicesList) {
                if(n.getmType()==3)
                {
                    SelectedMulti = n.getmAddr();
                }
            }
//            if(SelectedMulti.equals("") && Setting.ICResponseDeviceType ==4)
//            {
//                for (Devices n:mDevicesList) {
//                    if(n.getmType()==4)
//                    {
//                        SelectedMulti =  n.getmAddr();
//                    }
//                }
//            }
        }
        return SelectedMulti;
    }

    /**
     * 현재 유효한 장치의 모든 주소를 가져온다.
     * @return
     */
    public String[] AllDeviceAddr()
    {
        ArrayList<String> Addr = new ArrayList<>();
        String ic = getICReaderAddr();
        String ic_multi = getMultiReaderAddr();
        String pad = getSignPadAddr();
        String Multi = getMultiAddr();
        if(!ic.equals(""))
            Addr.add(ic);
        if(!pad.equals(""))
            Addr.add(pad);
        if(!Multi.equals(""))
            Addr.add(Multi);
        if(!ic_multi.equals(""))
            Addr.add(ic_multi);

        String[] tmp = new String[Addr.size()];
        for(int i=0;i<Addr.size();i++)
        {
            tmp[i]=Addr.get(i);
        }
        return tmp;
    }

    // 여기서 부터는 전문 발송 관련 함수 정리

    /**
     * 시리얼 장치에 NAK를 보낸다
     * @param _target
     */
    public void __SendNak(String[] _target)
    {
        SendData(Command.getSCommand(Command.NAK),_target);
    }

    /**
     * 시리얼 장치에 EOT를 보낸다
     * @param _target
     */
    public void __SendEOT(String[] _target)
    {
        SendData(Command.getSCommand(Command.EOT),_target);
    }


    /**
     * 포스 초기화 밴코드
     * @param _vancode
     */
    public void __PosInit(String _vancode,SerialInterface.DataListener _datalistener,String[] _target)
    {
        if(getUsbDevice().size() > 0)
        {
            setSerialDataLinstener(_datalistener);
            SendData(Command.pad_init(_vancode),_target);
        }
    }

    /**
     * 장비 자동 인식 코드에서 장비 초기화
     * @param _vancode
     * @param _datalistener
     * @param _target
     */
    public void __PosInitAutoDetect(String _vancode,SerialInterface.DataListener _datalistener,String[] _target)
    {

        setSerialDataLinstener(_datalistener);
        SendData(Command.pad_init(_vancode),_target);
    }

    /**
     *  보안키 갱신 생성 요청 (PC -> 멀티패드)
     * @param _datalistener
     * @param _target 시리얼 장치 포트 번호 배열값
     */
    public void __SecurityKeyUpdateReady(SerialInterface.DataListener _datalistener,String[] _target)
    {
        setSerialDataLinstener(_datalistener);
        SendData(Command.securityKey_update_ready_req(),_target);
    }

    /**
     * 보안키 업데이트
     * @param _date
     * @param _data
     * @param _datalistener
     * @param _target
     */
    public void __SecurityKeyUpdate(String _date,byte[] _data,SerialInterface.DataListener _datalistener,String[] _target)
    {
        setSerialDataLinstener(_datalistener);
        SendData(Command.securityKey_update_req(_date,_data),_target);
    }

    /**
     * 무결성 검사 함수
     * @param _datalistener
     * @param _target 무결성 장치 시리얼 주소
     */
    public void __Integrity(SerialInterface.DataListener _datalistener,String[] _target)
    {
        setSerialDataLinstener(_datalistener);
        SendData(Command.selfChecksecurity_req(),_target);
    }
    /**
     * 포스 정보 요청 발송
     * @param _date YYYYMMDDhhmmss
     */
    public boolean __PosInfo(String _date,SerialInterface.DataListener _datalistener,String[] _target)
    {
        setSerialDataLinstener(_datalistener);
        if(_date.length()!=14){return false;}
        SendData(Command.pos_info(_date),_target);
        return true;
    }

    /**
     * 포스 정보 요청 발송
     * @param _date YYYYMMDDhhmmss
     */
    public boolean __PosInfoAutoDetect(String _date,SerialInterface.DataListener _datalistener,String _target)
    {
        setSerialDataLinstener(_datalistener);
        if(_date.length()!=14){return false;}
        String[] tmp = new String[1];
        tmp[0] = _target;
        SendData(Command.pos_info(_date),tmp);
        return true;
    }


    /**
     * 사인 데이터 요청
     * @param _datalistener
     * @param _target 시리얼 장치 포트 번호 배열값
     * @return
     */
    public boolean __SignDataRequest(SerialInterface.DataListener _datalistener,String[] _target)
    {
        setSerialDataLinstener(_datalistener);
        SendData(Command.sendsginreq(),_target);
        return true;
    }
    public boolean __SignDataFinish(SerialInterface.DataListener _datalistener,String[] _target)
    {
        setSerialDataLinstener(_datalistener);
        SendData(Command.finishsignreq(),_target);
        return true;

    }

    /**
     * 서명패드 화면이 글씨 표시 하는 함수
     * @param _msg1
     * @param _msg2
     * @param _msg3
     * @param _msg4
     * @param _time
     * @param _datalistener
     * @param _target 시리얼 장치 포트 번호 배열값
     * @return
     */
    public boolean __PadDisplayMessage(String _msg1,String _msg2,String _msg3,String _msg4,String _time,SerialInterface.DataListener _datalistener,String[] _target)
    {
        setSerialDataLinstener(_datalistener);
        try {
            if(_msg1.getBytes("euc-kr").length>16 || _msg2.getBytes("euc-kr").length>16 ||
                    _msg3.getBytes("euc-kr").length>16 || _msg4.getBytes("euc-kr").length> 16 || _time.length()!=2 )
            {
                return false;
            }
        }
        catch (UnsupportedEncodingException ex)
        {

        }

        SendData(Command.Message_Display_Req(_msg1,_msg2,_msg3,_msg4,_time),_target);

        return true;
    }

    /**
     * 사인패드 서명 입력 요청 전문
     * @param _hashcode
     * @param _signData
     * @param _workingKey
     * @param _msg1
     * @param _msg2
     * @param _msg3
     * @param _msg4
     * @return
     */
    public boolean __PadRequestSign(String _hashcode,String _signData,String _workingKey,String _msg1,String _msg2,String _msg3,String _msg4,SerialInterface.DataListener _datalistener,String[] _target)
    {
        mSerialDatalistener = _datalistener;
        try {
            if(_msg1.getBytes("euc-kr").length>16 || _msg2.getBytes("euc-kr").length>16 )
            {
                return false;
            }
        }
        catch (UnsupportedEncodingException ex)
        {

        }
        SendData(Command.inputsignreq(_hashcode,_signData,_workingKey,_msg1,_msg2,_msg3,_msg4),_target);
        return true;
    }

    /**
     * 서명 전송 요청 및 응답
     * @return
     */
    public boolean __PadRequestSignData(String[] _target)
    {
        SendData(Command.sendsginreq(),_target);
        return true;
    }

    /**
     * 암호화하지 않은 번호 요청 의 전문 포맷
     * @param _flag
     * @param _maxlength
     * @param _msg1
     * @param _msg2
     * @param _msg3
     * @param _msg4
     * @param _displaytime
     * @param _datalistener
     * @param _target 시리얼 장치 포트 번호 배열값
     * @return
     */
    public boolean __PadRequestInputPasswd_no_encypt(String _flag,String _maxlength,String _msg1,String _msg2,String _msg3,String _msg4,String _displaytime ,SerialInterface.DataListener _datalistener,String[] _target)
    {
        if(_maxlength.length()>2){return false;}
        mSerialDatalistener = _datalistener;
        SendData(Command.pinpad_no_encypt_req(_flag,_maxlength,_msg1,_msg2,_msg3,_msg4,_displaytime),_target);
        return true;
    }


    public boolean __PadRequestInputPasswd_no_encypt_Cash2(String _flag,String _maxlength,String _msg1,String _msg2,String _msg3,String _msg4,String _displaytime ,SerialInterface.DataListenerCash _datalistenerCash,String[] _target)
    {
        if(_maxlength.length()>2){return false;}
        mSerialDatalistenerCash = _datalistenerCash;
        mSendUSBCashType = true;
        SendData(Command.pinpad_no_encypt_req(_flag,_maxlength,_msg1,_msg2,_msg3,_msg4,_displaytime),_target);
        return true;
    }

    /**
     * QR 리딩
     */
    public boolean __BarcodeReader(String _msg1title, String _msg2moneymsg,  String _msg3qrmsg, SerialInterface.DataListener _datalistener,String[] _target)
    {
        mSerialDatalistener = _datalistener;
        try {
            if(_msg1title.getBytes("euc-kr").length>32 || _msg2moneymsg.getBytes("euc-kr").length>32 || _msg3qrmsg.getBytes("euc-kr").length>32 )
            {
                return false;
            }
        }
        catch (UnsupportedEncodingException ex)
        {

        }
        SendData(Command.barcode_reading_req(_msg1title,_msg2moneymsg,_msg3qrmsg),_target);
        return true;
    }

    /**
     * 상호인증_펌웨어업데이트관련_0x52
     * 상호인증 요청 (PC -> 멀티패드)
     * @param _type : 0001:최신펌웨어, 0003:EMV Key
     * @return
     */
    public boolean __authenticatoin_req(String _type,SerialInterface.DataListener _datalistener,String[] _target)
    {
        mSerialDatalistener = _datalistener;
        String mLog = "펌웨어구분 : " + _type;
        cout("CARD_READER_SEND : 상호인증요청 0x52",Utils.getDate("yyyyMMddHHmmss"),mLog);
        SendData(Command.mutual_authenticatoin_req(_type),_target);
        return true;
    }

    /**
     * 상호인증_결과요청 펌웨어업데이트관련_0x54
     *  상호 인증정보 결과 요청 (PC  -> 멀티패드)
     * @param _date 시간 14 Char YYYYMMDDHHMMSS
     * @param _multipadAuth 멀티패드인증번호 32 Char 인증업체로부터 발급받은 단말 인증 번호
     * @param _multipadSerial 멀티패드시리얼번호 10 Char 멀티패드 시리얼 번호
     * @param _code 응답코드 4 * Char 정상응답 : ‚0000‛(그외 실패) 실패 수신 시 ‚0x55‛응답도 실패(01)처리
     * @param _resMsg 응답메세지
     * @param _key 접속보안키 48 Bin Srandom(16)+KEY2_ENC(IPEK+Crandom, 32)
     * @param _dataCount 데이터 개수 4 Char 주4) 데이터의 개수
     * @param _protocol 전송 프로토콜 5 Char "SFTP"
     * @param _Addr 다운로드서버주소
     * @param _port 포트
     * @param _id 계정
     * @param _passwd 비밀번호
     * @param _ver 버전 및 데이터 구분
     * @param _verDesc 버전 설명
     * @param _fn 파일명
     * @param _fnSize 파일크기
     * @param _fnCheckType 파일체크방식
     * @param _fnChecksum 파일체크섬
     * @param _dscrKey 파일복호화키
     * @return
     */
    public boolean __authenticatoin_result_req(byte[] _date,byte[] _multipadAuth,byte[] _multipadSerial,byte[] _code,byte[] _resMsg,byte[] _key,byte[] _dataCount,byte[] _protocol,
                                               byte[] _Addr,byte[] _port,byte[] _id,byte[] _passwd,byte[] _ver,byte[] _verDesc,byte[] _fn,byte[] _fnSize,byte[] _fnCheckType,
                                               byte[] _fnChecksum,byte[] _dscrKey,SerialInterface.DataListener _datalistener,String[] _target)
    {
        mSerialDatalistener = _datalistener;
        String mLog = "시간 : " + Utils.bytesToHex_0xType(_date) + "," +"멀티패드인증번호 : " + Utils.bytesToHex_0xType(_multipadAuth) + "," + "멀티패드시리얼번호 : " + Utils.bytesToHex_0xType(_multipadSerial) + "," +
                "응답코드 : " + Utils.bytesToHex_0xType(_code) + "," + "응답메세지 : " + Utils.bytesToHex_0xType(_resMsg) + "," + "접속보안키 : " +  Utils.bytesToHex_0xType(_key) + "," +
                "데이터개수 : " + Utils.bytesToHex_0xType(_dataCount) + "," + "프로토콜 : " + Utils.bytesToHex_0xType(_protocol) + "," +
                "다운로드서버주소 : " + Utils.bytesToHex_0xType(_Addr) + "," + "포트 : " + Utils.bytesToHex_0xType(_port) + "," + "계정 : " + Utils.bytesToHex_0xType(_id) + "," +
                "비밀번호 : " + Utils.bytesToHex_0xType(_passwd) + "," + "버전및데이터구분 : " + Utils.bytesToHex_0xType(_ver) + "," + "버전설명 : " + Utils.bytesToHex_0xType(_verDesc) + "," +
                "파일명 : " + Utils.bytesToHex_0xType(_fn) + "," + "파일크기 : " + Utils.bytesToHex_0xType(_fnSize) + "," + "파일체크방식 : " + Utils.bytesToHex_0xType(_fnCheckType) + "," +
                "파일체크섬 : " + Utils.bytesToHex_0xType(_fnChecksum) + "," + "파일복호화키 : " + Utils.bytesToHex_0xType(_dscrKey);

        cout("CARD_READER_SEND : 상호인증정보결과요청 0x54",Utils.getDate("yyyyMMddHHmmss"),mLog);
        SendData(Command.authenticatoin_result_req(_date,_multipadAuth,_multipadSerial,_code,_resMsg,_key,_dataCount,_protocol,
                _Addr,_port,_id,_passwd,_ver,_verDesc,_fn,_fnSize,_fnCheckType,_fnChecksum,_dscrKey),_target);
        return true;
    }

    /**
     *  업데이트 파일전송 (PC -> 멀티패드)
     * @param _type 요청데이타구분 4 Char 0001:최신펌웨어, 0003:EMV Key
     * @param _dataLength 데이터 총 크기
     * @param _sendDataSize 전송중인 데이터 크기
     * @param _data 데이터
     * @return
     */
    public boolean __updatefile_transfer_req(String _type,String _dataLength,String _sendDataSize,int _defaultSize,byte[] _data,SerialInterface.DataListener _datalistener,String[] _target)
    {
        mSerialDatalistener = _datalistener;
        String mLog = "요청데이타구분 : " + _type+ "," +"데이터 총크기 : " + _dataLength + "," + "전송중인 데이터크기 : " + _sendDataSize + "," + "데이터 : " + Utils.bytesToHex_0xType(_data);
        cout("CARD_READER_SEND : 업데이트파일전송 0x56",Utils.getDate("yyyyMMddHHmmss"),mLog);
        SendData(Command.updatefile_transfer_req(_type,_dataLength,_sendDataSize,_defaultSize,_data),_target);
        return true;
    }

    /**
     * 신용 카드 결제 관련 함수
     * @param _type
     * @param _Tid
     * @param _money
     * @param date
     * @param _usePad
     * @param _cashIC
     * @param _printCount
     * @param _signType
     * @param _minPasswd
     * @param _MaxPasswd
     * @param _workingKeyIndex
     * @param _workingkey
     * @param _cashICRnd
     * @param _datalistener
     * @param _target
     */
    public void __icreq(String _type,String _Tid,String _money,String date,String _usePad,
                        String _cashIC,String _printCount,String _signType,String _minPasswd,
                        String _MaxPasswd,String _workingKeyIndex,String _workingkey,
                        String _cashICRnd,SerialInterface.DataListener _datalistener,String[] _target)
    {
        mSerialDatalistener = _datalistener;
        if(_type.length()!=2)
        {
            return;
        }

        String mLog = "거래구분 : " + _type + "," +"금액 : " + _money + "," + "날짜 : " + date + "," + "서명패드입력여부 : " + _usePad + "," +
                "현금IC간소화 : " + _cashIC + "," + "전표출력카운터 : " + _printCount + "," + "서명데이터치환방식 : " + _signType + "," + "입력최소길이 : " + _minPasswd + "," +
                "입력최대길이 : " + _MaxPasswd + "," + "WorkingKeyIndex : " + _workingKeyIndex + "," + "Workingkey : " + _workingkey + "," + "현금IC난수 : " + _cashICRnd;
        cout("CARD_READER_SEND : CARD_READER",Utils.getDate("yyyyMMddHHmmss"),mLog);

        SendData(Command.IC_trade_req(_type,_money, date,_usePad,_cashIC,_printCount,_signType,_minPasswd,_MaxPasswd,_workingKeyIndex,_workingkey,_cashICRnd),_target);
    }

    /**
     * ble 신용 카드 결제 관련 함수
     * @param _type
     * @param _Tid
     * @param _money
     * @param date
     * @param _usePad
     * @param _cashIC
     * @param _printCount
     * @param _signType
     * @param _minPasswd
     * @param _MaxPasswd
     * @param _workingKeyIndex
     * @param _workingkey
     * @param _cashICRnd
     * @param resDataListener
    */
    public void __Bleicreq(String _type,
                           String _Tid,
                           String _money,
                           String date,
                           String _usePad,
                           String _cashIC,
                           String _printCount,
                           String _signType,
                           String _minPasswd,
                           String _MaxPasswd,
                           String _workingKeyIndex,
                           String _workingkey,
                           String _cashICRnd,
                           bleSdkInterface.ResDataListener resDataListener){

        setResDataLinstener(resDataListener);
        if(_type.length()!=2)
        {
            return;
        }

        String mLog = "거래구분 : " + _type + "," +"금액 : " + _money + "," + "날짜 : " + date + "," + "서명패드입력여부 : " + _usePad + "," +
                "현금IC간소화 : " + _cashIC + "," + "전표출력카운터 : " + _printCount + "," + "서명데이터치환방식 : " + _signType + "," + "입력최소길이 : " + _minPasswd + "," +
                "입력최대길이 : " + _MaxPasswd + "," + "WorkingKeyIndex : " + _workingKeyIndex + "," + "Workingkey : " + _workingkey + "," + "현금IC난수 : " + _cashICRnd;
        cout("CARD_READER_SEND : CARD_READER",Utils.getDate("yyyyMMddHHmmss"),mLog);
        byte[] sendData = Command.IC_trade_req(_type,_money, date,_usePad,_cashIC,_printCount,_signType,_minPasswd,_MaxPasswd,_workingKeyIndex,_workingkey,_cashICRnd);
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) ) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }
    /**
     * 다운로드(키업데이트)
     * @param _Command  "D10" : 가맹점다운로드  "D11" : 복수가맹점다운로드  "D20" : 키업데이트 6
     * @param _Tid 단말기 ID
     * @param _date 거래일시 12 N YYMMDDhhmmss
     * @param _posVer 전문일련번호
     * @param _etc 미정
     * @param _length 길이 4 N O O 128byte => 0128
     * @param _posCheckdata 단말 검증 요청 데이터
     *                      길이(3byte, ex.087)+TrsmID(7)+Crandom(16)+KEY1_ENC(25바이 트Timpstamp+가변SvrInfo+7바이트TID+16바이트CRandom) MDO : 가맹점 정보 다운로드 ONLY(Key 다운로드 안함) 15
     * @param _businessnum 가맹점 사업자번호
     * @param _posSerialnum 장비 제조일련번호(Serial)
     * @param _posData 가맹점데이터
     */
    public void ___registedShopInfo_KeyDownload(String _Command,String _Tid,String _date,String _posVer,String _etc,String _length,
                                                String _posCheckdata,String _businessnum,String _posSerialnum,String _posData,String _macAddress,
                                                TcpInterface.DataListener _tcpdatalistener)
    {
        mTcpDatalistener = _tcpdatalistener;
        TCPSendData(TCPCommand.TCP_StoreDownloadKeyReq(_Command,_Tid,_date,_posVer,_etc,_length,_posCheckdata.getBytes(),_businessnum,_posSerialnum,_posData,_macAddress));
    }
    public void ___registedShopInfo_KeyDownload(String _Command,String _Tid,String _date,String _posVer,String _etc,String _length,
                                                byte[] _posCheckdata,String _businessnum,String _posSerialnum,String _posData,String _macAddress,
                                                TcpInterface.DataListener _tcpdatalistener) {
        mTcpDatalistener = _tcpdatalistener;
        TCPSendData(TCPCommand.TCP_StoreDownloadKeyReq(_Command, _Tid, _date, _posVer, _etc, _length, _posCheckdata, _businessnum, _posSerialnum, _posData,_macAddress));
    }

    /**
     * 신용요청
     * @param _Command  "A10" : 신용 승인 요청     * "A20" : 신용 승인 취소 요청
     * @param _Tid  단말기 ID
     * @param _date YYMMDDhhmmss
     * @param _posVer   단말 : 단말 version( 5자리) / 현재 단말의 최신버전정보로 응답 POS : DLL(4byte)`
     * @param _etc  미정
     * @param _cancelInfo   취소구분(1) + 원거래일자(6) + 원승인번호(12)
     * - 취소구분
     *  '0' : 사용자 취소
     *  'I' : 단말 EOT 수신 오류 망취소(카드번호필수) '1' : 단말 EOT 수신
     * 오류 망취소
     *  'J' : IC chip 카드 거절 취소 (카드번호필수) '3' : IC chip 카드 거
     * 절 취소
     *  '5' : 직전 취소
     *  'a' : 거래고유키 직전취소
     * - 원거래일자 : YYMMDD
     * - 원승인번호
     *  일반적인 국내신용 승인번호는 8자리이며 서버 전송 시 8자리만 전
     * 송(스페이스 padding 필요 없음)
     *  해외신용 승인번호는 6자리가 존재하며 서버 전송 시 6자리만 전
     * 송(스페이스 padding 필요 없음)
     * @param _inputType    'K' : Keyin , 'S' : Swipe, 'I' : IC, 'R' : RF, 'M' : Mobile RF, 'E' : PayOn, 'F' : Mobile PayOn
     * @param _CardNumber   암호화 : '일반 가맹점의 경우 Track의 BIN 정보 6자리만 전송
     * 비암호화 : 유효 track2 데이터 전송 ex) "9400123412341234=19121234123451234567"(첫번째자리 공백아님)
     * @param _length   58byte => 0058
     * @param _encryptInfo  KSN(10byte) + 암호화정보(48byte)로 구성
     * - 망취소 시 : KSN만 전송 망취소 시 : 카드번호 필수
     * - 암호화정보
     *  Track정보는 40byte 기준으로 좌측정렬에 스페이스로 패딩해서 암
     * 호화 할 것
     *  ex 1) 유효 Track2 :
     * "9400123412341234=19121234123451234567"(37byte) , ( swipe, ic
     * 등 )
     *  암호화 대상 :
     * "9400123412341234=19121234123451234567 "(40byte)
     *  2) 유효 Track2 : "9400123412341234=1912"(21byte) , ( keyin )
     *  암호화 대상 : "94001234****1234=1912
     * "(40byte)
     * - 비암호화(평문) 전송의 경우 미설정
     * @param _money    승인 : 공급가액, 취소 : 원승인거래총액
     * @param _tax
     * @param _serviceCharge
     * @param _taxfree
     * @param _currency
     * @param _Installment
     * @param _PoscertificationNumber
     * @param _tradeType
     * @param _emvData
     * @param _fallbackreason
     * @param _ICreqData
     * @param _workingKeyIndex
     * @param _passwd
     * @param _oilsurpport
     * @param _texfreeoil
     * @param _Dccflag
     * @param _DccreqInfo
     * @param _pointCardCode
     * @param _ptCardNum
     * @param _ptCardEncprytInfo
     * @param _digSignInfo
     * @param _signPadSerial
     * @param _digSignData
     * @param _Certification
     * @param _posData
     * @param _kocesUid
     * @param _tcpdatalistener
     * @param _macAddress
     * @param _hardwareKey
     */
    public void ___ictrade(String _Command,String _Tid,String _date,String _posVer,String _etc,String _cancelInfo,String _inputType,String _CardNumber,String _length,
                           byte[] _encryptInfo,String _money, String _tax,String _serviceCharge,String _taxfree,String _currency,String _Installment,String _PoscertificationNumber,
                           String _tradeType,String _emvData,String _fallbackreason, String _length2,byte[] _ICreqData,String _workingKeyIndex,String _passwd,String _oilsurpport,
                           String _texfreeoil,String _Dccflag,String _DccreqInfo,String _pointCardCode, String _ptCardNum,String _length3,byte[] _ptCardEncprytInfo,String _digSignInfo,
                           String _signPadSerial,String _length4,byte[] _digSignData,String _Certification, String _posData,String _kocesUid, String _uniqueCode,
                           String _macAddress, String _hardwareKey, TcpInterface.DataListener _tcpdatalistener)
    {
        mTcpDatalistener = _tcpdatalistener;

        String mLog = "거래구분 : " + _Command + "," +"단말기ID : " + _Tid + "," + "거래일시 : " + _date + "," + "단말버전 : " + _posVer + "," +
                "추가정보 : " + _etc + "," + "취소정보 : " + _cancelInfo + "," + "입력방법 : " + _inputType + "," + "거래금액 : " + _money + "," +
                "세금 : " + _tax + "," + "봉사료 : " + _serviceCharge + "," + "비과세 : " + _taxfree + "," + "통화코드 : " + _currency + "," +
                "할부개월 : " + _Installment + "," + "단말인증번호 : " + _PoscertificationNumber + "," + "신용거래구분 : " + _tradeType + "," + "fallback사유 : " + _fallbackreason + "," +
                "유류지원정보 : " + _oilsurpport + "," + "면세유정보 : " + _texfreeoil + "," + "DCC_flag : " + _Dccflag + "," + "DCC_요청정보 : " + _DccreqInfo + "," +
                "포인트카드코드 : " + _pointCardCode + "," + "전자서명사용여부 : " + _digSignInfo + "," + "시리얼번호 : " + _signPadSerial + "," +
                "인증구분 : " + _Certification + "," + "가맹점데이터 : " + _posData + "," + "거래고유번호 : " + _kocesUid + "," + "접속업체코드 : " + _uniqueCode;
        cout("VAN_SEND : CREDIT",Utils.getDate("yyyyMMddHHmmss"),mLog);

        TCPSendData(TCPCommand.TCP_ICReq(_Command,_Tid,_date,_posVer,_etc,_cancelInfo,_inputType,_CardNumber,_encryptInfo,_money,_tax,_serviceCharge,_taxfree,_currency,
                _Installment, _PoscertificationNumber,_tradeType, _emvData,_fallbackreason,_ICreqData,_workingKeyIndex,_passwd,_oilsurpport,_texfreeoil,_Dccflag,_DccreqInfo,_pointCardCode,
                _ptCardNum,_ptCardEncprytInfo,_digSignInfo,_signPadSerial,_digSignData,_Certification,_posData,_kocesUid,_uniqueCode, _macAddress, _hardwareKey));
    }

    /**
     카카오페이
     */
    public void ___KakaoPay(String 전문번호,String _Tid,String 거래일시,String 단말버전,String 단말추가정보,String 취소정보,String 입력방법,
                            String 바코드번호,byte[] OTC카드번호, String 거래금액,String 세금,String 봉사료,String 비과세,String 통화코드,
                            String 할부개월, String 결제수단, String 취소종류, String 취소타입, String 점포코드, String _PEM, String _trid,
                            String 카드BIN, String 조회고유번호, String _WorkingKeyIndex, String 전자서명사용여부, String 사인패드시리얼번호,
                            byte[] 전자서명데이터, String 가맹점데이터, TcpInterface.DataListener _tcpdatalistener)
    {
        mTcpDatalistener = _tcpdatalistener;

        String mLog = "거래구분 : " + 전문번호 + "," +"단말기ID : " + _Tid + "," + "거래일시 : " + 거래일시 + "," + "단말버전 : " + 단말버전 + "," +
                "추가정보 : " + 단말추가정보 + "," + "취소정보 : " + 취소정보 + "," + "입력방법 : " + 입력방법 + "," + "거래금액 : " + 거래금액 + "," +
                "세금 : " + 세금 + "," + "봉사료 : " + 봉사료 + "," + "비과세 : " + 비과세 + "," + "통화코드 : " + 통화코드 + "," +
                "할부개월 : " + 할부개월 + "," + "결제수단 : " + 결제수단 + "," + "취소종류 : " + 취소종류 + "," + "취소타입 : " + 취소타입 + "," +
                "점포코드 : " + 점포코드 + "," + "_PEM : " + _PEM + "," + "_trid : " + _trid + "," + "카드BIN : " + 카드BIN + "," +
                "조회고유번호 : " + 조회고유번호 + "," + "_WorkingKeyIndex : " + _WorkingKeyIndex + "," + "전자서명사용여부 : " + 전자서명사용여부 + "," +
                "사인패드시리얼번호 : " + 사인패드시리얼번호 + "," + "가맹점데이터 : " + 가맹점데이터;
        cout("VAN_SEND : EASY_KAKAOPAY",Utils.getDate("yyyyMMddHHmmss"),mLog);

        TCPSendData(TCPCommand.TCP_KAKAOReq(전문번호,_Tid,거래일시,단말버전,단말추가정보,취소정보,입력방법,바코드번호,OTC카드번호,거래금액,세금,봉사료,비과세,통화코드,
                할부개월, 결제수단,취소종류, 취소타입,점포코드,_PEM,_trid,카드BIN,조회고유번호,_WorkingKeyIndex,전자서명사용여부,사인패드시리얼번호,전자서명데이터,
                가맹점데이터));
    }

    /**
     제로페이
     */
    public void ___ZeroPay(String 전문번호,String _Tid,String 거래일시,String 단말버전,String 단말추가정보,String 취소정보,String 입력방법,String 원거래일자,String 원승인번호,
                           String 바코드번호,String 거래금액,String 세금,String 봉사료,String 비과세,String 통화코드,String 할부개월,String 가맹점추가정보,String 가맹점데이터,
                           String KOCES거래고유번호, TcpInterface.DataListener _tcpdatalistener)
    {
        mTcpDatalistener = _tcpdatalistener;

        String mLog = "거래구분 : " + 전문번호 + "," +"단말기ID : " + _Tid + "," + "거래일시 : " + 거래일시 + "," + "단말버전 : " + 단말버전 + "," +
                "추가정보 : " + 단말추가정보 + "," + "취소정보 : " + 취소정보 + "," + "입력방법 : " + 입력방법 + "," + "거래금액 : " + 거래금액 + "," +
                "세금 : " + 세금 + "," + "봉사료 : " + 봉사료 + "," + "비과세 : " + 비과세 + "," + "통화코드 : " + 통화코드 + "," +
                "할부개월 : " + 할부개월 + "," + "가맹점추가정보 : " + 가맹점추가정보 + "," + "가맹점데이터 : " + 가맹점데이터 + "," + "KOCES거래고유번호 : " + KOCES거래고유번호;
        cout("VAN_SEND : EASY_ZEROPAY",Utils.getDate("yyyyMMddHHmmss"),mLog);

        TCPSendData(TCPCommand.TCP_ZEROReq(전문번호,_Tid,거래일시,단말버전,단말추가정보,취소정보,입력방법,원거래일자,원승인번호,바코드번호,거래금액,세금,봉사료,비과세,통화코드,
                할부개월, 가맹점추가정보, 가맹점데이터, KOCES거래고유번호));
    }

    /**
     * 현금영수증요청
     *
     */
    public void ___cashtrade(String _Command,String _Tid,String _date,String _posVer,String _etc,String _cancelInfo,String _inputMethod,byte[] _id,byte[] _idencrpyt,String _money,
                             String _tax,String _serviceCharge, String _taxfree,String _privateOrCorp,String _cancelReason,String _pointCardCode,String _pointAcceptNum,
                             String _businessData,String _bangi, String _kocesNumber, TcpInterface.DataListener _tcpdatalistener)
    {
        mTcpDatalistener = _tcpdatalistener;
        Setting.mInputCashMethod = _inputMethod;
        Setting.mPrivateOrCorp = _privateOrCorp;

        String mLog = "거래구분 : " + _Command + "," +"단말기ID : " + _Tid + "," + "거래일시 : " + _date + "," + "단말버전 : " + _posVer + "," +
                "추가정보 : " + _etc + "," + "취소정보 : " + _cancelInfo + "," + "입력방법 : " + _inputMethod + "," + "거래금액 : " + _money + "," +
                "세금 : " + _tax + "," + "봉사료 : " + _serviceCharge + "," + "비과세 : " + _taxfree + "," + "개인/법인구분 : " + _privateOrCorp + "," +
                "취소사유 : " + _cancelReason + "," + "포인트카드코드 : " + _pointCardCode + "," + "가맹점데이터 : " + _businessData + "," +
                "반기지급명세기간 : " + _bangi + "," + "거래고유번호 : " + _kocesNumber;
        cout("VAN_SEND : CASH",Utils.getDate("yyyyMMddHHmmss"),mLog);

        TCPSendData(TCPCommand.cashReceipt( _Command, _Tid, _date, _posVer, _etc, _cancelInfo, _inputMethod, _id, _idencrpyt, _money,
                _tax, _serviceCharge,  _taxfree, _privateOrCorp, _cancelReason, _pointCardCode, _pointAcceptNum, _businessData, _bangi, _kocesNumber));
    }
    /**
     * TMS 단말기 데이타 다운로드 정보 요청
     * @param _Command
     * @param _Tid
     * @param _swVer
     * @param _serialNum
     * @param _dataType
     * @param _secKey
     * @param _tcpdatalistener
     */
    public void ___TMSDataDownInfo(String _Command,String _Tid,String _swVer,byte[] _serialNum,String _dataType,byte[] _secKey,TcpInterface.DataListener _tcpdatalistener)
    {
        mTcpDatalistener = _tcpdatalistener;
        String mLog = "거래구분 : " + _Command + "," +"단말기ID : " + _Tid + "," + "소프트웨어버전 : " + _swVer + "," + "시리얼넘버 : " + Utils.bytesToHex_0xType(_serialNum) + "," +
                "데이터타입 : " + _dataType + "," + "보안키 : " + Utils.bytesToHex_0xType(_secKey);
        cout("VAN_SEND : TMS_단말기데이타다운로드정보요청 9240",Utils.getDate("yyyyMMddHHmmss"),mLog);
        TCPTMPSendData(TCPCommand.TCP_TMSDownInfo(_Command,_Tid,_swVer,_serialNum,_dataType,_secKey));
    }

    /**
     * 통신망 조회
     * @param _Command
     * @param _Tid
     * @param _date
     * @param _posVer
     * @param _etc
     * @param _tcpdatalistener
     */
    public void ___CheckServerConnectState(String _Command,String _Tid,String _date,String _posVer,String _etc,TcpInterface.DataListener _tcpdatalistener)
    {
        mTcpDatalistener = _tcpdatalistener;
        TCPSendData(TCPCommand.TCP_CheckServerConnectStateReq(_Command,_Tid,_date,_posVer,_etc));
    }
    /**
     * PaymentActivity에서 카드넣기 요청
     */
    public boolean __Cardinsert(SerialInterface.DataListener _datalistener,String[] _target)
    {
        mSerialDatalistener = _datalistener;
        SendData(Command.insert_card_req(),_target);
        return true;
    }
    public boolean __CardDelete(SerialInterface.DataListener _datalistener,String[] _target)
    {
        mSerialDatalistener = _datalistener;
        SendData(Command.remove_card_req(),_target);
        return true;
    }
    public boolean __CardStatusCheck(SerialInterface.DataListener _datalistener,String[] _target){
        mSerialDatalistener = _datalistener;
        SendData(Command.check_card_state_req(),_target);
        return true;
    }

    public boolean __fallbackCancel(SerialInterface.DataListener _datalistener,String[] _target)
    {
        mSerialDatalistener = _datalistener;
        SendData(Command.finishsignreq(),_target);
        return true;
    }

    /**
     * 2nd Gen. 함수
     * @param _date
     * @param _resData
     * @param _issuer
     * @param _IssuerScript
     * @param _result
     * @param _datalistener
     * @param _target
     * @return
     */
    public boolean __emvComplete(String _date, byte[] _resData, byte[] _issuer, byte[] _IssuerScript,String _result,SerialInterface.DataListener _datalistener,String[] _target)
    {
        if(_resData==null || _issuer==null || _IssuerScript==null) {
            return false;
        }
        mSerialDatalistener = _datalistener;
        SendData(Command.IC_result_req(_date,_resData,_issuer,_IssuerScript,_result),_target);
        return true;
    }


    public boolean __unionCardSelect(String _flag,String card_count, String card_op1, String card_op2, String card_op3, String card_op4, String card_op5, String _time,
                                     SerialInterface.DataListener _datalistener, String[] _target)
    {
        if(_flag.equals(""))
            return false;
        mSerialDatalistener = _datalistener;
        SendData(Command.UnionPay_IC_card_Select_req(_flag,card_count,card_op1,card_op2,card_op3,card_op4,card_op5,_time),_target);
        return true;
    }

    public boolean __unionPasswordReq(String _resData, SerialInterface.DataListener _datalistener, String[] _target)
    {
        if(_resData.equals(""))
            return false;
        mSerialDatalistener = _datalistener;
        SendData(Command.UnionPay_ic_password_req(_resData),_target);
        return true;
    }

    public boolean __pinInputPassword(String _cardNumber,String _workKeyIndex,String _workKey,String minPasswd,
                                      String maxPasswd,String _msg1,String _msg2,String _msg3,String _msg4,String _displaytime,
                                      SerialInterface.DataListener _datalistener, String[] _target)
    {
        if(_cardNumber.equals(""))
            return false;
        mSerialDatalistener = _datalistener;
        String CardNumber = _cardNumber;
        CardNumber = CardNumber.substring(0,CardNumber.indexOf("="));
        StringBuffer sb = new StringBuffer();

        sb.append(CardNumber);
        sb.replace(6, 12, "000000");

//        CardNumber = CardNumber.replace("*","0");

        SendData(Command.pinpad_encypt_req( sb.toString(), _workKeyIndex, _workKey, minPasswd, maxPasswd, _msg1, _msg2, _msg3, _msg4, _displaytime),_target);
        return true;
    }
    //* 전문 끝부분---------------------------------------------------------------------------------------------

    byte _command = 0x00;   //포스->단말기로 전문요청을 보낼때 현재 어떤 전문을 보냈는지를 확인한다(이를통해서 현재 IC요청중이면 취소할때 USB 가아닌 거래취소를 보낸다.)
    SerialTimer serialTimer;

    /**
     * 시리얼 장비에 데이터를 전송하는 함수
     * @param _b 데이터
     * @param _target   보낼 장비 주소
     */
    private void SendData(byte[] _b,String[] _target)
    {
        Setting.setTimeOutValue(Setting.getSerialTimeOutValue(_b[3]));
//        mSerialTimer.cancel();
//        mSerialTimer.start();
        if(serialTimer!= null)
        {
            serialTimer.cancel();
            serialTimer = null;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(mSendStatus !=1)
                {
                    _command = _b[3];
                    serialTimer = new SerialTimer(Setting.g_iSerialTimeOutValue,1000);
                    serialTimer.cancel();
                    serialTimer.start();
                }

                Setting.setTimeOutValue(0); //시리얼 타이머를 초기화 한다.
                mSendStatus = 1; //pos로 데이터보낸다
                mSerial.write(_b,_target);
            }
        });

    }

    CountDownTimer mCountDownTimer;

    /**
     * 서버에 데이터 보내는 함수
     * @param _b 데이터
     */
    private void TCPSendData(byte[] _b)
    {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mCountDownTimer = new CountDownTimer(30000, 1000) {
                    @Override
                    public void onTick(long l) {
                        //Log.d(TAG,"UntilFinished : " + l/100);
                    }

                    @Override
                    public void onFinish() {
                        //Todo 카운트다운으로 커맨드가 정상적으로 수신을 못받았단 것 체크 가능
                        if(mSendStatus ==2)    //tcp 로 보냈는데 답이없다
                        {
                            Message message = new Message();
                            message.what = 2002;
                            message.obj = null;
                            mHandler.sendMessage(message);
                        }
                    }
                };
                mCountDownTimer.cancel();
                mCountDownTimer.start();
            }
        });
        mSendStatus = 2; //tcp로 데이터보낸다
        mTcpClient.write(_b);
    }

    private void TCPTMPSendData(byte[] _b)
    {

        mSendStatus = 2; //tcp로 데이터보낸다
        mTcpClient.SetTMPNetwork(10203, _b, new TmpNetworkInterface.ConnectListener() {
            @Override
            public void onState(int _internetState, boolean _bState, String _EventMsg) {
                Log.d(TAG,"_bState : " + _bState);
            }
        });

    }

    AutoDetectDevices autoDetectDevices;
    SerialInterface.ConnectListener connectListener = new SerialInterface.ConnectListener() {
        @Override
        public void onState(boolean _bState, String _EventMsg,String _BusName) {
            if(_bState)
            {
                cout("DEVICE STATE",Utils.getDate("yyyyMMddHHmmss"),_EventMsg);

                if(!_BusName.equals("") && Setting.g_bDeviceScanOnOff)
                {
                    //autoDetectDevices = null;
                    //2020-07-31 kim.jy

                    if(autoDetectDevices==null) {
                        autoDetectDevices = new AutoDetectDevices(mFocusActivity, _BusName);
                    }

                    if(Setting.g_WaitingSerialDeviceAddr.isEmpty())
                    {
                        cout("DEVICE STATE",Utils.getDate("yyyyMMddHHmmss"),"연결 대기큐에가 현재 빈상태");
                        cout("DEVICE STATE",Utils.getDate("yyyyMMddHHmmss"),_BusName + "큐에 삽입");
                        Setting.g_WaitingSerialDeviceAddr.offer(_BusName);
                        autoDetectDevices.ReloadDevicQueueeMethod();
                    }
                    else
                    {
                        cout("DEVICE STATE",Utils.getDate("yyyyMMddHHmmss"),_BusName + "현재 큐에 연결 대기 장치 주소가 있음");
                        cout("DEVICE STATE",Utils.getDate("yyyyMMddHHmmss"),_BusName + "대기 큐에 삽입");
                        Setting.g_WaitingSerialDeviceAddr.offer(_BusName);
                    }
                }
            }
            if(!_bState)
            {
                cout("DEVICE STATE",Utils.getDate("yyyyMMddHHmmss"),_EventMsg);
            }
        }
    };
    void getRunActivity(){
        ActivityManager activity_manager = (ActivityManager) m_Context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> task_info = activity_manager.getRunningTasks(9999);
        ComponentName componentName =  task_info.get(0).topActivity;
    }

    int mCount = 0; //2번씩 들어오는 경우가 있다.
    private final Handler mHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg) {

            mSendStatus = 0; //데이터를 받으면 초기화한다
            if(msg.what==1001)
            {
                byte[] _tmp = (byte[])msg.obj;
                if(_tmp.length>10 || _tmp[3]==(byte)0x16)
                {
                    Setting.ICResponseDeviceType = msg.arg1;
                }
                if(serialTimer != null)
                {
                    if(!Setting.g_paymentState) {
//                        mSerialTimer.cancel();
                        serialTimer.cancel();
                    }
                    else
                    {
                        Setting.g_paymentState = false;
                        serialTimer.cancel();
                    }
                }

                if (!mSendUSBCashType) {
                    ProtocolResponse((byte[])msg.obj,0);
                } else {
                    ProtocolResponseCash((byte[])msg.obj,0);
                }

            }
            else if(msg.what==2001)  //서버에서 오는 메세지
            {
                if(serialTimer != null) {
//                    mSerialTimer.cancel();
                    serialTimer.cancel();
                }
                if(mCountDownTimer != null)
                    mCountDownTimer.cancel();
                TcpProtocolResponse((byte[])msg.obj);
            }
            else if(msg.what==3001) //사인데이터
            {
                if(serialTimer != null) {
//                    mSerialTimer.cancel();
                    serialTimer.cancel();
                }
                final byte[] bData = (byte[])msg.obj;;
                if(mSerialDatalistener!=null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        mSerialDatalistener.onReceived(bData,1);
                    });
                }
            }
            else if(msg.what == 1002) //serial_timeout
            {
                if(serialTimer != null) {
//                    mSerialTimer.cancel();
                    serialTimer.cancel();
                }
                if(mFocusActivity != null){
                    mFocusActivity.ReadyDialogHide();
                }

                //TODO 여기서 이 코드를 쓰는 이유를 모르겠음 . 내가 만든 코드 인데 내가 이해가 안감.
//                mSerial = null;
//                mSerial = new KocesSerial(m_Context, mHandler, connectListener);

                if(mFocusActivity instanceof AppToAppActivity)
                {

                }
                else
                {
                    Toast.makeText(mFocusActivity,"USB 장치로부터 아무 응답이 없습니다 ",Toast.LENGTH_SHORT).show();
                }

//                Setting.g_bMainIntegrity = true;
                mCount++;
                if(mCount==1) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Setting.g_bMainIntegrity = true;
//                            DeviceReset();
                            //TODO: 이미 시리얼장비주소가 변동되어 받을 수 없는 상황에서 계쏙 리셋을 날리니 문제가 생긴다 리셋하지 않는다(jiw 23-07-26
                            mCount=0;
                            if (mAToAUsbStatelistner != null) {
                                if(_command == Command.CMD_IC_REQ)
                                {
                                    mAToAUsbStatelistner.onState(-5);
                                }
                                else {
                                    mAToAUsbStatelistner.onState(-1);
                                }
                            }
                        }
                    }, 500);
                } else {
                    Setting.g_bMainIntegrity = true;
                }


            }
            else if(msg.what == 2002) //tcp_timeout
            {
                if(serialTimer != null) {
//                    mSerialTimer.cancel();
                    serialTimer.cancel();
                }
                if(mFocusActivity != null){
                    mFocusActivity.ReadyDialogHide();
                }
//                mSerial = null;
//                mSerial = new KocesSerial(m_Context, mHandler, connectListener);
                mTcpClient.Dispose();

                mTcpClient = null;
                mTcpClient = new KocesTcpClient(Setting.getIPAddress(m_Activity),Integer.parseInt(Setting.getVanPORT(m_Activity)) ,
                        Setting.getCatIPAddress(m_Activity),Integer.parseInt(Setting.getCatVanPORT(m_Activity)) ,Integer.parseInt(Setting.getTMPVanPORT(m_Activity)),
                        mHandler, mnetworkConnectionListener);
                DeviceReset();
                if(mFocusActivity instanceof AppToAppActivity)
                {

                }
                else
                {
                    Toast.makeText(mFocusActivity,"서버로 부터 아무 응답이 없습니다. 인터넷 연결을 확인 하십시요",Toast.LENGTH_SHORT).show();
                }

                if(mAToAUsbStatelistner!=null)
                {
                    mAToAUsbStatelistner.onState(-2);
                }

            }
            else if(msg.what == 2003) //IC신용거래 중 LRC오류
            {
                if(serialTimer != null) {
//                    mSerialTimer.cancel();
                    serialTimer.cancel();
                }
                if(mCountDownTimer != null)
                    mCountDownTimer.cancel();

                if(mFocusActivity != null){
                    mFocusActivity.ReadyDialogHide();
                }

//                mSerial = null;
//                mSerial = new KocesSerial(m_Context, mHandler, connectListener);
                mTcpClient = null;
                mTcpClient = new KocesTcpClient(Setting.getIPAddress(m_Activity),Integer.parseInt(Setting.getVanPORT(m_Activity)) ,
                        Setting.getCatIPAddress(m_Activity),Integer.parseInt(Setting.getCatVanPORT(m_Activity)) ,Integer.parseInt(Setting.getTMPVanPORT(m_Activity)),
                        mHandler, mnetworkConnectionListener);
                DeviceReset();
                if(mFocusActivity instanceof AppToAppActivity)
                {

                }
                else
                {
                    Toast.makeText(mFocusActivity,"거래 중 카드 LRC 장애가 발생하였습니다",Toast.LENGTH_SHORT).show();
                }
                if(mAToAUsbStatelistner!=null)
                {
                    mAToAUsbStatelistner.onState(-3);
                }

            }
            else if(msg.what == 2004) //IC신용거래 중 NAK발생
            {
                if(serialTimer != null) {
//                    mSerialTimer.cancel();
                    serialTimer.cancel();
                }
                if(mCountDownTimer != null)
                    mCountDownTimer.cancel();

                if(mFocusActivity != null){
                    mFocusActivity.ReadyDialogHide();
                }

//                mSerial = null;
//                mSerial = new KocesSerial(m_Context, mHandler, connectListener);
                mTcpClient = null;
                mTcpClient = new KocesTcpClient(Setting.getIPAddress(m_Activity),Integer.parseInt(Setting.getVanPORT(m_Activity)) ,
                        Setting.getCatIPAddress(m_Activity),Integer.parseInt(Setting.getCatVanPORT(m_Activity)) ,Integer.parseInt(Setting.getTMPVanPORT(m_Activity)),
                        mHandler, mnetworkConnectionListener);
                DeviceReset();
                if(mFocusActivity instanceof AppToAppActivity)
                {

                }
                else
                {
                    Toast.makeText(mFocusActivity,"거래 중 NAK 발생. 인터넷 연결을 확인 하십시오",Toast.LENGTH_SHORT).show();
                }

                if(mAToAUsbStatelistner!=null)
                {
                    mAToAUsbStatelistner.onState(-4);
                }

            }
            else
            {
                if(serialTimer != null) {
//                    mSerialTimer.cancel();
                    serialTimer.cancel();
                }
                if(mCountDownTimer != null)
                    mCountDownTimer.cancel();

                if(mFocusActivity != null){
                    mFocusActivity.ReadyDialogHide();
                }
            }
        }
    };

    //device reset

    /**
     * 연결된 모든 디바이스 초기화 함수
     */
    public void DeviceReset()
    {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //usb 있는지없는지체크
                if(getUsbDevice().size() > 0)
                {
                    __PosInit("99",null,AllDeviceAddr());

                }
//                if(getUsbDevice().size() > 0){
//                    __PosInit("99",null,getICReaderAddr2());
//                    if(Setting.getSignPadType(getActivity()) == 0 || Setting.getSignPadType(getActivity()) == 3)
//                    {
//
//                    }
//                    else
//                    {
//                        __PosInit("99",null,new String[]{getSignPadAddr(),getMultiAddr()});
//                    }
//                }
            }
        });
    }

    public String[] getSerialDevicesBusList()
    {
        return mSerial.getSerialBusAddress();
    }
    private void ProtocolResponse(byte[] _res,int _type) {
        final byte[] bData =_res;
        if(mSerialDatalistener!=null && _res != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                mSerialDatalistener.onReceived(_res,_type);
            });
        }
    }

    // 현금영수증에서 2개의 장치에 서로 다른 전문을 보내야 할 경우가 있다
    private void ProtocolResponseCash(byte[] _res,int _type) {
        final byte[] bData =_res;
        mSendUSBCashType = false;
        if(mSerialDatalistenerCash!=null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                mSerialDatalistenerCash.onReceived(_res,_type);
            });
        }
    }

    private void TcpProtocolResponse(byte[] _res)
    {

        if(mTcpDatalistener!=null)
        {
            new Handler(Looper.getMainLooper()).post(()->{
                //byte[] cache = new byte[bResData.length-2];   //STX,ETX,LRC 제거를 위한 코드
                //System.arraycopy(bResData,1,cache,0,bResData.length - 2); //STX,ETX,LRC 제거를 위한 코드
                //mTcpDatalistener.onRecviced(cache); //STX,ETX,LRC 제거를 하고 보낸다
                final byte[] bResData = _res;
                mTcpDatalistener.onRecviced(bResData);
            });
        }
    }

    private void Res_MessageSend(Command.ProtocolInfo proto)
    {
        if(proto.Command!=Command.CMD_SEND_MSG_RES){return;}
        String tmp = "응답 코드:" + Utils.intToHexString(proto.Command);
        OnlyTestPageActivity(tmp);
    }

    //삭제가 필요한 코드
    public void OnlyTestPageActivity(String _str)
    {

        Intent intent = new Intent(m_Context, PopupActivity.class);
        intent.putExtra("contents", _str);
        m_Context.startActivity(intent);
    }

    /**
     * LogFile 클래스를 통해서 로컬에 로그를 기록하는 함수
     * @param _title
     * @param _time
     * @param _Contents
     */
    public void cout(String _title, String _time, String _Contents)
    {
        if(mFocusActivity!=null) {
            if (_title != null && !_title.equals("")) {
                mFocusActivity.WriteLogFile("\n");
                mFocusActivity.WriteLogFile("<" + _title + ">\n");
            }

            if (_time != null && !_time.equals("")) {
                mFocusActivity.WriteLogFile("[" + _time + "]  ");
            }

            if (_Contents != null && !_Contents.equals("")) {
                mFocusActivity.WriteLogFile(_Contents);
                mFocusActivity.WriteLogFile("\n");
            }
            else
            {
                mFocusActivity.WriteLogFile("\n");
            }
        }
    }

    private NetworkInterface.ConnectListener mnetworkConnectionListener = new NetworkInterface.ConnectListener() {
        @Override
        public void onState(int _internetState,boolean _bState,String _EventMsg) {
            if(_internetState==-1)
            {
                if(mFocusActivity!=null)
                {
                    mFocusActivity.ReadyDialogHide();
                }
                new Handler(Looper.getMainLooper()).post(()->{

                    if(mFocusActivity instanceof AppToAppActivity) {
                        Toast.makeText(m_Context, "인터넷을 연결 할 수 없습니다.", Toast.LENGTH_SHORT).show();
                        if (serialTimer != null) serialTimer.cancel();
                        if (mCountDownTimer != null) mCountDownTimer.cancel();

                        if(mAToAUsbStatelistner!=null)
                        {
                            mAToAUsbStatelistner.onState(-2);
                        }
                    }
                    else
                    {
                        if (serialTimer != null) serialTimer.cancel();
                        if (mCountDownTimer != null) mCountDownTimer.cancel();
                        Toast.makeText(m_Context,"인터넷을 연결 할 수 없습니다.",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(m_Context, Main2Activity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        m_Context.startActivity(intent);
                    }
                });

            }
            if(_bState)
            {
                new Handler(Looper.getMainLooper()).post(()->{
                    //인증에 방해 되기 때문에 표시 하지 않는다.
                    //Log.d(TAG,"TCP서버 연결 상태:true " + _EventMsg);

                });

            }
            else
            {
                new Handler(Looper.getMainLooper()).post(()->{
                    //인증에 방해 되기 때문에 표시 하지 않는다.
                    //Log.d(TAG,"TCP서버 연결 상태:false " + _EventMsg);

                });

            }
        }
    };

    class SerialTimer extends CountDownTimer
    {
        public SerialTimer(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d(TAG,"SerialDeviceTimeOutCount : " + millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {
            if(mSendStatus == 1)    //pos 로 보냈는데 답이없다
            {   Message message = new Message();
                message.what = 1002;
                message.obj = null;
                mHandler.sendMessage(message);
            }
        }
    }

    /**
     * 여기부터 BLE 장비와 연동작업
     */

    public void setResDataLinstener(bleSdkInterface.ResDataListener I) {
        this.mResDataListener = I;
    }



    public void BleConnectionListener(bleSdkInterface.ConnectionListener I) {
        mResultConnectlistener = I;
    }

    private bleSdkInterface.ConnectionListener mbleConnectionListener = new bleSdkInterface.ConnectionListener() {
        @Override
        public void onState(boolean result) {
            Setting.setBleIsConnected(result);
            if (result == true) {

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mResultConnectlistener != null) {
                        mResultConnectlistener.onState(true);
                    }
                    //Toast.makeText(mCtx, "ble 연결 되었습니다.", Toast.LENGTH_SHORT).show();


                });
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mResultConnectlistener != null) {
                        mResultConnectlistener.onState(false);
                    }

//                    Toast.makeText(m_Context, "ble 연결이 해제 되었습니다", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if(!BleIsConnected()){
                        Toast.makeText(m_Context, "ble 연결이 해제 되었습니다", Toast.LENGTH_SHORT).show();
                    }

                },3000);
            }
        }
    };

    public boolean BleConnect(Context _ctx,String _addr, String _name) {
        if (Utils.isNullOrEmpty(_addr)) {
            return false;
        }
        if (Utils.isNullOrEmpty(_name)) {
            return false;
        }
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if (_name.contains(Constants.WSP) || _name.contains(Constants.WOOSIM) ) {
            Setting.setIsWoosim(true);
            mbleWoosimSdk.Connect(_addr,_name);
        } else {
            Setting.setIsWoosim(false);
            mblsSdk.Connect(_ctx,_addr,_name);
        }
        return true;
    }

    public void BleregisterReceiver(Context _ctx)
    {
        mblsSdk.registerReceiver(_ctx);
    }

    public void BleUnregisterReceiver(Context _ctx)
    {
//        m_Context = _ctx;
        mblsSdk.unRegisterReceiver(_ctx);
    }

    public void BleDisConnect()
    {
        mblsSdk.DisConnect();
    }

    public boolean BleIsConnected()
    {
        if (mbleWoosimSdk != null) {
            if (mblsSdk.getConnected() || mbleWoosimSdk.getConnected()) {
                Setting.setBleIsConnected(true);
            } else {
                Setting.setBleIsConnected(false);
            }
        } else {
            if (mblsSdk.getConnected() ) {
                Setting.setBleIsConnected(true);
            } else {
                Setting.setBleIsConnected(false);
            }
        }
        return Setting.getBleIsConnected();
    }

    private BleWoosimReceiveTimer m_blewoosimreceivertimer = null;
    class BleWoosimReceiveTimer extends CountDownTimer
    {
        public BleWoosimReceiveTimer(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d(TAG,"BleWoosimReceiveTimer : " + millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {
            mBuffer = null;
            mRcvBuffer.clear();
            mRcvTread = null;
            Log.d(TAG,"BleWoosimReceiveTimer : onFinish");
            Toast.makeText(m_Context, "단말기로부터 데이터 수신시간 초과", Toast.LENGTH_SHORT).show();

        }
    }
    /**
     * BLE 데이터 수신
     */
    private final Handler mbleHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
//            if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
            if (Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)){
                switch (msg.what) {
                    case MESSAGE_DEVICE_NAME:
                        // save the connected device's name
//                        String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//                        Toast.makeText(m_Context, "연결 장치 : " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                        mbleWoosimConnectionListener.onState(true);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            mbleWoosimConnectionListener.onState(true);
                        });
                        break;
                    case MESSAGE_TOAST:
                        Toast.makeText(m_Context, msg.getData().getInt(TOAST), Toast.LENGTH_SHORT).show();
                        break;
                    case MESSAGE_CONNECTED_FAIL:
                    case MESSAGE_CONNECTED_TIMEOUT:
                        Toast.makeText(m_Context, msg.getData().getInt(TOAST), Toast.LENGTH_SHORT).show();
                        mbleWoosimConnectionListener.onState(false);
                        mbleWoosimSdk.Disconnect();
                        break;
                    case MESSAGE_READ:
                        byte[] data = (byte[])msg.obj;
                        String sendBufferString = Utils.bytesToHex(data);
                        Log.d("MESSAGE_READ => ", sendBufferString);
                        if (m_blewoosimreceivertimer != null) {
                            m_blewoosimreceivertimer.cancel();
                            m_blewoosimreceivertimer = null;
                        }
                        m_blewoosimreceivertimer = new BleWoosimReceiveTimer(3 * 1000, 1000);
                        m_blewoosimreceivertimer.start();

                        if (mStatusWaiting > 0 && msg.arg1 == 1 && (data[0] & 0x30) == 0x30) {
                            mStatusWaiting--;
                            mPrinterStatus = data[0] & 0xff;
                        } else {
                            // Bluetooth device로부터 data 수신
                            mRcvBuffer.put((byte[])msg.obj);

                            if(mBuffer==null)
                            {
                                if(data.length != 0 && data[0] == Command.STX) {
                                    mBuffer = new byte[data.length];
                                    System.arraycopy(data, 0, mBuffer, 0, data.length);
                                }
                                else if (data.length != 0){
                                    for(int i = 0;i<data.length;i++){
                                        if(data[i] == Command.STX){
                                            mBuffer = new byte[data.length - i];
                                            System.arraycopy(data,i,mBuffer,0,data.length);
                                            break;
                                        }
                                    }
                                }
                            }
                            else
                            {
                                byte[] buffer = new byte[mBuffer.length];
                                System.arraycopy(mBuffer,0,buffer,0,mBuffer.length);
                                mBuffer = new byte[data.length+mBuffer.length];
                                System.arraycopy(buffer,0,mBuffer,0,buffer.length);
                                System.arraycopy(data,0,mBuffer,buffer.length,data.length);
                            }

                            if (mBuffer != null && mBuffer.length>= GattAttributes.SPEC_MIN_SIZE) {
                                if (mBuffer[0] != GattAttributes.STX) /* 만약에 버퍼 시작값이 STX가 아닌 경우 전문 이상으로 보고 모든 버퍼를 지운다.*/ {
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
                                if (mBuffer.length >= protlength + GattAttributes.SPEC_HEADER_SIZE)
                                {
                                    if(mBuffer.length == protlength + GattAttributes.SPEC_HEADER_SIZE){
//                                        mResultLinstener.MessageResultLinstener(mBuffer.clone());
                                        if (mRcvTread == null) {
                                            if (m_blewoosimreceivertimer != null) {
                                                m_blewoosimreceivertimer.cancel();
                                                m_blewoosimreceivertimer = null;
                                            }
                                            mRcvTread = new ReceiveTread();
                                            mRcvTread.start();
                                        }
                                    }else{
                                        byte[] temp=new byte[protlength + GattAttributes.SPEC_HEADER_SIZE];
                                        System.arraycopy(mBuffer,0,temp,0,temp.length);
//                                        mResultLinstener.MessageResultLinstener(temp.clone());
                                        if (mRcvTread == null) {
                                            if (m_blewoosimreceivertimer != null) {
                                                m_blewoosimreceivertimer.cancel();
                                                m_blewoosimreceivertimer = null;
                                            }
                                            mRcvTread = new ReceiveTread();
                                            mRcvTread.start();
                                        }
                                    }

                                    mBuffer = null;
                                }

                            }


                        }
                        break;
                    case WoosimService.MESSAGE_PRINTER:
                        if (m_blewoosimreceivertimer != null) {
                            m_blewoosimreceivertimer.cancel();
                            m_blewoosimreceivertimer = null;
                        }
                        if (msg.arg1 == WoosimService.MSR) {
                            if (msg.arg2 == 0) {
//                            Toast.makeText(m_ctx, "MSR reading failure", Toast.LENGTH_SHORT).show();
                            } else {
                                byte[][] track = (byte[][]) msg.obj;
                                if (track[0] != null) {
                                    String str = new String(track[0]);
//                            mTrack1View.setText(str);
                                }
                                if (track[1] != null) {
                                    String str = new String(track[1]);
//                            mTrack2View.setText(str);
                                }
                                if (track[2] != null) {
                                    String str = new String(track[2]);
//                            mTrack3View.setText(str);
                                }
                            }
                        }
                        break;
                }
                return;
            }
            if (msg.arg1 == Setting.HANDLER_MSGCODE_SENDING_FAIL) {
                Toast.makeText(m_Context, (String) msg.obj, Toast.LENGTH_LONG).show();
                return;
            }
            if (msg.arg1 == Setting.HANDLER_MSGCODE_RECVDATA) {
                //장치로 부터 데이터 수신
                final byte[] bytes = (byte[]) msg.obj;
                Log.d(TAG, new String(bytes));

                if (bytes.length == 20){
                    if (bytes[0] == 0x01 && bytes[1] == 0x02 && bytes[2] == 0x44 && bytes[3] == 0x41 && bytes[4] == 0x54 && bytes[5] == 0x41) {
                        ///내부 데이터 전송 에러
                        Log.d(TAG,"내부 데이터 전송 에러 발생 ");
                        return;
                    }
                }
                if (mResDataListener != null) {
                    /** BLE 전원의 응답인 경우 결과값을 보내지 않고 종료한다 */
                    if(bytes.length > 3)
                    {
                        if(bytes[3] == 0x4F)
                        {
                            return;
                        }
                    }
                    mResDataListener.OnResult(bytes);
                }
            }
        }
    };

    /**
     * 우심 ble
     */

    public void BleWoosimConnectionListener(bleWoosimInterface.ConnectionListener I) {
        mResultWoosimConnectlistener = I;
    }
    private bleWoosimInterface.ConnectionListener mbleWoosimConnectionListener = new bleWoosimInterface.ConnectionListener() {
        @Override
        public void onState(boolean result) {
            Setting.setBleIsConnected(result);
            if (result == true) {

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mResultWoosimConnectlistener != null) {
                        mResultWoosimConnectlistener.onState(true);
                    }
                    //Toast.makeText(mCtx, "ble 연결 되었습니다.", Toast.LENGTH_SHORT).show();


                });
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mResultWoosimConnectlistener != null) {
                        mResultWoosimConnectlistener.onState(false);
                    }

//                    Toast.makeText(m_Context, "ble 연결이 해제 되었습니다", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if(!BleIsConnected()){
                        Toast.makeText(m_Context, "ble 연결이 해제 되었습니다", Toast.LENGTH_SHORT).show();
                    }

                },2000);
            }
        }
    };

    /**
     * BLE 초기화
     */
    public void __BLEDeviceInit(bleSdkInterface.ResDataListener resDataListener,String _vancode) {
        if(getInstance().BleIsConnected())
        {
            setResDataLinstener(resDataListener);
            byte[] sendData = Command.BLEDeviceInit(_vancode);
//            if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
            if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
                mbleWoosimSdk.mPrintService.write(sendData);
            } else {
                mblsSdk.writeDevice(sendData);
            }
        }

    }

    /// 장치 무결성 검사
    public void __BLEGetVerity(bleSdkInterface.ResDataListener resDataListener){
        setResDataLinstener(resDataListener);
        byte[] sendData = Command.BLEGetVerity();
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }

    /**
     * 상호인증_펌웨어업데이트관련_0x52
     * 상호인증 요청 (PC -> BLE)
     * @param _type : 0001:최신펌웨어, 0003:EMV Key
     * @return
     */
    public void __BLEauthenticatoin_req(String _type,bleSdkInterface.ResDataListener resDataListener)
    {
        setResDataLinstener(resDataListener);
        String mLog = "펌웨어구분 : " + _type;
        cout("BLE_READER_SEND : 상호인증요청 0x52",Utils.getDate("yyyyMMddHHmmss"),mLog);
        byte[] sendData = Command.mutual_authenticatoin_req(_type);
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }

    /**
     * 상호인증_결과요청 펌웨어업데이트관련_0x54
     *  상호 인증정보 결과 요청 (PC  -> BLE)
     * @param _date 시간 14 Char YYYYMMDDHHMMSS
     * @param _multipadAuth 멀티패드인증번호 32 Char 인증업체로부터 발급받은 단말 인증 번호
     * @param _multipadSerial 멀티패드시리얼번호 10 Char 멀티패드 시리얼 번호
     * @param _code 응답코드 4 * Char 정상응답 : ‚0000‛(그외 실패) 실패 수신 시 ‚0x55‛응답도 실패(01)처리
     * @param _resMsg 응답메세지
     * @param _key 접속보안키 48 Bin Srandom(16)+KEY2_ENC(IPEK+Crandom, 32)
     * @param _dataCount 데이터 개수 4 Char 주4) 데이터의 개수
     * @param _protocol 전송 프로토콜 5 Char "SFTP"
     * @param _Addr 다운로드서버주소
     * @param _port 포트
     * @param _id 계정
     * @param _passwd 비밀번호
     * @param _ver 버전 및 데이터 구분
     * @param _verDesc 버전 설명
     * @param _fn 파일명
     * @param _fnSize 파일크기
     * @param _fnCheckType 파일체크방식
     * @param _fnChecksum 파일체크섬
     * @param _dscrKey 파일복호화키
     * @return
     */
    public void __BLEauthenticatoin_result_req(byte[] _date,byte[] _multipadAuth,byte[] _multipadSerial,byte[] _code,byte[] _resMsg,byte[] _key,byte[] _dataCount,byte[] _protocol,
                                               byte[] _Addr,byte[] _port,byte[] _id,byte[] _passwd,byte[] _ver,byte[] _verDesc,byte[] _fn,byte[] _fnSize,byte[] _fnCheckType,
                                               byte[] _fnChecksum,byte[] _dscrKey,bleSdkInterface.ResDataListener resDataListener)
    {
        setResDataLinstener(resDataListener);
        String mLog = "시간 : " + Utils.bytesToHex_0xType(_date) + "," +"멀티패드인증번호 : " + Utils.bytesToHex_0xType(_multipadAuth) + "," + "멀티패드시리얼번호 : " + Utils.bytesToHex_0xType(_multipadSerial) + "," +
                "응답코드 : " + Utils.bytesToHex_0xType(_code) + "," + "응답메세지 : " + Utils.bytesToHex_0xType(_resMsg) + "," + "접속보안키 : " +  Utils.bytesToHex_0xType(_key) + "," +
                "데이터개수 : " + Utils.bytesToHex_0xType(_dataCount) + "," + "프로토콜 : " + Utils.bytesToHex_0xType(_protocol) + "," +
                "다운로드서버주소 : " + Utils.bytesToHex_0xType(_Addr) + "," + "포트 : " + Utils.bytesToHex_0xType(_port) + "," + "계정 : " + Utils.bytesToHex_0xType(_id) + "," +
                "비밀번호 : " + Utils.bytesToHex_0xType(_passwd) + "," + "버전및데이터구분 : " + Utils.bytesToHex_0xType(_ver) + "," + "버전설명 : " + Utils.bytesToHex_0xType(_verDesc) + "," +
                "파일명 : " + Utils.bytesToHex_0xType(_fn) + "," + "파일크기 : " + Utils.bytesToHex_0xType(_fnSize) + "," + "파일체크방식 : " + Utils.bytesToHex_0xType(_fnCheckType) + "," +
                "파일체크섬 : " + Utils.bytesToHex_0xType(_fnChecksum) + "," + "파일복호화키 : " + Utils.bytesToHex_0xType(_dscrKey);

        cout("BLE_READER_SEND : 상호인증정보결과요청 0x54",Utils.getDate("yyyyMMddHHmmss"),mLog);

        byte[] sendData = Command.authenticatoin_result_req(_date,_multipadAuth,_multipadSerial,_code,_resMsg,_key,_dataCount,_protocol,
                _Addr,_port,_id,_passwd,_ver,_verDesc,_fn,_fnSize,_fnCheckType,_fnChecksum,_dscrKey);

//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }

    /**
     *  업데이트 파일전송 (PC -> BLE)
     * @param _type 요청데이타구분 4 Char 0001:최신펌웨어, 0003:EMV Key
     * @param _dataLength 데이터 총 크기
     * @param _sendDataSize 전송중인 데이터 크기
     * @param _data 데이터
     * @return
     */
    public void __BLEupdatefile_transfer_req(String _type,String _dataLength,String _sendDataSize,int _defaultSize,byte[] _data,bleSdkInterface.ResDataListener resDataListener)
    {
        setResDataLinstener(resDataListener);
        String mLog = "요청데이타구분 : " + _type+ "," +"데이터 총크기 : " + _dataLength + "," + "전송중인 데이터크기 : " + _sendDataSize + "," + "데이터 : " + Utils.bytesToHex_0xType(_data);
        cout("BLE_READER_SEND : 업데이트파일전송 0x56",Utils.getDate("yyyyMMddHHmmss"),mLog);

        byte[] sendData = Command.updatefile_transfer_req(_type,_dataLength,_sendDataSize,_defaultSize,_data);
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }

    /**
     *  BLE 보안키 갱신 생성 요청 (PC -> 멀티패드)
     * @param resDataListener
     */
    public void __BLESecurityKeyUpdateReady(bleSdkInterface.ResDataListener resDataListener)
    {
        setResDataLinstener(resDataListener);
        byte[] sendData = Command.securityKey_update_ready_req();
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }

    /**
     * BLE보안키 업데이트
     * @param _date
     * @param _data
     * @param resDataListener
     */
    public void __BLESecurityKeyUpdate(String _date,byte[] _data,bleSdkInterface.ResDataListener resDataListener)
    {
        setResDataLinstener(resDataListener);
        byte[] sendData = Command.securityKey_update_req(_date,_data);
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }

    /**
     * 포스 정보 요청 발송
     * @param _date YYYYMMDDhhmmss
     */
    public void __BLEPosInfo(String _date,bleSdkInterface.ResDataListener resDataListener)
    {
        setResDataLinstener(resDataListener);
        if(_date.length()!=14){return;}
        byte[] sendData = Command.BLEpos_info(_date);
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }
    /**
     * 포스 장치 초기화
     * @param _vancode 벤코드 99
     */
    public void __BLEPosinit(String _vancode,bleSdkInterface.ResDataListener resDataListener)
    {
        setResDataLinstener(resDataListener);
        byte[] sendData = Command.pad_init(_vancode);
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM)) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }

    /**
     * ble 장치 리딩 취소
     */
    public void __BLEReadingCancel(bleSdkInterface.ResDataListener resDataListener)
    {
        setResDataLinstener(resDataListener);
        byte[] sendData = {0x1B, 0x1B, 0x1B, 0x1B, 0x1B};
//        mblsSdk.writeDevice(sendData);
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) ) {
            byte[] sendData2 = Command.BLEDeviceInit("99");
            mbleWoosimSdk.mPrintService.write(sendData2);
        }
//        else if (Setting.getBleName().contains(Constants.ZOA_KRE)) {
//            byte[] sendData2 = Command.BLEDeviceInit("99");
//            mblsSdk.writeCancelDevice(sendData2);
//        }
        else {
            byte[] sendData2 = Command.BLEDeviceInit("99");
            mblsSdk.writeCancelDevice(sendData2);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mblsSdk.writeCancelDevice(sendData);
                }
            },500);

        }
    }

    /**
     * ble 페어링 해제 후 전원 유지 시간
     */
    public void BlePowerManager(bleSdkInterface.ResDataListener resDataListener,byte _res)
    {
        setResDataLinstener(resDataListener);
        byte[] sendData = Command.PowerManager(_res);
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) ) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }
    /**
     * Ble 2nd Gen. 함수
     * @param _date
     * @param _resData
     * @param _issuer
     * @param _IssuerScript
     * @param _result
     * @param resDataListener
     * @return
     */
    public boolean __BLEemvComplete(String _date, byte[] _resData, byte[] _issuer, byte[] _IssuerScript,String _result,bleSdkInterface.ResDataListener resDataListener)
    {
        if(_resData==null || _issuer==null || _IssuerScript==null) {
            return false;
        }
        setResDataLinstener(resDataListener);
        byte[] sendData = Command.IC_result_req(_date,_resData,_issuer,_IssuerScript,_result);
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) ) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
        return true;
    }
    /**
     * 장치 정보 요청(DS 테스트용입니다. 해당데이터지워야함
     */
    public void __DeviceInfo(bleSdkInterface.ResDataListener resDataListener) {
        setResDataLinstener(resDataListener);
        byte[] sendData = Command.makeLrcData(Command.GetSystemInfo());
//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) ) {
            mbleWoosimSdk.mPrintService.write(sendData);
        } else {
            mblsSdk.writeDevice(sendData);
        }
    }

    public void __Printer(byte[] _print,bleSdkInterface.ResDataListener resDataListener)
    {
        setResDataLinstener(resDataListener);


//        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) || Setting.getBleName().contains(Constants.KWANGWOO_KRE)) {
        if(Setting.getBleName().contains(Constants.WSP) || Setting.getBleName().contains(Constants.WOOSIM) ) {
//            mbleWoosimSdk.mPrintService.write(sendData);
            Log.d("BLEPRINT", Utils.bytesToHex_0xType(_print));
            mbleWoosimSdk.mPrintService.write(_print);
        } else {
            byte[] sendData = Command.Print(_print);
            cout("SEND : PRINT_BLE",Utils.getDate("yyyyMMddHHmmss"),Utils.bytesToHex_0xType(sendData));
            Log.d("BLEPRINT", Utils.bytesToHex_0xType(sendData));
            mblsSdk.writeDevice(sendData);
        }
    }

    /**
     * CAT연동
     */
    public void CatConnect(String _ip, int _port, CatNetworkInterface.ConnectListener _CatnetworkConnectListener)
    {
        mTcpClient.SetCatNetwork(_ip,_port,_CatnetworkConnectListener);
    }

    /**
     * TMP연동
     */
    public void TmpConnect(int _port, byte[] _data, TmpNetworkInterface.ConnectListener _TmpnetworkConnectListener)
    {
        mTcpClient.SetTMPNetwork(_port, _data,_TmpnetworkConnectListener);
    }


    /**
     * 신용/현금 에 들어갈 때 체크해야 할 항목을 처리한다
     * @param _ctx
     * @param _type
     */
    public boolean CreditCashInCheck(Context _ctx, String _type)
    {
        switch (_type)
        {
            case sqliteDbSdk.TradeMethod.Cash:
                switch (Setting.g_PayDeviceType) {
                    case NONE:
                        Toast.makeText(_ctx, "장치설정을 완료하여야 합니다", Toast.LENGTH_SHORT).show();
                        return false;
                    case BLE:       //BLE의 경우

                        break;
                    case CAT:       //WIFI CAT의 경우
                        return true;
                    case LINES:     //유선장치의 경우

                        break;
                    default:
                        break;
                }
                break;
            case sqliteDbSdk.TradeMethod.Credit:

                switch (Setting.g_PayDeviceType) {
                    case NONE:
                        Toast.makeText(_ctx, "장치설정을 완료하여야 합니다", Toast.LENGTH_SHORT).show();
                        return false;
                    case BLE:       //BLE의 경우
                        BleIsConnected();
                        if (!Setting.getBleIsConnected())
                        {
                            Toast.makeText(_ctx, "BLE 장비가 연결되지 않았습니다", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        break;
                    case CAT:       //WIFI CAT의 경우
                        return true;
                    case LINES:     //유선장치의 경우
                        if (!CheckDeviceState())
                        {
                            Toast.makeText(_ctx, "USB 장비가 연결되지 않았습니다", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case sqliteDbSdk.TradeMethod.EasyPay:
                switch (Setting.g_PayDeviceType) {
                    case NONE:
                        Toast.makeText(_ctx, "장치설정을 완료하여야 합니다", Toast.LENGTH_SHORT).show();
                        return false;
                    case BLE:       //BLE의 경우
                        BleIsConnected();
                        if (!Setting.getBleIsConnected())
                        {
                            Toast.makeText(_ctx, "BLE 장비가 연결되지 않았습니다", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        break;
                    case CAT:       //WIFI CAT의 경우
                        return true;
                    case LINES:     //유선장치의 경우
                        if (!CheckDeviceState())
                        {
                            Toast.makeText(_ctx, "USB 장비가 연결되지 않았습니다", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;

        }
        if (Setting.getPreference(_ctx, Constants.STORE_NO).equals(""))
        {
            Toast.makeText(_ctx, "가맹점설정을 완료하여야 합니다", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Setting.getPreference(_ctx, Constants.TID).equals(""))
        {
            Toast.makeText(_ctx, "가맹점설정을 완료하여야 합니다", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Setting.getPreference(_ctx, Constants.REGIST_DEVICE_SN).equals(""))
        {
            Toast.makeText(_ctx, "가맹점설정을 완료하여야 합니다", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /** 연결된 장치가 카드리더기 또는 멀티카드리더기인지를 체크한다 */
    private boolean CheckDeviceState()
    {
        boolean tmp = false;
        if(getUsbDevice().size()>0 && !getICReaderAddr().equals("") &&
                CheckConnectedUsbSerialState(getICReaderAddr()))
        {
            return true;
        }
        if(getUsbDevice().size()>0 && !getMultiReaderAddr().equals("") &&
                CheckConnectedUsbSerialState(getMultiReaderAddr()))
        {
            return true;
        }
        return tmp;
    }

    private byte[] mBuffer;
    class ReceiveTread extends Thread {
        public void run() {
            try {
                // response 전문 수신 대기
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 전문 추출
            final byte[] respPacket = new byte[mRcvBuffer.position()];
            mRcvBuffer.flip();
            mRcvBuffer.get(respPacket);
            // remove byte buffer
            mRcvBuffer.clear();
            mRcvBuffer.put(new byte[mRcvBuffer.capacity()]);
            mRcvBuffer.clear();
            // 전문수신 완료 flag setting
//            mPacketProcessing = false;
            // time-out delayed message 무시하기 위해 command ID update
//            mCmdId ++;
            // 수신된 전문 처리
//            mMsgHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    Packet packet = parsePacketBluetooth(respPacket);
//
//                    if (mResultListener != null) {
//                        mResultListener.onCardReaderResult(packet);
//                    }
//                }
//            });
            if (m_blewoosimreceivertimer != null) {
                m_blewoosimreceivertimer.cancel();
                m_blewoosimreceivertimer = null;
            }
            //장치로 부터 데이터 수신
            Log.d(TAG, new String(respPacket));
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (respPacket.length == 20){
                        if (respPacket[0] == 0x01 && respPacket[1] == 0x02 && respPacket[2] == 0x44 && respPacket[3] == 0x41 && respPacket[4] == 0x54 && respPacket[5] == 0x41) {
                            ///내부 데이터 전송 에러
                            Log.d(TAG,"내부 데이터 전송 에러 발생 ");
                            return;
                        }
                    }
                    if (mResDataListener != null) {
                        /** BLE 전원의 응답인 경우 결과값을 보내지 않고 종료한다 */
                        if(respPacket.length > 3)
                        {
                            if(respPacket[3] == 0x4F)
                            {
                                return;
                            }

                            //만일 우심프린트 전문일 경우 처리 추가
//                            if(respPacket[3] == (byte)0xC6)
//                            {
//                                if (m_blewoosimreceivertimer != null) {
//                                    m_blewoosimreceivertimer.cancel();
//                                    m_blewoosimreceivertimer = null;
//                                }
//                                mFocusActivity.ReadyDialogHide();
//                                return;
//                            }
                        }
                        mResDataListener.OnResult(respPacket);
                    }
                }
            });


            mRcvTread = null;
        }
    }
}
