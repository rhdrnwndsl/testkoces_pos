package com.koces.androidpos.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.koces.androidpos.PopupActivity;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.popupInterface;

import java.util.Queue;

public class Setting {
    /** 전역으로 사용하는 장치를 설정 한다.
     * 2021.11.24 kim.jy BLE, CAT 때문에 기존의 DEVICES 클래스를 사용 할 수 없어서 추가 한다.
     * 최악의 경우이다
     * */
    public static PayDeviceType g_PayDeviceType = PayDeviceType.NONE;

    public enum PayDeviceType {
        NONE {
            @Override
            public String toString() {
                return "NONE";
            }
        },
        LINES {
            @Override
            public String toString() {
                return "LINES";
            }
        },
        BLE {
            @Override
            public String toString() {
                return "BLE";
            }
        },
        CAT {
            @Override
            public String toString() {
                return "CAT";
            }
        }
    }
    /**
     * TAB INDEX 사용하지 않음
     */
    public static final int TAB_MAIN = 1001;
    public static final int TAB_PAYMENT = 1002;
    public static final int TAB_STORE = 1101;
    public static final int TAB_DEVICE = 1201;
    public static final int TAB_DEVICE_CLASSFI = 1202;
    public static final int TAB_MANAGER = 1301;

    /**
     * 사용되는 IP, Port 를 셋팅
     */
//    public static final String HOST = "211.192.167.87";
    public static final String HOST = "211.192.167.38";
    public static final int PORT = 10555;

    public static final String CATHOST = "192.168.0.100";
    public static final int CATPORT = 9100;

    public static final int TMPPORT = 10203;
    /**
     * 앱 비밀 번호
     */
    public final static String GLOBAL_PASSWORD_FIX = "3415";

    private final static String SharePerfenceceKey = "com.KocesPos.Preference";
    /** 무결성 성공여부 */
    public static boolean IntegrityResult = false;
    /** Tcp 연결 상태 */
    private static boolean mTcpServerConnectionState;
    /** CAT Tcp 연결 상태 */
    private static boolean mCATTcpServerConnectionState;
    /** TMP Tcp 연결 상태 */
    private static boolean mTMPTcpServerConnectionState;
    /** 사용하지 않음 */
    public static popupInterface.popupListener mPopupListener;
    /** 장비 스캔 할지 말지 여부 */
    public static boolean g_bDeviceScanOnOff = false;
    /** 메인 화면에서 무결성 검사 진행 완료(성공여부관계없음) */
    public static boolean g_bMainIntegrity = false;
    /** 각 커맨드 마다 타임아웃 시간을 설정한다 */
    public static int g_iSerialTimeOutValue = 30000;
    /**
     * eot 수신이 안되었을 때 체크
     */
    private static int mEOTComplete = 0;

    /**
     * 오토디텍트에서 가장 최상위 context 알려주기
     */
    private static Context mTopContext;
    /** 터치 서명 값을 저장 하기 위해서 */
    public static String g_sDigSignInfo="";
    /** 사용안함단말기인증번호 tmicno */
    private static String mCardReaderNumber = "";
    /** 단말기에서받은 제품코드번호 */
    private static String mCodeVersionNumber = "";
    /** 가맹점데이터 */
    private static String mMchData = "";
    /** IC 결제를 요청하고 먼저 응답한 장치를 설정 한다 */
    public static int ICResponseDeviceType = 0;
    /** 페이먼트 결제 중인 경우 timeout. tcp들어가기 전에 */
    public static boolean g_paymentState = false;
    /** 캐쉬의 인풋메쏘드, 캐쉬거래에서 EOT 미수신으로 망취소를 날릴때 저장된 인풋메쏘드가 필요하다 */
    public static String mInputCashMethod = "";
    /** 캐쉬의 개인/법인구분자, 캐쉬거래에서 EOT 미수신으로 망취소를 날릴때 저장된 개인/법인구분자가 필요하다 */
    public static String mPrivateOrCorp = "";
    /** 현재 장치 재검색(새장비를 인식중)일 때 유저가 메인함수를 킬 경우 2개가 동시에 동작되는 것을 막을 필요가 있다 */
    public static boolean g_AutoDetect = false;
    /** 현재 사용하지 않음 */
    public static boolean g_InitReloadDevice = false;
    /** 단말기번호 메인화면에 해당 번호를 집어넣는다 */
    public static String mAuthNum = "";
    /** 앱투앱으로 보안키여부를 물어볼 때 */
    public static String mLineHScrKeyYn = "";
    /** 앱투앱으로 보안키여부를 물어볼 때 */
    public static String mBleHScrKeyYn = "";
    /** 큐에 현재 connect 된 디바이스의 주소를 추가한다 */
    public static Queue<String> g_WaitingSerialDeviceAddr;
    /** 앱의 최초 실행여부를 체크한다. 이를통해 앱투앱에서 IC앱을 실행할지를 정한다 */
    public static boolean g_bfirstexecAppToApp = true;
    /** ble 연결 상태 */
    public static boolean bleisConnected = false;  //ble 연결 상태

    /** 내부 값 설정 */
    public final static int HANDLER_MSGCODE_SENDING_FAIL = 30001;
    public final static int HANDLER_MSGCODE_RECVDATA = 65001;

    /** 앱투앱으로 실행시 전자서명을 할지 무서명일지 체크 */
    private static String mDscYn = "1"; //전자서명사용여부(현금X) 0:무서명 1:전자서명 2: bmp data 사용

    /** 앱투앱으로 실행시 보내온 전자서명 데이터 */
    private static String mDscData = "";

    /** 최초실행인지 체크 true = 한번실행함 */
    private static boolean mFirst = false;

    /** 간편결제인지 체크. 사인데이터처리를 위해 */
    private static boolean mEasyCheck = false;

    /** 기존ble 연결이면 무결성검사 등을 안하기 위해처리 */
    private static String mbleName = "mbleName";
    private static String mbleAddr = "mbleAddr";

    /** 현재 앱투앱인지 아닌지 체크. 이유는 앱투앱일 경우 ic메세지박스같은 경우 하단바를 안보이게 하기 위해 */
    private static boolean mIsAppToApp = false;

    /** usb 통신을 처리하는데 현재 usb로 통신설정되어 있는데 처음실행인지를 체크하며 앱투앱인지도 체크하여 true,false를 처리한다
     * 1. 앱투앱으로 실행인지를 체크
     * 2. 본앱으로 실행인데 아직 한번도 실행안한 건지를 체크
     * 3. 한번도 실행안했는데 usbserial 인지를 체크
     * 4. usbserial인 경우 일단 serial 만 등록하고 백그라운드로 내림
     * */
    private static int mIsAppForeGround = 0; // 0=최초시작, 1=포그라운드 2=백그라운드
//    private static boolean mIsUSBAutoMain2UI = true;
    private static boolean mIsUSBConnectMainStart = false;

    private static long MIN_CLICK_INTERVAL = 1000;
    private static long mLastClickTime = 0;
    public synchronized static void setIsmLastClickTime (long _mLastClickTime)
    {
        mLastClickTime = _mLastClickTime;
    }
    public synchronized static long getIsmLastClickTime ()
    {
        return mLastClickTime;
    }
    public synchronized static long getIsMIN_CLICK_INTERVAL ()
    {
        return MIN_CLICK_INTERVAL;
    }
    /**
     * mIsAppForeGround 포그라운드인지 백그라운드인지 최초실행인지 체크
     * @param _IsAppForeGround
     */
    public synchronized static void setIsAppForeGround (int _IsAppForeGround)
    {
        mIsAppForeGround = _IsAppForeGround;
    }
    /** usb 통신을 처리하는데 현재 usb로 통신설정되어 있는데 처음실행인지를 체크하며 앱투앱인지도 체크하여 true,false를 처리한다 */
    public synchronized static int getIsAppForeGround ()
    {
        return mIsAppForeGround;
    }
//    public synchronized static void setIsUSBAutoMain2UI(boolean _IsUSBAutoMain2UI)
//    {
//        mIsUSBAutoMain2UI = _IsUSBAutoMain2UI;
//    }
//    /** usb 통신을 처리하는데 현재 usb로 통신설정되어 있는데 처음실행인지를 체크하며 앱투앱인지도 체크하여 true,false를 처리한다 */
//    public synchronized static boolean getIsUSBAutoMain2UI()
//    {
//        return mIsUSBAutoMain2UI;
//    }
    public synchronized static void setIsUSBConnectMainStart(boolean _IsUSBConnectMainStart)
    {
        mIsUSBConnectMainStart = _IsUSBConnectMainStart;
    }
    /** usb 통신을 처리하는데 현재 usb로 통신설정되어 있는데 처음실행인지를 체크하며 앱투앱인지도 체크하여 true,false를 처리한다 */
    public synchronized static boolean getIsUSBConnectMainStart()
    {
        return mIsUSBConnectMainStart;
    }


    /**
     * 현재 앱투앱인지 아닌지 체크. 이유는 앱투앱일 경우 ic메세지박스같은 경우 하단바를 안보이게 하기 위해
     * @param _IsAppToApp
     */
    public synchronized static void setIsAppToApp(boolean _IsAppToApp)
    {
        mIsAppToApp = _IsAppToApp;
    }
    /** 현재 앱투앱인지 아닌지 체크. 이유는 앱투앱일 경우 ic메세지박스같은 경우 하단바를 안보이게 하기 위해 */
    public synchronized static boolean getIsAppToApp()
    {
        return mIsAppToApp;
    }

    /**
     * 기존ble 연결이면 무결성검사 등을 안하기 위해처리
     * @param _name
     */
    public synchronized static void setBleName(String _name)
    {
        mbleName = _name;
    }
    /** 기존ble 연결이면 무결성검사 등을 안하기 위해처리 */
    public synchronized static String getBleName()
    {
        return mbleName;
    }
    /**
     * 기존ble 연결이면 무결성검사 등을 안하기 위해처리
     * @param _addr
     */
    public synchronized static void setBleAddr(String _addr)
    {
        mbleAddr = _addr;
    }
    /** 기존ble 연결이면 무결성검사 등을 안하기 위해처리 */
    public synchronized static String getBleAddr()
    {
        return mbleAddr;
    }

    /**
     * 간편결제인지 체크 저장 true = 간편결제
     * @param _check
     */
    public synchronized static void setEasyCheck(boolean _check)
    {
        mEasyCheck = _check;
    }
    /** 최초실행인지 상태 저장 true = 한번실행함 */
    public synchronized static boolean getEashCheck()
    {
        return mEasyCheck;
    }

    /**
     * 최초실행인지 상태 저장 true = 한번실행함
     * @param _first
     */
    public synchronized static void setOnFirst(boolean _first)
    {
        mFirst = _first;
    }
    /** 최초실행인지 상태 저장 true = 한번실행함 */
    public synchronized static boolean getOnFirst()
    {
        return mFirst;
    }

    /**
     * CAT Tcp 연결 상태 저장
     * @param _state
     */
    public synchronized static void setCatTcpServerConnectionState(boolean _state)
    {
        mCATTcpServerConnectionState = _state;
    }
    /** CAT Tcp 연결 상태 응답 */
    public synchronized static boolean getCatTcpServerConnectionState()
    {
        return mCATTcpServerConnectionState;
    }

    /**
     * TMP Tcp 연결 상태 저장
     * @param _state
     */
    public synchronized static void setTmpTcpServerConnectionState(boolean _state)
    {
        mTMPTcpServerConnectionState = _state;
    }
    /** CAT Tcp 연결 상태 응답 */
    public synchronized static boolean getTmpTcpServerConnectionState()
    {
        return mTMPTcpServerConnectionState;
    }


    /** Tcp 연결 상태 응답 */
    public synchronized static boolean getTcpServerConnectionState()
    {
        return mTcpServerConnectionState;
    }

    /**
     * Tcp 연결 상태 저장
     * @param _state
     */
    public synchronized static void setTcpServerConnectionState(boolean _state)
    {
        mTcpServerConnectionState = _state;
    }

    /**BLE CAT 연결 상태에서 설정한다. */
    public static void setBleIsConnected(boolean state){
        bleisConnected = state;
    }
    /** BLE 연결 상태를 얻어 온다 */
    public static boolean getBleIsConnected(){
        return bleisConnected;
    }
    /**
     * 안드로이드에 저장되어 있던 설정들(디바이스 등의 정보)을 불러온다
     * @param _ctx
     * @return
     */
    private static SharedPreferences getSharedPreferences(Context _ctx) {
        return _ctx.getSharedPreferences(SharePerfenceceKey, _ctx.MODE_PRIVATE);
    }

    /**
     * 안드로이드에 설정들(디바이스 등의 정보)를 저장한다
     * @param _ctx 해당엑티비티
     * @param _key 값이 저장될 키
     * @param _val 저장할 값
     */
    public static void setPreference(Context _ctx, String _key, String _val) {
        SharedPreferences Pref = getSharedPreferences(_ctx);
        SharedPreferences.Editor editor = Pref.edit();
        editor.putString(_key, _val);
        editor.commit();
    }

    /**
     * 안드로이드에서 불러온 설정들을 해당 엑티비티로 보낸다
     * @param _ctx 해당엑티비티
     * @param _key 저장된 값들의 키
     * @return
     */
    public static String getPreference(Context _ctx, String _key) {
        SharedPreferences Pref = getSharedPreferences(_ctx);
        String tmp = Pref.getString(_key, "");
        return tmp;
    }

    /** 사용하지 않음 */
    public static void clearPreference(Context _ctx) {
        SharedPreferences Pref = getSharedPreferences(_ctx);
        SharedPreferences.Editor editor = Pref.edit();
        editor.clear();
        editor.commit();
    }

    /**
     * 벤사 서버를 가져 온다. 이 때 설정된 값이 없다면 기본 설정한 주소를 반환 한다.
     * @param _ctx
     * @return
     */
    public static String getIPAddress(Context _ctx) {
        String ip = getPreference(_ctx, Constants.VAN_IP);
        if (ip.equals("")) {
            return HOST;
        }
        return ip;
    }

    /**
     * 벤사 서버주소를 저장한다. 환경설정에서 셋팅한다
     * @param _ctx
     * @param _vanIP
     */
    public static void setIPAddress(Context _ctx, String _vanIP) {
        if (_vanIP != null) {
            setPreference(_ctx, Constants.VAN_IP, _vanIP);
        }
    }

    /**
     * 벤사 서버 포트번호를 가져온다. 이 때 설정된 값이 없다면 기본 설정된 포트번호를 반환 한다
     * @param _ctx
     * @return
     */
    public static String getVanPORT(Context _ctx) {
        String port = getPreference(_ctx, Constants.VAN_PORT);
        if (port.equals("")) {
            return String.valueOf(PORT);
        }
        return port;
    }

    /**
     * 벤사 서버 포트번호를 저장한다. 환경설정에서 셋팅한다
     * @param _ctx
     * @param _vanPORT
     */
    public static void setVanPORT(Context _ctx, String _vanPORT) {
        if (_vanPORT != null) {
            setPreference(_ctx, Constants.VAN_PORT, _vanPORT);
        }
    }

    /**
     * 벤사 서버를 가져 온다. 이 때 설정된 값이 없다면 기본 설정한 주소를 반환 한다.
     * @param _ctx
     * @return
     */
    public static String getCatIPAddress(Context _ctx) {
        String ip = getPreference(_ctx, Constants.CAT_IP);
        if (ip.equals("")) {
            return CATHOST;
        }
        return ip;
    }

    /**
     * 벤사 서버주소를 저장한다. 환경설정에서 셋팅한다
     * @param _ctx
     * @param _catIP
     */
    public static void setCatIPAddress(Context _ctx, String _catIP) {
        if (_catIP != null) {
            setPreference(_ctx, Constants.CAT_IP, _catIP);
        }
    }

    /**
     * 벤사 서버 포트번호를 가져온다. 이 때 설정된 값이 없다면 기본 설정된 포트번호를 반환 한다
     * @param _ctx
     * @return
     */
    public static String getCatVanPORT(Context _ctx) {
        String port = getPreference(_ctx, Constants.CAT_PORT);
        if (port.equals("")) {
            return String.valueOf(CATPORT);
        }
        return port;
    }

    /**
     * 벤사 서버 포트번호를 가져온다. 이 때 설정된 값이 없다면 기본 설정된 포트번호를 반환 한다
     * @param _ctx
     * @return
     */
    public static String getTMPVanPORT(Context _ctx) {
        String port = getPreference(_ctx, Constants.TMP_PORT);
        if (port.equals("")) {
            return String.valueOf(TMPPORT);
        }
        return port;
    }

    /**
     * 벤사 서버 포트번호를 저장한다. 환경설정에서 셋팅한다
     * @param _ctx
     * @param _catPORT
     */
    public static void setCatVanPORT(Context _ctx, String _catPORT) {
        if (_catPORT != null) {
            setPreference(_ctx, Constants.CAT_PORT, _catPORT);
        }
    }


    /**
     * 디바이스 설정에서 ICREADER 콤보 박스에서 설정하는 값을 저정한다.
     * @param _ctx
     * @return
     */
    public static void setICReaderType(Context _ctx, int _iType) {
        setPreference(_ctx, Constants.IC_READER_TYPE, String.valueOf(_iType));
    }

    /**
     * 디바이스 설정에서 ICREADER 콤보 박스에서 설정하는 값을 가져한다
     * @param _ctx
     * @return
     */
    public static int getICReaderType(Context _ctx) {
        if (getPreference(_ctx, Constants.IC_READER_TYPE).equals("")) {
            return 0;
        }

        int type = Integer.parseInt(getPreference(_ctx, Constants.IC_READER_TYPE));
        return type;
    }

    /**
     * 디바이스 설정에서 사인패드 콤보 박스에서 설정하는 값을 저정한다.
     * @param _ctx
     * @return
     */
    public static void setSignPadType(Context _ctx, int _iType) {
        setPreference(_ctx, Constants.SIGN_PAD_TYPE, String.valueOf(_iType));
    }

    /**
     * 디바이스 설정에서 사인패드 콤보 박스에서 설정하는 값을 가져한다
     * @param _ctx
     * @return
     */
    public static int getSignPadType(Context _ctx) {
        if (getPreference(_ctx, Constants.SIGN_PAD_TYPE).equals("")) {
            return 0;
        }

        int type = Integer.parseInt(getPreference(_ctx, Constants.SIGN_PAD_TYPE));
        return type;
    }

    /**
     * 최상위 엑티비티가 무엇인지를 반환한다
     * @return
     */
    public synchronized static Context getTopContext() {
        return mTopContext;
    }

    /**
     * 현재 최상위 엑티비티를 셋팅한다. 각 엑티비티가 실행될 때 실행한다
     * @param _context
     */
    public synchronized static void setTopContext(Context _context) {
        mTopContext = _context;
    }

    /**
     * eot 수신이 안되었을 때를 체크하여 반환한다 값이 1이면 eot 미수신
     * @return
     */
    public synchronized static int getEOTResult() {
        return mEOTComplete;
    }

    /**
     * eot 수신,미수신을 체크한다 기본값=0 미수신=1 정상적으로 수신받지못했다면= -1
     * @param _result
     */
    public synchronized static void setEOTResult(int _result) {
        mEOTComplete = _result;
    }

    /**
     * 등록된 TID인지를 체크 한다. APP TO APP 때문에 이 함수를 만든다
     * @param _ctx
     * @param _Tid
     * @return
     */
    public static boolean CheckAppToAppTIDregistration(Context _ctx, String _Tid) {
        //DB를 검사 한다.

        //현재 저장 되어 있는 TID를 검사 한다.
        String PreferenceTid = Setting.getPreference(_ctx, Constants.APPTOAPP_TID);
        if (PreferenceTid.equals(_Tid)) {
            return true;
        }
        return false;
    }

    /**
     * 사용안함거래고유키 취소시에 단말인증번호를 리더기에서 받아오지 못하니 해당 단말기인증번호를 등록해놓고 사용한다.
     * @return
     */
    public synchronized static String getCardReaderNumber(){
        return mCardReaderNumber;
    }

    /**
     * 사용안함거래고유키 취소시에 단말인증번호를 리더기에서 받아오지 못하니 해당 단말기인증번호를 등록해놓고 사용한다
     * @param _cardreadernumber
     */
    public synchronized static void setCardReaderNumber(String _cardreadernumber){
        mCardReaderNumber = _cardreadernumber;
    }

    /**
     * 단말기 제품코드버전번호를 거래시 보낸다
     * @return
     */
    public synchronized static String getCodeVersionNumber(){
        return mCodeVersionNumber;
    }

    /**
     * 단말기 제품코드버전번호를 저장한다
     * @param _codeversion
     */
    public synchronized static void setCodeVersionNumber(String _codeversion){
        mCodeVersionNumber = _codeversion;
    }

    /**
     * 앱투앱으로 가맹점데이터를 받았는데 사인결제시 사인패드에 갔다오면 해당 내용이 사라진다
     * @return
     */
    public synchronized static String getMchdata(){
        return mMchData;
    }

    /**
     * 앱투앱으로 가맹점데이터를 받았는데 사인결제시 사인패드에 갔다오면 해당 내용이 사라진다
     * @param _mchdata
     */
    public synchronized static void setMchdata(String _mchdata){
        mMchData = _mchdata;
    }

    /**
     * 앱투앱으로 전자서명 사용여부를 받았다. 사인결제시 사인패드에 갔다오면 해당 내용이 사라진다
     * @return
     */
    public synchronized static String getDscyn(){
        return mDscYn;
    }

    /**
     * 앱투앱으로 전자서명 데이터를 받았다. 해당데이터가 있으명 이 데이터를 사용하여 사인결제를 한다
     * @param _mDscData
     */
    public synchronized static void setDscData(String _mDscData){
        mDscData = _mDscData;
    }

    /**
     * 앱투앱으로 전자서명 데이터를 받았다. 해당 데이터가 있으면 서명을 받지 않고 해당 데이터로 결제한다
     * @return
     */
    public synchronized static String getDscData(){
        return mDscData;
    }

    /**
     * 앱투앱으로 가맹점데이터를 받았는데 사인결제시 사인패드에 갔다오면 해당 내용이 사라진다
     * @param _mDscYn
     */
    public synchronized static void setDscyn(String _mDscYn){
        mDscYn = _mDscYn;
    }

    /** 사용하지 않음 */
    public static boolean Showpopup(Context _ctx, String _Message, String _imgPath1, String _imgPath2) {
        if (mPopupListener == null) {
            Intent intent = new Intent(_ctx, PopupActivity.class);
            intent.putExtra("contents", _Message);
            intent.putExtra("img1", _Message);
            intent.putExtra("img2", _Message);
            _ctx.startActivity(intent);
        } else {
            setPopupMessage(_Message, _imgPath1, _imgPath2);
        }

        return true;
    }

    /** 사용하지 않음 */
    public static boolean setPopupMessage(String _Message, String _imgPath1, String _imgPath2) {

        if (mPopupListener == null) {
            return false;
        } else {
            mPopupListener.onState(true, _Message, _imgPath1, _imgPath2);
            return true;
        }
    }

    /** 사용하지 않음 */
    public static boolean HidePopup() {
        if (mPopupListener != null) {
            mPopupListener.onState(false, null, null, null);
        }

        return true;
    }

    /**
     * 각 커맨드마다 타임아웃을 설정한다
     * @param _time
     */
    public static void setTimeOutValue(int _time)
    {
        if(_time==0)
        {
            g_iSerialTimeOutValue = getSerialTimeOutValue((byte)0x00);
            return;
        }
        int temp = _time;
        if(temp< 1000)  //사람들이 단위를 초로 생각 할 수 있기 때문에
        {
            temp = temp * 1000;
        }
        if(temp>0)
        {
            g_iSerialTimeOutValue=temp;
            return;
        }
    }

    /**
     * 각 커맨드마다 고유의 시간을 셋팅한다
     * @param _COMMAND
     * @return
     */
    public static int getSerialTimeOutValue(byte _COMMAND)
    {
        int defalutTime = 30000;
        switch (_COMMAND)
        {
            case Command.CMD_INIT:
                defalutTime = Command.TMOUT_CMD_INIT;
                break;
            case Command.CMD_RF_INIT:
                defalutTime = Command.TMOUT_CMD_RF_INIT;
                break;
            case Command.CMD_POSINFO_REQ:
                defalutTime = Command.TMOUT_CMD_POSINFO_REQ;
                break;
            case Command.CMD_SIGN_REQ:
                defalutTime = Command.TMOUT_CMD_SIGN_REQ;
                break;
            case Command.CMD_SIGN_REQ1:
                defalutTime = Command.TMOUT_CMD_SIGN_REQ1;
                break;
            case Command.CMD_NO_ENCYPT_NUMBER_REQ:
                defalutTime = Command.TMOUT_CMD_NO_ENCYPT_NUMBER_REQ;
                break;
            case Command.CMD_ENCYPT_NUMBER_REQ:
                defalutTime = Command.TMOUT_CMD_ENCYPT_NUMBER_REQ;
                break;
            case Command.CMD_SEND_MSG_REQ:
                defalutTime = Command.TMOUT_CMD_SEND_MSG_REQ;
                break;
            case Command.CMD_RF_TRADE_REQ:
                defalutTime = Command.TMOUT_CMD_RF_TRADE_REQ;
                break;
            case Command.CMD_QR_SEND_REQ:
                defalutTime = Command.TMOUT_CMD_QR_SEND_REQ;
                break;
            case Command.CMD_BACCOUNT_REQ:
                defalutTime = Command.TMOUT_CMD_BACCOUNT_REQ;
                break;
            case Command.CMD_CDC_SELECT_REQ:
                defalutTime = Command.TMOUT_CMD_CDC_SELECT_REQ;
                break;
            case Command.CMD_IC_REQ:  //APP TO APP 에서 설정한 값이 있는 경우에는 APP TO APP 값을 우선으로 사용한다.
                defalutTime = Command.TMOUT_CMD_IC_REQ;
                break;
            case Command.CMD_UNION_IC_:
                defalutTime = Command.TMOUT_CMD_UNION_IC_;
                break;
            case Command.CMD_IC_RESULT_REQ:
                defalutTime = Command.TMOUT_CMD_IC_RESULT_REQ;
                break;
            case Command.CMD_KEYUPDATE_READY_REQ:
                defalutTime = Command.TMOUT_CMD_KEYUPDATE_READY_REQ;
                break;
            case Command.CMD_KEYUPDATE_REQ:
                defalutTime = Command.TMOUT_CMD_KEYUPDATE_REQ;
                break;
            case Command.CMD_UNIONPAY_PARASSWORD_REQ:
                defalutTime = Command.TMOUT_CMD_UNIONPAY_PARASSWORD_REQ;
                break;
            default:
                break;
        }
        return defalutTime;
    }

    /**
     * 앱투앱으로 타임아웃시간이 들어오면 해당하는 타임아웃시간으로 커맨드의 거래관련 타임아웃시간을 재설정한다
     */
    public static void setCommandTimeOut(int _time)
    {
        int temp = _time;
        if(temp< 1000)  //사람들이 단위를 초로 생각 할 수 있기 때문에
        {
            temp = temp * 1000;
        }
        if(temp>0)
        {
            Command.TMOUT_CMD_SIGN_REQ = temp;
            Command.TMOUT_CMD_SIGN_REQ1 = temp;
            Command.TMOUT_CMD_NO_ENCYPT_NUMBER_REQ = temp;
            Command.TMOUT_CMD_ENCYPT_NUMBER_REQ = temp;
            Command.TMOUT_CMD_RF_TRADE_REQ = temp;
            Command.TMOUT_CMD_QR_SEND_REQ = temp;
            Command.TMOUT_CMD_IC_REQ = temp;
            Command.TMOUT_CMD_UNION_IC_ = temp;
            Command.TMOUT_CMD_IC_RESULT_REQ = temp;
            Command.TMOUT_CMD_UNIONPAY_PARASSWORD_REQ = temp;
        }
    }

    /**
     * 장치설정이 어떻걸로 되어있는지 체크. 유선이 아닌경우로 되어있는데 유선을 연결하면 앱이죽는다. 앱이 자동으로 유선을 연결하려고 하기 때문으로 보인다.
     * @param _ctx
     * @return
     */
    public static PayDeviceType DeviceType(Context _ctx)
    {
        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(_ctx, Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }

        return Setting.g_PayDeviceType;
    }

    private static boolean isBleWoosim = false; //ble 연결하려는 것이 우심인지 아닌지를 체크
    /**BLE 연결 상태에서 우심인지 설정한다. */
    public static void setIsWoosim(boolean isWoosim){
        isBleWoosim = isWoosim;
    }
    /** BLE 연결 상태를 우심인지 얻어 온다 */
    public static boolean getIsWoosim(){
        return isBleWoosim;
    }

    /**
     * 사용하지 않음
     * 내부적으로 장치 설정에 관현 값을 정의 한다.
     * 예를 들어 카드리더기, 멀티리더기 인지.
     */
    class DeviceSetting
    {
        DeviceSetting instance;
        private int mReaderType;
        private int mSignPadType;
        private int mReaderAddr;
        private int mSignPadAddr;
        public DeviceSetting()
        {
            load_DeviceSetting_value();
        }

        public DeviceSetting getInstance()
        {
            if(instance==null)
            {
                instance = new DeviceSetting();
            }
            return instance;
        }

        /**
         * setprefernce값을 읽어서 설정 한다.
         */
        private void load_DeviceSetting_value()
        {}
        private void save_DeviceSEtting_value()
        {}
        public void updateValue(int _type,String _value)
        {

        }


    }
}
