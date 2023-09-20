package com.koces.androidpos.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.koces.androidpos.AppToAppActivity;
import com.koces.androidpos.BaseActivity;
import com.koces.androidpos.CashLoadingActivity;
import com.koces.androidpos.CreditLoadingActivity;
import com.koces.androidpos.R;
import com.koces.androidpos.SignPadActivity;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.ble.bleSdkInterface;
import com.koces.androidpos.sdk.db.sqliteDbSdk;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.TcpInterface;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class BlePaymentSdk {
    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 결제관련 기능을 수행하기 위해 사용한다 */
    private KocesPosSdk mPosSdk;
    /* 결제관련SDK를 수행하는 위치 Context */
    private Context mCtx;
    /* ble */
    private bleSdk mBleSdk;
    /** 신용=1/현금=2 */
    private int TradeType = 0;
    /** 총거래금액 - 단말기로 보내는 금액(mMoney + mTax + mServiceCharge) */
    private int mConvertMoney;
    /** 거래금액 */
    private int mMoney;
    /** 할부개월 */
    private int mInstallment;
    /** 세금 */
    private String mTax;
    /** 봉사료 */
    private String mServiceCharge;
    /** 면세료 */
    private String mTaxfree;
    /** 개인/법인 구분(신용X) 1:개인 2:법인 3:자진발급 4:원천 */
    private int mPB;
    /** tid */
    private String mTid;
    private String mStoreName;
    private String mStoreAddr;
    private String mStoreNumber;
    private String mStorePhone;
    private String mStoreOwner;

    /** (앱투앱으로 실행시 앱투앱에서 받는다) 서명데이터 결과값을 받을 시 결과 키값 */
    private int REQUEST_SIGNPAD = 10001;
    /** UI화면에 표시 또는 APP TO APP 데이터 전달용 HashMap 결과 데이터 */
    HashMap<String,String> sendData;
    /** ic fallbackk 처리할때 폴백인지 아닌지 미리체크 */
    private boolean isFallBack = false;

    /** cancel 관련 정보  */
    String mCancelInfo = "";
    String mCancelReason = "";
    String mInputMethod = "";
    String mPtCardCode = "";
    String mPtAcceptNumber = "";
    String mKocesTradeCode = "";
    String mBusinessData = "";
    String mBangi = "";
    String mICCancelInfo = "";
    String mICType = "";
    String mICInputMethod = "";
    String mICKocesTranUniqueNum = "";
    String mICPassword = "";
    String mCompCode ="";
    /** 망취소 발생시 카운트함 -> 현금쪽은 망취소 발생하여 해당 내역을 다시 수신하면 거래성공으로 간주해버리기때문에 */
    int mEotCancel = 0;

    /** 거래 시 사용되는 데이터 */
    byte[] mTmicno; byte[] mEncryptInfo; byte[] mKsn_track2data; String mEMVTradeType; byte[] mIcreqData;
    String mFallbackreason=""; String mUnionPasswd=""; String mMchdata=""; String mCodeVersion = ""; byte[] mCashTrack;
    byte[] mCashTrack2data;
    /** 2차제너레이션 시 사용되는 데이터 */
    byte[] mARD; byte[] mIAD; byte[] mIS;
    /** 같은 커맨드가 2회연속 호출되는 경우를 방지 */
    private static byte LASTCOMAND = (byte)0x00;
    /** ApptoAppActivity 또는 PaymentActivity 로 데이터를 보내기 위한 리스너 */
    private SerialInterface.PaymentListener mPaymentListener;
    private bleSdkInterface.ResDataListener mbleDataListener;
    /** 앱투앱으로 결제처리한건지 체크 */
    boolean mAppToApp = false;
    /** 폴백사용유무 0=사용 1=미사용 */
    String mFbYn = "0";

    /** 원거래일자. 취소시 원거래일자 항목을 삽입하기 위해 사용한다. 해당 데이터는 프린트시 출력에 표시하기 위한 내용이다 */
    String mOriAudate = "";

    /** 원거래일자. 취소시 거래내역을 업데이트를 위한 비교구반자 들 중 하나 */
    String mOriAuNum = "";

    //카드리딩 딜레이 => 이값을 줄일것 2000 => 500
    private int mReadingDelay = 500;

    //재요청이거나 리딩종료 딜레이 => 이값을 줄일것 1000 => 500
    private int mReDelay = 500;

    public BlePaymentSdk(int _tradeType,SerialInterface.PaymentListener _PaymentListener)
        {
            TradeType = _tradeType;
            mPosSdk = KocesPosSdk.getInstance();
            mBleSdk = bleSdk.getInstance();
            mPaymentListener = _PaymentListener;
            Clear();
        }

        /** 사용되는 변수들 초기화 */
    public void Clear()
        {
            mConvertMoney = 0;
            mMoney = 0;
            mInstallment = 0;
            mPB = 0;
            mTax = "";
            mServiceCharge = "";
            mTaxfree ="";
            mCancelInfo = "";
            mCancelReason = "";
            mInputMethod = "";
            mPtCardCode = "";
            mPtAcceptNumber = "";
            mBusinessData = "";
            mBangi = "";
            mKocesTradeCode = "";
            mICCancelInfo = "";
            mICType = "";
            mICInputMethod = "";
            mICKocesTranUniqueNum = "";
            mICPassword = "";
            mAppToApp = false;

            mTmicno =null; mEncryptInfo=null; mKsn_track2data=null;  mIcreqData=null;
            mFallbackreason=""; Setting.g_sDigSignInfo = ""; mUnionPasswd = ""; mEMVTradeType="";
            mMchdata = "";  mCompCode =""; mCodeVersion=""; mCashTrack=null; mCashTrack2data= null;

            mARD = null; mIAD = null; mIS = null;

            mFbYn = "0";
            mOriAudate = "";
            mOriAuNum = "";

            mStoreName = "";
            mStoreAddr = "";
            mStoreNumber = "";
            mStorePhone = "";
            mStoreOwner = "";
        }

    /** 사용하지 않음 */
    public void Reset()
    {

    }

    /**
     * 신용거래(단말기에 요청)
     * @param _ctx 현재엑티비티
     * @param _Tid tid
     * @param _money 거래금액
     * @param _tax 세금
     * @param _serviceCharge 봉사료
     * @param _taxfree 면세료
     * @param installment 할부개월
     * @param _cancelInfo 취소정보
     * @param _mchData 가맹점데이터
     * @param _kocesTradeCode 코세스거래고유번호
     * @param _compCode 업체코드
     * @param _AppToApp 앱투앱인지 아닌지체크
     * @param _fbYn 폴백사용유무
     */
    public void CreditIC(Context _ctx,String _Tid,String _money,int _tax,int _serviceCharge,int _taxfree,
                         String installment, String _oriDate, String _cancelInfo, String _mchData,
                         String _kocesTradeCode, String _compCode, boolean _AppToApp, String _fbYn,
                         String _storeName, String _storeAddr, String _storeNumber, String _storePhone, String _storeOwner)
    {
        mCtx = _ctx;
        mTid = _Tid;
        mStoreName = _storeName;
        mStoreAddr = _storeAddr;
        mStoreNumber = _storeNumber;
        mStorePhone = _storePhone;
        mStoreOwner = _storeOwner;
        mICCancelInfo = _cancelInfo;
        mTax = String.valueOf(_tax);
        mServiceCharge = String.valueOf(_serviceCharge);
        mTaxfree = String.valueOf(_taxfree);
        mMchdata = _mchData;
        mICKocesTranUniqueNum = _kocesTradeCode;
        mCompCode = _compCode;
        mAppToApp = _AppToApp==true ? true:false;
        mFbYn = _fbYn;

        if (!_cancelInfo.equals("")) {
            mOriAudate = _oriDate;
            String _tmpAunum = _cancelInfo.replace(_oriDate, "");
            mOriAuNum = _tmpAunum.substring(1);
        }

        if (!_money.equals("") && !_money.equals("          ")) //금액이 이상한 경우
        {
            String _icMoney = "";
            for(int i=0; i<_money.length(); i++)
            {
                if(!_money.substring(i,i+1).equals(" "))
                    _icMoney += _money.substring(i,i+1);
            }
            mMoney = Integer.valueOf(_icMoney);

            if (mMoney < 0 || mMoney > 900000000)  //금액은 최대 9억을 넘을 수 없다.
            {
                mPaymentListener.result(_ctx.getResources().getString(R.string.error_input_wrong_Payment_total_money),"ERROR",new HashMap<>());
                return;
            }
        }
        else
        {
            mPaymentListener.result(_ctx.getResources().getString(R.string.error_input_wrong_Payment_total_money),"ERROR",new HashMap<>());
            return;
        }
        if (installment!= null && !installment.equals("")) //할부금액이 이상한 경우
        {
            mInstallment = Integer.valueOf(installment);
            {
                if(mInstallment==0){}
                else
                {
                    if (mInstallment < 2 || mInstallment > 99) //할부의 경우에는 최대 99개월을 넘길 수 없다.
                    {
                        mPaymentListener.result(_ctx.getResources().getString(R.string.error_input_wrong_Payment_installment), "ERROR", new HashMap<>());
                        return;
                    }
                }
            }
        }

        mConvertMoney = mMoney + _tax + _serviceCharge;
        if (!_cancelInfo.equals(""))
        {
            mMoney = mMoney + _tax + _serviceCharge;
            mTax = "0";
            mServiceCharge = "0";
        }



        final String CovertMoney = Utils.StringAlignrightzero(String.valueOf(mConvertMoney),10);

        mPosSdk.__BLEPosinit("99",null);   //신용거래 전 장치를 한번 초기화 해준다

        new Thread(new Runnable() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //"01"신용ic "02"은련ic
                        LASTCOMAND = Command.CMD_IC_REQ;
                        mICType = "01";
                        Setting.ICResponseDeviceType = 0;   //IC응답장비초기화. 거래요청 하고 응답한 장비가 무엇인지셋팅
                        /* 신용거래요청(카드단말기) */
                        mPosSdk.__Bleicreq(mICType,mTid , CovertMoney, Utils.getDate("yyyyMMddHHmmss"),
                                "0", "0", "0000","0", "00",
                                "06", Constants.WORKING_KEY_INDEX, Constants.WORKING_KEY,
                                Constants.CASHIC_RANDOM_NUMBER,res -> {
                                    Log.d("kim.jy","신용거래 카드 요청 데이터 :" + Utils.bytesToHex(res));
                                    Ble_ParsingData(res);
                                });
                    }
                },mReadingDelay);
            }
        }).start();
    }

    /**
     * BLE 로부터 받은 신용데이터의 커맨드에 따라 분류를 보낸다
     */
    private void Ble_ParsingData(byte[] res)
    {
        if(LASTCOMAND==res[3]) //신용/현금 시 같은 명령이 연속적으로 올라오는 경우가 있어서 이를 막는다
        {
            return;
        }
        Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(res);

        switch (protocolInfo.Command) {
            case Command.ACK:
                break;
            case Command.CMD_IC_RES:
                if(TradeType==1)    //신용=1/현금=2/현금ic=3 구분
                {
                    if(mICPassword.equals("01")){
                        mICPassword = "";
                        HideMessageBox();
                        mPaymentListener.result("비밀번호오류","ERROR",new HashMap<>());
                    }
                    else
                    {
                        Setting.g_paymentState = true;
                        Res_Credit(res);
                    }
                }
                else if(TradeType==2)
                {
                    Res_CashRecipt(res,false);
                }
//                                            else if(TradeType==2)
//                                            {
//                                                Res_CashRecipt(res,false);
//                                            }
//                                            else if(TradeType==3)
//                                            {
//                                                Res_CashIC(res);
//                                            }
                break;
            case Command.CMD_IC_RESULT_RES:
                Res_EmvComplete(res);
                break;
            case Command.ESC:
                HideMessageBox();
                mPaymentListener.result("단말기 코드 오류. 키갱신 요망.","ERROR",new HashMap<>());

                DeviceReset();
                break;
            case Command.NAK:
                HideMessageBox();
                ICTradeCancel();
                break;
            default:
                break;
        }
    }

    /**
     * LogFile 클래스를 통해서 로컬에 로그를 기록하는 함수
     * @param _title
     * @param _time
     * @param _Contents
     */
    private void cout(String _title,String _time, String _Contents)
    {
        if(mPosSdk.mFocusActivity!=null) {
            if (_title != null && !_title.equals("")) {
                mPosSdk.mFocusActivity.WriteLogFile("\n");
                mPosSdk.mFocusActivity.WriteLogFile("<" + _title + ">\n");
            }

            if (_time != null && !_time.equals("")) {
                mPosSdk.mFocusActivity.WriteLogFile("[" + _time + "]  ");
            }

            if (_Contents != null && !_Contents.equals("")) {
                mPosSdk.mFocusActivity.WriteLogFile(_Contents);
                mPosSdk.mFocusActivity.WriteLogFile("\n");
            }
            else
            {
                mPosSdk.mFocusActivity.WriteLogFile("\n");
            }
        }
    }

    /**
     * 단말기로부터 받은 신용거래 정보를 서버로 보낸다
     * @param _res
     */
    private void Res_Credit(byte[] _res)
    {
        final byte[] res = _res;
        String mLog = Utils.bytesToHex_0xType(_res);
        cout("테스트용 Device => App : ",Utils.getDate("yyyyMMddHHmmss"),mLog);
        Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(res);
        if (protocolInfo.Command != Command.CMD_IC_RES) {
            return;
        }

        KByteArray  b = new KByteArray(protocolInfo.Contents);

        Log.d("BLE_Payment",Utils.bytesToHex_0xType(res));
//        byte[] TmlcNo = b.CutToSize(32);
//        TmlcNo[31] = (byte) 0x20;
        byte[] TmlcNo = new byte[32];
        System.arraycopy( b.CutToSize(16),0,TmlcNo,0,16);
        //단말 인증 번호가 32개가 올라오지만 그 중에서 앞 16자리만 사용하고 나머지는 APPID로 채운다.
        b.CutToSize(16);    //나머지 16바이트는 버린다.
        byte [] AppID = Utils.getAppID().getBytes();
        System.arraycopy(AppID,0,TmlcNo,16,16);

        byte[] tmpTrack = b.CutToSize(40);

        byte[] Ksn = b.CutToSize(10);
        byte[] Track2_Data = b.CutToSize(48);
        byte[] EMVTradeType = b.CutToSize(1);
//        byte[] Pos_Entry_mode_code = b.CutToSize(2);
//        byte[] Card_sequence_num = b.CutToSize(2);
//        byte[] Add_Pos_info = b.CutToSize(6);
//        byte[] Issuer_Script_result = b.CutToSize(23);
//        byte[] tmpApp_Crypt = b.CutToSize(11);
//        byte[] tmpCrypt_info_data = b.CutToSize(4);
//        byte[] tmpIssuer_app_data = b.CutToSize(35);
//        byte[] tmpUnpred_num = b.CutToSize(7);
//        byte[] tmpATC = b.CutToSize(5);
//        byte[] tmpTVR = b.CutToSize(7);
//        byte[] tmpT_date = b.CutToSize(5);
//        byte[] tmpT_type = b.CutToSize(3);
//        byte[] tmpT_Amount = b.CutToSize(9);
//        byte[] tmpT_Currency = b.CutToSize(5);
//        byte[] tmpAIP = b.CutToSize(4);
//        byte[] tmpTerminal_country = b.CutToSize(5);
//        byte[] tmpAmount_other = b.CutToSize(9);
//        byte[] tmpCVM_result = b.CutToSize(6);
//        byte[] tmpTerminal_Capabilities = b.CutToSize(6);
//        byte[] tmpTerminal_type = b.CutToSize(4);
//        byte[] tmpIFD_serial_num = b.CutToSize(11);
//        byte[] tmpTransaction_category = b.CutToSize(4);
//        byte[] tmpDedicated_filename = b.CutToSize(18);
//        byte[] tmpTerminal_app_version_num = b.CutToSize(5);
//        byte[] tmpTransaction_sequence_counter = b.CutToSize(7);
        byte[] Pos_Entry_mode_code = b.CutToSize(2);    //고정
        byte[] Card_sequence_num = b.CutToSize(2);      //고정

        byte[] Add_Pos_info = b.CutToSize(6);   //첫바이트 길이

        b.CutToSize(2);
        byte[] Issuer_Script_result = b.CutToSize(21);

        b.CutToSize(2);
        byte[] tmpApp_Crypt = b.CutToSize(9);

        b.CutToSize(2);
        byte[] tmpCrypt_info_data = b.CutToSize(2);

        b.CutToSize(2);
        byte[] tmpIssuer_app_data = b.CutToSize(33);

        b.CutToSize(2);
        byte[] tmpUnpred_num = b.CutToSize(5);

        b.CutToSize(2);
        byte[] tmpATC = b.CutToSize(3);

        b.CutToSize(1);
        byte[] tmpTVR = b.CutToSize(6);

        b.CutToSize(1);
        byte[] tmpT_date = b.CutToSize(4);

        b.CutToSize(1);
        byte[] tmpT_type = b.CutToSize(2);

        b.CutToSize(2);
        byte[] tmpT_Amount = b.CutToSize(7);

        b.CutToSize(2);
        byte[] tmpT_Currency = b.CutToSize(3);

        b.CutToSize(1);
        byte[] tmpAIP = b.CutToSize(3);

        b.CutToSize(2);
        byte[] tmpTerminal_country = b.CutToSize(3);

        b.CutToSize(2);
        byte[] tmpAmount_other = b.CutToSize(7);

        b.CutToSize(2);
        byte[] tmpCVM_result = b.CutToSize(4);

        b.CutToSize(2);
        byte[] tmpTerminal_Capabilities = b.CutToSize(4);

        b.CutToSize(2);
        byte[] tmpTerminal_type = b.CutToSize(2);

        b.CutToSize(2);
        byte[] tmpIFD_serial_num = b.CutToSize(9);

        b.CutToSize(2);
        byte[] tmpTransaction_category = b.CutToSize(2);

        b.CutToSize(1);
        byte[] tmpDedicated_filename = b.CutToSize(17);

        b.CutToSize(2);
        byte[] tmpTerminal_app_version_num = b.CutToSize(3);

        b.CutToSize(2);
        byte[] tmpTransaction_sequence_counter = b.CutToSize(5);

        b.CutToSize(40); //track2

        byte[] tmpPaywaveFFIValueTag = b.CutToSize(2);
        byte[] tmpPaywaveFFIValue = b.CutToSize(5);
        byte[] tmpInputType = b.CutToSize(1);
        byte[] tmpFiller = b.CutToSize(50);

//        b.CutToSize(10); //ksn2
//        b.CutToSize(48); //track2data2

        String result_code = new String(b.CutToSize(2));
        byte[] tmpCode_version = b.CutToSize(16); //codeversion
        for (int i = 0; i< tmpCode_version.length; i++) {
            if (tmpCode_version[i] == (byte)0x00) {
                tmpCode_version[i] = (byte)0x20;
            }
        }
//        if (tmpCode_version.equals(new byte[]{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
//                0x00,0x00,0x00,0x00,0x00,0x00})) {
//            tmpCode_version = new byte[]{0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,
//                    0x20,0x20,0x20,0x20,0x20,0x20};
//        }

        b.CutToSize(2); //signlength
        b.CutToSize(1); //FS
        b.CutToSize(2); //passwordlength
        b.CutToSize(2); //workingkeyindex
        b.CutToSize(16); //Encrypted_password

        KByteArray  _tmpicreqData = new KByteArray();
        _tmpicreqData.Add(Pos_Entry_mode_code);
        _tmpicreqData.Add(Card_sequence_num);
        if(Add_Pos_info[0] == 0  )
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
            String _emv = new String(EMVTradeType);
            if(_emv.equals("F"))
            {
                byte[] _tmp = {(byte)0x90,(byte)0x91};
                _tmpicreqData.Add(_tmp);
            }
            else {
//            _tmpicreqData.Add(Add_Pos_info);
                _tmpicreqData.Add(Utils.reByte(Add_Pos_info, 0, 0, Add_Pos_info[0]));
            }
        }

//        _tmpicreqData.Add(Utils.reByte(Issuer_Script_result, 2, 0));
        if(Issuer_Script_result[0] == 0    )
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
            //       _tmpicreqData.Add(Issuer_Script_result);
            _tmpicreqData.Add(Utils.reByte(Issuer_Script_result, 0, 0,Issuer_Script_result[0]));
        }

//        _tmpicreqData.Add(Utils.reByte(tmpApp_Crypt, 2, 0));
        if(tmpApp_Crypt[0] == 0  )
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpApp_Crypt);
            _tmpicreqData.Add(Utils.reByte(tmpApp_Crypt, 0, 0,tmpApp_Crypt[0]));
        }

//        _tmpicreqData.Add(Utils.reByte(tmpCrypt_info_data, 2, 0));
        if(tmpCrypt_info_data[0] == 0 )
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpCrypt_info_data);
            _tmpicreqData.Add(Utils.reByte(tmpCrypt_info_data, 0, 0,tmpCrypt_info_data[0]));
        }

//        _tmpicreqData.Add(Utils.reByte(tmpIssuer_app_data, 2, 0));
        if(tmpIssuer_app_data[0] == 0 )
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpIssuer_app_data);
            _tmpicreqData.Add(Utils.reByte(tmpIssuer_app_data, 0, 0,tmpIssuer_app_data[0]));
        }

        String mLog3 = Utils.bytesToHex_0xType(_tmpicreqData.value());
//        cout("테스트용 EMV data 중간정산 issuer_app_data 까지 : ",Utils.getDate("yyyyMMddHHmmss"),mLog3);

//        _tmpicreqData.Add(Utils.reByte(tmpUnpred_num, 2, 0));
        if(tmpUnpred_num[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpUnpred_num);
            _tmpicreqData.Add(Utils.reByte(tmpUnpred_num, 0, 0,tmpUnpred_num[0]));
        }

//        _tmpicreqData.Add(Utils.reByte(tmpATC, 2, 0));
        if(tmpATC[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpATC);
            _tmpicreqData.Add(Utils.reByte(tmpATC, 0, 0,tmpATC[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpTVR, 1, 0));
        if(tmpTVR[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpTVR);
            _tmpicreqData.Add(Utils.reByte(tmpTVR, 0, 0,tmpTVR[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpT_date, 1, 0));
        if(tmpT_date[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpT_date);
            _tmpicreqData.Add(Utils.reByte(tmpT_date, 0, 0,tmpT_date[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpT_type, 1, 0));
        if(tmpT_type[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpT_type);
            _tmpicreqData.Add(Utils.reByte(tmpT_type, 0, 0,tmpT_type[0]));
        }

        String mLog4 = Utils.bytesToHex_0xType(_tmpicreqData.value());
//        cout("테스트용 EMV data 중간정산 T_type 까지 : ",Utils.getDate("yyyyMMddHHmmss"),mLog4);

//        _tmpicreqData.Add(Utils.reByte(tmpT_Amount, 2, 0));
        if(tmpT_Amount[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpT_Amount);
            _tmpicreqData.Add(Utils.reByte(tmpT_Amount, 0, 0,tmpT_Amount[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpT_Currency, 2, 0));
        if(tmpT_Currency[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpT_Currency);
            _tmpicreqData.Add(Utils.reByte(tmpT_Currency, 0, 0,tmpT_Currency[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpAIP, 1, 0));
        if(tmpAIP[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpAIP);
            _tmpicreqData.Add(Utils.reByte(tmpAIP, 0, 0,tmpAIP[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpTerminal_country, 2, 0));
        if(tmpTerminal_country[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpTerminal_country);
            _tmpicreqData.Add(Utils.reByte(tmpTerminal_country, 0, 0,tmpTerminal_country[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpAmount_other, 2, 0));
        if(tmpAmount_other[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpAmount_other);
            _tmpicreqData.Add(Utils.reByte(tmpAmount_other, 0, 0,tmpAmount_other[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpCVM_result, 2, 0));
        if(tmpCVM_result[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpCVM_result);
            _tmpicreqData.Add(Utils.reByte(tmpCVM_result, 0, 0,tmpCVM_result[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpTerminal_Capabilities, 2, 0));
        if(tmpTerminal_Capabilities[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpTerminal_Capabilities);
            _tmpicreqData.Add(Utils.reByte(tmpTerminal_Capabilities, 0, 0,tmpTerminal_Capabilities[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpTerminal_type, 2, 0));
        if(tmpTerminal_type[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpTerminal_type);
            _tmpicreqData.Add(Utils.reByte(tmpTerminal_type, 0, 0,tmpTerminal_type[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpIFD_serial_num, 2, 0));
        if(tmpIFD_serial_num[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpIFD_serial_num);
            _tmpicreqData.Add(Utils.reByte(tmpIFD_serial_num, 0, 0,tmpIFD_serial_num[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpTransaction_category, 2, 0));
        if(tmpTransaction_category[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpTransaction_category);
            _tmpicreqData.Add(Utils.reByte(tmpTransaction_category, 0, 0,tmpTransaction_category[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpDedicated_filename, 1, 0));
        if(tmpDedicated_filename[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpDedicated_filename);
            _tmpicreqData.Add(Utils.reByte(tmpDedicated_filename, 0, 0,tmpDedicated_filename[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpTerminal_app_version_num, 2, 0));
        if(tmpTerminal_app_version_num[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpTerminal_app_version_num);
            _tmpicreqData.Add(Utils.reByte(tmpTerminal_app_version_num, 0, 0,tmpTerminal_app_version_num[0]));
        }
//        _tmpicreqData.Add(Utils.reByte(tmpTransaction_sequence_counter, 2, 0));
        if(tmpTransaction_sequence_counter[0] == 0)
        {
            _tmpicreqData.Add((byte)0x00);
        }
        else
        {
//            _tmpicreqData.Add(tmpTransaction_sequence_counter);
            _tmpicreqData.Add(Utils.reByte(tmpTransaction_sequence_counter, 0, 0,tmpTransaction_sequence_counter[0]));
        }

        String mLog5 = Utils.bytesToHex_0xType(_tmpicreqData.value());
//        cout("테스트용 EMV data 중간정산 tmpTransaction_sequence_counter 까지 : ",Utils.getDate("yyyyMMddHHmmss"),mLog5);

        if(tmpPaywaveFFIValueTag[0] == (byte)0x9f && tmpPaywaveFFIValueTag[1] == (byte)0x6e)
        {
            _tmpicreqData.Add(Utils.reByte(tmpPaywaveFFIValue, 0, 0,tmpPaywaveFFIValue[0]));

        }
        else
        {
            _tmpicreqData.Add((byte)0x00);
        }

        String mLog6 = Utils.bytesToHex_0xType(_tmpicreqData.value());
//        cout("테스트용 EMV data 중간정산 tmpPaywaveFFIValueTag 까지 : ",Utils.getDate("yyyyMMddHHmmss"),mLog6);


//        _tmpicreqData.Add(Pos_Entry_mode_code);
//        _tmpicreqData.Add(Card_sequence_num);
//        _tmpicreqData.Add(Add_Pos_info);
//
//        _tmpicreqData.Add(Utils.reByte(Issuer_Script_result, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpApp_Crypt, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpCrypt_info_data, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpIssuer_app_data, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpUnpred_num, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpATC, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpTVR, 1, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpT_date, 1, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpT_type, 1, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpT_Amount, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpT_Currency, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpAIP, 1, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpTerminal_country, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpAmount_other, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpCVM_result, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpTerminal_Capabilities, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpTerminal_type, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpIFD_serial_num, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpTransaction_category, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpDedicated_filename, 1, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpTerminal_app_version_num, 2, 0));
//        _tmpicreqData.Add(Utils.reByte(tmpTransaction_sequence_counter, 2, 0));

        KByteArray  _ksn_track2data = new KByteArray();
        _ksn_track2data.Add(Ksn);
        _ksn_track2data.Add(Track2_Data);

        // Req_tcp_Credit() 에 사용될 정보들 셋팅
        mTmicno =TmlcNo;
        //      Setting.setCardReaderNumber(new String(mTmicno));
        mEncryptInfo=tmpTrack;
//        mKsn_track2data=_ksn_track2data.value();
        mKsn_track2data=new byte[_ksn_track2data.getlength()];
        //mKsn_track2data=new byte[58];
        System.arraycopy( _ksn_track2data.value(),0,mKsn_track2data,0,mKsn_track2data.length);
//        mIcreqData=_tmpicreqData.value();
        mIcreqData=new byte[_tmpicreqData.getlength()];
        System.arraycopy( _tmpicreqData.value(),0,mIcreqData,0,mIcreqData.length);
        mCodeVersion = new String(tmpCode_version);
        Setting.setCodeVersionNumber(new String(tmpCode_version));
        //  b.Clear();
        //  _tmpicreqData.Clear();
        //  _ksn_track2data.Clear();

        String mLog2 = Utils.bytesToHex_0xType(mIcreqData);
        cout("테스트용 EMV data : ",Utils.getDate("yyyyMMddHHmmss"),mLog2);

        //emv data
        if(EMVTradeType[0] == (byte)0x00 || EMVTradeType[0] == (byte)0x20 || EMVTradeType[0] == (byte)0x30)
        {
            mEMVTradeType = " ";
        }
        else
        {
            mEMVTradeType = new String(EMVTradeType);
        }

        if(result_code.equals("K ") || result_code.equals("00") || result_code.equals("R ") ||result_code.equals("M ") ||
                result_code.equals("E ") ||result_code.equals("F ") ||result_code.equals("99"))
        {
            if(result_code.equals("00")){
                mICInputMethod = "I";
            }
            else if(result_code.equals("99")){
                mICInputMethod = "R";
            }
            else if(result_code.equals("K ")){
                mICInputMethod = "K";
            }
            else if(result_code.equals("R ")){
                mICInputMethod = "R";
            }
            else if(result_code.equals("M ")){
                mICInputMethod = "M";
            }
            else if(result_code.equals("E ")){
                mICInputMethod = "E";
            }
            else if(result_code.equals("F ")){
                mICInputMethod = "F";
            }
        }
        else if(result_code.equals("01") || result_code.equals("02") || result_code.equals("03") ||result_code.equals("04") ||
                result_code.equals("05") ||result_code.equals("06") ||result_code.equals("07"))  //ic오류로 인한 폴백사유
        {
            HideMessageBox();

            //폴백 미사용이라면 폴백처리 없이 거래를 종료한다
            if (mFbYn.equals("1"))
            {
                mPaymentListener.result("IC거래불가,폴백거래미사용,거래종료","ERROR",new HashMap<>());
                // DeviceReset();
                new Handler().postDelayed(() -> {
                    //mPosSdk.__fallbackCancel(mDataListener,Command.TYPEDEFINE_ICCARDREADER);
                    //사용자 카드관련 정보 초기화_________________________________________________________________________
                    MemClear();
                    //__________________________________________________________________________________________________
                    b.Clear();
                    _ksn_track2data.Clear();
                    _tmpicreqData.Clear();
                    DeviceReset();
                }, mReDelay);
                return;
            }

            //Setting.Showpopup(mCtx,"IC오류입니다. 마그네틱을 읽혀주세요",null,null);

            ShowMessageBox("IC오류입니다. 마그네틱을 읽혀주세요",Integer.parseInt(Setting.getPreference(mCtx == null ? mPosSdk.getActivity() : mCtx,Constants.BLE_TIME_OUT)));
            //fallback은 여기서만 폴백이유를 받는다.
            mFallbackreason=result_code;
            //      Toast.makeText(mCtx,"IC오류입니다. 마그네틱을 읽혀주세요",Toast.LENGTH_SHORT).show();
            //    DeviceReset();  //reset usb devices
            //mICtype = "01" 신용 , "02" 은련
            if(mEMVTradeType.equals("C"))
                mICType ="02";
            else
                mICType ="01";
            if(mICType == "01")
            {
                //사용자 카드관련 정보 초기화_________________________________________________________________________
                MemClear();
                //__________________________________________________________________________________________________
                b.Clear();
                _ksn_track2data.Clear();
                _tmpicreqData.Clear();
                new Handler().postDelayed(() -> {
                    //DeviceReset();
                    //type "07" fallback msr  "10" 은련fallback msr
                    LASTCOMAND = Command.CMD_IC_REQ;
                    mPosSdk.__Bleicreq("07",mTid,Utils.StringAlignrightzero(String.valueOf(mConvertMoney),10), Utils.getDate("yyyyMMddHHmmss"),
                            "0", "0", "0000", "0", "00", "06",
                            Constants.WORKING_KEY_INDEX, Constants.WORKING_KEY, Constants.CASHIC_RANDOM_NUMBER,rev -> {
                                Log.d("kim.jy", "신용거래 카드 요청 데이터 :" + Utils.bytesToHex(rev));
                                Ble_ParsingData(rev);
                            });

                }, mReDelay);
                return;
            }
            else {
                new Handler().postDelayed(() -> {
                    //DeviceReset();
                    //사용자 카드관련 정보 초기화_________________________________________________________________________
                    MemClear();
                    //__________________________________________________________________________________________________
                    b.Clear();
                    _ksn_track2data.Clear();
                    _tmpicreqData.Clear();
                    //DeviceReset();
                    LASTCOMAND = Command.CMD_IC_REQ;
                    mPosSdk.__Bleicreq("10", mTid, Utils.StringAlignrightzero(String.valueOf(mConvertMoney), 10), Utils.getDate("yyyyMMddHHmmss"),
                            "0", "0", "0000", "0", "00", "06",
                            Constants.WORKING_KEY_INDEX, Constants.WORKING_KEY, Constants.CASHIC_RANDOM_NUMBER,rev -> {
                                Log.d("kim.jy", "신용거래 카드 요청 데이터 :" + Utils.bytesToHex(rev));
                                Ble_ParsingData(rev);
                            });

                    //type "07" fallback msr  "10" 은련fallback msr

                }, mReDelay);
                return;
            }
        }
        else if(result_code.equals("08"))
        {
            HideMessageBox();
//            Toast.makeText(mCtx,"거래불가 카드입니다. 다른카드로 거래해 주세요",Toast.LENGTH_SHORT).show();
            mPaymentListener.result("카드읽기가 취소되었습니다","ERROR",new HashMap<>());
            // DeviceReset();
            new Handler().postDelayed(() -> {
                //mPosSdk.__fallbackCancel(mDataListener,Command.TYPEDEFINE_ICCARDREADER);
                //사용자 카드관련 정보 초기화_________________________________________________________________________
                MemClear();
                //__________________________________________________________________________________________________
                b.Clear();
                _ksn_track2data.Clear();
                _tmpicreqData.Clear();
                DeviceReset();
            }, mReDelay);
            return;
        }
        else if(result_code.equals("09"))
        {
            mICType = "01";
//            ((BaseActivity)mCtx).ReadyDialogHide();
//            ((BaseActivity)mCtx).ReadyDialogShow(mCtx, "IC 카드입니다. IC로 우선 거래해 주세요",20,true);
            ShowMessageBox("IC 카드입니다. IC로 우선 거래해 주세요",Integer.parseInt(Setting.getPreference(mCtx == null ? mPosSdk.getActivity() : mCtx,Constants.BLE_TIME_OUT)));
            //Setting.Showpopup(mCtx,"IC 카드입니다. IC로 우선 거래해 주세요",null,null);
            //Toast.makeText(mCtx,"IC 카드입니다. IC로 우선 거래해 주세요",Toast.LENGTH_SHORT).show();
            //  DeviceReset();  //reset usb devices
            //사용자 카드관련 정보 초기화_________________________________________________________________________
            MemClear();
            //__________________________________________________________________________________________________
            b.Clear();
            _ksn_track2data.Clear();
            _tmpicreqData.Clear();
            new Handler().postDelayed(() -> {
                LASTCOMAND = Command.CMD_IC_REQ;
                mPosSdk.__Bleicreq(mICType,mTid,Utils.StringAlignrightzero(String.valueOf(mConvertMoney),10),
                        Utils.getDate("yyyyMMddHHmmss"), "0", "0", "0000",
                        "0", "00", "06", Constants.WORKING_KEY_INDEX,
                        Constants.WORKING_KEY,Constants.CASHIC_RANDOM_NUMBER,rev -> {
                            Log.d("kim.jy", "신용거래 카드 요청 데이터 :" + Utils.bytesToHex(rev));
                            Ble_ParsingData(rev);
                        });

            }, mReadingDelay);
            return;
        }
        else if(result_code.equals("10") || result_code.equals("11"))
        {
           HideMessageBox();
//            Toast.makeText(mCtx,"거래불가 카드입니다. 다른카드로 거래해 주세요",Toast.LENGTH_SHORT).show();
            mPaymentListener.result("거래불가 카드입니다. 다른카드로 거래해 주세요","ERROR",new HashMap<>());
            // DeviceReset();
            new Handler().postDelayed(() -> {
                //mPosSdk.__fallbackCancel(mDataListener,Command.TYPEDEFINE_ICCARDREADER);
                //사용자 카드관련 정보 초기화_________________________________________________________________________
                MemClear();
                //__________________________________________________________________________________________________
                b.Clear();
                _ksn_track2data.Clear();
                _tmpicreqData.Clear();
                DeviceReset();
            }, mReDelay);
            return;
        }
        else if(result_code.equals("12"))
        {
           HideMessageBox();
//            Toast.makeText(mCtx,"거래불가 카드입니다. 다른카드로 거래해 주세요",Toast.LENGTH_SHORT).show();
            mPaymentListener.result("거래불가 카드입니다. 해외은련 카드 지원불가","ERROR",new HashMap<>());
            // DeviceReset();
            new Handler().postDelayed(() -> {
                //mPosSdk.__fallbackCancel(mDataListener,Command.TYPEDEFINE_ICCARDREADER);
                //사용자 카드관련 정보 초기화_________________________________________________________________________
                MemClear();
                //__________________________________________________________________________________________________
                b.Clear();
                _ksn_track2data.Clear();
                _tmpicreqData.Clear();
                DeviceReset();
            }, mReDelay);
            return;
        }
        else
        {
          HideMessageBox();
//            Toast.makeText(mCtx,"거래가 취소 되었습니다.",Toast.LENGTH_SHORT).show();
            mPaymentListener.result("거래가 취소 되었습니다.","ERROR",new HashMap<>());

            new Handler().postDelayed(() -> {
                //사용자 카드관련 정보 초기화_________________________________________________________________________
                MemClear();
                //__________________________________________________________________________________________________
                b.Clear();
                _ksn_track2data.Clear();
                _tmpicreqData.Clear();
                // mPosSdk.__fallbackCancel(mDataListener,Command.TYPEDEFINE_ICCARDREADER);
                DeviceReset();
            }, mReDelay);
            return;
        }

        //사용자 카드관련 정보 초기화_________________________________________________________________________
        //__________________________________________________________________________________________________
        b.Clear();
        _ksn_track2data.Clear();
        _tmpicreqData.Clear();
        //금액이 50000 만원 이상인 경우이며 어떤종류든 패드를 사용할 시 사인을 받는다.
        HideMessageBox();
        int TaxConvert = 0 ;
        if(mTax!=null) {
            TaxConvert = mTax.equals("") ? 0 : Integer.valueOf(mTax);
        }
        int SrvCharge = 0;
        if(mServiceCharge!=null) {
            SrvCharge = mServiceCharge.equals("") ? 0 : Integer.valueOf(mServiceCharge);
        }
        int TotalMoney = mMoney + TaxConvert + SrvCharge;
        int compareMoney = 0;
        compareMoney = Integer.parseInt(
                (Setting.getPreference(mCtx,Constants.UNSIGNED_SETMONEY)).equals("") ?
                        "50000":Setting.getPreference(mCtx,Constants.UNSIGNED_SETMONEY)
        );

        if (mAppToApp)
        {
            if(TotalMoney > compareMoney)
            {
                if(Setting.getDscyn().equals("1"))
                {
                    Setting.g_sDigSignInfo = "6";
                    ReadySignPad(mCtx,TotalMoney);
                }
                else if(Setting.getDscyn().equals("2"))
                {
                    Setting.g_sDigSignInfo = "B";
                    Req_tcp_Credit(mTid,Setting.getDscData().getBytes(),"", mICCancelInfo,"");
                }
                else    //금액이 오만원 이하의 경우에는 서버에 결제 요청을 진행 한다.
                {
                    //무서명으로 올경우 향후 제거될 예정
                    Setting.g_sDigSignInfo = "6";

                    ReadySignPad(mCtx,TotalMoney);
//                    Setting.g_sDigSignInfo = "5";
//                    Req_tcp_Credit(mTid,null,"", mICCancelInfo,"");
                }
            }
            else
            {
                //금액이 5만원이하
                Setting.g_sDigSignInfo = "5";
                Req_tcp_Credit(mTid,null,"", mICCancelInfo,"");

            }

        }
        else
        {
            if(TotalMoney > compareMoney)
            {
                Setting.g_sDigSignInfo = "B";

                ReadySignPad(mCtx,TotalMoney);
            }
            else    //금액이 오만원 이하의 경우에는 서버에 결제 요청을 진행 한다.
            {
                //금액이 5만원이하
                Setting.g_sDigSignInfo = "5";
                Req_tcp_Credit(mTid,null,"", mICCancelInfo,"");
            }
        }



    }


    /**
     * 단말기로 부터 받은 신용거래정보를 서버로 보낸다
     * @param _Tid
     * @param _img
     * @param _CodeNversion
     * @param _cancelInfo
     * @param _kocesUnique
     */
    public void Req_tcp_Credit(String _Tid,byte[] _img,String _CodeNversion, String _cancelInfo, String _kocesUnique)
    {
        //DeviceReset();
        if(!_cancelInfo.equals(""))
        {
            mICCancelInfo = _cancelInfo;
        }
        if(!_kocesUnique.equals(""))
        {
            mICKocesTranUniqueNum = _kocesUnique;
        }
        if(!_CodeNversion.equals(""))
        {
            mCodeVersion = _CodeNversion;
        }
        if(mCodeVersion.equals(""))
        {
            mCodeVersion = Setting.getCodeVersionNumber();
        }
        if(mMchdata.equals(""))
        {
            mMchdata = Setting.getMchdata();
        }
        String tmpTmicno = new String(mTmicno);
        String tmpCardNumber = new String(mEncryptInfo);
        ShowMessageBox("거래 승인 중 입니다",Constants.SERVER_TIME_OUT);
//        ((BaseActivity)mCtx).ReadyDialogShow(mCtx,
//                "거래 승인 중 입니다",
//                Integer.parseInt(Setting.getPreference(mCtx,Constants.BLE_TIME_OUT)));
        //포인트 암호화 정보가 없는 관계로 길이를 0000으로해서 보낸다
//        if(mICInputMethod.equals("K")){mEMVTradeType = " ";}

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mICCancelInfo.equals(""))    //취소사유가 없는 경우 신용거래요청
                {
                    if(!mEMVTradeType.contains("F"))    //폴백거래인지아닌지
                    {
                        isFallBack = false;
                        //만일 emv거래값이 스페이스나 null값으로 들어왔을때 정상승인은 스와이프로 처리한다.
                        if(mEMVTradeType.equals(" ")&& mICInputMethod.equals("I")){mICInputMethod="S";}
                        mPosSdk.___ictrade(TCPCommand.CMD_ICTRADE_REQ, _Tid, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "",
                                mICCancelInfo, mICInputMethod, tmpCardNumber.substring(0,6), "",
                                mKsn_track2data, String.valueOf(mMoney), mTax, mServiceCharge, mTaxfree, "410", String.valueOf(mInstallment),
                                tmpTmicno, "", mEMVTradeType, "", "",mIcreqData, Constants.WORKING_KEY_INDEX, mUnionPasswd,
                                "","","","","","","",null,Setting.g_sDigSignInfo,
                                mCodeVersion,"",_img,"",mMchdata,"", mCompCode,
                                Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mAppToApp == true ? true:false,_Tid),mTcpDatalistener);
                    }
                    else
                    {
                        isFallBack = true;
                        mPosSdk.___ictrade(TCPCommand.CMD_ICTRADE_REQ, _Tid, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "",
                                mICCancelInfo, mICInputMethod, tmpCardNumber.substring(0,6), "",
                                mKsn_track2data, String.valueOf(mMoney), mTax, mServiceCharge, mTaxfree, "410", String.valueOf(mInstallment),
                                tmpTmicno, "", mEMVTradeType, mFallbackreason, "", mIcreqData, Constants.WORKING_KEY_INDEX, mUnionPasswd,
                                "", "", "", "", "", "", "", null, Setting.g_sDigSignInfo,
                                mCodeVersion,"", _img, "", mMchdata, "", mCompCode,
                                Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mAppToApp == true ? true:false,_Tid),mTcpDatalistener);
                    }
                }
                else    //거래취소 요청
                {
                    if(!mEMVTradeType.contains("F"))    //폴백거래인지아닌지
                    {
                        isFallBack = false;
                        //만일 emv거래값이 스페이스나 null값으로 들어왔을때 정상승인은 스와이프로 처리한다.
                        if(mEMVTradeType.equals(" ")&& mICInputMethod.equals("I")){mICInputMethod="S";}
                        mPosSdk.___ictrade(TCPCommand.CMD_ICTRADE_CANCEL_REQ,_Tid, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "",
                                mICCancelInfo, mICInputMethod, tmpCardNumber.substring(0,6), "",
                                mKsn_track2data, String.valueOf(mMoney), mTax, mServiceCharge, mTaxfree, "410", String.valueOf(mInstallment),
                                tmpTmicno, "", mEMVTradeType, "", "", mIcreqData, Constants.WORKING_KEY_INDEX, mUnionPasswd,
                                "", "", "", "", "", "", "", null, Setting.g_sDigSignInfo,
                                mCodeVersion,"", _img, "", mMchdata, mICKocesTranUniqueNum, mCompCode,
                                Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mAppToApp == true ? true:false,_Tid),mTcpDatalistener);
                    }
                    else
                    {
                        isFallBack = true;
                        mPosSdk.___ictrade(TCPCommand.CMD_ICTRADE_CANCEL_REQ,_Tid, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "",
                                mICCancelInfo, mICInputMethod, tmpCardNumber.substring(0,6), "",
                                mKsn_track2data, String.valueOf(mMoney), mTax, mServiceCharge, mTaxfree, "410", String.valueOf(mInstallment),
                                tmpTmicno, "", mEMVTradeType, mFallbackreason, "", mIcreqData, Constants.WORKING_KEY_INDEX, mUnionPasswd,
                                "", "", "", "", "", "", "", null, Setting.g_sDigSignInfo,
                                mCodeVersion,"", _img, "", mMchdata, mICKocesTranUniqueNum, mCompCode,
                                Utils.getMacAddress(mCtx), Utils.getHardwareKey(mCtx, mAppToApp == true ? true:false,_Tid),mTcpDatalistener);
                    }
                }


            }
        }).start();
    }
    /**
     * 서버로 부터 신용거래를 받아서 eot 수신을 체크한다
     * @param _res
     * @param _date
     * @param _TerminalID
     * @param _responseCode
     * @param _creditCode
     */
    private void Res_TCP_Credit(List<byte[]> _res, String _date, String _TerminalID, String _responseCode, String _creditCode)
    {
        if(_res==null)
        {
            HideMessageBox();
            mPaymentListener.result("서버 수신 데이터 NULL","ERROR",new HashMap<>());
            return;
        }
        final List<byte[]> data =_res;
        String code = new String(data.get(0));
        if(code.equals("0000") || code.equals("0001") || code.equals("0002")) {
            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }

            int _tmpEot = 0;
            while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
            {
                if (Setting.getEOTResult() != 0) {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult()); break;
                }
                try { Thread.sleep(100); } catch (InterruptedException e) {e.printStackTrace();}
                _tmpEot ++;
                if(_tmpEot >= 30)
                {
                    Log.d("getEOTResult","getEOTResult : " + Setting.getEOTResult());
                    break;
                }
            }

            if (Setting.getEOTResult() == 1 && _creditCode.equals("A15"))   //eot정상 정상응답
            {
                Res_TCP_Credit_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }
            else if (Setting.getEOTResult() == -1  && _creditCode.equals("A15")) {  //망취소한다
                String ic_appro_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2)); //신용승인번호
                mICCancelInfo = "I" + _date + ic_appro_number;
                byte[] _tmpCancel = mICCancelInfo.getBytes();
                _tmpCancel[0] = (byte) 0x49;
                mICCancelInfo = new String(_tmpCancel);
                mEotCancel=1;
                //TODO: 221229 망취소 도 취소로 간주하여 망취소를 날릴때는 금액을 취소처럼 거래금액에 모두 실어서 보낸다. 다른 부가금액들은 0원 설정
                mMoney = mMoney + Integer.parseInt(mTax) + Integer.parseInt(mServiceCharge) + Integer.parseInt(mTaxfree);
                mTax = "0";
                mServiceCharge = "0";
                mTaxfree = "0";
                Req_tcp_Credit(mTid, null, "", mICCancelInfo,"");
                mICCancelInfo = "";
                Setting.setEOTResult(0);
            }
            else if(Setting.getEOTResult() == 0 && _creditCode.equals("A15"))   //승인일때 들어와서 시도하며 취소일때는 하지 않는다. 망취소한다
            {
                mEotCancel=1;
                //TODO: 221229 망취소 도 취소로 간주하여 망취소를 날릴때는 금액을 취소처럼 거래금액에 모두 실어서 보낸다. 다른 부가금액들은 0원 설정
                mMoney = mMoney + Integer.parseInt(mTax) + Integer.parseInt(mServiceCharge) + Integer.parseInt(mTaxfree);
                mTax = "0";
                mServiceCharge = "0";
                mTaxfree = "0";
                mICCancelInfo = "I" + _date.substring(0,6) + Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2));
                Req_tcp_Credit(mTid,null,"", mICCancelInfo,"");
            }
            else if(_creditCode.equals("A25"))  //정상취소
            {
                Res_TCP_Credit_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
            }
        }
        else    //기타오류코드로인한 종료
        {
            String ic_result_message = "";
            try {
                ic_result_message = Utils.getByteToString_euc_kr(data.get(1)); //IC_응답메세지
                // sendData.put("Message",ic_result_message);
            } catch (UnsupportedEncodingException ex) {

            }
//            mICKocesTranUniqueNum = "";
//            String codeMessage = "";
//            MemClear();
//            try {
//                codeMessage = Utils.getByteToString_euc_kr(data.get(1));
//            } catch (UnsupportedEncodingException ex) {
//
//            }
//            mPaymentListener.result(code + ":" + codeMessage,"ERROR",null);
//            DeviceReset();
            Res_TCP_Credit_Sucess(data,_date,_TerminalID,mEotCancel,code,ic_result_message,_creditCode);
        }

    }

    /**
     * eot수신 체크 후에 서버로부터 받은 신용거래 결과
     * @param data
     * @param _date
     * @param _TerminalID
     * @param _EotCancel
     * @param code
     * @param ic_result_message
     * @param _creditCode
     */
    private void Res_TCP_Credit_Sucess(List<byte[]> data,String _date,String _TerminalID,int _EotCancel,String code,String ic_result_message,String _creditCode)
    {
        //String ic_result_message = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(1)); //IC_응답메세지
        String ic_appro_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2)); //신용승인번호
        String koces_tran_unique_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3)); //koces거래고유번호
        String tmpCardNum1 = new String(data.get(4));
        tmpCardNum1.replace(" ","");
        String[] tmpCardNum2 = tmpCardNum1.split("-");
        tmpCardNum1 = "";
        for(String n:tmpCardNum2)
        {
            tmpCardNum1 += n;
        }

        /** 마스킹카드번호 변경(22.04.01 jiw 앱투앱으로 보낼 때 8자리를 보내고 그외 본앱에서 사용 및 전표출력번호는 데이터 확인 후 처리 */
        byte[] _CardNum = tmpCardNum1.getBytes();
        String print_card_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum);
        StringBuffer sb = new StringBuffer();
        sb.append(tmpCardNum1);
        if (_CardNum.length > 12) {
            sb.replace(8, 12, "****");
        }
        if (tmpCardNum1.indexOf("=") > 0) {
            sb.replace(tmpCardNum1.indexOf("="), tmpCardNum1.indexOf("=") + 1, "*");
        }
        if (_CardNum.length > 0) {
            sb.replace(_CardNum.length-1, _CardNum.length, "*");
        }

        //앱투앱인지를 체크
        if (mAppToApp)
        {
            if (tmpCardNum1.length() > 13)  //현금영수증인지 아닌지를 체크
            {
                print_card_num = sb.substring(0,8);
            }
            else
            {
                print_card_num = sb.toString();
            }
        }
        else
        {
            print_card_num = sb.toString();
        }

//        byte[] _CardNum = tmpCardNum1.getBytes();
//
//        if(_CardNum != null){
//            //System.arraycopy( data.get(4),0,_CardNum,0,6);
//            for(int i=6; i<_CardNum.length; i++){
//                //* = 0x2A
//                _CardNum[i] = (byte)0x2A;
//            }
//            //카드번호가 올라오지만 그중 6개만 사용하고 나머지는 *로 바꾼다
//        }

//        String print_card_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum); //출력용카드번호

        //카드 정보 삭제____________________________________________________________________________________
        tmpCardNum1 = "";
        tmpCardNum2 = null;
        Random rand = new Random();
        sb.replace(0,sb.length(),String.valueOf(rand.nextInt(255)));
        sb.replace(0,sb.length(),String.valueOf(0x01));
        sb.replace(0,sb.length(),String.valueOf(0x00));
        sb.delete(0,sb.length());
        sb = null;
        for(int i=0; i<_CardNum.length; i++){
            //* = 0x2A
            _CardNum[i] = (byte)rand.nextInt(255);;
        }
        data.set(4,_CardNum);
        Arrays.fill(_CardNum,(byte)0x01);
        data.set(4,_CardNum);
        Arrays.fill(_CardNum,(byte)0x00);
        data.set(4,_CardNum);
        //__________________________________________________________________________________________________
        //사용자 카드관련 정보 초기화_________________________________________________________________________
        MemClear();
        //__________________________________________________________________________________________________

        String card_type = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(6)); //카드종류
        String issuer_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(7)); //발급사코드
        String issuer_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(8),"EUC-KR"); //발급사명
        String purchaser_code = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(9)); //매입사코드
        String purchaser_name = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(10),"EUC-KR"); //매입사명
        String ddc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(11)); //DDC 여부
        String edc_status = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(12)); //EDC 여부

        String giftcard_money = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(15)); //기프트카드 잔액
        String merchant_number = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(16)); //가맹점번호
        String encryp_key_expiration_date = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(25)); //암호키만료잔여일
        String merchant_data = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(26)); //가맹점데이터

        String trd_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(27)); //거래금액
        String tax_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(28)); //세금
        String svc_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(29)); //봉사료
        String tax_free_amt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(30)); //비과세

        mICKocesTranUniqueNum = koces_tran_unique_num;
        sendData = new HashMap<>();
        sendData.put("TrdType",_creditCode);
        sendData.put("TermID",_TerminalID);
        sendData.put("TrdDate",_date);
        sendData.put("AnsCode", code);
        sendData.put("Message", ic_result_message);
        sendData.put("AuNo",ic_appro_number);
        sendData.put("TradeNo", mICKocesTranUniqueNum);
//        if(!print_card_num.equals(""))
//        {
//            print_card_num = print_card_num.substring(0,6);
//        }

        sendData.put("CardNo", print_card_num);
        sendData.put("Keydate", encryp_key_expiration_date);
        sendData.put("MchData", merchant_data);
        sendData.put("CardKind", card_type); //카드종류 1':신용, '2':체크, '3' : 기프트, '4': 기타 ( 스페이스일 경우 신용으로 처리 )
        sendData.put("OrdCd", issuer_code);   //발급사코드
        sendData.put("OrdNm", issuer_name);   //발급사명
        sendData.put("InpNm", purchaser_name);   //매입사명
        sendData.put("InpCd", purchaser_code);   //매입사코드
        sendData.put("DDCYn", ddc_status);   //DDC 여부
        sendData.put("EDCYn", edc_status);   //EDC 여부
        sendData.put("GiftAmt", giftcard_money); //기프트카드 잔액
        sendData.put("MchNo", merchant_number);   //가맹점번호

        if(isFallBack)
        {
            String finalPrint_card_num = print_card_num;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run()
                {
                    if(_EotCancel==0)
                    {
                        mEotCancel=0;
                        if (!mAppToApp)
                        {
                            if(card_type.equals("3") || card_type.equals("4"))
                            {
                                if(code.equals("0000") || code.equals("0001") || code.equals("0002")) {
                                    mPosSdk.setSqliteDB_InsertTradeData(
                                            mTid,
                                            mStoreName, mStoreAddr, mStoreNumber, mStorePhone, mStoreOwner,
                                            sqliteDbSdk.TradeMethod.Credit,
                                            _creditCode.equals("A15") ? sqliteDbSdk.TradeMethod.NoCancel : sqliteDbSdk.TradeMethod.Cancel,
                                            mMoney,
                                            ic_result_message,
                                            Integer.parseInt(mTax),
                                            Integer.parseInt(mServiceCharge),
                                            Integer.parseInt(mTaxfree),
                                            mInstallment,
                                            sqliteDbSdk.TradeMethod.NULL,
                                            sqliteDbSdk.TradeMethod.NULL,
                                            "",
                                            MarkingCardNumber(finalPrint_card_num),
                                            card_type,
                                            purchaser_name.replace(" ", ""),
                                            issuer_name.replace(" ", ""),
                                            merchant_number.replace(" ", ""),
                                            _date.replace(" ", ""),
                                            _creditCode.equals("A15") ? "" : mOriAudate,
                                            ic_appro_number.replace(" ", ""),
                                            _creditCode.equals("A15") ? "" : mOriAuNum,
                                            mICKocesTranUniqueNum.replace(" ", ""), ic_result_message,
                                            "", "", "", "", "", "", "", "",
                                            "", "", "", "", "", "", "", "");
                                }
                            }
                            else
                            {
                                if(code.equals("0000") || code.equals("0001") || code.equals("0002")) {
                                    mPosSdk.setSqliteDB_InsertTradeData(
                                            mTid,
                                            mStoreName, mStoreAddr, mStoreNumber, mStorePhone, mStoreOwner,
                                            sqliteDbSdk.TradeMethod.Credit,
                                            _creditCode.equals("A15") ? sqliteDbSdk.TradeMethod.NoCancel : sqliteDbSdk.TradeMethod.Cancel,
                                            mMoney,
                                            giftcard_money.equals("") ? "0" : giftcard_money,
                                            Integer.parseInt(mTax),
                                            Integer.parseInt(mServiceCharge),
                                            Integer.parseInt(mTaxfree),
                                            mInstallment,
                                            sqliteDbSdk.TradeMethod.NULL,
                                            sqliteDbSdk.TradeMethod.NULL,
                                            "",
                                            MarkingCardNumber(finalPrint_card_num),
                                            card_type,
                                            purchaser_name.replace(" ", ""),
                                            issuer_name.replace(" ", ""),
                                            merchant_number.replace(" ", ""),
                                            _date.replace(" ", ""),
                                            _creditCode.equals("A15") ? "" : mOriAudate,
                                            ic_appro_number.replace(" ", ""),
                                            _creditCode.equals("A15") ? "" : mOriAuNum,
                                            mICKocesTranUniqueNum.replace(" ", ""), ic_result_message,
                                            "", "", "", "", "", "", "", "",
                                            "", "", "", "", "", "", "", "");
                                }
                            }

                        }
                        mPaymentListener.result("","COMPLETE_IC",sendData);
                    }
                    else
                    {
                        mEotCancel=0;
                        mPaymentListener.result("망취소 발생, 거래 실패" + ":" + ic_result_message,"ERROR",new HashMap<>());
                    }

                }
            });
            //       DeviceReset();
            return;
        }

        if(!code.equals("0002"))
        {
            mICKocesTranUniqueNum = "";
            //       mARD = 0x00; mIAD = null; mIS = null;
            String codeMessage = "";
            try {
                codeMessage = Utils.getByteToString_euc_kr(data.get(1));
                sendData.put("Message",codeMessage);
            } catch (UnsupportedEncodingException ex) {

            }

            if(!code.equals("0000"))
            {
                mEotCancel=0;
                mPaymentListener.result("거래 실패(" + code + ") " +  ":" + codeMessage,"ERROR",new HashMap<>());
                return;
            }

            if(_EotCancel==0)
            {
                mEotCancel=0;
                if (!mAppToApp)
                {
                    if(card_type.equals("3") || card_type.equals("4"))
                    {
                        if(code.equals("0000") || code.equals("0001") || code.equals("0002")) {
                            mPosSdk.setSqliteDB_InsertTradeData(
                                    mTid,
                                    mStoreName, mStoreAddr, mStoreNumber, mStorePhone, mStoreOwner,
                                    sqliteDbSdk.TradeMethod.Credit,
                                    _creditCode.equals("A15") ? sqliteDbSdk.TradeMethod.NoCancel : sqliteDbSdk.TradeMethod.Cancel,
                                    mMoney,
                                    ic_result_message,
                                    Integer.parseInt(mTax),
                                    Integer.parseInt(mServiceCharge),
                                    Integer.parseInt(mTaxfree),
                                    mInstallment,
                                    sqliteDbSdk.TradeMethod.NULL,
                                    sqliteDbSdk.TradeMethod.NULL,
                                    "",
                                    MarkingCardNumber(print_card_num),
                                    card_type,
                                    purchaser_name.replace(" ", ""),
                                    issuer_name.replace(" ", ""),
                                    merchant_number.replace(" ", ""),
                                    _date.replace(" ", ""),
                                    _creditCode.equals("A15") ? "" : mOriAudate,
                                    ic_appro_number.replace(" ", ""),
                                    _creditCode.equals("A15") ? "" : mOriAuNum,
                                    mICKocesTranUniqueNum.replace(" ", ""), ic_result_message,
                                    "", "", "", "", "", "", "", "",
                                    "", "", "", "", "", "", "", "");
                        }
                    }
                    else
                    {
                        if(code.equals("0000") || code.equals("0001") || code.equals("0002")) {
                            mPosSdk.setSqliteDB_InsertTradeData(
                                    mTid,
                                    mStoreName, mStoreAddr, mStoreNumber, mStorePhone, mStoreOwner,
                                    sqliteDbSdk.TradeMethod.Credit,
                                    _creditCode.equals("A15") ? sqliteDbSdk.TradeMethod.NoCancel : sqliteDbSdk.TradeMethod.Cancel,
                                    mMoney,
                                    giftcard_money.equals("") ? "0" : giftcard_money,
                                    Integer.parseInt(mTax),
                                    Integer.parseInt(mServiceCharge),
                                    Integer.parseInt(mTaxfree),
                                    mInstallment,
                                    sqliteDbSdk.TradeMethod.NULL,
                                    sqliteDbSdk.TradeMethod.NULL,
                                    "",
                                    MarkingCardNumber(print_card_num),
                                    card_type,
                                    purchaser_name.replace(" ", ""),
                                    issuer_name.replace(" ", ""),
                                    merchant_number.replace(" ", ""),
                                    _date.replace(" ", ""),
                                    _creditCode.equals("A15") ? "" : mOriAudate,
                                    ic_appro_number.replace(" ", ""),
                                    _creditCode.equals("A15") ? "" : mOriAuNum,
                                    mICKocesTranUniqueNum.replace(" ", ""), ic_result_message,
                                    "", "", "", "", "", "", "", "",
                                    "", "", "", "", "", "", "", "");
                        }
                    }

                }
                mPaymentListener.result(codeMessage,"COMPLETE_IC",sendData);//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
            }
            else
            {
                mEotCancel=0;
                mPaymentListener.result("망취소 발생, 거래 실패" + ":" + codeMessage,"ERROR",new HashMap<>());//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
            }

            //    DeviceReset();
        }
        else
        {
            mICKocesTranUniqueNum = "";
            new Thread(new Runnable() {
                @Override
                public void run() {
                    KByteArray  b = new KByteArray(data.get(17));
                    byte[] icResultLength = b.CutToSize(4);
                    String _resultLength = new String(icResultLength);
                    byte[] ARD;
                    byte[] IAD;
                    byte[] IS;
                    if(_resultLength.equals("0000"))
                    {
                        ARD = new byte[1];
                        ARD[0] = (byte)0x30;
                        IAD = new byte[1];
                        IAD[0] = (byte)0x30;
                        IS = new byte[1];
                        IS[0] = (byte)0x30;
                        mICCancelInfo = "I" + _date + ic_appro_number;
                    }
                    else
                    {
                        ARD = b.CutToSize(26);
                        IAD = b.CutToSize(12);
                        IS = b.CutToSize(260);
                        mICCancelInfo = "J" + _date + ic_appro_number;
                    }
                    mPosSdk.__BLEemvComplete(Utils.getDate("yyyyMMddHHmmss"),ARD,IAD,IS,"00",res -> {
                       //TODO 2021.11.30 kim.jy 여기 작업해야 함.
                        Log.d("kim.jy","신용거래 2nd gene :" + Utils.bytesToHex(res));
                        Ble_ParsingData(res);
                    });
                    //b.Clear();
                }

            }).start();
        }
    }

    /**
     * 현금 영수증 제일 처음 시작 부분
     * @param _ctx 현재 엑티비티
     * @param _Tid tid
     * @param _money 거래금액
     * @param _tax 세금
     * @param _serviceCharge 봉사료
     * @param _taxfree 면세료
     * @param _privateorbusiness 개인/법인
     * @param _reciptIndex
     * @param _CancelInfo 취소정보
     * @param _oriDate 취소시 사용
     * @param _InputMethod 입력방법(키인)
     * @param _cancelReason 취소사유
     * @param _ptCardCode 포인트카드
     * @param _ptAcceptNum 포인트번호
     * @param _BusinessData 가맹점데이터
     * @param _bangi
     * @param _KocesTradeUnique 코세스거래고유번호
     * @param _AppToApp 앱투앱인지 아닌지 체크
     */
    public void CashRecipt(Context _ctx,String _Tid,String _money,int _tax,int _serviceCharge,int _taxfree,
                           int _privateorbusiness,String _reciptIndex,String _CancelInfo,
                           String _oriDate, String _InputMethod,String _cancelReason,
                           String _ptCardCode,String _ptAcceptNum,String _BusinessData,String _bangi,
                           String _KocesTradeUnique,boolean _AppToApp,
                           String _storeName, String _storeAddr, String _storeNumber, String _storePhone, String _storeOwner)
    {
        mCtx = _ctx;
        mCancelInfo = _CancelInfo;
        mCancelReason = _cancelReason;
        mInputMethod = _InputMethod;
        mPtCardCode = _ptCardCode;
        mPtAcceptNumber = _ptAcceptNum;
        mKocesTradeCode = _KocesTradeUnique;
        mBusinessData = _BusinessData;
        mBangi = _bangi;
        mTid = _Tid;
        mStoreName = _storeName;
        mStoreAddr = _storeAddr;
        mStoreNumber = _storeNumber;
        mStorePhone = _storePhone;
        mStoreOwner = _storeOwner;
        mTax = String.valueOf(_tax);
        mServiceCharge = String.valueOf(_serviceCharge);
        mTaxfree = String.valueOf(_taxfree);

        if (!_CancelInfo.equals("")) {
            mOriAudate = _oriDate;
            String _tmpAunum = _CancelInfo.replace(_oriDate, "");
            mOriAuNum = _tmpAunum.substring(1);
        }

        mAppToApp = _AppToApp==true ? true:false;

        String _icMoney = "";
        for(int i=0; i<_money.length(); i++)
        {
            if(!_money.substring(i,i+1).equals(" "))
                _icMoney += _money.substring(i,i+1);
        }
        mMoney = Integer.valueOf(_icMoney);

        mConvertMoney = mMoney + _tax + _serviceCharge;
        if (!_CancelInfo.equals(""))
        {
            mMoney = mMoney + _tax + _serviceCharge;
            mTax = "0";
            mServiceCharge = "0";
        }



        final String CovertMoney = Utils.StringAlignrightzero(String.valueOf(mConvertMoney),10);

        mPB = _privateorbusiness;

        mPosSdk.__BLEPosinit("99",null);  //먼저 장비를 초기화 시킨다.
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String type = "06"; //거래구분
                        if(mPB==3)
                        {
                            type = "09";
                        }
                        LASTCOMAND = Command.CMD_IC_REQ;

                        mPosSdk.__Bleicreq(type,mTid,  CovertMoney, Utils.getDate("yyyyMMddHHmmss"),
                                "0", "0", _reciptIndex,"0", "01",
                                "40", Constants.WORKING_KEY_INDEX, Constants.WORKING_KEY,
                                Constants.CASHIC_RANDOM_NUMBER, res -> {
                                    Log.d("kim.jy","현금영수증 요청 데이터 :" + Utils.bytesToHex(res));
                                    Ble_ParsingData(res);
                                });
                    }
                },mReadingDelay);
            }
        }).start();
    }

    /**
     * 단말기로부터 현금거래 데이터를 받음
     * @param _res 데이터
     * @param justNumber 현금/IC현금 체크
     */
    private void Res_CashRecipt(byte[] _res,boolean justNumber)
    {
        if(!justNumber) {
            final byte[] res = _res;
            Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(res);
            if (protocolInfo.Command != Command.CMD_IC_RES) {
                return;
            }

            KByteArray  b = new KByteArray(protocolInfo.Contents);

            byte[] TmlcNo = new byte[32];
            System.arraycopy( b.CutToSize(16),0,TmlcNo,0,16);
            //단말 인증 번호가 32개가 올라오지만 그 중에서 앞 16자리만 사용하고 나머지는 APPID로 채운다.
            b.CutToSize(16);    //나머지 16바이트는 버린다.
            byte[] AppID = Utils.getAppID().getBytes();
            System.arraycopy(AppID,0,TmlcNo,16,16);
            byte[] Track = b.CutToSize(6);
            mCashTrack = Track;
            //앞에 여섯자리면 서버에 전송 한다.
            b.CutToSize(34); //그래서 나머지 34바이트를 버린다.
            byte[] Ksn = b.CutToSize(10);
            byte[] Track2_Data = b.CutToSize(48);
            b.CutToSize(1);b.CutToSize(2);
            b.CutToSize(2);b.CutToSize(6);
            b.CutToSize(23);b.CutToSize(11);
            b.CutToSize(4);b.CutToSize(35);
            b.CutToSize(7);b.CutToSize(5);
            b.CutToSize(7);b.CutToSize(5);
            b.CutToSize(3);b.CutToSize(9);
            b.CutToSize(5);b.CutToSize(4);
            b.CutToSize(5);b.CutToSize(9);
            b.CutToSize(6);b.CutToSize(6);
            b.CutToSize(4);b.CutToSize(11);
            b.CutToSize(4);b.CutToSize(18);
            b.CutToSize(5);b.CutToSize(7);
            b.CutToSize(40);b.CutToSize(10);
            b.CutToSize(48);String result_code = new String(b.CutToSize(2));
            b.CutToSize(16);

            KByteArray  _ksn_track2data = new KByteArray();
            _ksn_track2data.Add(Ksn);
            _ksn_track2data.Add(Track2_Data);

            mCashTrack2data=new byte[_ksn_track2data.getlength()];
            System.arraycopy( _ksn_track2data.value(),0,mCashTrack2data,0,mCashTrack2data.length);

            b.Clear();
            Ksn = null;
            Track = null;
            _ksn_track2data.Clear();

            String resultCode = Command.Check_IC_result_code(result_code);  //result 코드를 확인 해서 처리 한다. 입력 메소드를 판단한다.

            //여기까지 왔다면 장비는 다 사용한 것으로 판단하여 초기화 시킨다.
            DeviceReset();
            if (resultCode.equals("K") || resultCode.equals("00") || resultCode.equals("R") || resultCode.equals("M") || resultCode.equals("E") || resultCode.equals("F")) {
                mInputMethod = resultCode;
                if (resultCode.equals("00")) {
                    mInputMethod = "S";
                }

                /* 단말기로 받은 현금거래정보를 서버로 보낸다 */
                Req_tcp_Cash(mTid,mCancelInfo, mInputMethod, mCashTrack, mCashTrack2data, String.valueOf(mPB), mCancelReason, mPtCardCode, mPtAcceptNumber, mBusinessData, mBangi, mKocesTradeCode);
                //b.Clear();
                //_ksn_track2data.Clear();
            } else {
                mPaymentListener.result(result_code, "ERROR", new HashMap<>());
            }
        }
        else
        {
            KByteArray  ba = new KByteArray(_res);
            ba.CutToSize(4);
            String length = new String(ba.CutToSize(2));
            String number = new String(ba.CutToSize(Integer.parseInt(length)));
            mCashTrack = number.getBytes();
            mInputMethod = "K";
            ba.Clear();

            Req_tcp_Cash(mTid,mCancelInfo, mInputMethod, number.getBytes(), null, String.valueOf(mPB), mCancelReason, mPtCardCode, mPtAcceptNumber, mBusinessData, mBangi, mKocesTradeCode);
            //ba.Clear();
        }
    }

    /**
     * 단말기로 받은 현금거래정보를 서버로 보낸다
     * @param _Tid
     * @param _CancelInfo
     * @param _InputMethod
     * @param _id
     * @param _idEncrpyt
     * @param _PB
     * @param _CancelReason
     * @param _ptCardCode
     * @param _ptAcceptNum
     * @param _businessData
     * @param _Bangi
     * @param _KocesTradeNumber
     */
    private void Req_tcp_Cash(String _Tid,String _CancelInfo,String _InputMethod,byte[] _id,byte[] _idEncrpyt,String _PB,String _CancelReason
            ,String _ptCardCode,String _ptAcceptNum,String _businessData,String _Bangi,String _KocesTradeNumber)
    {
        ShowMessageBox("거래 승인 중 입니다",Constants.SERVER_TIME_OUT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!_CancelInfo.equals(""))
                {
                    mPosSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ, _Tid, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "",
                            _CancelInfo, _InputMethod, _id, _idEncrpyt,
                            String.valueOf(mMoney), mTax, mServiceCharge, mTaxfree, _PB, _CancelReason, _ptCardCode, _ptAcceptNum,_businessData
                            , _Bangi, _KocesTradeNumber, mTcpDatalistener);
                }
                else
                {
                    //현금영수증 : 개인/법인 구분('1' : 개인,  '2' : 법인, '3' : 자진, '4' : 원천, '5' : 반기지급명세) , 미설정 시 개인('1')로 처리
                    mPosSdk.___cashtrade(TCPCommand.CMD_CASH_RECEIPT_REQ, _Tid, Utils.getDate("yyMMddHHmmss"), Constants.TEST_SOREWAREVERSION, "",
                            _CancelInfo, _InputMethod,  _id, _idEncrpyt,
                            String.valueOf(mMoney), mTax, mServiceCharge, mTaxfree, _PB, _CancelReason, _ptCardCode, _ptAcceptNum,_businessData
                            , _Bangi, _KocesTradeNumber, mTcpDatalistener);
                }
            }
        }).start();
    }
    /**
     * 서버로부터 받은 현금거래를 eot 수신체크한다
     * @param _res
     * @param _date
     * @param _TerminalID
     * @param _responseCode
     * @param _receivecode
     */
    private void Res_TCP_CastEOTReceive(List<byte[]> _res,String _date,String _TerminalID,String _responseCode, String _receivecode)
    {
        if(_res==null)
        {
            HideMessageBox();
            mPaymentListener.result("서버 수신 데이터 NULL","ERROR",new HashMap<>());
            return;
        }

        final List<byte[]> data =_res;
        String code = new String(data.get(0));
        if(code.equals("0000"))
        {
            int _tmpEot = 0;
            while (true)    //TCP EOT 수신 될때 까지 기다리기 위해서
            {
                if (Setting.getEOTResult() != 0) {
                    Log.d("getEOTResult", "getEOTResult : " + Setting.getEOTResult());
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                _tmpEot++;
                if (_tmpEot >= 30) {
                    Log.d("getEOTResult", "getEOTResult : " + Setting.getEOTResult());
                    break;
                }
            }

            if (Setting.getEOTResult() == 1 && _receivecode.equals("B15")) {    //eot정상 정상승인
                Res_TCP_CashRecipt(_res, _date, _TerminalID, mEotCancel, _receivecode);
            } else if (Setting.getEOTResult() == -1 && _receivecode.equals("B15")) {    //eot비정상 망취소시도
                String cash_appro_num = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2)); //신용승인번호
                mCancelInfo = "1" +  _date.substring(0, 6) + cash_appro_num;
                mEotCancel =1;
                //TODO: 221229 망취소 도 취소로 간주하여 망취소를 날릴때는 금액을 취소처럼 거래금액에 모두 실어서 보낸다. 다른 부가금액들은 0원 설정
                mMoney = mMoney + Integer.parseInt(mTax) + Integer.parseInt(mServiceCharge) + Integer.parseInt(mTaxfree);
                mTax = "0";
                mServiceCharge = "0";
                mTaxfree = "0";
                Req_tcp_Cash(mTid, mCancelInfo, Setting.mInputCashMethod, mCashTrack, mCashTrack2data, Setting.mPrivateOrCorp,
                        "1", "", "", Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(9)), "",
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3)));
                mCancelInfo = "";
                Setting.setEOTResult(0);
            } else if (Setting.getEOTResult() == 0 && _receivecode.equals("B15"))   //승인일때 들어와서 시도하며 취소일때는 하지 않는다 망취소시도
            {
                mCancelInfo = "1" + _date.substring(0, 6) + Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2));
                mEotCancel =1;
                //TODO: 221229 망취소 도 취소로 간주하여 망취소를 날릴때는 금액을 취소처럼 거래금액에 모두 실어서 보낸다. 다른 부가금액들은 0원 설정
                mMoney = mMoney + Integer.parseInt(mTax) + Integer.parseInt(mServiceCharge) + Integer.parseInt(mTaxfree);
                mTax = "0";
                mServiceCharge = "0";
                mTaxfree = "0";
                Req_tcp_Cash(mTid, mCancelInfo, Setting.mInputCashMethod, mCashTrack, mCashTrack2data, Setting.mPrivateOrCorp,
                        "1", "", "", Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(9)), "",
                        Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3)));
            } else if (_receivecode.equals("B25")) {    //취소승인
                Res_TCP_CashRecipt(_res, _date, _TerminalID, mEotCancel, _receivecode);

            }

        }
        else
        {
//            String codeMessage = "";
//            try {
//                codeMessage = Utils.getByteToString_euc_kr(data.get(1));
//            } catch (UnsupportedEncodingException ex) {
//
//            }
//
//            mPaymentListener.result(code + ":" + codeMessage,"ERROR",null);
            Res_TCP_CashRecipt(_res, _date, _TerminalID, mEotCancel, _receivecode);
        }
    }

    /**
     * eot 수신 체크후 서버로부터 받은 현금거래 결과
     * @param _res
     * @param _date
     * @param _TerminalID
     * @param _EotCancel
     * @param _receivecode
     */
    private void Res_TCP_CashRecipt(List<byte[]> _res,String _date,String _TerminalID,int _EotCancel, String _receivecode)
    {
        if(_res==null)
        {
            HideMessageBox();
            mPaymentListener.result("서버 수신 데이터 NULL","ERROR",new HashMap<>());
            return;
        }
        final List<byte[]> data =_res;
        String code = new String(data.get(0));
        String Message = "";
        try {
            Message = Utils.getByteToString_euc_kr(data.get(1));
        } catch (UnsupportedEncodingException ex) {

        }

//        if(code.equals("0000"))
//        {
        String authorizationnumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(2)); //현금영수증 승인번호
        String kocesTradeUniqueNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(3)); //KOCES거래고유번호

        String tmpCardNum1 = new String(data.get(4));
        String[] tmpCardNum2 = tmpCardNum1.split("-");
        tmpCardNum1 = "";
        for(String n:tmpCardNum2)
        {
            tmpCardNum1 += n;
        }

        String _현금영수증발급형태 = sqliteDbSdk.TradeMethod.NULL;
        if (tmpCardNum1.length() < 13) {
            _현금영수증발급형태 = sqliteDbSdk.TradeMethod.CashDirect;
        } else {
            _현금영수증발급형태 = sqliteDbSdk.TradeMethod.CashMs;
        }

        /** 마스킹카드번호 변경(22.04.01 jiw 앱투앱으로 보낼 때 8자리를 보내고 그외 본앱에서 사용 및 전표출력번호는 데이터 확인 후 처리 */
        byte[] _CardNum = tmpCardNum1.getBytes();
        String PrintIDCheckNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum); //카드번호
        StringBuffer sb = new StringBuffer();
        sb.append(tmpCardNum1);
        if (_CardNum.length > 12) {
            sb.replace(8, 12, "****");
        }
        if (tmpCardNum1.indexOf("=") > 0) {
            sb.replace(tmpCardNum1.indexOf("="), tmpCardNum1.indexOf("=") + 1, "*");
        }
        sb.replace(_CardNum.length-1, _CardNum.length, "*");
        //앱투앱인지를 체크
        if (mAppToApp)
        {
            if (tmpCardNum1.length() > 13)  //현금영수증인지 아닌지를 체크
            {
                PrintIDCheckNumber = sb.substring(0,8);
            }
            else
            {
                PrintIDCheckNumber = sb.toString();
            }
        }
        else
        {
            PrintIDCheckNumber = sb.toString();
        }

        tmpCardNum1 = "";
        tmpCardNum2 = null;

        Random rand = new Random();
        //카드 정보 삭제____________________________________________________________________________________
        sb.replace(0,sb.length(),String.valueOf(rand.nextInt(255)));
        sb.replace(0,sb.length(),String.valueOf(0x01));
        sb.replace(0,sb.length(),String.valueOf(0x00));
        sb.delete(0,sb.length());
        //__________________________________________________________________________________________________
        sb = null;
        //카드 정보 삭제____________________________________________________________________________________
        rand = new Random();
        for(int i=0; i<_CardNum.length; i++){
            //* = 0x2A
            _CardNum[i] = (byte)rand.nextInt(255);;
        }
        data.set(4,_CardNum);
        Arrays.fill(_CardNum,(byte)0x01);
        data.set(4,_CardNum);
        Arrays.fill(_CardNum,(byte)0x00);
        data.set(4,_CardNum);
        //__________________________________________________________________________________________________
        //사용자 카드관련 정보 초기화_________________________________________________________________________
        MemClear();
        //__________________________________________________________________________________________________


//        byte[] _CardNum = tmpCardNum1.getBytes();
//        if(_CardNum != null){
//            //System.arraycopy( data.get(4),0,_CardNum,0,6);
//            for(int i=6; i<_CardNum.length; i++){
//                //* = 0x2A
//                _CardNum[i] = (byte)0x2A;
//            }
//            //카드번호가 올라오지만 그중 6개만 사용하고 나머지는 *로 바꾼다
//        }
//        String PrintIDCheckNumber = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(_CardNum); //카드번호
        //String PrintIDCheckNumber  = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(4));

        String PtResCode  = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(5));
        String PtResMessage  = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(6));
        String PtResInfo  = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(7));
        String Encryptionkey_expiration_date  =Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(8));
        String StoreData  =Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(9));
        String Money  = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(10));
        String Tax  = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(11));
        String ServiceCharge  =Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(12));
        String Tax_exempt = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(13));
        String bangi  = Utils.ByteArrayToString_IfContains_Null_it_returns_empty(data.get(14));

        sendData = new HashMap<>();
        sendData.put("TrdType",_receivecode);
        sendData.put("AnsCode",code);
        sendData.put("date",_date);
        sendData.put("TermID",_TerminalID);
        sendData.put("AuNo",authorizationnumber);
        sendData.put("TradeNo",kocesTradeUniqueNumber);
        sendData.put("CardNo",PrintIDCheckNumber);
        sendData.put("PtResCode",PtResCode);
        sendData.put("PtResMessage",PtResMessage);
        sendData.put("PtResInfo",PtResInfo);
        sendData.put("Keydate",Encryptionkey_expiration_date);
        sendData.put("MchData",StoreData);
        sendData.put("Money",Money);
        sendData.put("Tax",Tax);
        sendData.put("ServiceCharge",ServiceCharge);
        sendData.put("Tax_exempt",Tax_exempt);
        sendData.put("bangi",bangi);


        String final_현금영수증발급형태 = _현금영수증발급형태;
        String finalPrintIDCheckNumber = PrintIDCheckNumber;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                String codeMessage = "";
                try {
                    codeMessage = Utils.getByteToString_euc_kr(data.get(1));
                    sendData.put("Message",codeMessage);
                } catch (UnsupportedEncodingException ex) {

                }
                if(_EotCancel ==0)
                {
                    mEotCancel = 0;
                    if(!mAppToApp)
                    {
                        if(code.equals("0000")) {

                            mPosSdk.setSqliteDB_InsertTradeData(
                                    mTid,
                                    mStoreName, mStoreAddr, mStoreNumber, mStorePhone, mStoreOwner,
                                    sqliteDbSdk.TradeMethod.Cash,
                                    _receivecode.equals("B15") ? sqliteDbSdk.TradeMethod.NoCancel : sqliteDbSdk.TradeMethod.Cancel,
                                    mMoney,
                                    "0",
                                    Integer.parseInt(mTax),
                                    Integer.parseInt(mServiceCharge),
                                    Integer.parseInt(mTaxfree),
                                    mInstallment,
                                    getCashTarget(mPB),
                                    final_현금영수증발급형태,
                                    MarkingCardNumber(finalPrintIDCheckNumber),
                                    "",
                                    "",
                                    "",
                                    "",
                                    "",
                                    _date.replace(" ", ""),
                                    _receivecode.equals("B15") ? "" : mOriAudate,
                                    authorizationnumber.replace(" ", ""),
                                    _receivecode.equals("B15") ? "" : mOriAuNum,
                                    kocesTradeUniqueNumber.replace(" ", ""), codeMessage,
                                    "", "", "", "", "", "", "", "",
                                    "", "", "", "", "", "", "", "");
                        }
                    }
                    mPaymentListener.result(codeMessage,"COMPLETE",sendData);//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
                }
                else
                {
                    mEotCancel = 0;
                    mPaymentListener.result("망취소 발생, 거래 실패" + ":" + codeMessage,"ERROR",new HashMap<>());//mPaymentListener 해당리스너를 통해 AppToAppActivity 에서 결과를 받는다
                }
            }
        });
//        }
//        else
//        {
//            String codeMessage = "";
//            try {
//                codeMessage = Utils.getByteToString_euc_kr(data.get(1));
//            } catch (UnsupportedEncodingException ex) {
//
//            }
//
//            mPaymentListener.result(code + ":" + codeMessage,"ERROR",null);
//        }


        //ShowDialog(tmpxx);
    }
    /**
     * 서버에서 응답을 받은 데이터
     */
    private TcpInterface.DataListener mTcpDatalistener = new TcpInterface.DataListener()
    {
        @Override
        public void onRecviced(byte[] _rev)
        {
            //mPaymentListener.result("여기까지 일단 서버 통신 완료","OK");
            Utils.CCTcpPacket tp = new Utils.CCTcpPacket(_rev);

            //신용 승인 취소 응답"A25" 이 오면 Eot 한번만 취소한고 끝낸다.
            KByteArray  _b = new KByteArray(_rev);
            _b.CutToSize(10);
            String _creditCode = new String(_b.CutToSize(3));

            switch (CheckResponseCategory(tp.getResponseCode()))
            {
                case 1:
                    Res_TCP_Credit(tp.getResData(),tp.Date,tp.getTerminalID(),tp.getResponseCode(), _creditCode);
                    break;
                case 2:
                    Res_TCP_CastEOTReceive(tp.getResData(),tp.Date,tp.getTerminalID(),tp.getResponseCode(),_creditCode);
//                    Res_TCP_CashRecipt(tp.getResData(),tp.Date,tp.getTerminalID(),tp.getResponseCode());
                    break;
                case 3:
                    //Res_TCP_CashIC(tp.getResData(),tp.Date);
                    //ble현금 IC 안함.
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 서버를 통해 신용/현금 거래를 체크한다
     * @param _code
     * @return
     */
    private int CheckResponseCategory(String _code)
    {
        if(_code.equals("A15") || _code.equals("A25"))
        {
            return 1;
        }
        else if(_code.equals("B15") || _code.equals("B25"))
        {
            return 2;
        }
        else if(_code.equals("C15") || _code.equals("C25") || _code.equals("C35") || _code.equals("C45") || _code.equals("C55") || _code.equals("C65"))
        {
            return 3;
        }
        return 0;
    }

    /** 단말기로 부터 NAK 발생시 거래 취소 */
    private void ICTradeCancel()
    {
        mPaymentListener.result("거래 완료 기록 중 NAK 발생","ERROR",new HashMap<>());

    }

    /**
     * 신용거래 후 최종 2차제너레이션 까지 마쳤을 때 데이터를 받는다
     * @param _res
     */
    private void Res_EmvComplete(byte[] _res)
    {
        final byte[] b = _res;
        mICCancelInfo = "";
        KByteArray byteArray  = new KByteArray(b);
        byteArray.CutToSize(4);
        String result = new String(byteArray.CutToSize(1));
        byte[] _resultMessage = byteArray.CutToSize(20);
        if(result.equals("0"))
        {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {

                    while(true)
                    {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if(Setting.getEOTResult() != 0){break;}
                    }

//                    new Handler(Looper.getMainLooper()).post(new Runnable() {
//                        @Override
//                        public void run() {
//
//                            while(true)
//                            {
//                                try {
//                                    Thread.sleep(100);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                if(Setting.getEOTResult() != 0){break;}
//                            }
//
//                            if(Setting.getEOTResult() == 1)
//                            {
//                                String codeMessage = "";
//                                try {
//                                    codeMessage = Utils.getByteToString_euc_kr(_resultMessage);
//                                    sendData.put("Message",codeMessage);
//                                } catch (UnsupportedEncodingException ex) {
//
//                                }
//                                Setting.setEOTResult(0);
//                                mPaymentListener.result(codeMessage,"COMPLETE_IC",sendData);
//                            }
//                            else if(Setting.getEOTResult() == -1)
//                            {
//                                byte[] _tmpCancel = mICCancelInfo.getBytes();
//                                _tmpCancel[0] = (byte)0x49;
//                                mICCancelInfo = new String(_tmpCancel);
//                                Req_tcp_Credit(mTid,null,"", mICCancelInfo,"");
//                                Setting.setEOTResult(0);
//                            }
//
//                        }
//                    });

                    Setting.setEOTResult(0);
                    String codeMessage = "";
                    try {
                        codeMessage = Utils.getByteToString_euc_kr(_resultMessage);
                        sendData.put("Message",codeMessage);
                    } catch (UnsupportedEncodingException ex) {

                    }
                    Setting.setEOTResult(0);
                    mPaymentListener.result(codeMessage,"COMPLETE_IC",sendData);

                }
            });
        }
        else
        {

            //TODO:여기서 망취소를 날린다. - 221229(이완재차장요청. 최근 카드거절취소가 많아져서 여기서 2차제너레이션이 실패해도 진행한다
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
//                    if(mICCancelInfo.equals(""))
//                    {
//                        mICCancelInfo = "";
//                    }
//                    else
//                    {
//                        Req_tcp_Credit(mTid,null,"", mICCancelInfo,"");
//                    }
//                }
//            });

            Setting.setEOTResult(0);
            String codeMessage = "";
            try {
                codeMessage = Utils.getByteToString_euc_kr(_resultMessage);
                sendData.put("Message",codeMessage);
            } catch (UnsupportedEncodingException ex) {

            }
            Setting.setEOTResult(0);
            mPaymentListener.result(codeMessage,"COMPLETE_IC",sendData);

        }
    }

    private String MarkingCardNumber(String _cardNum)
    {
//        if(_cardNum.equals(""))
//        {
//            return _cardNum;
//        }
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

    public void ShowMessageBox(String _msg, int _count)
    {
        if(mAppToApp) {
            Setting.setIsAppToApp(true);
            if(_msg.contains("사인패드"))
            {

            }
            else
            {
                ((AppToAppActivity) mCtx).ReadyDialogHide();
                ((BaseActivity) mCtx).ReadyDialogShow(mCtx,
                        _msg,
                        _count, true);
            }

        }
        else
        {
            Setting.setIsAppToApp(false);
            if (mCtx instanceof CreditLoadingActivity)
            {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {

                        if(_msg.contains("사인패드"))
                        {
                            ((CreditLoadingActivity) mCtx).setMCountSign(true);
                            ((CreditLoadingActivity) mCtx).setMCount(5000);
                        }
                        else
                        {
                            ((CreditLoadingActivity) mCtx).setMCountSign(false);

                            if(_msg.contains("거래 승인"))
                            {
                                ((CreditLoadingActivity) mCtx).setMCount(30);
                                ((CreditLoadingActivity) mCtx).mBtnCancel.setVisibility(View.GONE);
                            } else {
                                ((CreditLoadingActivity) mCtx).setMCount(Integer.valueOf(Setting.getPreference(mCtx,Constants.BLE_TIME_OUT)));
                            }
                            ((CreditLoadingActivity) mCtx).mTvwMessage.setText(_msg);

                        }

                    }
                });

            }
            else if (mCtx instanceof CashLoadingActivity)
            {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(_msg.contains("사인패드"))
                        {
                            ((CashLoadingActivity) mCtx).setMCountSign(true);
                            ((CashLoadingActivity) mCtx).setMCount(5000);
                        }
                        else
                        {
                            ((CashLoadingActivity) mCtx).setMCountSign(false);

                            if(_msg.contains("거래 승인"))
                            {
                                ((CashLoadingActivity) mCtx).setMCount(30);
                                ((CashLoadingActivity) mCtx).mBtnCancel.setVisibility(View.GONE);
                            } else {
                                ((CashLoadingActivity) mCtx).setMCount(Integer.valueOf(Setting.getPreference(mCtx,Constants.BLE_TIME_OUT)));
                            }
                            ((CashLoadingActivity) mCtx).mTvwMessage.setText(_msg);

                        }

                    }
                });

            }
        }
    }

    //사인패드실행하는 함수
    public void ReadySignPad(Context _ctx, int _money)
    {
//        if(!mAppToApp)
//        {
//            if (_ctx instanceof CreditLoadingActivity)
//            {
//                int _count = ((CreditLoadingActivity) _ctx).getMCount();
//                ((CreditLoadingActivity) _ctx).setMCount( ( _count + 10));
//            }
//            else if (_ctx instanceof CashLoadingActivity)
//            {
//                int _count = ((CashLoadingActivity) _ctx).getMCount();
//                ((CashLoadingActivity) _ctx).setMCount( ( _count + 10));
//            }
//        }
        ShowMessageBox("사인패드",5000);
        Intent intent = new Intent((Activity)_ctx, SignPadActivity.class);
        intent.putExtra("Money",_money);
        Activity Ac = (Activity)_ctx;
        Ac.startActivityForResult(intent,REQUEST_SIGNPAD);
    }

    public void HideMessageBox()
    {
        if(mAppToApp){((BaseActivity) mCtx).ReadyDialogHide();}

    }

    /** 거래 시작 또는 거래 종료 후에 장치를 초기화 한다 */
    public void DeviceReset()
    {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mPosSdk.__BLEPosinit("99",null);
                mPosSdk.__BLEReadingCancel(null);
            }
        });
    }
    /** 거래종류 후 정보 초기화 */
    public void MemClear()
    {
        //ksn_track2data 정보 삭제____________________________________________________________________________________
        Random rand = new Random();
        if(mKsn_track2data!=null)
        {
            for(int i=0;i<mKsn_track2data.length;i++)
            {
                mKsn_track2data[i] = (byte)rand.nextInt(255);
            }
            Arrays.fill(mKsn_track2data,(byte)0x01);
            Arrays.fill(mKsn_track2data,(byte)0x00);
        }
        mKsn_track2data = null;
        //__________________________________________________________________________________________________

        //mIcreqData 정보 삭제____________________________________________________________________________________
        rand = new Random();
        if(mIcreqData!=null)
        {
            for(int i=0;i<mIcreqData.length;i++)
            {
                mIcreqData[i] = (byte)rand.nextInt(255);
            }
            Arrays.fill(mIcreqData,(byte)0x01);
            Arrays.fill(mIcreqData,(byte)0x00);
        }
        mIcreqData = null;
        //__________________________________________________________________________________________________

        //mEMVTradeType 정보 삭제____________________________________________________________________________________
        mEMVTradeType = "";
        //__________________________________________________________________________________________________

        //mFallbackreason 정보 삭제____________________________________________________________________________________
        mFallbackreason = "";
        //__________________________________________________________________________________________________

        //mUnionPasswd 정보 삭제____________________________________________________________________________________
        mUnionPasswd = "";
        //__________________________________________________________________________________________________

        //mMchdata 정보 삭제____________________________________________________________________________________
        mMchdata = "";
        //__________________________________________________________________________________________________

        //mCompCode 정보 삭제____________________________________________________________________________________
        mCompCode ="";
        //__________________________________________________________________________________________________

        //mCodeVersion 정보 삭제____________________________________________________________________________________
        mCodeVersion = "";
        //__________________________________________________________________________________________________

        //mCashTrack 정보 삭제____________________________________________________________________________________
        mCashTrack = null;
        //__________________________________________________________________________________________________

        //mCashTrack2data 정보 삭제____________________________________________________________________________________
        mCashTrack2data = null;
        //__________________________________________________________________________________________________

        //mEncryptInfo 정보 삭제____________________________________________________________________________________
        rand = new Random();
        if(mEncryptInfo!=null)
        {
            for(int i=0;i<mEncryptInfo.length;i++)
            {
                mEncryptInfo[i] = (byte)rand.nextInt(255);
            }
            Arrays.fill(mEncryptInfo,(byte)0x01);
            Arrays.fill(mEncryptInfo,(byte)0x00);
        }
        mEncryptInfo = null;
        //__________________________________________________________________________________________________

        //mTmicno 정보 삭제____________________________________________________________________________________
        rand = new Random();
        if(mTmicno!=null)
        {
            for(int i=0;i<mTmicno.length;i++)
            {
                mTmicno[i] = (byte)rand.nextInt(255);
            }
            Arrays.fill(mTmicno,(byte)0x01);
            Arrays.fill(mTmicno,(byte)0x00);
        }
        mTmicno = null;
        //__________________________________________________________________________________________________
    }

}
