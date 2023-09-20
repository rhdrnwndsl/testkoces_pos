package com.koces.androidpos.sdk.Devices;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.service.media.MediaBrowserService;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.koces.androidpos.AppToAppActivity;
import com.koces.androidpos.BaseActivity;
import com.koces.androidpos.CashActivity2;
import com.koces.androidpos.CashCancelActivity;
import com.koces.androidpos.CashICActivity;
import com.koces.androidpos.CashLoadingActivity;
import com.koces.androidpos.CreditActivity;
import com.koces.androidpos.CreditCancelActivity;
import com.koces.androidpos.CreditLoadingActivity;
import com.koces.androidpos.EasyLoadingActivity;
import com.koces.androidpos.EasyPayActivity;
import com.koces.androidpos.Main2Activity;
import com.koces.androidpos.MainActivity;
import com.koces.androidpos.OtherPayActivity;
import com.koces.androidpos.PaymentActivity;
import com.koces.androidpos.PinInputActivity;
import com.koces.androidpos.PrefpasswdActivity;
import com.koces.androidpos.ReceiptCashActivity;
import com.koces.androidpos.ReceiptCreditActivity;
import com.koces.androidpos.ReceiptEasyActivity;
import com.koces.androidpos.SalesInfoActivity;
import com.koces.androidpos.TradeListActivity;
import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.log.LogFile;
import com.koces.androidpos.sdk.van.Constants;

/**
 * 장치 검색 클래스
 */
public class AutoDetectDevices {
    KocesPosSdk mPosSdk;
    int mDCount = 0;
    int mCount = 0;
    int mCount4 = 0;
    boolean bNext = false;
    boolean b_detectSet = false;
    /**
     * 임시로 저장 되는 장치 주소
     */
    private String[] busAddr;
    menu2Activity m_act;
    BaseActivity m_baseCtx;
    BaseActivity mctx;
    boolean mCheckOption = false;
    LogFile logFile;
    String mAuth="";
    AutoDetectDeviceInterface.DataListener mDataListener = null;
    private static String TmepAddr = "";
    AutoDetectTimer mAutoTimer;
    /**
     * 환경 설정에서 사용하는 장치 재검색 함수
     * @param _ctx
     * @param _mDataListener
     */
    public AutoDetectDevices(menu2Activity _ctx, AutoDetectDeviceInterface.DataListener _mDataListener)
    {
        b_detectSet=false;
        mPosSdk = KocesPosSdk.getInstance();
        m_act = _ctx;
        mDataListener = _mDataListener;
        m_act.ReadyDialogShow(m_act,  Command.MSG_START_RESCAN_DEVICE,0);
        //m_act.ReadyDialogShow(m_act, "AutoDetectDevice 자동 스캔 ",0);
        CheckDeviceCount();
        mPosSdk.mDevicesList = new ArrayList<>();
        mCount = 0;
        logFile = LogFile.getinstance();
        Start();
    };

    /**
     * 메인 및 앱투앱에서 유선 연결시 장치 재검색하는 함수
     * @param _ctx
     * @param _mDataListener
     */
    public AutoDetectDevices(Activity _ctx, AutoDetectDeviceInterface.DataListener _mDataListener)
    {
        b_detectSet=false;
        mPosSdk = KocesPosSdk.getInstance();
        m_baseCtx = (BaseActivity) _ctx;
        mDataListener = _mDataListener;

//        m_baseCtx.ReadyDialogShow(m_baseCtx,  Command.MSG_START_RESCAN_DEVICE,0);
        m_baseCtx.ReadyToastShow(m_baseCtx,Command.MSG_START_RESCAN_DEVICE,1);
        //m_act.ReadyDialogShow(m_act, "AutoDetectDevice 자동 스캔 ",0);
        CheckDeviceCount();
        mPosSdk.mDevicesList = new ArrayList<>();
        mCount = 0;
        logFile = LogFile.getinstance();
        Start2();
    };

    /**
     * 앱 시작시 호출 해서 디바이스 체크 하는 함수<br>
     * (현재 사용하지 않음)
     * @param _ctx
     * @param Compare
     */
    public AutoDetectDevices(BaseActivity _ctx,boolean Compare,AutoDetectDeviceInterface.DataListener _mDataListener)
    {
        b_detectSet=false;
        mCheckOption = Compare;
        mPosSdk = KocesPosSdk.getInstance();
        mctx = _ctx;
//        mctx.ReadyDialogShow(mctx,Command.MSG_START_RESCAN_DEVICE,0);
        mDataListener = _mDataListener;
        logFile = LogFile.getinstance();
        //mctx.ReadyDialogShow(mctx,"APP to APP 사용하는 장치 스캔  ",0);
        CheckDeviceCount();
        //mPosSdk.mDevicesList = new ArrayList<>();
        if(!Compare) {
            ReCheckScan();
        }
        else
        {
            ReCompareScan();
        }

    }

    /**
     * 장치usb가 안드로이드에 새로 연결되었을 때 AutoDetectDevices 를 초기설정한다
     * @param _ctx
     * @param _busName
     */
    public AutoDetectDevices(Context _ctx,String _busName)
    {
        b_detectSet=false;
        mPosSdk = KocesPosSdk.getInstance();
        String topActivity = getRunActivity(Command.MSG_START_RESCAN_DEVICE);
        if (logFile == null) {
            logFile = new LogFile(mPosSdk.getContext());
        } else {
            logFile = LogFile.getinstance();
        }


 //       if(mctx==null){   ((BaseActivity)Setting.getTopContext()).ReadyToastShow(Command.MSG_START_RESCAN_DEVICE);}
    //    else{   ((BaseActivity)Setting.getTopContext()).ReadyToastShow(Command.MSG_START_RESCAN_DEVICE);}
        //mPosSdk.mDevicesList = new ArrayList<>();
        //ReloadDeviceMethod3(_busName);
    }

    /**
     * 재연결 장치 정보 가져오는 함수<br>
     * Queue기반으로 재귀 호출로 장치 정보를 가져옮
     */
    public void ReloadDevicQueueeMethod()
    {
        b_detectSet=false;
        mPosSdk = KocesPosSdk.getInstance();
        mctx = null;
        String topActivity = getRunActivity(Command.MSG_START_RESCAN_DEVICE);
        logFile = LogFile.getinstance();

        Setting.g_AutoDetect = true;
        readCableDevicesRecord();
        if(mctx==null){   ((BaseActivity)Setting.getTopContext()).ReadyToastShow(Setting.getTopContext(),Command.MSG_START_RESCAN_DEVICE,0);}

        ReloadDeviceMethod3();
    }

    /**
     * 로컬에 저장된 유선 디바이스 정보를 읽어 변수에 설정한다.
     */
    private void readCableDevicesRecord()
    {

        //저장 되어 있는 DEVICE정보를 읽고 설정 한다.
        mPosSdk.mDevicesList = new ArrayList<>();
        Devices tempDevice;
        String SelectedCardReader = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER);
        if(!SelectedCardReader.equals(""))
        {
            tempDevice = new Devices(SelectedCardReader,SelectedCardReader,false);
            tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL));
            tempDevice.setmType(1);
            mPosSdk.mDevicesList.add(tempDevice);
        }
        String SelectedMultiReader = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER);
        if(!SelectedMultiReader.equals(""))
        {
            tempDevice = new Devices(SelectedMultiReader,SelectedMultiReader,false);
            tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL));
            tempDevice.setmType(4);
            mPosSdk.mDevicesList.add(tempDevice);
        }
        String SelectedSignPad = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_DEVICE_SIGN_PAD);
        if(!SelectedSignPad.equals(""))
        {
            tempDevice = new Devices(SelectedSignPad,SelectedSignPad,false);
            tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD_SERIAL));
            tempDevice.setmType(2);
            mPosSdk.mDevicesList.add(tempDevice);
        }
        String SelectedMultiPad = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_DEVICE_MULTI_SIGN_PAD);
        if(!SelectedMultiPad.equals(""))
        {
            tempDevice = new Devices(SelectedMultiPad,SelectedMultiPad,false);
            tempDevice.setDeviceSerial(Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL));
            tempDevice.setmType(3);
            mPosSdk.mDevicesList.add(tempDevice);
        }


    }

    /**
     * 현재 어떤 엑티비티에서 실행되고 있는지를 체크하여 해당 엑티비티 위에서 다이얼로그 메세지박스를 불러온다
     * @param _msr
     * @return
     */
    private String getRunActivity(String _msr){
        ActivityManager activity_manager = (ActivityManager) mPosSdk.getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> task_info = activity_manager.getRunningTasks(9999);
        if (task_info.size() == 0) {
            return "";
        }
        String tmp = task_info.get(0).topActivity.getClassName();
        String tmp2[] = tmp.split("\\.");
        String _class = tmp2[3];
        Activity _act = (Activity) Setting.getTopContext();
        if(!_msr.equals("")) {
            switch (_class) {
                case "PaymentActivity":
                    if (_act.getLocalClassName() != "PaymentActivity") {
                        return "";
                    }
                    PaymentActivity act0 = (PaymentActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act0.ReadyDialogShow(act0, _msr, 0);
                    act0.ReadyToastShow(act0,_msr,0);
                    break;
                case "menu2Activity":
                    if (_act.getLocalClassName() != "menu2Activity") {
                        return "";
                    }
                    menu2Activity act1 = (menu2Activity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act1.ReadyDialogShow(act1, _msr, 0);
                    act1.ReadyToastShow(act1,_msr,0);
                    break;
                case "Main2Activity":
                    if (_act.getLocalClassName() != "Main2Activity") {
                        return "";
                    }
                    Main2Activity act2 = (Main2Activity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act2.ReadyDialogShow(act2, _msr, 0);
                    act2.ReadyToastShow(act2,_msr,0);
                    break;
                case "PrefpasswdActivity":
                    if (_act.getLocalClassName() != "PrefpasswdActivity") {
                        return "";
                    }
                    PrefpasswdActivity act3 = (PrefpasswdActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act3.ReadyDialogShow(act3, _msr, 0);
                    act3.ReadyToastShow(act3,_msr,0);
                    break;
                case "CashActivity2":
                    if (_act.getLocalClassName() != "CashActivity2") {
                        return "";
                    }
                    CashActivity2 act4 = (CashActivity2) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act4.ReadyDialogShow(act4, _msr, 0);
                    act4.ReadyToastShow(act4,_msr,0);
                    break;
                case "CashCancelActivity":
                    if (_act.getLocalClassName() != "CashCancelActivity") {
                        return "";
                    }
                    CashCancelActivity act5 = (CashCancelActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act5.ReadyDialogShow(act5, _msr, 0);
                    act5.ReadyToastShow(act5,_msr,0);
                    break;
                case "CashICActivity":
                    if (_act.getLocalClassName() != "CashICActivity") {
                        return "";
                    }
                    CashICActivity act6 = (CashICActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act6.ReadyDialogShow(act6, _msr, 0);
                    act6.ReadyToastShow(act6,_msr,0);
                    break;
                case "CashLoadingActivity":
                    if (_act.getLocalClassName() != "CashLoadingActivity") {
                        return "";
                    }
                    CashLoadingActivity act7 = (CashLoadingActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act7.ReadyDialogShow(act7, _msr, 0);
                    act7.ReadyToastShow(act7,_msr,0);
                    break;
                case "CreditActivity":
                    if (_act.getLocalClassName() != "CreditActivity") {
                        return "";
                    }
                    CreditActivity act8 = (CreditActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act8.ReadyDialogShow(act8, _msr, 0);
                    act8.ReadyToastShow(act8,_msr,0);
                    break;
                case "CreditCancelActivity":
                    if (_act.getLocalClassName() != "CreditCancelActivity") {
                        return "";
                    }
                    CreditCancelActivity act9 = (CreditCancelActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act9.ReadyDialogShow(act9, _msr, 0);
                    act9.ReadyToastShow(act9,_msr,0);
                    break;
                case "CreditLoadingActivity":
                    if (_act.getLocalClassName() != "CreditLoadingActivity") {
                        return "";
                    }
                    CreditLoadingActivity act10 = (CreditLoadingActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act10.ReadyDialogShow(act10, _msr, 0);
                    act10.ReadyToastShow(act10,_msr,0);
                    break;
                case "EasyLoadingActivity":
                    if (_act.getLocalClassName() != "EasyLoadingActivity") {
                        return "";
                    }
                    EasyLoadingActivity act11 = (EasyLoadingActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act11.ReadyDialogShow(act11, _msr, 0);
                    act11.ReadyToastShow(act11,_msr,0);
                    break;
                case "EasyPayActivity":
                    if (_act.getLocalClassName() != "EasyPayActivity") {
                        return "";
                    }
                    EasyPayActivity act12 = (EasyPayActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act12.ReadyDialogShow(act12, _msr, 0);
                    act12.ReadyToastShow(act12,_msr,0);
                    break;
                case "OtherPayActivity":
                    if (_act.getLocalClassName() != "OtherPayActivity") {
                        return "";
                    }
                    OtherPayActivity act13 = (OtherPayActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act13.ReadyDialogShow(act13, _msr, 0);
                    act13.ReadyToastShow(act13,_msr,0);
                    break;
                case "ReceiptCashActivity":
                    if (_act.getLocalClassName() != "ReceiptCashActivity") {
                        return "";
                    }
                    ReceiptCashActivity act14 = (ReceiptCashActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act14.ReadyDialogShow(act14, _msr, 0);
                    act14.ReadyToastShow(act14,_msr,0);
                    break;
                case "ReceiptCreditActivity":
                    if (_act.getLocalClassName() != "ReceiptCreditActivity") {
                        return "";
                    }
                    ReceiptCreditActivity act15 = (ReceiptCreditActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act15.ReadyDialogShow(act15, _msr, 0);
                    act15.ReadyToastShow(act15,_msr,0);
                    break;
                case "ReceiptEasyActivity":
                    if (_act.getLocalClassName() != "ReceiptEasyActivity") {
                        return "";
                    }
                    ReceiptEasyActivity act16 = (ReceiptEasyActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act16.ReadyDialogShow(act16, _msr, 0);
                    act16.ReadyToastShow(act16,_msr,0);
                    break;
                case "SalesInfoActivity":
                    if (_act.getLocalClassName() != "SalesInfoActivity") {
                        return "";
                    }
                    SalesInfoActivity act17 = (SalesInfoActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act17.ReadyDialogShow(act17, _msr, 0);
                    act17.ReadyToastShow(act17,_msr,0);
                    break;
                case "TradeListActivity":
                    if (_act.getLocalClassName() != "TradeListActivity") {
                        return "";
                    }
                    TradeListActivity act18 = (TradeListActivity) Setting.getTopContext();
                    mctx = (BaseActivity) Setting.getTopContext();
                    act18.ReadyDialogShow(act18, _msr, 0);
                    act18.ReadyToastShow(act18,_msr,0);
                    break;
                default:
                    break;
            }
        }
        return tmp2[3];
    }

    /**
     * 현재 사용되지 않음
     */
    public void ReCheckScan()
    {
        if(mDCount==0)  //연결된 장치가 없다면
        {
            Toast.makeText(mPosSdk.getActivity(),Command.MSG_NO_SCAN_DEVICE,Toast.LENGTH_SHORT).show();
            //Setting.setPopupMessage(Command.MSG_NO_SCAN_DEVICE,null,null);
            ReloadFinish2();
        }
        else if(mDCount>0)
        {
            busAddr = mPosSdk.getSerialDevicesBusList();
//            mctx.ReadyDialogShow(mctx, "장치 연결 확인 중입니다",0);
//            Setting.Showpopup(mPosSdk.getActivity(),"장치 연결 중입니다", null, null);
            mCount = 0;
            ReloadDeviceMethod2();
        }
    }

    /**
     * 현재 사용하지 않음
     */
    private void ReCompareScan()
    {
        Setting.g_InitReloadDevice = true;
        int SerialCount = mPosSdk.ConnectedUsbSerialCount();
        if(SerialCount==0)  //연결된 장치가 없다면
        {
            //Toast.makeText(mPosSdk.getActivity(),Command.MSG_NO_SCAN_DEVICE,Toast.LENGTH_SHORT).show();
            //Setting.setPopupMessage(Command.MSG_NO_SCAN_DEVICE,null,null);
            ReloadFinish3();
        }
        else if(SerialCount>0) {
            KIMJAEYONGDEBUG_CODE("Shin 현재 연결 되어 있는 장치 카운트 => " + String.valueOf(SerialCount));
            busAddr = mPosSdk.ConnectedUsbSerialDevicesAddr();
            //kim.jy 2020-07-27
            KIMJAEYONGDEBUG_CODE("장치 카운트 " + String.valueOf(busAddr.length));
            mctx.ReadyDialogShow(mctx, "앱 초기화 작업 중입니다", 0);
//            Setting.Showpopup(mPosSdk.getActivity(),"장치 연결 중입니다", null, null);
            mCount4 = 0;

            ReloadDeviceMethod4();
        }
    }

    /**
     * 환경 설정에서 장치 재검색시 시작 함수
     */
    public void Start()
    {
        if(mDCount==0)  //연결된 장치가 없다면
        {
            Toast.makeText(mPosSdk.getActivity(),Command.MSG_NO_SCAN_DEVICE ,Toast.LENGTH_SHORT).show();
            ReloadFinish();
        }
        else if(mDCount>0)
        {
            busAddr = mPosSdk.getSerialDevicesBusList();
            m_act.ReadyDialogShow(m_act, "장치 연결 확인 중입니다",0);
            ReloadDeviceMethod();
        }
    }

    /**
     * 메인, 앱투앱에서 장치 재검색시 시작 함수
     */
    public void Start2()
    {
        if(mDCount==0)  //연결된 장치가 없다면
        {
            Toast.makeText(mPosSdk.getActivity(),Command.MSG_NO_SCAN_DEVICE ,Toast.LENGTH_SHORT).show();
            ReloadFinish_mainNApptoApp();
        }
        else if(mDCount>0)
        {
            busAddr = mPosSdk.getSerialDevicesBusList();
            m_baseCtx.ReadyToastShow(m_baseCtx,"장치 연결 확인 중입니다",1);
//            m_baseCtx.ReadyDialogShow(m_baseCtx, "장치 연결 확인 중입니다",0);
            ReloadDeviceMethod_mainNApptoApp();
        }
    }

    /** 현재 사용하지 않음 */
    private void ReloadDeviceMethod2()   //재귀 호촐 코드 포함
    {
        TmepAddr = busAddr[mCount];
        logFile.writeLog("Shin mPosSdk.ConnectedUsbSerialAddr " + TmepAddr);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mPosSdk.__PosInfoAutoDetect(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener()
                {
                    @Override
                    public void onReceived(byte[] _rev, int _type)
                    {
                        mCount++;
                        if (_type == Command.PROTOCOL_TIMEOUT) {
                            mctx.ReadyDialogShow(mctx, "장치 연결에 실패 하였습니다.",0);
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                        }
                        else if (_rev[3] ==Command.NAK) {
                            //장비에서 NAK 올라 옮
                            mctx.ReadyDialogShow(mctx, "장치로부터 NAK가 수신 되었습니다." ,0);
//                            Devices tmp1 = new Devices(TmepAddr, TmepAddr, true);
//                            tmp1.setDeviceSerial("NOSERIAL");
//                            //m_ctx.ReadyDialogShow(m_ctx, "장치 시리얼 : " + "NOSERIAL",0);
//                            mPosSdk.mDevicesList.add(tmp1);
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                        }
                        else if(_rev[3] ==Command.ACK)
                        {}
                        else
                        {
                            if(_rev.length>=53) {
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
                                String key = new String(KByteArray.CutToSize(2));
                                Setting.mLineHScrKeyYn = key;

                                //공백을 제거하여 추가 한다.
                                String tmp = authNum.trim();
                                //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다

                                for (Devices n : mPosSdk.mDevicesList) {
                                    if (n.getDeviceSerial().equals(serialNum)) {
                                        if (n.getmAddr().equals(TmepAddr)) {
                                            switch (n.getmType()) {
                                                case 4: //Multi reader
                                                    n.setmAddr(TmepAddr);
                                                    n.setDeviceName(TmepAddr);
                                                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER, TmepAddr);
                                                    break;
                                                case 0:
                                                    break;
                                                case 1: //IC reader
                                                    n.setmAddr(TmepAddr);
                                                    n.setDeviceName(TmepAddr);
                                                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER, TmepAddr);
                                                    break;
                                                case 2: //sign pad
                                                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD, TmepAddr);
                                                    n.setmAddr(TmepAddr);
                                                    n.setDeviceName(TmepAddr);
                                                    break;
                                                case 3: //multi pad
                                                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD, TmepAddr);
                                                    n.setmAddr(TmepAddr);
                                                    n.setDeviceName(TmepAddr);
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        //이 코드는 필요 없어 보임

                        if (mPosSdk.getSerialDevicesBusList().length == mCount)
                        {
                            b_detectSet = true;
                            ReloadFinish2();
                        }
                        else
                        {
                            ReloadDeviceMethod2();
                        }

                    }

                }, TmepAddr);
            }
        },1000);
    }

    /**
     * 현재 사용하지 않음
     */
    private void ReloadDeviceMethod4()   //재귀 호촐 코드 포함
    {
        if (mPosSdk.ConnectedUsbSerialCount() ==  mCount4) {
            mctx.ReadyDialogShow(mctx, "초기 장치 검색이 완료 되었습니다.", 0);
            ReloadFinish4();
            return;
        }

        busAddr = null;
        busAddr = mPosSdk.ConnectedUsbSerialDevicesAddr();
        TmepAddr = busAddr[mCount4];

        mPosSdk.CheckConnectState(TmepAddr);
        //kim.jy 2020-07-27
        KIMJAEYONGDEBUG_CODE("장치 주소 " + TmepAddr);
        //mctx.ReadyDialogShow(mctx, "변경된 디바이스 주소:" + TmepAddr,0);
        KIMJAEYONGDEBUG_CODE("장치 초기화 " + TmepAddr);
        //mPosSdk.__PosInitAutoDetect("99",null,new String[]{TmepAddr});

        //TODO-0728jiw 향후 교체할 것
 //       mctx.ReadyDialogShow(mctx, "장치 초기화 중입니다"+"\n" + "["+TmepAddr+"]",0);
        mctx.ReadyDialogShow(mctx, String.valueOf(mCount4+1) + "번 시리얼 장치 초기화 중입니다",0);

        new Handler().postDelayed(()->{
            mPosSdk.__PosInitAutoDetect("99",null,new String[]{TmepAddr});
        },1000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                KIMJAEYONGDEBUG_CODE("장치 정보 요청" + TmepAddr);

                //TODO-0728jiw 향후 교체할 것
//                mctx.ReadyDialogShow(mctx, "장치 정보 요청 중입니다"+"\n" + "["+TmepAddr+"]",0);
                mctx.ReadyDialogShow(mctx, String.valueOf(mCount4+1) + "번 장치 정보 요청 중입니다",0);

                mPosSdk.__PosInfoAutoDetect(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener()
                {
                    @Override
                    public void onReceived(byte[] _rev, int _type)
                    {
                        //kim.jy 2020-07-27
                        KIMJAEYONGDEBUG_CODE("장치 응답 " + TmepAddr);
                        if (_type == Command.PROTOCOL_TIMEOUT) {
                            mctx.ReadyDialogShow(mctx, "장치 연결에 실패 하였습니다.",0);
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                        }
                        else if (_rev[3] ==Command.NAK) {
                            //장비에서 NAK 올라 옮
                            mctx.ReadyDialogShow(mctx, "장치로부터 NAK가 수신 되었습니다." ,0);
//                            Devices tmp1 = new Devices(TmepAddr, TmepAddr, true);
//                            tmp1.setDeviceSerial("NOSERIAL");
//                            //m_ctx.ReadyDialogShow(m_ctx, "장치 시리얼 : " + "NOSERIAL",0);
//                            mPosSdk.mDevicesList.add(tmp1);
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                        }
                        else if(_rev[3] ==Command.ACK)
                        {}
                        else
                        {
                            if(_rev.length>=53) {
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
                                String key = new String(KByteArray.CutToSize(2));
                                Setting.mLineHScrKeyYn = key;

                                //공백을 제거하여 추가 한다.
                                String tmp = authNum.trim();
                                //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
//                                mctx.ReadyDialogShow(mctx, "장치 정보를 수신 하였습니다.",0);
                                KIMJAEYONGDEBUG_CODE("장치 정보를 수신 :" + TmepAddr);
                                for (Devices n : mPosSdk.mDevicesList) {
                                    if (n.getDeviceSerial().equals(serialNum)) {
                                        //if (n.getmAddr().equals(TmepAddr)) {
                                        switch (n.getmType()) {
                                            case 4: //Multi reader
                                                n.setmAddr(TmepAddr);
                                                n.setDeviceName(TmepAddr);
                                                n.setConnected(true);
                                                mPosSdk.setMultiReaderAddr(TmepAddr);
                                                Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER, TmepAddr);
                                                KIMJAEYONGDEBUG_CODE("SELECTED_DEVICE_MULTI_READER :" + TmepAddr);
                                                break;
                                            case 0:
                                                KIMJAEYONGDEBUG_CODE("장치 설정 안함 :" + TmepAddr);
                                                break;
                                            case 1: //IC reader
                                                n.setmAddr(TmepAddr);
                                                n.setDeviceName(TmepAddr);
                                                n.setConnected(true);
                                                mPosSdk.setICReaderAddr(TmepAddr);
                                                Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER, TmepAddr);
                                                KIMJAEYONGDEBUG_CODE("SELECTED_DEVICE_CARD_READER :" + TmepAddr);
                                                break;
                                            case 2: //sign pad
                                                Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD, TmepAddr);
                                                n.setmAddr(TmepAddr);
                                                n.setDeviceName(TmepAddr);
                                                n.setConnected(true);
                                                mPosSdk.setSignPadAddr(TmepAddr);
                                                KIMJAEYONGDEBUG_CODE("SELECTED_DEVICE_SIGN_PAD :" + TmepAddr);
                                                break;
                                            case 3: //multi pad
                                                Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD, TmepAddr);
                                                n.setmAddr(TmepAddr);
                                                n.setDeviceName(TmepAddr);
                                                n.setConnected(true);
                                                mPosSdk.setMultiPadAddr(TmepAddr);
                                                KIMJAEYONGDEBUG_CODE("SELECTED_DEVICE_MULTI_SIGN_PAD :" + TmepAddr);
                                                break;
                                            default:
                                                break;
                                        }
                                        //}
                                    }
                                }
                            }
                        }
                        //이 코드는 필요 없어 보임
                        mCount4++;

                        ReloadDeviceMethod4();
                    }

                }, TmepAddr);
            }
        },2000);
    }

    /**
     * 환경설정에서 장치 재검색하여 장치 시리얼 정보 업데이트
     */
    private void ReloadDeviceMethod()   //재귀 호촐 코드 포함
    {
        if(mPosSdk.getSerialDevicesBusList() == null || mCount>=mPosSdk.getSerialDevicesBusList().length)
        {
            return;
        }
        TmepAddr = busAddr[mCount];
        mPosSdk.__PosInitAutoDetect("99",null,new String[]{TmepAddr});

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mAutoTimer!= null)
                {
                    mAutoTimer.cancel();
                    mAutoTimer = null;
                }
                mAutoTimer = new AutoDetectTimer(5000,1000, "ReloadDeviceMethod");
                mAutoTimer.cancel();
                mAutoTimer.start();
                mPosSdk.__PosInfoAutoDetect(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener()
                {
                    @Override
                    public void onReceived(byte[] _rev, int _type)
                    {
                        mCount++;
                        if (_type == Command.PROTOCOL_TIMEOUT) {
                            m_act.ReadyDialogShow(m_act, "장치 연결에 실패 하였습니다.",0);
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                        }
                        else if (_rev[3] ==Command.NAK) {
                            //장비에서 NAK 올라 옮
                            m_act.ReadyDialogShow(m_act, "장치로부터 NAK가 수신 되었습니다." ,0);
//                            Devices tmp1 = new Devices(TmepAddr, TmepAddr, true);
//                            tmp1.setDeviceSerial("NOSERIAL");
//                            //m_ctx.ReadyDialogShow(m_ctx, "장치 시리얼 : " + "NOSERIAL",0);
//                            mPosSdk.mDevicesList.add(tmp1);
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                        }
                        else if(_rev[3] ==Command.ACK)
                        {}
                        else
                        {
                            if(_rev.length>=53) {
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
                                String key = new String(KByteArray.CutToSize(2));
                                Setting.mLineHScrKeyYn = key;

                                Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                                //공백을 제거하여 추가 한다.
                                String tmp = authNum.trim();


                                //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
                                Devices tmp1 = new Devices(TmepAddr, TmepAddr, true);
                                tmp1.setDeviceSerial(serialNum);
                                m_act.ReadyDialogShow(m_act, "장치 시리얼 : " + serialNum, 0);
                                mPosSdk.mDevicesList.add(tmp1);
                            }
                        }
                        //이 코드는 필요 없어 보임

                        if (mPosSdk.getSerialDevicesBusList().length == mCount)
                        {
                            b_detectSet = true;
                            ReloadFinish();
                        }
                        else
                        {
                            ReloadDeviceMethod();
                        }

                    }

                }, TmepAddr);
            }
        },1000);

    }

    /**
     * 메인,앱투앱에서 장치 재검색하여 장치 시리얼 정보 업데이트
     */
    private void ReloadDeviceMethod_mainNApptoApp()   //재귀 호촐 코드 포함
    {
        if(mPosSdk.getSerialDevicesBusList() == null || mCount>=mPosSdk.getSerialDevicesBusList().length)
        {
            return;
        }
        TmepAddr = busAddr[mCount];
        mPosSdk.__PosInitAutoDetect("99",null,new String[]{TmepAddr});

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mAutoTimer!= null)
                {
                    mAutoTimer.cancel();
                    mAutoTimer = null;
                }
                mAutoTimer = new AutoDetectTimer(5000,1000, "ReloadDeviceMethod_mainNApptoApp");
                mAutoTimer.cancel();
                mAutoTimer.start();
                mPosSdk.__PosInfoAutoDetect(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener()
                {
                    @Override
                    public void onReceived(byte[] _rev, int _type)
                    {
                        mCount++;
                        if (_type == Command.PROTOCOL_TIMEOUT) {
                            m_baseCtx.ReadyToastShow(m_baseCtx,"장치 연결에 실패 하였습니다.",1);
//                            m_baseCtx.ReadyDialogShow(m_baseCtx, "장치 연결에 실패 하였습니다.",0);
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                        }
                        else if (_rev[3] ==Command.NAK) {
                            //장비에서 NAK 올라 옮
                            m_baseCtx.ReadyToastShow(m_baseCtx,"장치로부터 NAK가 수신 되었습니다.",1);
//                            m_baseCtx.ReadyDialogShow(m_baseCtx, "장치로부터 NAK가 수신 되었습니다." ,0);
//                            Devices tmp1 = new Devices(TmepAddr, TmepAddr, true);
//                            tmp1.setDeviceSerial("NOSERIAL");
//                            //m_ctx.ReadyDialogShow(m_ctx, "장치 시리얼 : " + "NOSERIAL",0);
//                            mPosSdk.mDevicesList.add(tmp1);
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                        }
                        else if(_rev[3] ==Command.ACK)
                        {}
                        else
                        {
                            if(_rev.length>=53) {
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
                                String key = new String(KByteArray.CutToSize(2));
                                Setting.mLineHScrKeyYn = key;

                                Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                                //공백을 제거하여 추가 한다.
                                String tmp = authNum.trim();


                                //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
                                Devices tmp1 = new Devices(TmepAddr, TmepAddr, true);
                                tmp1.setDeviceSerial(serialNum);
                                m_baseCtx.ReadyToastShow(m_baseCtx,"장치 시리얼 : " + serialNum,1);
//                                m_baseCtx.ReadyDialogShow(m_baseCtx, "장치 시리얼 : " + serialNum, 0);
                                mPosSdk.mDevicesList.add(tmp1);
                            }
                        }
                        //이 코드는 필요 없어 보임

                        if (mPosSdk.getSerialDevicesBusList().length == mCount)
                        {
                            b_detectSet = true;
                            ReloadFinish_mainNApptoApp();
                        }
                        else
                        {
                            ReloadDeviceMethod_mainNApptoApp();
                        }

                    }

                }, TmepAddr);
            }
        },1000);

    }

    /**
     * 장치 재 연결시 장치 정보 가져오는 함수
     */
    private void ReloadDeviceMethod3()   //재귀 호촐 코드 포함
    {
        //2020-07-30 kim.jy
        if(Setting.g_InitReloadDevice){return;}
        //2020-07-30 kim.jy
  //      ((BaseActivity)Setting.getTopContext()).ReadyToastShow("장치를 다시 연결 합니다.");
//        if(mctx != null){mctx.ReadyDialogShow(mctx,"장치를 다시 연결 합니다.",0);}
//        else{((BaseActivity)Setting.getTopContext()).ReadyToastShow("장치를 다시 연결 합니다.");}
        String _ready = getRunActivity("장치를 다시 연결 합니다.");
        logFile.writeLog("[SERIAL_DEVICE]" + "장치를 다시 연결 합니다. - ReloadDeviceMethod3()" + "\n");
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                String _busName = Setting.g_WaitingSerialDeviceAddr.poll();
//                if(mctx!=null){mctx.ReadyDialogShow(mctx,"장치 정보를 요청 합니다.",0);}
//                else{((BaseActivity)Setting.getTopContext()).ReadyToastShow("장치 정보를 요청 합니다.");}
                String _ready = getRunActivity("장치 정보를 요청 합니다.");
                logFile.writeLog("[SERIAL_DEVICE]" + "장치 정보를 요청 합니다 - ReloadDeviceMethod3()" + "\n");
//                Toast.makeText(mPosSdk.getActivity(),"장치를 정보를 요청 합니다.",Toast.LENGTH_SHORT).show();
                if(mAutoTimer!= null)
                {
                    mAutoTimer.cancel();
                    mAutoTimer = null;
                }
                mAutoTimer = new AutoDetectTimer(5000,1000,"ReloadDeviceMethod3");
                logFile.writeLog("[SERIAL_DEVICE]" + "mAutoTimer 타이머생성 - ReloadDeviceMethod3()" + "\n");
                mAutoTimer.cancel();
                mAutoTimer.start();
                logFile.writeLog("[SERIAL_DEVICE]" + "mAutoTimer 타이머시작 - ReloadDeviceMethod3()" + "\n");
                mPosSdk.__PosInfoAutoDetect(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener()
                {
                    @Override
                    public void onReceived(byte[] _rev, int _type)
                    {
                        if (_type == Command.PROTOCOL_TIMEOUT) {
                            logFile.writeLog("[SERIAL_DEVICE]" + "PROTOCOL_TIMEOUT - ReloadDeviceMethod3()" + "\n");
                            if(mctx != null) {
//                                mctx.ReadyDialogShow(mctx, "장치 연결에 실패 하였습니다.", 0);
                            }
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                        }
                        else if (_rev[3] ==Command.NAK) {
                            //장비에서 NAK 올라 옮
                            if(mctx != null) {
//                                mctx.ReadyDialogShow(mctx, "장치로부터 NAK가 수신 되었습니다.", 0);
                            }
//                            Devices tmp1 = new Devices(TmepAddr, TmepAddr, true);
//                            tmp1.setDeviceSerial("NOSERIAL");
//                            //m_ctx.ReadyDialogShow(m_ctx, "장치 시리얼 : " + "NOSERIAL",0);
//                            mPosSdk.mDevicesList.add(tmp1);
//                    Setting.Showpopup(mPosSdk.getActivity(),"장치 연결에 실패 하였습니다.", null, null);
                            logFile.writeLog("[SERIAL_DEVICE]" + "Command.NAK - ReloadDeviceMethod3()" + "\n");
                        }
                        else if(_rev[3] ==Command.ACK)
                        {}
                        else
                        {
                            if(_rev.length>=53) {
                                //2020-07-30 kim.jy
//                                if(mctx!=null){mctx.ReadyDialogShow(mctx,"장치 정보를 수신 하였습니다",0);}
//                                else{((BaseActivity)mPosSdk.getActivity()).ReadyToastShow("장치 정보를 수신 하였습니다");}
                                logFile.writeLog("[SERIAL_DEVICE]" + "장치 정보를 수신 하였습니다 - ReloadDeviceMethod3()"+ "\n" +
                                        Utils.bytesToHex_0xType(_rev) + "\n");
                                String _ready = getRunActivity("장치 정보를 수신 하였습니다");
//                                Toast.makeText(mPosSdk.getActivity(),"장치 정보를 수신 하였습니다",Toast.LENGTH_SHORT).show();
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
                                String key = new String(KByteArray.CutToSize(2));
                                Setting.mLineHScrKeyYn = key;

                                //공백을 제거하여 추가 한다.
                                String tmp = authNum.trim();
                                //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
                                logFile.writeLog("[SERIAL_DEVICE]" + "정보파싱 성공 - ReloadDeviceMethod3()" + "\n");
                                for (Devices n : mPosSdk.mDevicesList) {
                                    if (n.getDeviceSerial().equals(serialNum)) {
                                        //if(n.getmAddr().equals(_busName)) {
                                        switch (n.getmType()) {
                                            case 0:
                                                break;
                                            case 4: //Multi reader
                                                n.setmAddr(_busName);
                                                n.setName(authNum);
                                                n.setVersion(version);
                                                n.setDeviceName(_busName);
                                                n.setConnected(true);
                                                Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER, _busName);
                                                mPosSdk.setMultiReaderAddr(_busName);
                                                tmp="";
                                                Setting.mAuthNum = authNum.trim();
                                                break;
                                            case 1: //IC reader
                                                n.setmAddr(_busName);
                                                n.setName(authNum);
                                                n.setVersion(version);
                                                n.setDeviceName(_busName);
                                                n.setConnected(true);
                                                Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER, _busName);
                                                mPosSdk.setICReaderAddr(_busName);
                                                tmp="";
                                                Setting.mAuthNum = authNum.trim();
                                                break;
                                            case 2: //sign pad
                                                Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD, _busName);
                                                n.setmAddr(_busName);
                                                n.setDeviceName(_busName);
                                                n.setConnected(true);
                                                mPosSdk.setSignPadAddr(_busName);
                                                break;
                                            case 3: //multi pad
                                                Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD, _busName);
                                                n.setmAddr(_busName);
                                                n.setDeviceName(_busName);
                                                n.setConnected(true);
                                                mPosSdk.setMultiPadAddr(_busName);
                                                break;
                                            default:
                                                break;
                                        }
                                        //}
                                    }
                                }
                                if(!tmp.equals(""))
                                {
                                    mAuth = new String(Setting.mAuthNum);
                                    Setting.mAuthNum = "";
                                }
                            }
                        }
                        //이 코드는 필요 없어 보임
                        if(Setting.g_WaitingSerialDeviceAddr.isEmpty())
                        {
                            logFile.writeLog("[SERIAL_DEVICE]" + "ReloadFinish2 이동 - ReloadDeviceMethod3()" + "\n");
                            Setting.g_WaitingSerialDeviceAddr.clear();
                            ReloadFinish2();
                        }
                        else
                        {
                            logFile.writeLog("[SERIAL_DEVICE]" + "ReloadDeviceMethod3 재귀함수실행 - ReloadDeviceMethod3()" + "\n");
                            ReloadDeviceMethod3();
                        }

                    }
                }, _busName);
            }
        },1500);
    }

    /**
     * 현재 연결된 장치 카운트를 mDCount에 설정하는 함수
     */
    private void CheckDeviceCount()
    {
        mDCount = mPosSdk.getUsbDevice().size();
    }

    /**
     * 장치 재검색 완료 여부를 체크하는 함수
     */
    public synchronized boolean CheckDetectComplete()
    {
        return b_detectSet;
    }

    /**
     * 환경설정에서 장치 재검색을 완료한다
     */
    private void ReloadFinish()
    {
        if(mAutoTimer!= null)
        {
            mAutoTimer.cancel();
            mAutoTimer = null;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                m_act.ReadyDialogHide();
                mDataListener.onReceived(CheckDetectComplete(),mDCount, mDCount==0? "연결된 장치 없음" : "장치 인식 완료");
            }
        },1000);

    }
    /**
     * 메인,앱투앱에서 장치 재검색을 완료한다
     */
    private void ReloadFinish_mainNApptoApp()
    {
        if(mAutoTimer!= null)
        {
            mAutoTimer.cancel();
            mAutoTimer = null;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                m_baseCtx.ReadyDialogHide();
                mDataListener.onReceived(CheckDetectComplete(),mDCount, mDCount==0? "연결된 장치 없음" : "장치 인식 완료");
            }
        },1000);

    }

    /**
     * 현재 사용하지 않음
     */
    private void ReloadFinish4()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                if(mctx!=null) {
//                    mctx.ReadyDialogHide();
//                }
                mDataListener.onReceived(true,mCount,"초기화 장치 인식 완료");
                Setting.g_InitReloadDevice = false;
            }
        },1000);

    }

    /**
     * 현재 사용하지 않음
     */
    private void ReloadFinish3()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                if(mctx!=null) {
//                    mctx.ReadyDialogHide();
//                }
                mDataListener.onReceived(false,mCount,"장치 인식 실패. 다시 시도해 주세요");
            }
        },1000);

    }

    /**
     * 장치 재 연결을 완료하는 함수
     */
    private void ReloadFinish2()
    {
        if(mAutoTimer!= null)
        {
            mAutoTimer.cancel();
            mAutoTimer = null;
        }
        if(Setting.g_WaitingSerialDeviceAddr.isEmpty()) {
            Setting.g_WaitingSerialDeviceAddr.clear();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mctx != null) {
                    mctx.ReadyDialogHide();
//                    mctx.ReadyToastShow("장치 인식 완료");
//                    Toast.makeText(mPosSdk.getActivity(),"장치 초기화 완료",Toast.LENGTH_SHORT).show();
                    String topActivity = getRunActivity("");
                    switch (topActivity)
                    {
                        case "Main2Activity":
                            Main2Activity act2 = (Main2Activity)Setting.getTopContext();
                            mctx = (BaseActivity)Setting.getTopContext();
                            act2.MainOnForeground();
                            break;
                        default:
                            break;
                    }
                }
                else
                {
                    ((BaseActivity)Setting.getTopContext()).ReadyDialogHide();
                }
                Setting.g_AutoDetect = false;
                Setting.g_bMainIntegrity = true;
            }
        },1500);

    }

    /**
     * 특정장비(테블렛)에서 장치 연결 시 여러 상황들이 발생하여 이를 로그파일로 확인하기 위한 함수(향후 제거 필요)
     * @param _str
     */
    private void KIMJAEYONGDEBUG_CODE(String _str)
    {
        //kim.jy 2020-07-27
        //KIMJYDEBUG_STRING += _str + "\n";
        //Toast.makeText(mPosSdk.getActivity(),KIMJYDEBUG_STRING,Toast.LENGTH_LONG).show();
        logFile.writeLog("[SERIAL_DEVICE]" + _str + "\n");
    }

    class AutoDetectTimer extends CountDownTimer
    {
        String mMethod = "";
        public AutoDetectTimer(long millisInFuture, long countDownInterval, String _method)
        {
            super(millisInFuture, countDownInterval);
            mMethod = _method;
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if (mMethod.equals("ReloadDeviceMethod"))
            {
                if(mCount<mPosSdk.getSerialDevicesBusList().length-1)
                {
                    mCount++;
                    ReloadDeviceMethod();
                }
                else
                {
                    ReloadFinish();
                }
            } else if (mMethod.equals("ReloadDeviceMethod_mainNApptoApp"))
            {
                if(mCount<mPosSdk.getSerialDevicesBusList().length-1)
                {
                    mCount++;
                    ReloadDeviceMethod_mainNApptoApp();
                }
                else
                {
                    ReloadFinish_mainNApptoApp();
                }
            }
            else if (mMethod.equals("ReloadDeviceMethod3"))
            {
                if(Setting.g_WaitingSerialDeviceAddr.isEmpty())
                {
                    Setting.g_WaitingSerialDeviceAddr.clear();
                    ReloadFinish2();
                }
                else
                {
                    ReloadDeviceMethod3();
                }
            }
        }
    }
}

