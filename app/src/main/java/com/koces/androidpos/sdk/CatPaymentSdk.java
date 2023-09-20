package com.koces.androidpos.sdk;

import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.koces.androidpos.AppToAppActivity;
import com.koces.androidpos.BaseActivity;
import com.koces.androidpos.CashLoadingActivity;
import com.koces.androidpos.CreditLoadingActivity;
import com.koces.androidpos.Main2Activity;
import com.koces.androidpos.ReceiptCashActivity;
import com.koces.androidpos.ReceiptCreditActivity;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.db.sqliteDbSdk;
import com.koces.androidpos.sdk.van.CatNetworkInterface;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.KocesTcpClient;
import com.koces.androidpos.sdk.van.NetworkInterface;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class CatPaymentSdk {

    private static CatPaymentSdk Instance;

    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 결제관련 기능을 수행하기 위해 사용한다 */
    private KocesPosSdk mPosSdk;
    /* 결제관련SDK를 수행하는 위치 Context */
    private Context mCtx;
    /** 신용/현금/현금IC 구분자 */
    private Constants.CatPayType TradeType = Constants.CatPayType.Cash;
    /** 거래금액 */
    private String Money;
    /** 할부개월 */
    private String InstallMent;
    /** 세금 */
    private String Tax;
    /** 봉사료 */
    private String Svc;
    /** 면세료 */
    private String Txf;
    /** 개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천 */
    private String pb;
    /** tid */
    private String Tid;
    private String mStoreName;
    private String mStoreAddr;
    private String mStoreNumber;
    private String mStorePhone;
    private String mStoreOwner;
    /** 원승인날짜 */
    private String AuDate;
    /** 원승인번호 */
    private String AuNo;
    /** 원서브승인번호(간편결제) */
    private String SubAuNo;
    /** 코세스고유번호 */
    private String UniqueNumber;
    /** 취소인지아닌지 */
    private boolean Cancel = false;
    /** 가맹점데이터 */
    private String MchData;
    /** 여유필드 */
    private String ExtraField;
    /** 앱카드인지체크 */
    private String QrBarcode;
    /** 취소사유 */
    private String CancelReason;
    /** 현금IC 업무구분자 */
    private Constants.CashICBusinessClassification Class = Constants.CashICBusinessClassification.Search;
    /** 간소화거래여부 */
    private String DirectTrade;
    /** 카드정보수록여부 */
    private String CardInfo;
    /** 원승인날짜(거래 후 받은 원승인날짜로 이 값을 db로 보낸다) */
    private String OriAuDate;
    /** 원승인번호(거래 후 받은 원승인번호로 이 값을 db로 보낸다 */
    private String OriAuNo;
    /** 현재 화면이 앱투앱인지 아닌지를 체크 DB 저장을 위해서 */
    private boolean mDBAppToApp = false;
    /** ApptoAppActivity 또는 PaymentActivity 로 데이터를 보내기 위한 리스너 */
    private SerialInterface.CatPaymentListener mCatPaymentListener;
    /** 앱투앱으로 넘기려는 데이터 */
    private HashMap<String,String> mRecv;
    /** 간편결제 승인,취소,제로페이취소조회 */
    private String TrdType;
    /** 간편결제 거래종류 카카오,제로,위쳇,앱카드 등 */
    private String EasyKind;
    /** 호스트가맹점데이터(간편결제) */
    private String HostMchData;

    /**
     * CatPaymentSdk 생성
     * @param _tradeType 신용/현금/현금IC
     * @param _CatPaymentListener 리스너
     */
    public CatPaymentSdk(Context _ctx, Constants.CatPayType _tradeType, SerialInterface.CatPaymentListener _CatPaymentListener)
    {
        Clear();
        Instance = this;
        mCtx = _ctx;
        TradeType = _tradeType;
        mPosSdk = KocesPosSdk.getInstance();
        mCatPaymentListener = _CatPaymentListener;
        mPosSdk.CatConnect(Setting.getCatIPAddress(mCtx),Integer.parseInt(Setting.getCatVanPORT(mCtx)),mCatNetworkConnectListener);

    }

    public static CatPaymentSdk getInstance() {
        if (Instance != null) {
            return Instance;
        }
        return null;
    }

    /** 사용되는 변수들 초기화 */
    public void Clear()
    {
        Tid = "";
        Money = "";
        Tax = "";
        Svc = "";
        Txf = "";
        AuDate = "";
        AuNo = "";
        UniqueNumber = "";
        InstallMent = "";
        Cancel = false;
        MchData = "";
        ExtraField = "";
        QrBarcode = "";
        pb = "";
        mDBAppToApp = false;
        CancelReason = "";
        Class = Constants.CashICBusinessClassification.Search;
        DirectTrade = "";
        CardInfo = "";
        OriAuDate = "";
        OriAuNo = "";
//        mPosSdk.mTcpClient.ParsingData = new byte[]{};
//        mCatTcpClient.DisConnectServer();

        mStoreName = "";
        mStoreAddr = "";
        mStoreNumber = "";
        mStorePhone = "";
        mStoreOwner = "";
    }

    private CatNetworkInterface.ConnectListener mCatNetworkConnectListener = new CatNetworkInterface.ConnectListener() {
        @Override
        public void onState(int _internetState,boolean _bState,String _EventMsg) {
            if(mDBAppToApp){ ((BaseActivity)mCtx).ReadyDialogHide(); }
            mPosSdk.mTcpClient.DisConnectCatServer();
            if (_bState) {
                if (_EventMsg == "Credit") {
                    mCatPaymentListener.result("COMPLETE_IC","COMPLETE_IC",mRecv);
                } else if (_EventMsg == "CashIC") {
                    mCatPaymentListener.result("COMPLETE","COMPLETE",mRecv);    //이부분은 어떻게 변경될지 알 수 없다.
                } else if (_EventMsg == "CashRecipt") {
                    mCatPaymentListener.result("COMPLETE","COMPLETE",mRecv);
                } else if (_EventMsg == "Print") {
                    mCatPaymentListener.result("COMPLETE_PRINT","COMPLETE_PRINT",mRecv);
                } else if (_EventMsg == "Easy") {
                    mCatPaymentListener.result("COMPLETE_IC","COMPLETE_IC",mRecv);
                }

            } else {
                mCatPaymentListener.result(_EventMsg,"ERROR",new HashMap<>());
            }

            //mRecv 를 어디로 넘길거냐
            Clear();
            return;

        }
    };

    private void ShowMessageBox(String _msg, int _count) {
        /** 아래 부분 다른 커스텀메세지박스를 만들어서 사용한다. 아래꺼로하기에는 분기를 해줘야 할 게 많다. */
        if(mDBAppToApp) {
            Setting.setIsAppToApp(true);
            ((AppToAppActivity) mCtx).ReadyDialogHide();
            ((BaseActivity) mCtx).ReadyDialogShow(mCtx,
                    _msg,
                    _count, true);
        }
        else
        {
            Setting.setIsAppToApp(false);
            if (mCtx instanceof CreditLoadingActivity)
            {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        ((CreditLoadingActivity) mCtx).mTvwMessage.setText(_msg);
                        ((CreditLoadingActivity) mCtx).setMCount(Integer.valueOf(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
                    }
                });

            }
            else if (mCtx instanceof CashLoadingActivity)
            {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        ((CashLoadingActivity) mCtx).mTvwMessage.setText(_msg);
                        ((CashLoadingActivity) mCtx).setMCount(Integer.valueOf(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
                    }
                });

            }

        }


    }

    //정상적으로 데이터를 받아서 캣단말기에 수신완료메세지를 보낼 때, G120 G150 등에 따른 응답인 G125 G155 로 보내도록 추출
    private String ResponseCommand(byte[] _Command) {
        String _cmd = "";
        try {
            _cmd = Utils.getByteToString_euc_kr(_Command);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        switch (_cmd) {
            case "G120":
                _cmd = "G125";
                break;
            case "G130":
                _cmd = "G135";
                break;
            case "G140":
                _cmd = "G145";
                break;
            case "T180":
                _cmd = "T185";
                break;
            case "G160":
                _cmd = "G165";
                break;
            case "G170":
                _cmd = "G175";
                break;
            default:
                break;
        }

        return _cmd;
    }

    private void FlowCatEasy1(){


        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                if (_recv[0] != Command.ACK) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK) 수신에 실패했습니다.");
                    return;
                }
                FlowCatEasy2();
            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });

    }

    private void FlowCatEasy2() {
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 거래종료");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                String str1 = "";
                try {
                    str1 = new String(_recv, "euc-kr");
                    //stringValue = new String(temp);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (str1.contains("A110")) {
                    byte[] ack = {Command.ACK, Command.ACK}; // TR Command가 오면 ACK 2개를 보낸다.
                    mPosSdk.mTcpClient.TcpSend(ack,new KocesTcpClient.CatTcpListener() {

                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                return;
                            }
                            FlowCatEasy3();
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });
                    return;
                } else if (_recv[1] == (byte)0x51) { //Q
                    mCatNetworkConnectListener.onState(0, false, "거래 가능한 TID 가 아닙니다");
                    return;
                }  else if (_recv[1] == (byte)0x45) { //E
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래 취소");
                    return;
                } else {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래에 실패했습니다");
                    return;
                }

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatEasy3() {
        ShowMessageBox("CAT단말기에서 서버에 요청 중입니다. 잠시만 기다려주세요",Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv[1] == (byte)0x54) {
                    byte[] last = new byte[_recv.length];
                    System.arraycopy(_recv, 0, last, 0, _recv.length);
                    KByteArray responseData = new KByteArray(last);
                    responseData.CutToSize(1);  //stx
                    byte[] stringcomand = responseData.CutToSize(4);
                    responseData.CutToSize(92);
                    byte[] resCode = responseData.CutToSize(4);
                    String anscode;
                    String ansmsg;
                    try {
                        anscode = Utils.getByteToString_euc_kr(resCode);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    byte[] msg = responseData.CutToSize(40);
                    try {
                        ansmsg = Utils.getByteToString_euc_kr(msg);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    byte[] finish = Command.Cat_ResponseTrade(ResponseCommand(stringcomand), new byte[]{0x30,0x30,0x30,0x30}, "정상수신");
                   Log.d("CAT_EASY",Utils.bytesToHex_0xType(finish));

                    responseData.Clear();
                    mPosSdk.mTcpClient.TcpSend(finish,new KocesTcpClient.CatTcpListener() {
                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(완료) 전송에 실패했습니다.");
                                return;
                            }
                            FlowCatEasy4(last);
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });

                } else if (_recv[1] == Command.NAK) {
                    //Nak 올라오면 재전송1회 시도
                    byte[] ack = {Command.ACK, Command.ACK}; // TR Command가 오면 ACK 2개를 보낸다.
                    mPosSdk.mTcpClient.TcpSend(ack,new KocesTcpClient.CatTcpListener() {

                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                return;
                            }

                            mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

                                @Override
                                public void onSendResult(boolean _result) {

                                }

                                @Override
                                public void onReceiveResult(boolean _result, byte[] _recv) {
                                    if (_recv == null) {
                                        mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                                        return;
                                    }
                                    if (_recv.length <= 0) {
                                        mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                                        return;
                                    }

                                    if (_recv[1] == (byte)0x54) {
                                        byte[] last = _recv;
                                        KByteArray responseData = new KByteArray(last);
                                        responseData.CutToSize(1);  //stx
                                        byte[] stringcomand = responseData.CutToSize(4);
                                        responseData.CutToSize(92);
                                        byte[] resCode = responseData.CutToSize(4);
                                        byte[] msg = responseData.CutToSize(40);
                                        byte[] finish = Command.Cat_ResponseTrade(ResponseCommand(stringcomand), new byte[]{0x30,0x30,0x30,0x30}, "정상수신");
                                        responseData.Clear();
                                        mPosSdk.mTcpClient.TcpSend(finish,new KocesTcpClient.CatTcpListener() {
                                            @Override
                                            public void onSendResult(boolean _result) {
                                                if(!_result){
                                                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(완료) 전송에 실패했습니다.");
                                                    return;
                                                }

                                                FlowCatEasy4(last);
                                            }

                                            @Override
                                            public void onReceiveResult(boolean _result, byte[] _recv) {

                                            }

                                            @Override
                                            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                                            }
                                        });

                                    }
                                }

                                @Override
                                public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                                }
                            });
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });




                }
            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatEasy4(byte[] last){

        mPosSdk.mTcpClient.TcpLastRead(last, new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                if (_recv[0] != Command.EOT)
                {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(EOT) 수신에 실패했습니다.");
                    return;
                }
                KByteArray responseData = new KByteArray(_resultData);
                responseData.CutToSize(1);  //stx
                byte[] stringcomand = responseData.CutToSize(4);    //거래구분(T180)
                responseData.CutToSize(4);  //길이
                String _all = "";
                try {
                    _all = new String(responseData.value(), "euc-kr");//전체
                    //stringValue = new String(temp);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String[] splited = _all.split(";");
                String resCode = splited[11].substring(4);
                String msg = splited[12].substring(4);
//                responseData.CutToSize(92); //
//                byte[] resCode = responseData.CutToSize(4);
//                byte[] msg = responseData.CutToSize(40);



                if (!resCode.equals("0000")) {
                    String Message = "";
                    String AnsCode = "";
                    Message = msg;
                    AnsCode = resCode;

                    mCatNetworkConnectListener.onState(0, false, AnsCode + "\n" + Message);
                    return;
                }

                HashMap<String,String> pData = ParsingEasyresData(_resultData, true);

                if (pData == null) {
                    mCatNetworkConnectListener.onState(0, false, "데이터 파싱이 잘봇되었습니다.");
                    return;
                }

                if(pData.get("AuNo") == null || pData.get("AuNo").equals(""))
                {
                    mCatNetworkConnectListener.onState(0, false, pData.get("AnsCode") + "\n" + pData.get("Message"));
                    return;
                }

                InsertDBTradeData_TypeEasy(pData,"Easy");


            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {

            }


        });





//            let finish = Command.Cat_ResponseTrade(Command: Command.CMD_CAT_CREDIT_RES, Code: resCode, Message: msg)




        //만일 apptoapp 이면 여기서 db 저장으로 가지 않고 델리게이트로 보낸다. 본앱이면 db로 간다
//            if apptoapp == true {
//                InsertDBTradeData_TypeCrdit(수신데이터: pData)
//            } else {
//                Result.onResult(CatState: 0, ResultData: recv)
//            }

    }

    public void EasyRecipt(String _trdType,String _tid,String _qr,String _money, String _tax,String _svc,String _txf,
                           String _easyKind,String _AuDate, String _AuNo, String _subAuNo,String _InstallMent,
                           String _MchData,String _hostMchData,String _UniqueNumber, boolean _appToapp,
                           String _storeName, String _storeAddr, String _storeNumber, String _storePhone, String _storeOwner)
    {
        Clear();
        CatPaymentSdk.Instance.TrdType = _trdType;
        CatPaymentSdk.Instance.QrBarcode = _qr;
        if(_trdType.equals("A10") || _trdType.equals("E10"))
        {
            CatPaymentSdk.Instance.TrdType = "A10";
        }
        else if(_trdType.equals("A20") || _trdType.equals("E20"))
        {
            CatPaymentSdk.Instance.TrdType = "A20";
        }
        else
        {
            CatPaymentSdk.Instance.TrdType = "Z30";
        }
        if (_trdType.equals("A20") || _trdType.equals("E20")) {
            CatPaymentSdk.Instance.Money =
                    String.valueOf(Integer.parseInt(_money) +
                            Integer.parseInt(_tax) +
                            Integer.parseInt(_svc) +
                            Integer.parseInt(_txf));
            CatPaymentSdk.Instance.Tax = "0";
            CatPaymentSdk.Instance.Svc = "0";
            CatPaymentSdk.Instance.Txf = "0";
            CatPaymentSdk.Instance.Cancel = true;
        } else {
            CatPaymentSdk.Instance.Money = _money;
            CatPaymentSdk.Instance.Tax = _tax;
            CatPaymentSdk.Instance.Svc = _svc;
            CatPaymentSdk.Instance.Txf = _txf;
            CatPaymentSdk.Instance.Cancel = false;
        }
        CatPaymentSdk.Instance.EasyKind = _easyKind;

        String _audate = _AuDate;
        if (_audate.length() >= 8 && _audate.substring(0,2) == "20") {
            _audate = _audate.substring(2,8);
        }
        if (_audate.length() >= 6) {
            _audate = _audate.substring(0,6);
        }
        CatPaymentSdk.Instance.AuDate = _audate;
        CatPaymentSdk.Instance.AuNo = _AuNo;
        CatPaymentSdk.Instance.SubAuNo = _subAuNo;
        CatPaymentSdk.Instance.InstallMent = _InstallMent;
        CatPaymentSdk.Instance.MchData = _MchData;
        CatPaymentSdk.Instance.HostMchData = _hostMchData;
        CatPaymentSdk.Instance.UniqueNumber = _UniqueNumber;

        CatPaymentSdk.Instance.mDBAppToApp = _appToapp;
        CatPaymentSdk.Instance.mStoreName = _storeName;
        CatPaymentSdk.Instance.mStoreAddr = _storeAddr;
        CatPaymentSdk.Instance.mStoreNumber = _storeNumber;
        CatPaymentSdk.Instance.mStorePhone = _storePhone;
        CatPaymentSdk.Instance.mStoreOwner = _storeOwner;

//        if ((_qr.replace(" ","")).equals(""))
//        {
//            mCatNetworkConnectListener.onState(0, false, "올바른 QR 데이터가 아닙니다");
//            return;
//        }

//        if (Scan_Data_Parser(_qr).equals(Constants.EasyPayMethod.App_Card.toString()) ||
//                Scan_Data_Parser(_qr).equals(Constants.EasyPayMethod.EMV.toString()))
//        {
//
//        }
//        else
//        {
//            mCatNetworkConnectListener.onState(0, false, "해당 BARCODE/QR 은 지원하지 않는 번호입니다.(앱카드만 사용가능)");
//            return;
//        }

        /** log : PayCredit */
//        LogFile.instance.InsertLog("CAT 신용결제 App -> CAT", Tid: _tid == "" ? Setting.shared.getDefaultUserData(_key: define.STORE_TID):_tid, TimeStamp: true)

        /** 아래 부분 다른 커스텀메세지박스를 만들어서 사용한다. 아래꺼로하기에는 분기를 해줘야 할 게 많다. */
        ShowMessageBox("CAT단말기에 거래 요청 중 입니다", Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
//        ((BaseActivity)mCtx).ReadyDialogShow(mCtx,
//                "CAT단말기에서 카드를 읽어주세요",
//                60,true);
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (mPosSdk.mTcpClient.ConnectServer() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 IP/PORT 설정이 잘못되었습니다.");
                    return;
                }
                if (mPosSdk.mTcpClient.CheckBeforeTrade() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 연결에 실패했습니다.");
                    return;
                }

                byte[] SendData = {};
                try {
                    SendData = Command.Cat_CreditAppCard(CatPaymentSdk.Instance.TrdType,CatPaymentSdk.Instance.Tid,
                            CatPaymentSdk.Instance.QrBarcode,CatPaymentSdk.Instance.Money,CatPaymentSdk.Instance.Tax,
                            CatPaymentSdk.Instance.Svc,CatPaymentSdk.Instance.Txf,CatPaymentSdk.Instance.EasyKind,
                            CatPaymentSdk.Instance.AuDate,CatPaymentSdk.Instance.AuNo,CatPaymentSdk.Instance.SubAuNo,
                            CatPaymentSdk.Instance.InstallMent,CatPaymentSdk.Instance.MchData,CatPaymentSdk.Instance.HostMchData,
                            CatPaymentSdk.Instance.UniqueNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mPosSdk.mTcpClient.TcpSend(SendData, new KocesTcpClient.CatTcpListener() {
                    @Override
                    public void onSendResult(boolean _result) {
                        if (!_result) {
                            mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(전문) 전송에 실패했습니다.");
                            return;
                        }
                        FlowCatEasy1();
                    }

                    @Override
                    public void onReceiveResult(boolean _result, byte[] _recv) {

                    }

                    @Override
                    public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                    }
                });

                return;

            }
        }).start();
    }

    private void FlowCatCredit1(){


        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                if (_recv[0] != Command.ACK) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK) 수신에 실패했습니다.");
                    return;
                }
                FlowCatCredit2();
            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });

    }

    private void FlowCatCredit2() {
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래종료");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                String str1 = "";
                try {
                    str1 = new String(_recv, "euc-kr");
                    //stringValue = new String(temp);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (str1.contains("A110")) {
                    byte[] ack = {Command.ACK, Command.ACK}; // TR Command가 오면 ACK 2개를 보낸다.
                    mPosSdk.mTcpClient.TcpSend(ack,new KocesTcpClient.CatTcpListener() {

                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                return;
                            }
                            FlowCatCredit3();
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });
                    return;
                } else if (_recv[1] == (byte)0x44) { //D
//                            Utils.CatAnimationViewInit(Message:"단말기에서 카드를 읽어주세요", Listener:

                    ShowMessageBox("단말기에서 카드를 읽어주세요", Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
                    CreditFallBack();
                    return;
                } else if (_recv[1] == (byte)0x46) { //F

                    if (!mDBAppToApp) {

                        if (Setting.getPreference(mCtx,Constants.FALLBACK_USE).equals("1")) {
                            Cat_SendCancelCommandE(false);
                            mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래 취소");
                            return;
                        }
                    }

                    ShowMessageBox("단말기에서 MSR을 읽어주세요", Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
                    CreditFallBack();
                    return;
                } else if (_recv[1] == (byte)0x51) { //Q
                    mCatNetworkConnectListener.onState(0, false, "거래 가능한 TID 가 아닙니다");
                    return;
                }  else if (_recv[1] == (byte)0x45) { //E
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래 취소");
                    return;
                } else {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래에 실패했습니다");
                    return;
                }

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatCredit3() {
        ShowMessageBox("CAT단말기에서 서버에 요청 중입니다. 잠시만 기다려주세요",Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv[1] == (byte)0x47) {
                    byte[] last = new byte[_recv.length];
                    System.arraycopy(_recv, 0, last, 0, _recv.length);
                    KByteArray responseData = new KByteArray(last);
                    responseData.CutToSize(1);  //stx
                    byte[] stringcomand = responseData.CutToSize(4);
                    responseData.CutToSize(92);
                    byte[] resCode = responseData.CutToSize(4);
                    byte[] msg = responseData.CutToSize(40);
                    byte[] finish = Command.Cat_ResponseTrade(ResponseCommand(stringcomand), new byte[]{0x30,0x30,0x30,0x30}, "정상수신");
                    responseData.Clear();
                    mPosSdk.mTcpClient.TcpSend(finish,new KocesTcpClient.CatTcpListener() {
                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(완료) 전송에 실패했습니다.");
                                return;
                            }
                            FlowCatCredit4(last);
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });

                } else if (_recv[1] == Command.NAK) {
                    //Nak 올라오면 재전송1회 시도
                    byte[] ack = {Command.ACK, Command.ACK}; // TR Command가 오면 ACK 2개를 보낸다.
                    mPosSdk.mTcpClient.TcpSend(ack,new KocesTcpClient.CatTcpListener() {

                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                return;
                            }

                            mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

                                @Override
                                public void onSendResult(boolean _result) {

                                }

                                @Override
                                public void onReceiveResult(boolean _result, byte[] _recv) {
                                    if (_recv == null) {
                                        mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                                        return;
                                    }
                                    if (_recv.length <= 0) {
                                        mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                                        return;
                                    }

                                    if (_recv[1] == (byte)0x47) {
                                        byte[] last = _recv;
                                        KByteArray responseData = new KByteArray(last);
                                        responseData.CutToSize(1);  //stx
                                        byte[] stringcomand = responseData.CutToSize(4);
                                        responseData.CutToSize(92);
                                        byte[] resCode = responseData.CutToSize(4);
                                        byte[] msg = responseData.CutToSize(40);
                                        byte[] finish = Command.Cat_ResponseTrade(ResponseCommand(stringcomand), new byte[]{0x30,0x30,0x30,0x30}, "정상수신");
                                        responseData.Clear();
                                        mPosSdk.mTcpClient.TcpSend(finish,new KocesTcpClient.CatTcpListener() {
                                            @Override
                                            public void onSendResult(boolean _result) {
                                                if(!_result){
                                                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(완료) 전송에 실패했습니다.");
                                                    return;
                                                }

                                                FlowCatCredit4(last);
                                            }

                                            @Override
                                            public void onReceiveResult(boolean _result, byte[] _recv) {

                                            }

                                            @Override
                                            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                                            }
                                        });

                                    }
                                }

                                @Override
                                public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                                }
                            });
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });




                }
            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatCredit4(byte[] last){

        mPosSdk.mTcpClient.TcpLastRead(last, new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                if (_recv[0] != Command.EOT)
                {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(EOT) 수신에 실패했습니다.");
                    return;
                }
                KByteArray responseData = new KByteArray(_resultData);
                responseData.CutToSize(1);  //stx
                byte[] stringcomand = responseData.CutToSize(4);
                responseData.CutToSize(92);
                byte[] resCode = responseData.CutToSize(4);
                byte[] msg = responseData.CutToSize(40);

                String resultCode = "";
                resultCode = new String(resCode);


                if (!resultCode.equals("0000")) {
                    String Message = "";
                    String AnsCode = "";
                    try {
                        Message = new String(msg, "euc-kr");
                        AnsCode = new String(resCode);
//                        AnsCode = new String(resCode, "euc-kr");
                        //stringValue = new String(temp);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    mCatNetworkConnectListener.onState(0, false, AnsCode + "\n" + Message);
                    return;
                }

                HashMap<String,String> pData = ParsingresData(_resultData, false);

                if (pData == null) {
                    mCatNetworkConnectListener.onState(0, false, "데이터 파싱이 잘봇되었습니다.");
                    return;
                }

                if(pData.get("AuNo") == null || pData.get("AuNo").equals(""))
                {
                    mCatNetworkConnectListener.onState(0, false, pData.get("AnsCode") + "\n" + pData.get("Message"));
                    return;
                }

                InsertDBTradeData_TypeCrdit(pData,"Credit");


            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {

            }


        });





//            let finish = Command.Cat_ResponseTrade(Command: Command.CMD_CAT_CREDIT_RES, Code: resCode, Message: msg)




        //만일 apptoapp 이면 여기서 db 저장으로 가지 않고 델리게이트로 보낸다. 본앱이면 db로 간다
//            if apptoapp == true {
//                InsertDBTradeData_TypeCrdit(수신데이터: pData)
//            } else {
//                Result.onResult(CatState: 0, ResultData: recv)
//            }

    }

    public void PayCredit(String _tid,String _money, String _tax,String _svc,String _txf,String _AuDate, String _AuNo,
                          String _UniqueNumber, String _InstallMent, boolean _Cancel,String _MchData, String _ExtraFiled, boolean _appToapp,
                          String _storeName, String _storeAddr, String _storeNumber, String _storePhone, String _storeOwner)
    {
        Clear();
        CatPaymentSdk.Instance.Tid = _tid;
        CatPaymentSdk.Instance.mStoreName = _storeName;
        CatPaymentSdk.Instance.mStoreAddr = _storeAddr;
        CatPaymentSdk.Instance.mStoreNumber = _storeNumber;
        CatPaymentSdk.Instance.mStorePhone = _storePhone;
        CatPaymentSdk.Instance.mStoreOwner = _storeOwner;
        if (_Cancel) {
            CatPaymentSdk.Instance.Money =
                    String.valueOf(Integer.parseInt(_money) +
                            Integer.parseInt(_tax) +
                            Integer.parseInt(_svc) +
                            Integer.parseInt(_txf));
            CatPaymentSdk.Instance.Tax = "0";
            CatPaymentSdk.Instance.Svc = "0";
            CatPaymentSdk.Instance.Txf = "0";
        } else {
            CatPaymentSdk.Instance.Money = _money;
            CatPaymentSdk.Instance.Tax = _tax;
            CatPaymentSdk.Instance.Svc = _svc;
            CatPaymentSdk.Instance.Txf = _txf;
        }

        String _audate = _AuDate;
        if (_audate.length() > 5 && _audate.substring(0,2) != "20") {
            _audate = "20" + _audate;
        }
        if (_audate.length() > 8) {
            _audate = _audate.substring(0,8);
        }
        CatPaymentSdk.Instance.AuDate = _audate;
        CatPaymentSdk.Instance.AuNo = _AuNo;
        CatPaymentSdk.Instance.UniqueNumber = _UniqueNumber;
        CatPaymentSdk.Instance.InstallMent = _InstallMent;
        CatPaymentSdk.Instance.Cancel = _Cancel;
        CatPaymentSdk.Instance.MchData = _MchData;
        CatPaymentSdk.Instance.ExtraField = _ExtraFiled;
        CatPaymentSdk.Instance.mDBAppToApp = _appToapp;
        /** log : PayCredit */
//        LogFile.instance.InsertLog("CAT 신용결제 App -> CAT", Tid: _tid == "" ? Setting.shared.getDefaultUserData(_key: define.STORE_TID):_tid, TimeStamp: true)

        /** 아래 부분 다른 커스텀메세지박스를 만들어서 사용한다. 아래꺼로하기에는 분기를 해줘야 할 게 많다. */
        ShowMessageBox("CAT단말기에서 카드를 읽어주세요", Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
//        ((BaseActivity)mCtx).ReadyDialogShow(mCtx,
//                "CAT단말기에서 카드를 읽어주세요",
//                60,true);
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (mPosSdk.mTcpClient.ConnectServer() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 IP/PORT 설정이 잘못되었습니다.");
                    return;
                }
                if (mPosSdk.mTcpClient.CheckBeforeTrade() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 연결에 실패했습니다.");
                    return;
                }

                byte[] SendData = {};
                try {
                    SendData = Command.Cat_Credit(_tid, CatPaymentSdk.Instance.Money, CatPaymentSdk.Instance.Tax,
                            CatPaymentSdk.Instance.Svc, CatPaymentSdk.Instance.Txf, CatPaymentSdk.Instance.AuDate,
                            _AuNo, _UniqueNumber, _InstallMent, _Cancel, _MchData, _ExtraFiled);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mPosSdk.mTcpClient.TcpSend(SendData, new KocesTcpClient.CatTcpListener() {
                    @Override
                    public void onSendResult(boolean _result) {
                        if (!_result) {
                            mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(전문) 전송에 실패했습니다.");
                            return;
                        }
                        FlowCatCredit1();
                    }

                    @Override
                    public void onReceiveResult(boolean _result, byte[] _recv) {

                    }

                    @Override
                    public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                    }
                });

                return;

            }
        }).start();
    }

    private void CreditFallBack() {
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {
            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                String str1 = "";
                try {
                    str1 = new String(_recv, "euc-kr");
                    //stringValue = new String(temp);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (str1.contains("A110")) {
                    byte[] ack = {Command.ACK, Command.ACK}; // TR Command가 오면 ACK 2개를 보낸다.
                    mPosSdk.mTcpClient.TcpSend(ack,new KocesTcpClient.CatTcpListener() {

                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                return;
                            }
                            FlowCatCredit3();
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });
                    return;
                } else if (_recv[1] == (byte)0x51) { //Q
                    mCatNetworkConnectListener.onState(0, false, "거래 가능한 TID 가 아닙니다");
                    return;
                }  else if (_recv[1] == (byte)0x45) { //E
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래 취소");
                    return;
                } else {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래에 실패했습니다");
                    return;
                }

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });

    }

    private void FlowCatCashRecipt1()
    {
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                if (_recv[0] != Command.ACK) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK) 수신에 실패했습니다.");
                    return;
                }
                FlowCatCashRecipt2();
            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatCashRecipt2()
    {
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래종료");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                String str1 = "";
                try {
                    str1 = new String(_recv, "euc-kr");
                    //stringValue = new String(temp);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (str1.contains("A110")) {
                    byte[] ack = {Command.ACK, Command.ACK}; // TR Command가 오면 ACK 2개를 보낸다.
                    mPosSdk.mTcpClient.TcpSend(ack,new KocesTcpClient.CatTcpListener() {

                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                return;
                            }
                            FlowCatCashRecipt3();
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });
                    return;
                } else if (_recv[1] == (byte)0x51) { //Q
                    mCatNetworkConnectListener.onState(0, false, "거래 가능한 TID 가 아닙니다");
                    return;
                }  else if (_recv[1] == (byte)0x45) { //E
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래 취소");
                    return;
                } else {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래에 실패했습니다");
                    return;
                }

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatCashRecipt3()
    {
        ShowMessageBox("CAT단말기에서 서버에 요청 중입니다. 잠시만 기다려주세요",Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv[1] == (byte)0x47) {
                    byte[] last = new byte[_recv.length];
                    System.arraycopy(_recv, 0, last, 0, _recv.length);
                    KByteArray responseData = new KByteArray(last);
                    responseData.CutToSize(1);  //stx
                    byte[] stringcomand = responseData.CutToSize(4);
                    responseData.CutToSize(92);
                    byte[] resCode = responseData.CutToSize(4);
                    byte[] msg = responseData.CutToSize(40);
                    byte[] finish = Command.Cat_ResponseTrade(ResponseCommand(stringcomand), new byte[]{0x30,0x30,0x30,0x30}, "정상수신");
                    responseData.Clear();
                    mPosSdk.mTcpClient.TcpSend(finish,new KocesTcpClient.CatTcpListener() {
                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(완료) 전송에 실패했습니다.");
                                return;
                            }
                            FlowCatCashRecipt4(last);
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });

                } else if (_recv[1] == Command.NAK) {
                    //Nak 올라오면 재전송1회 시도
                    byte[] ack = {Command.ACK, Command.ACK}; // TR Command가 오면 ACK 2개를 보낸다.
                    mPosSdk.mTcpClient.TcpSend(ack,new KocesTcpClient.CatTcpListener() {

                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                return;
                            }

                            mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

                                @Override
                                public void onSendResult(boolean _result) {

                                }

                                @Override
                                public void onReceiveResult(boolean _result, byte[] _recv) {
                                    if (_recv == null) {
                                        mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                                        return;
                                    }
                                    if (_recv.length <= 0) {
                                        mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                                        return;
                                    }

                                    if (_recv[1] == (byte)0x47) {
                                        byte[] last = _recv;
                                        KByteArray responseData = new KByteArray(last);
                                        responseData.CutToSize(1);  //stx
                                        byte[] stringcomand = responseData.CutToSize(4);
                                        responseData.CutToSize(92);
                                        byte[] resCode = responseData.CutToSize(4);
                                        byte[] msg = responseData.CutToSize(40);
                                        byte[] finish = Command.Cat_ResponseTrade(ResponseCommand(stringcomand), new byte[]{0x30,0x30,0x30,0x30}, "정상수신");
                                        responseData.Clear();
                                        mPosSdk.mTcpClient.TcpSend(finish,new KocesTcpClient.CatTcpListener() {
                                            @Override
                                            public void onSendResult(boolean _result) {
                                                if(!_result){
                                                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(완료) 전송에 실패했습니다.");
                                                    return;
                                                }

                                                FlowCatCashRecipt4(last);
                                            }

                                            @Override
                                            public void onReceiveResult(boolean _result, byte[] _recv) {

                                            }

                                            @Override
                                            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                                            }
                                        });

                                    }
                                }

                                @Override
                                public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                                }
                            });
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });
                }
            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatCashRecipt4(byte[] last)
    {
        mPosSdk.mTcpClient.TcpLastRead(last, new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                if (_recv[0] != Command.EOT)
                {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(EOT) 수신에 실패했습니다.");
                    return;
                }
                KByteArray responseData = new KByteArray(_resultData);
                responseData.CutToSize(1);  //stx
                byte[] stringcomand = responseData.CutToSize(4);
                responseData.CutToSize(92);
                byte[] resCode = responseData.CutToSize(4);
                byte[] msg = responseData.CutToSize(40);

                String resultCode = "";
                resultCode = new String(resCode);


                if (!resultCode.equals("0000")) {
                    String Message = "";
                    String AnsCode = "";
                    try {
                        Message = new String(msg, "euc-kr");
                        AnsCode = new String(resCode);
//                        AnsCode = new String(resCode, "euc-kr");
                        //stringValue = new String(temp);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    mCatNetworkConnectListener.onState(0, false, AnsCode + "\n" + Message);
                    return;
                }

                HashMap<String,String> pData = ParsingresData(_resultData, false);

                if (pData == null) {
                    mCatNetworkConnectListener.onState(0, false, "데이터 파싱이 잘봇되었습니다.");
                    return;
                }

                if(pData.get("AuNo") == null || pData.get("AuNo").equals(""))
                {
                    mCatNetworkConnectListener.onState(0, false, pData.get("AnsCode") + "\n" + pData.get("Message"));
                    return;
                }

                InsertDBTradeData_TypeCrdit(pData,"CashRecipt");


            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {

            }


        });
    }

    public void CashRecipt(String _tid,String _money, String _tax,String _svc,String _txf,String _AuDate, String _AuNo,
                           String _UniqueNumber, String _InstallMent,String _id,String _pb, boolean _Cancel,String _CancelReason ,
                           String _MchData, String _ExtraFiled, boolean _appToapp,
                           String _storeName, String _storeAddr, String _storeNumber, String _storePhone, String _storeOwner)
    {
        Clear();
        CatPaymentSdk.Instance.Tid = _tid;
        CatPaymentSdk.Instance.mStoreName = _storeName;
        CatPaymentSdk.Instance.mStoreAddr = _storeAddr;
        CatPaymentSdk.Instance.mStoreNumber = _storeNumber;
        CatPaymentSdk.Instance.mStorePhone = _storePhone;
        CatPaymentSdk.Instance.mStoreOwner = _storeOwner;
        if (_Cancel) {
            CatPaymentSdk.Instance.Money =
                    String.valueOf(Integer.parseInt(_money) +
                    Integer.parseInt(_tax) +
                    Integer.parseInt(_svc) +
                    Integer.parseInt(_txf));
            CatPaymentSdk.Instance.Tax = "0";
            CatPaymentSdk.Instance.Svc = "0";
            CatPaymentSdk.Instance.Txf = "0";
        } else {
            CatPaymentSdk.Instance.Money = _money;
            CatPaymentSdk.Instance.Tax = _tax;
            CatPaymentSdk.Instance.Svc = _svc;
            CatPaymentSdk.Instance.Txf = _txf;
        }
        String _audate = _AuDate;
        if (_audate.length() > 5 && _audate.substring(0,2) != "20") {
            _audate = "20" + _audate;
        }
        if (_audate.length() > 8) {
            _audate = _audate.substring(0,8);
        }
        CatPaymentSdk.Instance.AuDate = _audate;
        CatPaymentSdk.Instance.AuNo = _AuNo;
        CatPaymentSdk.Instance.UniqueNumber = _UniqueNumber;
        CatPaymentSdk.Instance.InstallMent = _InstallMent;
        CatPaymentSdk.Instance.Cancel = _Cancel;
        CatPaymentSdk.Instance.MchData = _MchData;
        CatPaymentSdk.Instance.ExtraField = _ExtraFiled;
        CatPaymentSdk.Instance.pb = _pb;
        CatPaymentSdk.Instance.CancelReason = _CancelReason;
        CatPaymentSdk.Instance.mDBAppToApp = _appToapp;
        /** log : CashRecipt */
//        LogFile.instance.InsertLog("CAT 현금영수증 App -> CAT", Tid: _tid == "" ? Setting.shared.getDefaultUserData(_key: define.STORE_TID):_tid, TimeStamp: true)
        /** 아래 부분 다른 커스텀메세지박스를 만들어서 사용한다. 아래꺼로하기에는 분기를 해줘야 할 게 많다. */
        ShowMessageBox("단말기에서 MSR을 읽어주세요", Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (mPosSdk.mTcpClient.ConnectServer() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 IP/PORT 설정이 잘못되었습니다.");
                    return;
                }
                if (mPosSdk.mTcpClient.CheckBeforeTrade() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 연결에 실패했습니다.");
                    return;
                }

                byte[] SendData = {};
                try {
                    SendData = Command.Cat_CashRecipt(_tid, CatPaymentSdk.Instance.Money, CatPaymentSdk.Instance.Tax,
                            CatPaymentSdk.Instance.Svc, CatPaymentSdk.Instance.Txf, CatPaymentSdk.Instance.AuDate,
                            CatPaymentSdk.Instance.AuNo, CatPaymentSdk.Instance.UniqueNumber, CatPaymentSdk.Instance.InstallMent, _id,
                            CatPaymentSdk.Instance.pb, _Cancel, CatPaymentSdk.Instance.CancelReason, CatPaymentSdk.Instance.MchData, CatPaymentSdk.Instance.ExtraField);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("CatCashRecipt : ", Utils.bytesToHex_0xType(SendData));
                mPosSdk.mTcpClient.TcpSend(SendData, new KocesTcpClient.CatTcpListener() {
                    @Override
                    public void onSendResult(boolean _result) {
                        if (!_result) {
                            mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(전문) 전송에 실패했습니다.");
                            return;
                        }
                        FlowCatCashRecipt1();
                    }

                    @Override
                    public void onReceiveResult(boolean _result, byte[] _recv) {

                    }

                    @Override
                    public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                    }
                });

                return;

            }
        }).start();


    }



    private void FlowCatCashICRecipt1()
    {
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                if (_recv[0] != Command.ACK) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK) 수신에 실패했습니다.");
                    return;
                }
                FlowCatCashICRecipt2();
            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatCashICRecipt2()
    {
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래종료");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                String str1 = "";
                try {
                    str1 = new String(_recv, "euc-kr");
                    //stringValue = new String(temp);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (str1.contains("A110")) {
                    byte[] ack = {Command.ACK, Command.ACK}; // TR Command가 오면 ACK 2개를 보낸다.
                    mPosSdk.mTcpClient.TcpSend(ack,new KocesTcpClient.CatTcpListener() {

                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                return;
                            }
                            FlowCatCashICRecipt3();
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });
                    return;
                } else if (_recv[1] == (byte)0x51) { //Q
                    mCatNetworkConnectListener.onState(0, false, "거래 가능한 TID 가 아닙니다");
                    return;
                }  else if (_recv[1] == (byte)0x45) { //E
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래 취소");
                    return;
                } else {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 거래에 실패했습니다");
                    return;
                }

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatCashICRecipt3()
    {
        ShowMessageBox("CAT단말기에서 서버에 요청 중입니다. 잠시만 기다려주세요",Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));
        mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv[1] == (byte)0x47) {
                    byte[] last = new byte[_recv.length];
                    System.arraycopy(_recv, 0, last, 0, _recv.length);
                    KByteArray responseData = new KByteArray(last);
                    responseData.CutToSize(1);  //stx
                    byte[] stringcomand = responseData.CutToSize(4);
                    responseData.CutToSize(25);
                    byte[] resCode = responseData.CutToSize(4);
                    byte[] msg = responseData.CutToSize(40);
                    byte[] finish = Command.Cat_ResponseTrade(ResponseCommand(stringcomand), new byte[]{0x30,0x30,0x30,0x30}, "정상수신");
                    responseData.Clear();
                    mPosSdk.mTcpClient.TcpSend(finish,new KocesTcpClient.CatTcpListener() {
                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(완료) 전송에 실패했습니다.");
                                return;
                            }
                            FlowCatCashICRecipt4(last);
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });

                } else if (_recv[1] == Command.NAK) {
                    //Nak 올라오면 재전송1회 시도
                    byte[] ack = {Command.ACK, Command.ACK}; // TR Command가 오면 ACK 2개를 보낸다.
                    mPosSdk.mTcpClient.TcpSend(ack,new KocesTcpClient.CatTcpListener() {

                        @Override
                        public void onSendResult(boolean _result) {
                            if(!_result){
                                mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(ACK,ACK) 전송에 실패했습니다.");
                                return;
                            }

                            mPosSdk.mTcpClient.TcpRead(new KocesTcpClient.CatTcpListener() {

                                @Override
                                public void onSendResult(boolean _result) {

                                }

                                @Override
                                public void onReceiveResult(boolean _result, byte[] _recv) {
                                    if (_recv == null) {
                                        mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                                        return;
                                    }
                                    if (_recv.length <= 0) {
                                        mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                                        return;
                                    }

                                    if (_recv[1] == (byte)0x47) {
                                        byte[] last = _recv;
                                        KByteArray responseData = new KByteArray(last);
                                        responseData.CutToSize(1);  //stx
                                        byte[] stringcomand = responseData.CutToSize(4);
                                        responseData.CutToSize(25);
                                        byte[] resCode = responseData.CutToSize(4);
                                        byte[] msg = responseData.CutToSize(40);
                                        byte[] finish = Command.Cat_ResponseTrade(ResponseCommand(stringcomand), new byte[]{0x30,0x30,0x30,0x30}, "정상수신");
                                        responseData.Clear();
                                        mPosSdk.mTcpClient.TcpSend(finish,new KocesTcpClient.CatTcpListener() {
                                            @Override
                                            public void onSendResult(boolean _result) {
                                                if(!_result){
                                                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(완료) 전송에 실패했습니다.");
                                                    return;
                                                }

                                                FlowCatCashICRecipt4(last);
                                            }

                                            @Override
                                            public void onReceiveResult(boolean _result, byte[] _recv) {

                                            }

                                            @Override
                                            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                                            }
                                        });

                                    }
                                }

                                @Override
                                public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                                }
                            });
                        }

                        @Override
                        public void onReceiveResult(boolean _result, byte[] _recv) {

                        }

                        @Override
                        public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                        }
                    });
                }
            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

            }
        });
    }

    private void FlowCatCashICRecipt4(byte[] last)
    {
        mPosSdk.mTcpClient.TcpLastRead(last, new KocesTcpClient.CatTcpListener() {

            @Override
            public void onSendResult(boolean _result) {

            }

            @Override
            public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {
                if (_recv == null) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }
                if (_recv.length <= 0) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터 수신에 실패했습니다.");
                    return;
                }

                if (_recv[0] != Command.EOT)
                {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(EOT) 수신에 실패했습니다.");
                    return;
                }
                KByteArray responseData = new KByteArray(_resultData);
                responseData.CutToSize(1);  //stx
                byte[] stringcomand = responseData.CutToSize(4);
                responseData.CutToSize(25);
                byte[] resCode = responseData.CutToSize(4);
                byte[] msg = responseData.CutToSize(40);

                String resultCode = "";
                resultCode = new String(resCode);


                if (!resultCode.equals("0000")) {
                    String Message = "";
                    String AnsCode = "";
                    try {
                        Message = new String(msg, "euc-kr");
                        AnsCode = new String(resCode);
//                        AnsCode = new String(resCode, "euc-kr");
                        //stringValue = new String(temp);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    mCatNetworkConnectListener.onState(0, false, AnsCode + "\n" + Message);
                    return;
                }

                HashMap<String,String> pData = ParsingresCashIC(_resultData);

                if (pData == null) {
                    mCatNetworkConnectListener.onState(0, false, "데이터 파싱이 잘봇되었습니다.");
                    return;
                }

                InsertDBTradeData_TypeCashIC(pData,"CashIC");


            }

            @Override
            public void onReceiveResult(boolean _result, byte[] _recv) {

            }


        });
    }

    public void CashIC(Constants.CashICBusinessClassification _Class, String _tid,
                       String _money,String _tax,String _svc,String _txf,String _AuDate,String _AuNo,
                       String _directTrade,String _cardInfo,boolean _cancel,String _mchData,
                       String _extrafield,boolean _appToapp,
                       String _storeName, String _storeAddr, String _storeNumber, String _storePhone, String _storeOwner)
    {
        Clear();
        CatPaymentSdk.Instance.Tid = _tid;
        CatPaymentSdk.Instance.mStoreName = _storeName;
        CatPaymentSdk.Instance.mStoreAddr = _storeAddr;
        CatPaymentSdk.Instance.mStoreNumber = _storeNumber;
        CatPaymentSdk.Instance.mStorePhone = _storePhone;
        CatPaymentSdk.Instance.mStoreOwner = _storeOwner;
        if (_cancel) {
            CatPaymentSdk.Instance.Money =
                    String.valueOf(Integer.parseInt(_money) +
                            Integer.parseInt(_tax) +
                            Integer.parseInt(_svc) +
                            Integer.parseInt(_txf));
            CatPaymentSdk.Instance.Tax = "0";
            CatPaymentSdk.Instance.Svc = "0";
            CatPaymentSdk.Instance.Txf = "0";
        } else {
            CatPaymentSdk.Instance.Money = _money;
            CatPaymentSdk.Instance.Tax = _tax;
            CatPaymentSdk.Instance.Svc = _svc;
            CatPaymentSdk.Instance.Txf = _txf;
        }

        String _audate = _AuDate;
        if (_audate.length() > 5 && _audate.substring(0,2) == "20") {
            _audate = _audate.substring(2);
        }
        if (_audate.length() > 6) {
            _audate = _audate.substring(0,6);
        }
        CatPaymentSdk.Instance.AuDate = _audate;
        CatPaymentSdk.Instance.AuNo = _AuNo;
        CatPaymentSdk.Instance.Class = _Class;
        CatPaymentSdk.Instance.DirectTrade = _directTrade;
        CatPaymentSdk.Instance.CardInfo = _cardInfo;
        CatPaymentSdk.Instance.Cancel = _cancel;
        CatPaymentSdk.Instance.MchData = _mchData;
        CatPaymentSdk.Instance.ExtraField = _extrafield;
        CatPaymentSdk.Instance.mDBAppToApp = _appToapp;

        /** log : CashIC */
//        LogFile.instance.InsertLog("CAT 현금영수증 App -> CAT", Tid: _tid == "" ? Setting.shared.getDefaultUserData(_key: define.STORE_TID):_tid, TimeStamp: true)
        /** 아래 부분 다른 커스텀메세지박스를 만들어서 사용한다. 아래꺼로하기에는 분기를 해줘야 할 게 많다. */
        ShowMessageBox("단말기에서 카드를 읽어주세요", Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (mPosSdk.mTcpClient.ConnectServer() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 IP/PORT 설정이 잘못되었습니다.");
                    return;
                }
                if (mPosSdk.mTcpClient.CheckBeforeTrade() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 연결에 실패했습니다.");
                    return;
                }

                byte[] SendData = {};
                try {
                    SendData = Command.Cat_CashIC(CatPaymentSdk.Instance.Class,_tid, CatPaymentSdk.Instance.Money, CatPaymentSdk.Instance.Tax,
                            CatPaymentSdk.Instance.Svc, CatPaymentSdk.Instance.Txf, CatPaymentSdk.Instance.AuDate,
                            CatPaymentSdk.Instance.AuNo, CatPaymentSdk.Instance.DirectTrade, CatPaymentSdk.Instance.CardInfo,
                            CatPaymentSdk.Instance.Cancel, CatPaymentSdk.Instance.MchData, CatPaymentSdk.Instance.ExtraField);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("CatCashRecipt : ", Utils.bytesToHex_0xType(SendData));
                mPosSdk.mTcpClient.TcpSend(SendData, new KocesTcpClient.CatTcpListener() {
                    @Override
                    public void onSendResult(boolean _result) {
                        if (!_result) {
                            mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(전문) 전송에 실패했습니다.");
                            return;
                        }
                        FlowCatCashICRecipt1();
                    }

                    @Override
                    public void onReceiveResult(boolean _result, byte[] _recv) {

                    }

                    @Override
                    public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                    }
                });

                return;

            }
        }).start();

    }

    //캣으로부터 E 취소커맨드가 들어오면 우리도 E 취소커맨드를 날리고 Ack가 들어오는지 확인후 종료한다
    public void Cat_SendCancelCommandE(boolean _appToapp) {
        Clear();
        mPosSdk.CatConnect(Setting.getCatIPAddress(mCtx),Integer.parseInt(Setting.getCatVanPORT(mCtx)),mCatNetworkConnectListener);
        CatPaymentSdk.Instance.mDBAppToApp = _appToapp;

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (mPosSdk.mTcpClient.ConnectServer() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 IP/PORT 설정이 잘못되었습니다.");
                    return;
                }
//                if (mPosSdk.mTcpClient.CheckBeforeTrade() == false) {
//                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 연결에 실패했습니다.");
//                    return;
//                }

                byte[] SendData = {};
                try {
                    SendData = Command.Cat_CancelCMD_E();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("Cat_SendCancelCommandE : ", Utils.bytesToHex_0xType(SendData));
                mPosSdk.mTcpClient.TcpSend(SendData, new KocesTcpClient.CatTcpListener() {
                    @Override
                    public void onSendResult(boolean _result) {
                        if (!_result) {
//                            mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(전문) 전송에 실패했습니다.");
                            return;
                        }
//                        mCatNetworkConnectListener.onState(0, false, "거래를 취소하였습니다");
                        return;
//                        FlowCatCashICRecipt1();
                    }

                    @Override
                    public void onReceiveResult(boolean _result, byte[] _recv) {

                    }

                    @Override
                    public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                    }
                });

                return;

            }
        }).start();
    }

    /**
     CAT 프린터
     */
    public void Print(String _Contents , boolean _appToapp) {
        Clear();

        CatPaymentSdk.Instance.mDBAppToApp = _appToapp;

        KByteArray temp = new KByteArray();

        if (_Contents.equals(Constants.PMoney) || _Contents.equals(Constants.PMoney_Tong))
        {

            temp.Add(Constants.Money);
        } else
        {
            String cont = _Contents.replace( Constants.PFont_LF, "\n");
            cont = cont.replace( "___LF___", "\n");
            String[] StrArr = cont.split( "\n");


            temp.Add(Constants.Init);
            temp.Add(Constants.Logo_Print);

            for(String n : StrArr) {

                String n1 = Command.CatPrintParser( n);
                byte[] euckrStrBuffer = new byte[0];
                try {
                    euckrStrBuffer = n1.getBytes("euc-kr");
                    //stringValue = new String(temp);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                temp.Add(euckrStrBuffer);
                temp.Add((byte)0x0A);

            }

            temp.Add((byte)0x0A);temp.Add((byte)0x0A);temp.Add((byte)0x0A);temp.Add((byte)0x0A);temp.Add((byte)0x0A);
            temp.Add(Constants.Cut_print);
        }


        /** 아래 부분 다른 커스텀메세지박스를 만들어서 사용한다. 아래꺼로하기에는 분기를 해줘야 할 게 많다. */
        ShowMessageBox("프린트 중입니다. 잠시만 기다려주세요", Integer.parseInt(Setting.getPreference(mCtx,Constants.CAT_TIME_OUT)));


        /** log : CashRecipt */
//        LogFile.instance.InsertLog("CAT 프린트 App -> CAT", Tid: Setting.shared.getDefaultUserData(_key: define.STORE_TID), TimeStamp: true)
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mPosSdk.mTcpClient.ConnectServer() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 IP/PORT 설정이 잘못되었습니다.");
                    return;
                }
                if (mPosSdk.mTcpClient.CheckBeforeTrade() == false) {
                    mCatNetworkConnectListener.onState(0, false, "CAT 단말기 연결에 실패했습니다.");
                    return;
                }

                byte[] SendData = {};
                try {
                    SendData = temp.value();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("Print : ", Utils.bytesToHex_0xType(SendData));
                mPosSdk.mTcpClient.TcpSend(SendData, new KocesTcpClient.CatTcpListener() {
                    @Override
                    public void onSendResult(boolean _result) {
                        if (!_result) {
                            mCatNetworkConnectListener.onState(0, false, "CAT 단말기 데이터(전문) 전송에 실패했습니다.");
                            return;
                        }
                        mCatNetworkConnectListener.onState(0, true, "Print");
                        return;
                    }

                    @Override
                    public void onReceiveResult(boolean _result, byte[] _recv) {

                    }

                    @Override
                    public void onReceiveLastResult(boolean _result, byte[] _recv, byte[] _resultData) {

                    }
                });

                return;
            }
        }).start();


    }

    private HashMap<String,String> ParsingEasyresData(byte[] _recv, boolean _easyCheck) {
        //데이터 사이즈 체크
        if (_recv.length < 6) {
            return null;
        }

        //데이터 시작이 stx가 아니면 취소
        if (_recv[0] != Command.STX) {
            return null;
        }

        KByteArray parseData = new KByteArray(_recv);   //데이터를 지워 가면서 작업을 해야 해서 임시로 변수 하나 지정
        //본격적인 파싱 시작
        HashMap<String, String> data = new HashMap<>();

        parseData.CutToSize(1); //STX 삭제
//        var rng = 4 //데이터를 짜르기 위한 범위
        String _tmp = "";
        try {
            _tmp = new String(parseData.CutToSize(4), "euc-kr");//명령코드 4byte
            //stringValue = new String(temp);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //TODO: 120 130 140 150 앱투앱일 경우

        parseData.CutToSize(4); //길이 삭제
        String _all = "";
        try {
            _all = new String(parseData.value(), "euc-kr");//전체
            //stringValue = new String(temp);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String[] splited = _all.split(";");
//        data.put("TrdType", _easyCheck == true ? "T180" : _tmp); //명령코드 4byte
        String _tpye = splited[0].substring(4);
        if(_tpye.equals("A15"))
        {
            _tpye = "E15";
        }
        else if(_tpye.equals("A25"))
        {
            _tpye = "E25";
        }
        else if(_tpye.equals("Z35"))
        {
            _tpye = "E35";
        }
        //TODO: 현재 캣 간편결제에서 승인날짜를 보내오지 않는다. 따라서 현재 거래시간을 토대로 집어넣는다.
        data.put("TrdDate", Utils.getDate("yyMMddHHmmss"));
        data.put("TrdType", _tpye); //업무구분 3byte
        data.put("ResponseNo", _tpye); //업무구분 3byte
        data.put("QrKind", splited[1].length() >=4 ? splited[1].substring(4):""); //간편결제 거래 종류
        data.put("TermID", splited[2].length() >=4 ? splited[2].substring(4):""); //Terminal ID
        data.put("CatVersion", splited[3].length() >=4 ? splited[3].substring(4):""); //단말기 Ver
        data.put("Month", splited[4].length() >=4 ? splited[4].substring(4):""); //할부기간
        data.put("TrdAmt", splited[5].length() >=4 ? splited[5].substring(4):""); //거래금액
        data.put("TaxAmt", splited[6].length() >=4 ? splited[6].substring(4):""); //세금
        data.put("SvcAmt", splited[7].length() >=4 ? splited[7].substring(4):""); //봉사료
        data.put("TaxFreeAmt", splited[8].length() >=4 ? splited[8].substring(4):""); //비과세
        data.put("DisAmt", splited[9].length() >=4 ? splited[9].substring(4):""); //카카오페이 할인 금액

        data.put("CardNo", splited[10].length() >=4 ? splited[10].substring(4):""); //전표출력 시 사용될 바코드번호
        data.put("AnsCode", splited[11].length() >=4 ? splited[11].substring(4):""); //응답코드
        data.put("Message", splited[12].length() >=4 ? splited[12].substring(4):""); //응답메세지
        data.put("AuNo", splited[13].length() >=4 ? splited[13].substring(4):""); //승인번호
        data.put("SubAuNo", splited[14].length() >=4 ? splited[14].substring(4):""); //서브승인번호
        data.put("TradeNo", splited[15].length() >=4 ? splited[15].substring(4):""); //KOCES 거래고유번호
        data.put("AuthType", splited[16].length() >=4 ? splited[16].substring(4):""); //결제수단
        data.put("AnswerTrdNo", splited[17].length() >=4 ? splited[17].substring(4):""); //출력용 거래고유번호
        data.put("CardKind", splited[18].length() >=4 ? splited[18].substring(4):""); //카드종류
        data.put("DDCYn", splited[19].length() >=4 ? splited[19].substring(4):""); //DDC여부
        data.put("EDCYn", splited[20].length() >=4 ? splited[20].substring(4):""); //EDC여부
        data.put("OrdCd", splited[21].length() >=4 ? splited[21].substring(4):""); //발급기관코드
        data.put("OrdNm", splited[22].length() >=4 ? splited[22].substring(4):""); //발급기관명
        data.put("InpCd", splited[23].length() >=4 ? splited[23].substring(4):""); //매입기관코드
        data.put("InpNm", splited[24].length() >=4 ? splited[24].substring(4):""); //매입기관명
        data.put("MchNo", splited[25].length() >=4 ? splited[25].substring(4):""); //가맹점번호
        data.put("ChargeAmt", splited[26].length() >=4 ? splited[26].substring(4):""); //가맹점수수료
        data.put("RefundAmt", splited[27].length() >=4 ? splited[27].substring(4):""); //가맹점환불금액
        data.put("MchData", splited[28].length() >=4 ? splited[28].substring(4):""); //가맹점데이터

        return data;
    }

    private HashMap<String,String> ParsingresData(byte[] _recv, boolean _easyCheck)
    {
        //데이터 사이즈 체크
        if (_recv.length < 6) {
            return null;
        }

        //데이터 시작이 stx가 아니면 취소
        if (_recv[0] != Command.STX) {
            return null;
        }

        KByteArray parseData = new KByteArray(_recv);   //데이터를 지워 가면서 작업을 해야 해서 임시로 변수 하나 지정
        //본격적인 파싱 시작
        HashMap<String,String> data = new HashMap<>();

        parseData.CutToSize(1); //STX 삭제
//        var rng = 4 //데이터를 짜르기 위한 범위
        String _tmp = "";
        try {
            _tmp = new String(parseData.CutToSize(4), "euc-kr");//명령코드 4byte
            //stringValue = new String(temp);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //TODO: 120 130 140 150 앱투앱일 경우
        if (CatPaymentSdk.Instance.mDBAppToApp)
        {
            if (_tmp.equals("G120"))
            {
                _tmp = (CatPaymentSdk.Instance.Cancel == false ? "A15":"A25");
                data.put("TrdType",_easyCheck == true ? "T180":_tmp); //명령코드 4byte
            }
            else if (_tmp.equals("G130"))
            {
                _tmp = (CatPaymentSdk.Instance.Cancel == false ? "B15":"B25");
                data.put("TrdType",_easyCheck == true ? "T180":_tmp); //명령코드 4byte
            }
        }
        else
        {
            data.put("TrdType",_easyCheck == true ? "T180":_tmp); //명령코드 4byte
        }

        parseData.CutToSize(4); //거래전문번호 4byte  //x
        try {
            _tmp = new String(parseData.CutToSize(10), "euc-kr"); //TID 10byte   //x
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("TermID",_tmp); //TID 10byte   //x
        parseData.CutToSize(5); //단말기 Ver 5byte  //x
        parseData.CutToSize(2); //할부 2byte //x
        parseData.CutToSize(10); //거래금액 10byte   //x
        parseData.CutToSize(9); //세금 9byte  //x
        parseData.CutToSize(9); //봉사료 9byte  //x
        parseData.CutToSize(9); //비과세 9byte  //x
        parseData.CutToSize(2); //원거래일자 중 앞에 2개(년도가 4자리로 보내준다) 를 제거 8byte    //x
        try {
            CatPaymentSdk.Instance.OriAuDate =  new String(parseData.CutToSize(6), "euc-kr");//원거래일자 8byte 중 뒤에 6개    //x
            CatPaymentSdk.Instance.OriAuNo = new String(parseData.CutToSize(12), "euc-kr");//원거래번호 12byte    //x
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        parseData.CutToSize(2); //거래일시 14byte 중 앞에 2자리 년도 제거
        String _trdDate = "", _ansCode = "", _message = "", _cardNo = "";
        try {
            _trdDate =  new String(parseData.CutToSize(12), "euc-kr");//거래일시 14byte Format:YYYYMMDDhhmmss -> yymmddhhmmss 12자리만 저장하기 위해서
            _ansCode =  new String(parseData.CutToSize(4), "euc-kr");//응답코드 정상응답(응답코드: 0000, 0001, 0002, 9901)
            _message =  new String(parseData.CutToSize(80), "euc-kr");//신용:앞 40Byte+뒤 40Byte Space
            _cardNo =  new String(parseData.CutToSize(40), "euc-kr");//신용,은련,DCC: 출력시 사용될 Track2 정보,현금: 출력 시 사용될 신분확인번호 정보
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        _trdDate = _trdDate.replace(" ","");
        data.put("TrdDate",_trdDate);
        data.put("AnsCode",_ansCode);
        data.put("Message",_message);
        _cardNo = _cardNo.replace("-","");
        _cardNo = _cardNo.replace(" ","");
        /** 마스킹카드번호 변경(22.04.01 jiw 앱투앱으로 보낼 때 8자리를 보내고 그외 본앱에서 사용 및 전표출력번호는 데이터 확인 후 처리 */

        StringBuffer sb = new StringBuffer();
        sb.append(_cardNo);
        if(_cardNo.length() > 12)
        {
            sb.replace(8, 12, "****");
        }
        if (_cardNo.indexOf("=") > 0) {
            sb.replace(_cardNo.indexOf("="), _cardNo.indexOf("=") + 1, "*");
        }
        if(_cardNo.length() > 0)
        {
            sb.replace(_cardNo.length()-1, _cardNo.length(), "*");
        }

        //앱투앱인지를 체크
        if (CatPaymentSdk.Instance.mDBAppToApp)
        {
            if (_cardNo.length() > 13)  //현금영수증인지 아닌지를 체크
            {
                data.put("CardNo", sb.substring(0,8));
            }
            else
            {
                data.put("CardNo", sb.toString());
            }
        }
        else
        {
            data.put("CardNo", sb.toString());
        }


//        if (_cardNo.length() > 13) {
//            data.put("CardNo",_cardNo.substring(0,6));
//        } else {
//            data.put("CardNo",_cardNo);
//        }

        String _auNo = "", _tradeNo = "";
        try {
            _auNo =  new String(parseData.CutToSize(12), "euc-kr");//승인번호
            _tradeNo =  new String(parseData.CutToSize(20), "euc-kr");//코세스거래고유번호
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        _auNo = _auNo.replace(" ","");
        _tradeNo = _tradeNo.replace(" ","");
        data.put("AuNo",_auNo);
        data.put("TradeNo",_tradeNo);

        parseData.CutToSize(8);//거래종류명  //x
        String _cardKind = "", _ordCd = "", _ordNm = "", _inpCd = "", _inpNm = "", _mchNo = "", _ddcYn = "", _edcYn = "";
        try {
            _cardKind =  new String(parseData.CutToSize(1), "euc-kr");//카드 종류
            _ordCd =  new String(parseData.CutToSize(4), "euc-kr");//발급사코드
            _ordNm =  new String(parseData.CutToSize(12), "euc-kr");//발급사명
            _inpCd =  new String(parseData.CutToSize(4), "euc-kr");//매입사코드
            _inpNm =  new String(parseData.CutToSize(12), "euc-kr");//매입사명
            _mchNo =  new String(parseData.CutToSize(16), "euc-kr");//가맹점번호
            _ddcYn =  new String(parseData.CutToSize(1), "euc-kr");//DDC여부 1:Yes, 0:No
            _edcYn =  new String(parseData.CutToSize(1), "euc-kr");//EDC여부 1:Yes, 0:No
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("CardKind",_cardKind);
        data.put("OrdCd",_ordCd);
        data.put("OrdNm",_ordNm);
        data.put("InpCd",_inpCd);
        data.put("InpNm",_inpNm);
        data.put("MchNo",_mchNo);
        data.put("DCCYn",_ddcYn);
        data.put("EDCYn",_edcYn);

        parseData.CutToSize(1);//전표구분 //x
        parseData.CutToSize(4);//전표번호   //x
        parseData.CutToSize(10);//전표구분명  //x
        parseData.CutToSize(1);//KeyIn 여부    //x
        parseData.CutToSize(1);//DCC 업체구분 A : Alliex G :GCMC    //x
        parseData.CutToSize(1); //DCC 응답 여부 0: 일반 신용승인,2:DCC 승인  //x
        parseData.CutToSize(3);//자국통화코드   //x
        parseData.CutToSize(12);//자국통화금액 //x
        parseData.CutToSize(1);//자국통화 소수점단 위,결제예정 자국통화 소수점, 엔화, 베트남 통 0, 나머지 2  //x
        parseData.CutToSize(9);//환율응답  //x
        parseData.CutToSize(1);//환율의 소수점단위 - 7 ex) 20.07    //x
        parseData.CutToSize(9);//표시 목적을 위한 역 환율 ex) 000049808 //x
        parseData.CutToSize(1);//역 환율의 소수점단위 4 ex) 04.98  //x
        parseData.CutToSize(1);//"지수(승수) - 자국통화표현단위 “0” : 1단위 ex) USD  //x

        //ex) USD 1.00 = KRW xxxx.xxx,
        //“2” : 100 단위
        //ex) JPY 100 = KRW xxxx.xxx
        //ex) VND 100 = KRW xxxx.xxx 일반적으로 0 이 전송되며, JPY, VND 와 같은 100 단위 통화의 경우는 2
        //ex) 2007722 VND * 04.98 / 100 = 99984.5556 WON"
        parseData.CutToSize(8);//소수점을 포함하는 환율에 적용된 Markup percentage (이 필드만 소수점이 포함된 값)   //x

        //ex) 4.000000 - DCC 업체 수수료
        parseData.CutToSize(1);//Markuppercentage 표시단위    //x
        parseData.CutToSize(8);//GCMC 전용  //x
        parseData.CutToSize(1);//GCMC 전용 //x
        parseData.CutToSize(3);//GCMC 전용  //x
        parseData.CutToSize(12);//GCMC 전용 //x
        parseData.CutToSize(1);//GCMC 전용 //x
        parseData.CutToSize(20);//GCMC 전용   //x

        parseData.CutToSize(1);//영문전표출력여부 '1: 영문전표 출력, 그외 : 한글전표 출력   //x

        try {
            _tmp = new String(parseData.CutToSize(50), "euc-kr");//가맹점데이터
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        data.put("MchData",_tmp);
        parseData.CutToSize(20);//GCMC 전용   //x

        if (parseData.getlength() == 2) {
            return data;
        }

        //남은데이터가 맞지 않는다. 파싱이 잘못되었다.
        return null;
    }

    /// 현금 ic 수신 데이터 파싱 함수
    /// - Parameter _recv: <#_recv description#>
    private HashMap<String,String> ParsingresCashIC(byte[] _recv)
    {
        //데이터 사이즈 체크
        if (_recv.length < 6) {
            return null;
        }

        //데이터 시작이 stx가 아니면 취소
        if (_recv[0] != Command.STX) {
            return null;
        }

        KByteArray parseData = new KByteArray(_recv);   //데이터를 지워 가면서 작업을 해야 해서 임시로 변수 하나 지정
        //본격적인 파싱 시작
        HashMap<String,String> data = new HashMap<>();

        parseData.CutToSize(1); //STX 삭제
//        var rng = 4 //데이터를 짜르기 위한 범위
        String _tmp = "";
        try {
            _tmp = new String(parseData.CutToSize(4), "euc-kr");//명령코드 4byte
            //stringValue = new String(temp);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //TODO: 120 130 140 150 앱투앱일 경우
        if (CatPaymentSdk.Instance.mDBAppToApp)
        {
            if (_tmp.equals("G120"))
            {
                _tmp = (CatPaymentSdk.Instance.Cancel == false ? "A15":"A25");
                data.put("TrdType",_tmp); //명령코드 4byte
            }
            else if (_tmp.equals("G130"))
            {
                _tmp = (CatPaymentSdk.Instance.Cancel == false ? "B15":"B25");
                data.put("TrdType",_tmp); //명령코드 4byte
            }
        }
        else
        {
            data.put("TrdType",_tmp); //명령코드 4byte
        }

        try {
            _tmp = new String(parseData.CutToSize(3), "euc-kr");//거래전문번호 3byte
            //stringValue = new String(temp);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("ResponseNo",_tmp); //거래전문번호 3byte  //x

        try {
            _tmp = new String(parseData.CutToSize(10), "euc-kr"); //TID 10byte   //x
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("TermID",_tmp); //TID 10byte   //x

        String _trdDate = "", _ansCode = "", _message = "", _auNo = "", _tradeNo = "", _cardNo = "";
        try {
            _trdDate =  new String(parseData.CutToSize(12), "euc-kr");//거래일시 12byte Format:YYMMDDhhmmss -> yymmddhhmmss 12자리만 저장하기 위해서
            _ansCode =  new String(parseData.CutToSize(4), "euc-kr");//응답코드 정상응답(응답코드: 0000, 0001, 0002, 9901)
            _message =  new String(parseData.CutToSize(64), "euc-kr");//신용:앞 40Byte+뒤 40Byte Space
            _auNo =  new String(parseData.CutToSize(13), "euc-kr");//승인번호
            _tradeNo =  new String(parseData.CutToSize(20), "euc-kr");//코세스거래고유번호
            _cardNo =  new String(parseData.CutToSize(20), "euc-kr");//신용,은련,DCC: 출력시 사용될 Track2 정보,현금: 출력 시 사용될 신분확인번호 정보
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("TrdDate",_trdDate);
        data.put("AnsCode",_ansCode);
        data.put("Message",_message);
        data.put("AuNo",_auNo);
        data.put("TradeNo",_tradeNo);

        _cardNo = _cardNo.replace("-","");
        _cardNo = _cardNo.replace(" ","");
        if (_cardNo.length() > 13) {
            data.put("CardNo",_cardNo.substring(0,8));
        } else {
            data.put("CardNo",_cardNo);
        }

        parseData.CutToSize(8); //거래종류명  //x

        String _mchNo = "", _ordCd = "", _ordNm = "", _inpCd = "", _inpNm = "", _serveMoney = "", _mchData = "";
        try {
            _mchNo =  new String(parseData.CutToSize(16), "euc-kr");//가맹점번호
            _ordCd =  new String(parseData.CutToSize(7), "euc-kr");//발급사코드
            _ordNm =  new String(parseData.CutToSize(16), "euc-kr");//발급사명
            _inpCd =  new String(parseData.CutToSize(7), "euc-kr");//매입사코드
            _inpNm =  new String(parseData.CutToSize(16), "euc-kr");//매입사명
            _serveMoney =  new String(parseData.CutToSize(25), "euc-kr");//잔액 -> 데이터가 이상해서 뒤에서부터 잘라서 총 12자리만 실어보냄
            _mchData =  new String(parseData.CutToSize(64), "euc-kr");//가맹점데이터
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("MchNo",_mchNo);
        data.put("OrdCd",_ordCd);
        data.put("OrdNm",_ordNm);
        data.put("InpCd",_inpCd);
        data.put("InpNm",_inpNm);
        data.put("ServeMoney",_serveMoney);
        data.put("MchData",_mchData);

        parseData.CutToSize(12); //거래금액 12byte   //x
        parseData.CutToSize(12); //세금 12byte  //x
        parseData.CutToSize(12); //봉사료 12byte    //x
        parseData.CutToSize(12); //비과세 12byte    //x

        if (parseData.getlength() == 2) {
        return data;
        }

        //남은데이터가 맞지 않는다. 파싱이 잘못되었다.
        return null;
    }

    /// 신용 결제 수신 데이터 DB에 추가 하는 함수
    /// - Parameter _recv: <#_recv description#>
    private void InsertDBTradeData_TypeCrdit(HashMap<String,String> _recv, String _TradeType) {
        String _신용현금 = sqliteDbSdk.TradeMethod.CAT_Credit;
        String _현금영수증타겟 = sqliteDbSdk.TradeMethod.NULL;
        String _현금영수증발급형태 = sqliteDbSdk.TradeMethod.NULL;
        String _현금발급번호 = "";
        String _카드번호 = "";
        String _취소여부 = sqliteDbSdk.TradeMethod.NoCancel;
        switch (_recv.get("TrdType")) {
            case "G120":
            case "G140":
                _신용현금 = sqliteDbSdk.TradeMethod.CAT_Credit;
                _카드번호 = MarkingCardNumber(_recv.get("CardNo"));
                break;
            case "G130":
                _신용현금 = sqliteDbSdk.TradeMethod.CAT_Cash;
                _현금영수증타겟 = getCashTarget(Integer.parseInt(CatPaymentSdk.Instance.pb));
                if (_recv.get("CardNo").length() > 8) {
                    _현금영수증발급형태 = sqliteDbSdk.TradeMethod.CashDirect;
                } else {
                    _현금영수증발급형태 = sqliteDbSdk.TradeMethod.CashMs;
                }
//            _현금영수증발급형태 = define.TradeMethod.NULL
                _현금발급번호 = MarkingCardNumber(_recv.get("CardNo"));
                break;
//            case "G140":
//                _신용현금 = sqliteDbSdk.TradeMethod.CAT_Credit;
//                _카드번호 = MarkingCardNumber(_recv.get("CardNo"));
//                break;
//            case "T180":
//                _신용현금 = sqliteDbSdk.TradeMethod.CAT_App;
//                _카드번호 = MarkingCardNumber(_recv.get("CardNo"));
//            _카드번호 = CatSdk.instance.AppCard
//                break;
            default:
                break;
        }

        if (!(CatPaymentSdk.Instance.OriAuDate.replace(" ", "")).equals("")) {
            _취소여부 = sqliteDbSdk.TradeMethod.Cancel;
        }


        if (CatPaymentSdk.Instance.mDBAppToApp) {
            mRecv = _recv;
            if (_TradeType == "Credit") {
                mCatNetworkConnectListener.onState(0, true, "Credit");
            } else if (_TradeType == "CashIC") {
                mCatNetworkConnectListener.onState(0, true, "CashIC");
            } else if (_TradeType == "CashRecipt") {
                mCatNetworkConnectListener.onState(0, true, "CashRecipt");
            }

            return;
        }


        if (_recv.get("CardKind").equals("3") || _recv.get("CardKind").equals("4")) {
            mPosSdk.setSqliteDB_InsertTradeData(
                    _recv.get("TermID"),
                    CatPaymentSdk.Instance.mStoreName,
                    CatPaymentSdk.Instance.mStoreAddr,
                    CatPaymentSdk.Instance.mStoreNumber,
                    CatPaymentSdk.Instance.mStorePhone,
                    CatPaymentSdk.Instance.mStoreOwner,
                    _신용현금,
                    _취소여부,
                    Integer.parseInt(CatPaymentSdk.Instance.Money),
                    _recv.get("Message"),
                    Integer.parseInt(CatPaymentSdk.Instance.Tax),
                    Integer.parseInt(CatPaymentSdk.Instance.Svc),
                    Integer.parseInt(CatPaymentSdk.Instance.Txf),
                    Integer.parseInt((CatPaymentSdk.Instance.InstallMent.replace(" ", "")).equals("")
                            ? "0" : CatPaymentSdk.Instance.InstallMent.replace(" ", "")),
                    _현금영수증타겟,
                    _현금영수증발급형태,
                    _현금발급번호,
                    _카드번호,
                    _recv.get("CardKind"),
                    _recv.get("InpNm").replace(" ", ""),
                    _recv.get("OrdNm").replace(" ", ""),
                    _recv.get("MchNo").replace(" ", ""),
                    _recv.get("TrdDate").replace(" ", ""),
                    CatPaymentSdk.Instance.OriAuDate.replace(" ", ""),
                    _recv.get("AuNo").replace(" ", ""),
                    CatPaymentSdk.Instance.OriAuNo.replace(" ", ""),
                    _recv.get("TradeNo").replace(" ", ""), _recv.get("Message"),
                    "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "");
        } else {
            mPosSdk.setSqliteDB_InsertTradeData(
                    _recv.get("TermID"),
                    CatPaymentSdk.Instance.mStoreName,
                    CatPaymentSdk.Instance.mStoreAddr,
                    CatPaymentSdk.Instance.mStoreNumber,
                    CatPaymentSdk.Instance.mStorePhone,
                    CatPaymentSdk.Instance.mStoreOwner,
                    _신용현금,
                    _취소여부,
                    Integer.parseInt(CatPaymentSdk.Instance.Money),
                    "",
                    Integer.parseInt(CatPaymentSdk.Instance.Tax),
                    Integer.parseInt(CatPaymentSdk.Instance.Svc),
                    Integer.parseInt(CatPaymentSdk.Instance.Txf),
                    Integer.parseInt((CatPaymentSdk.Instance.InstallMent.replace(" ", "")).equals("")
                            ? "0" : CatPaymentSdk.Instance.InstallMent.replace(" ", "")),
                    _현금영수증타겟,
                    _현금영수증발급형태,
                    _현금발급번호,
                    _카드번호,
                    _recv.get("CardKind"),
                    _recv.get("InpNm").replace(" ", ""),
                    _recv.get("OrdNm").replace(" ", ""),
                    _recv.get("MchNo").replace(" ", ""),
                    _recv.get("TrdDate").replace(" ", ""),
                    CatPaymentSdk.Instance.OriAuDate.replace(" ", ""),
                    _recv.get("AuNo").replace(" ", ""),
                    CatPaymentSdk.Instance.OriAuNo.replace(" ", ""),
                    _recv.get("TradeNo").replace(" ", ""), _recv.get("Message"),
                    "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "");
        }

        mRecv = _recv;
        if (_TradeType == "Credit") {
            mCatNetworkConnectListener.onState(0, true, "Credit");
        } else if (_TradeType == "CashIC") {
            mCatNetworkConnectListener.onState(0, true, "CashIC");
        } else if (_TradeType == "CashRecipt") {
            mCatNetworkConnectListener.onState(0, true, "CashRecipt");
        }
    }

    private void InsertDBTradeData_TypeCashIC(HashMap<String,String> _recv, String _TradeType)
    {
        if (CatPaymentSdk.Instance.mDBAppToApp) {
            mRecv = _recv;
            if (_TradeType == "Credit") {
                mCatNetworkConnectListener.onState(0, true, "Credit");
            } else if (_TradeType == "CashIC") {
                mCatNetworkConnectListener.onState(0, true, "CashIC");
            } else if (_TradeType == "CashRecipt") {
                mCatNetworkConnectListener.onState(0, true, "CashRecipt");
            }

            return;
        }


        String _현금영수증타겟 = sqliteDbSdk.TradeMethod.NULL;
        String _현금영수증발급형태 = sqliteDbSdk.TradeMethod.NULL;
        String _현금발급번호 = "";
//        _현금영수증타겟 = getCashTarget(대상입력: Int(CatSdk.instance.pb) ?? 0)
        if (_recv.get("CardNo").length() > 8) {
            _현금영수증발급형태 = sqliteDbSdk.TradeMethod.CashDirect;
        } else {
            _현금영수증발급형태 = sqliteDbSdk.TradeMethod.CashMs;
        }
        _현금발급번호 = MarkingCardNumber(_recv.get("CardNo"));

        switch (_recv.get("ResponseNo")) {
        case "C15":
            mPosSdk.setSqliteDB_InsertTradeData(
                    _recv.get("TermID"),
                    CatPaymentSdk.Instance.mStoreName == null ? "":CatPaymentSdk.Instance.mStoreName,
                    CatPaymentSdk.Instance.mStoreAddr == null ? "":CatPaymentSdk.Instance.mStoreAddr,
                    CatPaymentSdk.Instance.mStoreNumber == null ? "":CatPaymentSdk.Instance.mStoreNumber,
                    CatPaymentSdk.Instance.mStorePhone == null ? "":CatPaymentSdk.Instance.mStorePhone,
                    CatPaymentSdk.Instance.mStoreOwner == null ? "":CatPaymentSdk.Instance.mStoreOwner,
                    sqliteDbSdk.TradeMethod.CAT_CashIC,
                    sqliteDbSdk.TradeMethod.NoCancel,
                    Integer.parseInt(CatPaymentSdk.Instance.Money),
                    "",
                    Integer.parseInt(CatPaymentSdk.Instance.Tax),
                    Integer.parseInt(CatPaymentSdk.Instance.Svc),
                    Integer.parseInt(CatPaymentSdk.Instance.Txf),
                    Integer.parseInt((CatPaymentSdk.Instance.InstallMent.replace(" ", "")).equals("")
                            ? "0" : CatPaymentSdk.Instance.InstallMent.replace(" ", "")),
                    sqliteDbSdk.TradeMethod.NULL,
                    _현금영수증발급형태,
                    _현금발급번호,
                    "",
                    "",
                    _recv.get("InpNm").replace(" ", ""),
                    _recv.get("OrdNm").replace(" ", ""),
                    _recv.get("MchNo").replace(" ", ""),
                    _recv.get("TrdDate").replace(" ", ""),
                    CatPaymentSdk.Instance.AuDate.replace(" ", ""),
                    _recv.get("AuNo").replace(" ", ""),
                    CatPaymentSdk.Instance.AuNo.replace(" ", ""),
                    _recv.get("TradeNo").replace(" ", ""), _recv.get("Message"),
                    "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "");

            break;
        case "C25":
            mPosSdk.setSqliteDB_InsertTradeData(
                    _recv.get("TermID"),
                    CatPaymentSdk.Instance.mStoreName == null ? "":CatPaymentSdk.Instance.mStoreName,
                    CatPaymentSdk.Instance.mStoreAddr == null ? "":CatPaymentSdk.Instance.mStoreAddr,
                    CatPaymentSdk.Instance.mStoreNumber == null ? "":CatPaymentSdk.Instance.mStoreNumber,
                    CatPaymentSdk.Instance.mStorePhone == null ? "":CatPaymentSdk.Instance.mStorePhone,
                    CatPaymentSdk.Instance.mStoreOwner == null ? "":CatPaymentSdk.Instance.mStoreOwner,
                    sqliteDbSdk.TradeMethod.CAT_CashIC,
                    sqliteDbSdk.TradeMethod.Cancel,
                    Integer.parseInt(CatPaymentSdk.Instance.Money),
                    "",
                    Integer.parseInt(CatPaymentSdk.Instance.Tax),
                    Integer.parseInt(CatPaymentSdk.Instance.Svc),
                    Integer.parseInt(CatPaymentSdk.Instance.Txf),
                    Integer.parseInt((CatPaymentSdk.Instance.InstallMent.replace(" ", "")).equals("")
                            ? "0" : CatPaymentSdk.Instance.InstallMent.replace(" ", "")),
                    sqliteDbSdk.TradeMethod.NULL,
                    _현금영수증발급형태,
                    _현금발급번호,
                    "",
                    "",
                    _recv.get("InpNm").replace(" ", ""),
                    _recv.get("OrdNm").replace(" ", ""),
                    _recv.get("MchNo").replace(" ", ""),
                    _recv.get("TrdDate").replace(" ", ""),
                    CatPaymentSdk.Instance.AuDate.replace(" ", ""),
                    _recv.get("AuNo").replace(" ", ""),
                    CatPaymentSdk.Instance.AuNo.replace(" ", ""),
                    _recv.get("TradeNo").replace(" ", ""), _recv.get("Message"),
                    "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "");

            break;
            case "C35":
            case "C45":
            case "C55":
                mRecv = _recv;
                if (_TradeType == "Credit") {
                    mCatNetworkConnectListener.onState(0, true, "Credit");
                } else if (_TradeType == "CashIC") {
                    mCatNetworkConnectListener.onState(0, true, "CashIC");
                } else if (_TradeType == "CashRecipt") {
                    mCatNetworkConnectListener.onState(0, true, "CashRecipt");
                }
                return;

            default:
                break;
        }

        mRecv = _recv;
        if (_TradeType == "Credit") {
            mCatNetworkConnectListener.onState(0, true, "Credit");
        } else if (_TradeType == "CashIC") {
            mCatNetworkConnectListener.onState(0, true, "CashIC");
        } else if (_TradeType == "CashRecipt") {
            mCatNetworkConnectListener.onState(0, true, "CashRecipt");
        }

    }

    private void InsertDBTradeData_TypeEasy(HashMap<String,String> _recv, String _TradeType)
    {
        String _신용현금 = sqliteDbSdk.TradeMethod.CAT_Credit;
        String _현금영수증타겟 = sqliteDbSdk.TradeMethod.NULL;
        String _현금영수증발급형태 = sqliteDbSdk.TradeMethod.NULL;
        String _현금발급번호 = "";
        String _카드번호 = "";
        String _취소여부 = sqliteDbSdk.TradeMethod.NoCancel;

        _현금발급번호 = MarkingCardNumber(_recv.get("CardNo"));

        if(_recv.get("QrKind").equals("AP"))
        {
            _신용현금 = sqliteDbSdk.TradeMethod.CAT_App;
        } else if(_recv.get("QrKind").equals("ZP"))
        {
            _신용현금 = sqliteDbSdk.TradeMethod.CAT_Zero;
        } else if(_recv.get("QrKind").equals("KP"))
        {
            _신용현금 = sqliteDbSdk.TradeMethod.CAT_Kakao;
        } else if(_recv.get("QrKind").equals("AL"))
        {
            _신용현금 = sqliteDbSdk.TradeMethod.CAT_Ali;
        } else if(_recv.get("QrKind").equals("WC"))
        {
            _신용현금 = sqliteDbSdk.TradeMethod.CAT_We;
        }

        _카드번호 = MarkingCardNumber(_recv.get("CardNo"));

        if (CatPaymentSdk.Instance.mDBAppToApp) {
            mRecv = _recv;
            mCatNetworkConnectListener.onState(0, true, "Easy");
            return;
        }

        switch (_recv.get("ResponseNo")) {
            case "E15":
                mPosSdk.setSqliteDB_InsertTradeData(
                        _recv.get("TermID"),
                        CatPaymentSdk.Instance.mStoreName == null ? "":CatPaymentSdk.Instance.mStoreName,
                        CatPaymentSdk.Instance.mStoreAddr == null ? "":CatPaymentSdk.Instance.mStoreAddr,
                        CatPaymentSdk.Instance.mStoreNumber == null ? "":CatPaymentSdk.Instance.mStoreNumber,
                        CatPaymentSdk.Instance.mStorePhone == null ? "":CatPaymentSdk.Instance.mStorePhone,
                        CatPaymentSdk.Instance.mStoreOwner == null ? "":CatPaymentSdk.Instance.mStoreOwner,
                        _신용현금,
                        sqliteDbSdk.TradeMethod.NoCancel,
                        Integer.parseInt(CatPaymentSdk.Instance.Money),
                        "",
                        Integer.parseInt(CatPaymentSdk.Instance.Tax),
                        Integer.parseInt(CatPaymentSdk.Instance.Svc),
                        Integer.parseInt(CatPaymentSdk.Instance.Txf),
                        Integer.parseInt((CatPaymentSdk.Instance.InstallMent.replace(" ", "")).equals("")
                                ? "0" : CatPaymentSdk.Instance.InstallMent.replace(" ", "")),
                        sqliteDbSdk.TradeMethod.NULL,
                        _현금영수증발급형태,
                        _현금발급번호,
                        _카드번호,
                        _recv.get("CardKind").replace(" ", ""),
                        _recv.get("InpNm").replace(" ", ""),
                        _recv.get("OrdNm").replace(" ", ""),
                        _recv.get("MchNo").replace(" ", ""),
                        _recv.get("TrdDate").replace(" ", ""),
                        CatPaymentSdk.Instance.AuDate.replace(" ", ""),
                        _recv.get("AuNo").replace(" ", ""),
                        CatPaymentSdk.Instance.AuNo.replace(" ", ""),
                        _recv.get("TradeNo").replace(" ", ""), _recv.get("Message"),
                        "", _recv.get("AuthType").replace(" ", ""),
                        "", _recv.get("DisAmt").replace(" ", ""),
                        "", "", "", "", "", "",
                        _recv.get("TradeNo").replace(" ", ""), "",
                        "", "", _recv.get("ChargeAmt").replace(" ", ""),
                        _recv.get("RefundAmt").replace(" ", ""));
                break;
            case "E25":
                mPosSdk.setSqliteDB_InsertTradeData(
                        _recv.get("TermID"),
                        CatPaymentSdk.Instance.mStoreName == null ? "":CatPaymentSdk.Instance.mStoreName,
                        CatPaymentSdk.Instance.mStoreAddr == null ? "":CatPaymentSdk.Instance.mStoreAddr,
                        CatPaymentSdk.Instance.mStoreNumber == null ? "":CatPaymentSdk.Instance.mStoreNumber,
                        CatPaymentSdk.Instance.mStorePhone == null ? "":CatPaymentSdk.Instance.mStorePhone,
                        CatPaymentSdk.Instance.mStoreOwner == null ? "":CatPaymentSdk.Instance.mStoreOwner,
                        _신용현금,
                        sqliteDbSdk.TradeMethod.Cancel,
                        Integer.parseInt(CatPaymentSdk.Instance.Money),
                        "",
                        Integer.parseInt(CatPaymentSdk.Instance.Tax),
                        Integer.parseInt(CatPaymentSdk.Instance.Svc),
                        Integer.parseInt(CatPaymentSdk.Instance.Txf),
                        Integer.parseInt((CatPaymentSdk.Instance.InstallMent.replace(" ", "")).equals("")
                                ? "0" : CatPaymentSdk.Instance.InstallMent.replace(" ", "")),
                        sqliteDbSdk.TradeMethod.NULL,
                        _현금영수증발급형태,
                        _현금발급번호,
                        _카드번호,
                        _recv.get("CardKind").replace(" ", ""),
                        _recv.get("InpNm").replace(" ", ""),
                        _recv.get("OrdNm").replace(" ", ""),
                        _recv.get("MchNo").replace(" ", ""),
                        _recv.get("TrdDate").replace(" ", ""),
                        CatPaymentSdk.Instance.AuDate.replace(" ", ""),
                        _recv.get("AuNo").replace(" ", ""),
                        CatPaymentSdk.Instance.AuNo.replace(" ", ""),
                        _recv.get("TradeNo").replace(" ", ""), _recv.get("Message"),
                        "", _recv.get("AuthType").replace(" ", ""),
                        "", _recv.get("DisAmt").replace(" ", ""),
                        "", "", "", "", "", "",
                        _recv.get("TradeNo").replace(" ", ""), "",
                        "", "", _recv.get("ChargeAmt").replace(" ", ""),
                        _recv.get("RefundAmt").replace(" ", ""));
                break;
            case "E35":
                mRecv = _recv;
                mCatNetworkConnectListener.onState(0, true, "Easy");
                return;

            default:
                break;
        }

        mRecv = _recv;
        mCatNetworkConnectListener.onState(0, true, "Easy");
    }



    private String MarkingCardNumber(String _cardNum)
    {
//        if(_cardNum.length() > 6) {
//            return _cardNum;
//        }
//        String cardNo = "";
//        cardNo = _cardNum.substring(0,4);
//        cardNo += "-";
//        cardNo += _cardNum.substring(4);
//        cardNo += "**-****-****";
//        return cardNo;
        return _cardNum;
    }

    private String getCashTarget(int _Target)
    {
        String tar = sqliteDbSdk.TradeMethod.NULL;
        switch (_Target) {
            case 1:
                tar = sqliteDbSdk.TradeMethod.CashPrivate;
                break;
            case 2:
                tar = sqliteDbSdk.TradeMethod.CashBusiness;
                break;
            case 3:
                tar = sqliteDbSdk.TradeMethod.CashSelf;
                break;
            default:
                tar = sqliteDbSdk.TradeMethod.NULL;
                break;
        }

        return tar;
    }

    /**
     스캔한 데이터를 파싱해서 제로/카카오/emv 등을 구분한다
     */
    private String Scan_Data_Parser(String _scan)
    {
        String returnData = "";
        if(_scan == null || _scan.equals("") || _scan.length() < 2)
        {
            return returnData;
        }
        if (_scan.length()>=7 && _scan.substring(0,7).equals("hQVDUFY")) {
            //EMV QR
            returnData = Constants.EasyPayMethod.EMV.toString();
        } else if (_scan.length()>=6 &&_scan.substring(0,6).equals("281006")) {
            //카카오페이
            returnData = Constants.EasyPayMethod.Kakao.toString();
        } else if (_scan.length()>=6 &&_scan.substring(0,6).equals("800088")) {
            //제로페이 Barcode
            returnData = Constants.EasyPayMethod.Zero_Bar.toString();
        } else if (_scan.substring(0,2).equals("3-")) {
            //제로페이 QRcode
            returnData = Constants.EasyPayMethod.Zero_Qr.toString();
        } else {
            if (_scan.substring(0,2).equals("11") || _scan.substring(0,2).equals("12") ||
                    _scan.substring(0,2).equals("13") || _scan.substring(0,2).equals("14") ||
                    _scan.substring(0,2).equals("15") || _scan.substring(0,2).equals("10"))  {
                //위쳇페이
                returnData = Constants.EasyPayMethod.Wechat.toString();
            } else if (_scan.substring(0,2).equals("25") || _scan.substring(0,2).equals("26") ||
                    _scan.substring(0,2).equals("27") || _scan.substring(0,2).equals("28") ||
                    _scan.substring(0,2).equals("29") || _scan.substring(0,2).equals("30")) {
                //알리페이
                returnData = Constants.EasyPayMethod.Ali.toString();
            } else {
                if (_scan.length() == 21) {
                    //APP 카드
                    returnData = Constants.EasyPayMethod.App_Card.toString();
                }
            }
        }

        return returnData;
    }
}
