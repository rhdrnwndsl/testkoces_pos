package com.koces.androidpos;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.DeviceSecuritySDK;
import com.koces.androidpos.sdk.Devices.Devices;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.ble.MyBleListDialog;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.ble.bleSdkInterface;
import com.koces.androidpos.sdk.db.sqliteDbSdk;
import com.koces.androidpos.sdk.van.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Main2Activity extends BaseActivity {
    /** Log를 위한 TAG 선언 */
    private final static String TAG = Main2Activity.class.getSimpleName();
    /** 무결성 검증 후 메인에 출력되는 카드리더기식별번호 */
    TextView mtv_icreader;//메인에서 사용하지 않는다. 다른 뷰에서 사용
    /** 메인에 출력되는 App식별번호 */
    TextView mtv_appID;
    /** 메인_하단의 무결성 검증 알람박스에 출력되는 메시지 */
    TextView mtv_notice;
    /** 메인에 출력되는 앱버전 */
    TextView mtv_ver_num;

    /** 최소화 버튼,가맹점정보,환경설정 버튼 */
    Button mbtn_exit,mbtn_store_info,mbtn_setting;
    ImageButton mBtn_credit,mBtn_cash,mBtn_easy,mBtn_etcpay,mBtn_tradelist,mBtn_salesInfo;
    /** KocesPosSdk - 실제 usb연결. 시리얼통신. 거래 등을 연동하는 곳 */
    KocesPosSdk mKocesPosSdk;
    /** KocesPosSdk - 실제 usb연결. 시리얼통신. 거래 등을 연동하는 곳 */
    bleSdk mbleSdk;
    /** 메인화면에서 3회연속 버전을 클릭하여 결제(테스트)로 이동시 해당 클릭횟수 */
    int CountShowCreditButton = 0;
    /** 앱투앱으로 실행 시 인텐트로 받은 변수(이 변수를 통해 앱을 최소화 시킨다) */
    int mAppToApp = 0;
    /** 무결성 검증및 장비셋팅에서 시리얼 주소 체크를 위해 사용하는 변수*/
    private String[] busAddr;
    /** 장치가 2개인 경우 각각의 장치를 초기화 및 셋팅을 위한 변수 */
    int Count = 0;
    /** busAddr 로 받은 주소들을 개별로 초기화 하기 위한 변수 */
    private static String TmepAddr = "";
    /** 저장되어 있던 장치일 경우를 체크하여 카드리더기 식별번호를 출력할지 말지를 정하는 변수 */
    boolean b_detectSet = false;

    Main2DetectTimer mMainTimer;

    Context mCtx;

    /** 테스트 기기에서만 자동하는 기능을 만들기 위하여 */
    String mAndroid_id;

    public LinearLayout mMain2Layout = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //코세스 장비 및 관련 전문 연동을 위한 준비 작업
        Intent intent = getIntent();
        try {
            mAppToApp = intent.getExtras().getInt("AppToApp");
            Setting.setOnFirst(true);
        }
        catch (NullPointerException ex)
        {
            Log.d(TAG,ex.toString());
            Setting.setOnFirst(false);
        }
        actAdd(this);
        Setting.setTopContext(this);
        mMain2Layout = (LinearLayout)findViewById(R.id.main2_linearlayout);
//        mMain2Layout.setVisibility(View.INVISIBLE);


        if(KocesPosSdk.getInstance()==null) {
            mKocesPosSdk = new KocesPosSdk(this);
            mKocesPosSdk.setFocusActivity(this,null);
        }
        else
        {
            mKocesPosSdk = KocesPosSdk.getInstance();
            mKocesPosSdk.setFocusActivity(this,null);
//            mKocesPosSdk.ResetSerial();
        }
        mKocesPosSdk.BleregisterReceiver(this);
        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }
        mCtx = this;
        Setting.g_bMainIntegrity = false;
        if (mAppToApp == 2) {
            Setting.g_bMainIntegrity = true;
        }
        switch (Setting.g_PayDeviceType) {
            case NONE:
                //메인2 가 아닌 메인에서 셋팅한다
//                ShowDialog("환경설정에 장치 설정을 해야 합니다.");
//                new Handler(Looper.getMainLooper()).postDelayed(()->{ReadyDialogHide();},3000);
//                mMain2Layout.setVisibility(View.VISIBLE);
//                GoToFront();
                break;
            case BLE:       //BLE의 경우
                //readBleDevicesRecord();
//                mMain2Layout.setVisibility(View.VISIBLE);
//                GoToFront();
                break;
            case CAT:       //WIFI CAT의 경우
//                mMain2Layout.setVisibility(View.VISIBLE);
//                GoToFront();
                break;
            case LINES:     //유선장치의 경우
//                if (mAppToApp != 2) {
//                    moveTaskToBack(true);
//                }

                readCableDevicesRecord();
                break;
            default:
                break;
        }

        initRes();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        if (Setting.getIsAppForeGround() == 2) {
//            moveTaskToBack(true);
//        }

        OnFirstStart();
    }
    @Override
    protected void onPause() {
        super.onPause();
//        Setting.setIsAppForeGround(2);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        Setting.setIsAppForeGround(1);
        if (mKocesPosSdk != null) {
            mKocesPosSdk.setFocusActivity(this,null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Setting.setIsAppForeGround(2);
        mKocesPosSdk.BleUnregisterReceiver(this);
        mKocesPosSdk.BleregisterReceiver(Setting.getTopContext());
    }

    int _bleCount = 0;
    private void setBleInitializeStep()
    {
        ReadyDialogShow(Setting.getTopContext(), "무결성 검증 중 입니다.",0);
        Debug_Test_Bus_OnOff("무결성 검사 진행중...");
        _bleCount += 1;
        /* 무결성 검증. 초기화진행 */
        DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(this, (result, Code, state, resultData) -> {

            if (result.equals("00")) {
                mKocesPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 1);  //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
//                ReadyDialogHide();
            }  else if (result.equals("9999")) {
                Log.d("Main2Activity","타임아웃을 경우 한번더 시도후 처리 db에 갱신하지 않음");
            }else {
                mKocesPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1);
//                ReadyDialogHide();
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (result.equals("00")) {
                        Toast.makeText(mKocesPosSdk.getActivity(), "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                        Debug_Test_Bus_OnOff("무결성 검사 성공");



                    }
                    else if(result.equals("9999"))
                    {
                        mbleSdk = bleSdk.getInstance();
                        if(_bleCount >1)
                        {
                            _bleCount = 0;
                            Toast.makeText(mKocesPosSdk.getActivity(), "네트워크 오류. 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
                            mKocesPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1);
                            Debug_Test_Bus_OnOff("무결성 검사 실패");
                            mbleSdk = bleSdk.getInstance();
                            mbleSdk.DisConnect();
                        }
                        else {
                            mbleSdk = bleSdk.getInstance();
                            mbleSdk.DisConnect();
                            new Handler().postDelayed(()->{
                                ShowDialog("무결성 검증 중 입니다.");
                            },200);

                            new Handler().postDelayed(()->{
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                                mKocesPosSdk.BleConnect(mKocesPosSdk.getActivity(),
                                        Setting.getPreference(mKocesPosSdk.getActivity(),Constants.BLE_DEVICE_ADDR),
                                        Setting.getPreference(mKocesPosSdk.getActivity(),Constants.BLE_DEVICE_NAME));
                            },500);
                        }



                    }
                    else {
                        Toast.makeText(mKocesPosSdk.getActivity(), "무결성 검증에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                        Debug_Test_Bus_OnOff("무결성 검사 실패");
                    }

                }
            },200);

            if(!result.equals("00"))
            {
                return;
            }
            new Handler().postDelayed(()->{
                //장치 정보 요청
                Toast.makeText(mKocesPosSdk.getActivity(), "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                BleDeviceInfo();
            },500);


        });

        deviceSecuritySDK.Req_BLEIntegrity();   /* 무결성 검증. 키갱신 요청 */


    }

    /**
     * ble 리더기 식별번호 표시를 위한 장치 정보 요청 함
     */
    int _bleDeviceCheck = 0;
    private void BleDeviceInfo(){
        _bleDeviceCheck += 1;
        mKocesPosSdk.__BLEPosInfo(Utils.getDate("yyyyMMddHHmmss"), res ->{
            Toast.makeText(mKocesPosSdk.getActivity(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
            ReadyDialogHide();
            if(res[3]==(byte)0x15){
                //장비에서 NAK 올라 옮
                ReadToWork();
                return;
            }
            if (res.length == 6) {      //ACK가 6바이트 올라옴
                return;
            }
            if (res.length < 50) {
                ReadToWork();
                return;
            }
            _bleDeviceCheck = 0;
            KByteArray KByteArray = new KByteArray(res);
            KByteArray.CutToSize(4);
            String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
            String serialNum = new String(KByteArray.CutToSize(10));
            String version = new String(KByteArray.CutToSize(5));
            String key = new String(KByteArray.CutToSize(2));
            Setting.mBleHScrKeyYn = key;

            Setting.setPreference(mKocesPosSdk.getActivity(),Constants.REGIST_DEVICE_NAME,authNum);
            Setting.setPreference(mKocesPosSdk.getActivity(),Constants.REGIST_DEVICE_VERSION,version);
            Setting.setPreference(mKocesPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
            //공백을 제거하여 추가 한다.
            String tmp = authNum.trim();
//            Setting.mAuthNum = authNum.trim(); //BLE는 이것을 쓰지 않는다. 유선이 사용한다
//            mtv_icreader.setText(tmp);//메인에서 사용하지 않는다. 다른 뷰에서 사용
            //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
            ReadToWork();
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(_bleDeviceCheck==1)
                {
                    mbleSdk = bleSdk.getInstance();
                    mbleSdk.DisConnect();
                    new Handler().postDelayed(()->{
                        ShowDialog("장치 연결 중 입니다.");
                    },200);

                    new Handler().postDelayed(()->{
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                        mKocesPosSdk.BleConnect(mKocesPosSdk.getActivity(),
                                Setting.getPreference(mKocesPosSdk.getActivity(),Constants.BLE_DEVICE_ADDR),
                                Setting.getPreference(mKocesPosSdk.getActivity(),Constants.BLE_DEVICE_NAME));
                    },500);
                    return;
                } else if (_bleDeviceCheck > 1)
                {
                    _bleDeviceCheck = 0;
                    Toast.makeText(mKocesPosSdk.getActivity(), "블루투스 통신 오류. 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
                    mbleSdk = bleSdk.getInstance();
                    mbleSdk.DisConnect();
                    return;
                }
                else if (_bleDeviceCheck == 0)
                {
                    //정상
                }

            }
        },2000);
    }
    /**
     * 로컬에 저장된 유선 디바이스 정보를 읽어 변수에 설정한다.
     */
    private void readCableDevicesRecord()
    {

        //저장 되어 있는 DEVICE정보를 읽고 설정 한다.
        if (mKocesPosSdk.mDevicesList != null)
        {
            return;
        }
        mKocesPosSdk.mDevicesList = new ArrayList<>();
        Devices tempDevice;
        String SelectedCardReader = Setting.getPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER);
        if(!SelectedCardReader.equals(""))
        {
            tempDevice = new Devices(SelectedCardReader,SelectedCardReader,false);
            tempDevice.setDeviceSerial(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL));
            tempDevice.setmType(1);
            mKocesPosSdk.mDevicesList.add(tempDevice);
        }
        String SelectedMultiReader = Setting.getPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER);
        if(!SelectedMultiReader.equals(""))
        {
            tempDevice = new Devices(SelectedMultiReader,SelectedMultiReader,false);
            tempDevice.setDeviceSerial(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL));
            tempDevice.setmType(4);
            mKocesPosSdk.mDevicesList.add(tempDevice);
        }
        String SelectedSignPad = Setting.getPreference(mKocesPosSdk.getActivity(),Constants.SELECTED_DEVICE_SIGN_PAD);
        if(!SelectedSignPad.equals(""))
        {
            tempDevice = new Devices(SelectedSignPad,SelectedSignPad,false);
            tempDevice.setDeviceSerial(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD_SERIAL));
            tempDevice.setmType(2);
            mKocesPosSdk.mDevicesList.add(tempDevice);
        }
        String SelectedMultiPad = Setting.getPreference(mKocesPosSdk.getActivity(),Constants.SELECTED_DEVICE_MULTI_SIGN_PAD);
        if(!SelectedMultiPad.equals(""))
        {
            tempDevice = new Devices(SelectedMultiPad,SelectedMultiPad,false);
            tempDevice.setDeviceSerial(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL));
            tempDevice.setmType(3);
            mKocesPosSdk.mDevicesList.add(tempDevice);
        }


    }
    private void readBleDevicesRecord(){
        mKocesPosSdk.BleConnectionListener(result -> {

            if(result==true)
            {

                Activity _ac = (Activity) Setting.getTopContext();
                if(!_ac.isFinishing()){
                    new Handler(Looper.getMainLooper()).post(()-> {
                        if (Setting.getBleName().equals(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                            BleDeviceInfo();
                        } else {
                            Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                            Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                            setBleInitializeStep();
                        }
                    });
                }
                else
                {
                    Toast.makeText(mKocesPosSdk.getActivity(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
                    ReadyDialogHide();
                }


            }
            else
            {
                ReadyDialogHide();
            }
        });
        mKocesPosSdk.BleWoosimConnectionListener(result -> {

            if(result==true)
            {

                Activity _ac = (Activity) Setting.getTopContext();
                if(!_ac.isFinishing()){
                    new Handler(Looper.getMainLooper()).post(()-> {
                        if (Setting.getBleName().equals(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                            BleDeviceInfo();
                        } else {
                            Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                            Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                            setBleInitializeStep();
                        }
                    });
                }
                else
                {
                    Toast.makeText(mKocesPosSdk.getActivity(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
                    ReadyDialogHide();
                }


            }
            else
            {
                ReadyDialogHide();
            }
        });

//            HideDialog();
        mKocesPosSdk.BleIsConnected();
        if(!Setting.getBleIsConnected()) {

            /**
             * STATE_OFF = 10;
             * STATE_TURNING_ON = 11;
             * STATE_ON = 12;
             * STATE_TURNING_OFF = 13;
             * STATE_BLE_TURNING_ON = 14;
             * STATE_BLE_ON = 15;
             * STATE_BLE_TURNING_OFF = 16;
             */

            switch (mKocesPosSdk.mblsSdk.GetBlueToothAdapter().getState())
            {
                case 10:
                case 13:
                case 16:
                    Toast.makeText(mKocesPosSdk.getContext(),"블루투스, 위치 설정을 사용으로 해 주세요", Toast.LENGTH_SHORT).show();
                    ReadyDialogHide();
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

            MyBleListDialog myBleListDialog = new MyBleListDialog(this) {
                @Override
                protected void onSelectedBleDevice(String bleName, String bleAddr) {
                    if (bleName.equals("") || bleAddr.equals(""))
                    {
                        Toast.makeText(mKocesPosSdk.getContext(),"연결을 취소하였습니다.", Toast.LENGTH_SHORT).show();
                        ReadyDialogHide();
                        return;
                    }

                    ReadyDialogShow(Setting.getTopContext(), "장치에 연결중입니다", 0);
//                    ShowDialog(); //여기서 장치연결중이라는 메세지박스를 띄워주어야 한다. 만들지 않아서 일단 주석처리    21-11-24.진우
                    if (!mKocesPosSdk.BleConnect(mKocesPosSdk.getContext(),bleAddr, bleName)) {
                        Toast.makeText(Setting.getTopContext(), "리더기 연결 작업을 먼저 진행해야 합니다", Toast.LENGTH_SHORT).show();
                    }
                    Setting.setBleAddr(bleAddr);
                    Setting.setBleName(bleName);
                }

                @Override
                protected void onFindLastBleDevice(String bleName, String bleAddr) {
                    ReadyDialogShow(Setting.getTopContext(), "장치에 연결중입니다", 0);
                    if (!mKocesPosSdk.BleConnect(this.getContext(),bleAddr, bleName)) {
                        Toast.makeText(Setting.getTopContext(), "리더기 연결에 실패하였습니다", Toast.LENGTH_SHORT).show();
                    }
                    Setting.setBleAddr(bleAddr);
                    Setting.setBleName(bleName);
                }
            };

            myBleListDialog.show();
        }


    }
    /** 각각의 변수, 버튼, 함수 초기화 */
    private void initRes()
    {
//        Setting.setIsAppForeGround(1);
        mCtx = this;

        mtv_notice = (TextView)findViewById(R.id.main2_txt_nitice);
        mtv_notice.setText("");
//        mtv_icreader = (TextView)findViewById(R.id.txt_main2_reader_id);//메인에서 사용하지 않는다. 다른 뷰에서 사용
//        mtv_appID = (TextView)findViewById(R.id.txt_main2_appid);
        mbtn_exit = (Button)findViewById(R.id.btn_main2_exit);
        mbtn_setting = (Button)findViewById(R.id.btn_main2_setting);
        mbtn_store_info = (Button)findViewById(R.id.main2_btn_store_info);

        //AppID 하드 코딩 되어 있어야 함.
//        mtv_appID.setText(Utils.getAppID());
        mbtn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MoveBackGround();
            }
        });

        //가맹점정보 이동 버튼 211222 kim.jy
        mbtn_store_info.setOnClickListener(v->{
            if(Setting.g_bMainIntegrity)
            {
                Intent intent = new Intent(getApplicationContext(), StoreMenuActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else
            {
                ShowDialog("무결성 검사가 진행 중입니다. 잠시만 기다려 주세요.");
                new Handler(Looper.getMainLooper()).postDelayed(()->{ReadyDialogHide();},1000);
            }
        });
        mbtn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Setting.g_bMainIntegrity)
                {
                    Intent intent = new Intent(getApplicationContext(), menu2Activity.class); // for tabslayout
                    //      actlist.remove(2);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else
                {
                    ShowDialog("무결성 검사가 진행 중입니다. 잠시만 기다려 주세요.");
                    new Handler(Looper.getMainLooper()).postDelayed(()->{ReadyDialogHide();},1000);
                }
            }
        });

        /** 22.04.24 jiw 앱버전코드삭제 */
//        mtv_ver_num = (TextView)findViewById(R.id.txt_version_number);

        //22.02.23 kim.jy 화면 가운데 아이콘 클릭 처리
        mBtn_credit = (ImageButton) findViewById(R.id.main2_btn_credit);
        mBtn_cash = (ImageButton) findViewById(R.id.main2_btn_cash);
        mBtn_easy = (ImageButton) findViewById(R.id.main2_btn_easy);
        mBtn_etcpay = (ImageButton) findViewById(R.id.main2_btn_etcpay);
        mBtn_tradelist = (ImageButton) findViewById(R.id.main2_btn_tradelist);
        mBtn_salesInfo = (ImageButton) findViewById(R.id.main2_btn_salesinfo);

        mBtn_credit.setOnClickListener(mainBtnClicklistener);
        mBtn_cash.setOnClickListener(mainBtnClicklistener);
        mBtn_easy.setOnClickListener(mainBtnClicklistener);
        mBtn_etcpay.setOnClickListener(mainBtnClicklistener);
        mBtn_tradelist.setOnClickListener(mainBtnClicklistener);
        mBtn_salesInfo.setOnClickListener(mainBtnClicklistener);


        mAndroid_id = Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID);
        // 나랑 진우만 테스트 할 수 있게 한다. 22.02.23 kim.jy
        if(mAndroid_id.equals("a4fe11ed58aa1f86") || mAndroid_id.equals("d8ffe43c525580b8")
        || mAndroid_id.equals("acb442f172dc9fcd") || mAndroid_id.equals("da97e3bb7f187425")) {

        }else{
//                mBtn_credit.setVisibility(View.INVISIBLE);
//                mBtn_cash.setVisibility(View.INVISIBLE);
//                mBtn_easy.setVisibility(View.INVISIBLE);
//                mBtn_etcpay.setVisibility(View.INVISIBLE);
//                mBtn_tradelist.setVisibility(View.INVISIBLE);
//                mBtn_salesInfo.setVisibility(View.INVISIBLE);
        }
    }
    /**
     *  화면 가운데 아이콘 클릭 처리
     */
    View.OnClickListener mainBtnClicklistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = null;
            //      actlist.remove(2);

            if (Setting.g_bMainIntegrity) {
                switch (v.getId()) {
                    case R.id.main2_btn_credit:
                        mBtn_credit.setImageResource(R.drawable.card_select);
                        mBtn_credit.setScaleType(ImageView.ScaleType.FIT_CENTER);

                        if(!mKocesPosSdk.CreditCashInCheck(Setting.getTopContext(),sqliteDbSdk.TradeMethod.Credit))
                        {
                            mBtn_credit.setImageResource(R.drawable.card_normal);
                            mBtn_credit.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            return;
                        }
                        intent = new Intent(getApplicationContext(), CreditActivity.class);
                        break;
                    case R.id.main2_btn_cash:
                        mBtn_cash.setImageResource(R.drawable.cash_select);
                        mBtn_cash.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        if(!mKocesPosSdk.CreditCashInCheck(Setting.getTopContext(),sqliteDbSdk.TradeMethod.Cash))
                        {
                            mBtn_cash.setImageResource(R.drawable.cash_normal);
                            mBtn_cash.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            return;
                        }
                        intent = new Intent(getApplicationContext(), CashActivity2.class);
                        break;
                    case R.id.main2_btn_easy:
                        mBtn_easy.setImageResource(R.drawable.easy_select);
                        mBtn_easy.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        if(!mKocesPosSdk.CreditCashInCheck(Setting.getTopContext(),sqliteDbSdk.TradeMethod.EasyPay))
                        {
                            mBtn_easy.setImageResource(R.drawable.easy_normal);
                            mBtn_easy.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            return;
                        }
                        intent = new Intent(getApplicationContext(), EasyPayActivity.class);
                        break;
                    case R.id.main2_btn_etcpay:
                        mBtn_etcpay.setImageResource(R.drawable.other_select);
                        mBtn_etcpay.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        intent = new Intent(getApplicationContext(), OtherPayActivity.class);
                        break;
                    case R.id.main2_btn_tradelist:
                        mBtn_tradelist.setImageResource(R.drawable.trade_select);
                        mBtn_tradelist.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        intent = new Intent(getApplicationContext(), TradeListActivity.class);
                        break;
                    case R.id.main2_btn_salesinfo:
                        mBtn_salesInfo.setImageResource(R.drawable.calendar_normal);
                        mBtn_salesInfo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        intent = new Intent(getApplicationContext(), SalesInfoActivity.class);
                        break;
                    default:
                        break;
                }
                if (intent != null) {
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
//                    return;
                }
            } else {
                ShowDialog("무결성 검사가 진행 중입니다. 잠시만 기다려 주세요.");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    ReadyDialogHide();
                }, 1000);
            }
        }
    };
    /**
     * 현재 연결 되어 있는 장비와 저장 되어 있는 장비 리스트를 비교하여 시리얼은 같으나 주소가 다른 경우에 자동으로 디바이스를 주소를 수정 하는 코드를 작성 한다.
     */
    private void CompareDeviceList()
    {
        ReadyDialogShow(this, Command.MSG_START_RESCAN_DEVICE,0);
//        int mDCount = mKocesPosSdk.ConnectedUsbSerialCount();;
        int mDCount = mKocesPosSdk.getUsbDevice().size();
        if(mDCount>0)
        {
            //장치 확인
//            busAddr = mKocesPosSdk.ConnectedUsbSerialDevicesAddr();
            busAddr = mKocesPosSdk.getSerialDevicesBusList();
//            ReadyDialogShow(this, "연결 확인 중입니다",0);
            Count=0;
            SendDetectData2();
        }
        else //연결된 장치가 없다면
        {
            if (mAppToApp == 0) {
                //메인2 가 아닌 메인에서 셋팅한다

            } else {
                //앱투앱에서 셋팅한다
                mAppToApp = 0;

            }
            HideDialog();
            ReadToWork();
            Toast.makeText(mKocesPosSdk.getActivity(),Command.MSG_NO_SCAN_DEVICE ,Toast.LENGTH_SHORT).show();
        }
    }

    /** 장치가 정상적으로 연결되어 통신할 수 있는 지를 체크하고 getDeviceInfo()를 통해 장치를 셋팅한다 */
    private void SendDetectData2()   //재귀 호촐 코드 포함
    {
//        busAddr = null;
//        busAddr = mKocesPosSdk.ConnectedUsbSerialDevicesAddr();
        if (busAddr.length <= Count) {
            return;
        }
        TmepAddr = busAddr[Count];
//        mKocesPosSdk.CheckConnectState(TmepAddr);
        mKocesPosSdk.__PosInitAutoDetect("99",null,new String[]{TmepAddr});
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getDeviceInfo();
            }
        },1000);
    }

    /** 기존의 장치면 데이터를 셋팅하고 이 후 무결성 검증으로 이동한다 */
    private void getDeviceInfo()
    {
        if(mMainTimer!= null)
        {
            mMainTimer.cancel();
            mMainTimer = null;
        }
        mMainTimer = new Main2DetectTimer(5000,1000);
        mMainTimer.cancel();
        mMainTimer.start();
        mKocesPosSdk.__PosInfoAutoDetect(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener()
        {
            @Override
            public void onReceived(byte[] _rev, int _type)
            {
                if (_type == Command.PROTOCOL_TIMEOUT) {
                    ReadyDialogShow(mKocesPosSdk.getActivity(), "장치 연결에 실패 하였습니다.",0);
                }
                else if (_rev[3] ==Command.NAK) {
                    //장비에서 NAK 올라 옮
                    ReadyDialogShow(mKocesPosSdk.getActivity(), "장치로부터 NAK가 수신 되었습니다." ,0);
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

                        for (Devices n : mKocesPosSdk.mDevicesList) {
                            if (n.getDeviceSerial().equals(serialNum)) {
//                                if (n.getmAddr().equals(TmepAddr)) {
                                switch (n.getmType()) {
                                    case 4: //multi reader
                                        Setting.setPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER, TmepAddr);
                                        n.setmAddr(TmepAddr);
                                        n.setName(authNum);
                                        n.setVersion(version);
                                        n.setDeviceName(TmepAddr);
                                        n.setConnected(true);
                                        b_detectSet = true;
                                        Setting.mAuthNum = authNum.trim();
//                                        mtv_icreader.setText(Setting.mAuthNum);//메인에서 사용하지 않는다. 다른 뷰에서 사용
                                        mKocesPosSdk.setMultiReaderAddr(TmepAddr);
                                    case 0: //multi reader
                                        break;
                                    case 1: //IC reader
                                        Setting.setPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER, TmepAddr);
                                        n.setmAddr(TmepAddr);
                                        n.setName(authNum);
                                        n.setVersion(version);
                                        n.setDeviceName(TmepAddr);
                                        n.setConnected(true);
                                        b_detectSet = true;
                                        Setting.mAuthNum = authNum.trim();
//                                        mtv_icreader.setText(Setting.mAuthNum);//메인에서 사용하지 않는다. 다른 뷰에서 사용
                                        mKocesPosSdk.setICReaderAddr(TmepAddr);
                                        break;
                                    case 2: //sign pad
                                        Setting.setPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD, TmepAddr);
                                        n.setmAddr(TmepAddr);
                                        n.setDeviceName(TmepAddr);
                                        n.setConnected(true);
                                //        b_detectSet = true;
                                        mKocesPosSdk.setSignPadAddr(TmepAddr);
                                        break;
                                    case 3: //multi pad
                                        Setting.setPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD, TmepAddr);
                                        n.setmAddr(TmepAddr);
                                        n.setDeviceName(TmepAddr);
                                        n.setConnected(true);
                                    //    b_detectSet = true;
                                        mKocesPosSdk.setMultiPadAddr(TmepAddr);
                                        break;
                                    default:
                                        break;
                                }
//                                }
                            }
                        }
                    }
                }
                //이 코드는 필요 없어 보임
                Count++;
                if (mKocesPosSdk.getSerialDevicesBusList() == null) {
                    return;
                }
                if (mKocesPosSdk.getSerialDevicesBusList().length == Count)
                {
                    ShowDialog("장치 스캔 완료");
                    //카드 리더기 식별 번호 가져오기
                    ReadICReaderUniqueID();
//                    ClassfiCheck(); //장치 스캔이 끝난 후에 무결성 검사를 진행 한다.
                }
                else {
                    SendDetectData2();
                }
            }
        }, TmepAddr);
    }

    /** 무결성 검증 시 카드리더기 식별번호를 출력하기 위해 다시 한번 장치정보를 호출한다 */
    private void ReadICReaderUniqueID()
    {

        if (! Setting.getOnFirst())
        {
            /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
            if(mMainTimer!= null)
            {
                mMainTimer.cancel();
                mMainTimer = null;
            }
            HideDialog();
            /** */
        }


        if(CheckDeviceState(Command.TYPEDEFINE_ICCARDREADER))
        {
            mKocesPosSdk.__PosInfo(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener() {
                @Override
                public void onReceived(byte[] _rev, int _type) {
                    if(_type==Command.PROTOCOL_TIMEOUT)
                    {
                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        /** */
                        Setting.g_bMainIntegrity = true;
                        return;
                    }
                    if(_rev[3]==(byte)0x15){
                        //장비에서 NAK 올라 옮
                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        /** */
                        ReadToWork();
                        return;
                    }
                    if(Setting.ICResponseDeviceType == 3)
                    {
                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        /** */
                        return;
                    }
                    KByteArray KByteArray = new KByteArray(_rev);
                    KByteArray.CutToSize(4);
                    String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                    String serialNum = new String(KByteArray.CutToSize(10));
                    String version = new String(KByteArray.CutToSize(5));
                    String key = new String(KByteArray.CutToSize(2));
                    Setting.mLineHScrKeyYn = key;

                    Setting.setPreference(mKocesPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                    //공백을 제거하여 추가 한다.
                    String tmp = authNum.trim();
                    Setting.mAuthNum = authNum.trim();
//                    mtv_icreader.setText(tmp);//메인에서 사용하지 않는다. 다른 뷰에서 사용
                    //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다

                    if (!Setting.getOnFirst())
                    {
                        ReadToWork();
                    } else{
                        ClassfiCheck();
                    }

                }
            },mKocesPosSdk.getICReaderAddr2());
        }
        else if(CheckDeviceState(Command.TYPEDEFINE_ICMULTIREADER))
        {
            mKocesPosSdk.__PosInfo(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener() {
                @Override
                public void onReceived(byte[] _rev, int _type) {
                    if(_type==Command.PROTOCOL_TIMEOUT)
                    {

                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        Setting.g_bMainIntegrity = true;
                        /** */
                        return;
                    }
                    if(_rev[3]==(byte)0x15){
                        //장비에서 NAK 올라 옮
                        /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
                        if(mMainTimer!= null)
                        {
                            mMainTimer.cancel();
                            mMainTimer = null;
                        }
                        HideDialog();
                        /** */
                        ReadToWork();
                        return;
                    }
                    KByteArray KByteArray = new KByteArray(_rev);
                    KByteArray.CutToSize(4);
                    String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                    String serialNum = new String(KByteArray.CutToSize(10));
                    String version = new String(KByteArray.CutToSize(5));
                    String key = new String(KByteArray.CutToSize(2));
                    Setting.mLineHScrKeyYn = key;

                    Setting.setPreference(mKocesPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                    //공백을 제거하여 추가 한다.
                    String tmp = authNum.trim();
                    Setting.mAuthNum = authNum.trim();
//                    mtv_icreader.setText(tmp);//메인에서 사용하지 않는다. 다른 뷰에서 사용
                    //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
                    if (!Setting.getOnFirst())
                    {
                        ReadToWork();
                    } else{
                        ClassfiCheck();
                    }
                }
            },mKocesPosSdk.getMultiReaderAddr2());
        }
        else
        {
            /** 본래는 무결성을 하고 장치정보를 요청했는데 이제는 장치정보를 먼저하고 무결성을 요청. 이유는 무결성을 안할 수는 있어도 장치정보는 가져와야 하기 때문이다 */
            if(mMainTimer!= null)
            {
                mMainTimer.cancel();
                mMainTimer = null;
            }
            HideDialog();
            /** */
            //장비가 연결 되어 있지 않아 무결성 검사도 카드리더기 식별 번호도 확인 할 수 없는 경우에 환경 설정을 넘기기 위해서 강제 세팅
            ReadToWork();
            b_detectSet = false;
        }
    }

    /** 본앱을 최초로 실행하여 유선을 셋팅했는데 아직 카드리더기인지 멀티패드인지 셋팅이 되어 있지 않다. 여기서 환경설정에 들어가지 않고 다 해결해버린다 */
    private void LineSet()
    {
        HideDialog();
        //카드리더기인지 멀티패드인지
        String SelectedCardReaderOption = Setting.getPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_CARD_READER_OPTION);
        /* 현재 설정되어 있는 카드리더기(카드리더기OR멀티리더기)의 장치이름 */
        String SelectedCardReader= Setting.getPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL);
        String SelectedMultiReader= Setting.getPreference(mKocesPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL);

        if(SelectedCardReaderOption.equals("카드리더기"))
        {
            //카드리더기와 같을 때
            if (mKocesPosSdk.getICReaderAddr2()[0] != null && !mKocesPosSdk.getICReaderAddr2()[0].equals(""))
            {
                //카드리더기가 정상적으로 설정되어 있다. 아무것도 하지 않는다
            } else {
                LineSetDialog mLineDialog = new LineSetDialog(this,  new LineSetDialog.DialogBoxListener() {
                    @Override
                    public void onClickCancel(String _msg) {
                        ShowDialog(_msg);
                        ReadToWork();
                    }

                    @Override
                    public void onClickConfirm(String _msg) {
                        ShowDialog(_msg);
                        ReadToWork();
                    }
                });
                mLineDialog.show();
                return;
            }
        }
        else if(SelectedCardReaderOption.equals("멀티패드"))
        {
            //멀티리더기와 같을 때
            if (mKocesPosSdk.getMultiReaderAddr2()[0] != null && !mKocesPosSdk.getMultiReaderAddr2()[0].equals(""))
            {
                //멀티리더기가 정상적으로 설정되어 있다. 아무것도 하지 않는다
            } else {
                LineSetDialog mLineDialog = new LineSetDialog(this,  new LineSetDialog.DialogBoxListener() {
                    @Override
                    public void onClickCancel(String _msg) {
                        ShowDialog(_msg);
                        ReadToWork();
                    }

                    @Override
                    public void onClickConfirm(String _msg) {
                        ShowDialog(_msg);
                        ReadToWork();
                    }
                });
                mLineDialog.show();
                return;
            }
        } else {
            LineSetDialog mLineDialog = new LineSetDialog(this,  new LineSetDialog.DialogBoxListener() {
                @Override
                public void onClickCancel(String _msg) {
                    ShowDialog(_msg);
                    ReadToWork();
                }

                @Override
                public void onClickConfirm(String _msg) {
                    ShowDialog(_msg);
                    ReadToWork();
                }
            });
            mLineDialog.show();
            return;
        }

        ReadToWork();
    }

    /** 무결성 검증을 진행한다 */
    private void ClassfiCheck()
    {
        if(mMainTimer!= null)
        {
            mMainTimer.cancel();
            mMainTimer = null;
        }
        HideDialog();
        int tmpreader = 0;
        Debug_Test_Bus_OnOff("무결성 검사 진행중 ...");
        String tmp = Setting.getPreference(this,Constants.SELECTED_DEVICE_CARD_READER);
        if(tmp.equals(""))
        {
            tmpreader =  Command.TYPEDEFINE_ICMULTIREADER;
        }
        else
        {
            tmpreader =  Command.TYPEDEFINE_ICCARDREADER;
        }
        DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(this,tmpreader,"", (result, Code, state, resultData) -> {
            if(result.equals("00")) {
                mKocesPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 1);
                Setting.IntegrityResult = true;
                if(b_detectSet) {
                    Debug_Test_Bus_OnOff("무결성 검증 성공");
                }
                else
                {
                    Debug_Test_Bus_OnOff(getResources().getString(R.string.error_need_device_setting));
                }
                /** 본래는 지웠으나 무결성검사가 마지막이기 때문에 다시 복원시킴 */
                ReadToWork(); //일단 지움. 아래에서 해결
            }
            else
            {
                mKocesPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1);
                Setting.IntegrityResult = false;
                /** 본래는 지웠으나 무결성검사가 마지막이기 때문에 다시 복원시킴 */
                ReadToWork(); //일단지움. 아래에서 해결
                Debug_Test_Bus_OnOff("무결성 검증 실패");
                runOnUiThread(()->{
                    ToastBox(getResources().getString(R.string.error_fail_ICVerify));
                });
            }
            //카드 리더기 식별 번호 가져오기
//            ReadICReaderUniqueID();
        });
        if(CheckDeviceState(Setting.getPreference(this,Constants.SELECTED_DEVICE_CARD_READER).equals("")? Command.TYPEDEFINE_ICMULTIREADER:Command.TYPEDEFINE_ICCARDREADER)) {
            deviceSecuritySDK.Req_Integrity();  //무결성 검증 코드
        }
        else
        {
            Debug_Test_Bus_OnOff(getResources().getString(R.string.error_need_device_setting));
            if (mAppToApp == 0) {
                //본앱에서 실행한경우 여기서 처리
                LineSet();
            } else {
                //앱투앱인경우. 앱투앱에서 처리
                ReadToWork();
                mAppToApp = 0;
            }
//            ReadToWork();
        }
    }

    /**
     * 다이얼로그 메시지 박스를 호출한다
     * @param _str
     */
    private void ShowDialog(String _str)
    {
        HideDialog();
        ReadyDialogShow(this,_str,0);
    }

    /**
     * 간단하게 사용하는 Toast 메세지 박스
     * @param _str
     */
    private void ToastBox(String _str)
    {
        Toast.makeText(this,_str,Toast.LENGTH_SHORT).show();
    }

    /** 다이얼로그 메시지 박스를 닫는다 */
    private void HideDialog() {
        ReadyDialogHide();
    }

    /**
     * 메인_하단의 무결성검증 관련 메시지를 출력하는 디버그박스
     * @param _str
     */
    private void Debug_Test_Bus_OnOff(String _str) {
        String tmp = mtv_notice.getText().toString();
        tmp += "\n" + _str;
        mtv_notice.setText(tmp);
    }

    /** 메인2엑티비티가 실행될 때 실행하며, 앱을 최소화(백그라운드이동) CompareDeviceList()를 호출을 실행한다 */
    private void OnFirstStart() {
//        Setting.setIsAppForeGround(1);

        if (mAppToApp == 2) {
            Setting.g_bMainIntegrity = true;
        } else {
            Setting.g_bMainIntegrity = false;
        }

        checkPermission(mAppToApp,mPermissionCheckListener);
        return;
//        if (mAppToApp == 0)
//        {
//            checkPermission(mAppToApp,mPermissionCheckListener);
//            return;
//        }
//        /** 앱투앱으로 통한 실행이 아닌(앱투앱을 열면 거래를 하지 않아도 본앱을 일단 연다.) 본앱을 다이렉트로 열었을 경우 실행된다. */
//        NotAppToApp();

    }

    /** 퍼미션을 체크하여 정상적으로 권한을 받아왔다면 다음을 실행한다. */
    BaseActivity.PermissionCheckListener mPermissionCheckListener = new PermissionCheckListener() {
        @Override
        public void onResult(boolean _result) {
            if (_result == true)
            {
                if(Setting.getOnFirst())
                {
                    //퍼미션 권한을 받아왔다.
                    Setting.setOnFirst(false);
                    NotAppToApp();
                }
                else
                {

                    if(Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
//                        Setting.setIsUSBAutoMain2UI(true);
//                        mMain2Layout.setVisibility(View.VISIBLE);
//                        GoToFront();
                        if (mAppToApp == 0) {
                            mKocesPosSdk.BleConnectionListener(result -> {

                                if(result==true)
                                {
                                    new Handler(Looper.getMainLooper()).post(()-> {
                                        if (Setting.getBleName().equals(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                                            BleDeviceInfo();
                                        } else {
                                            Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                                            Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                                            setBleInitializeStep();
                                        }
                                    });

                                }
                                else
                                {
                                    ReadyDialogHide();
                                }
                            });
                            mKocesPosSdk.BleWoosimConnectionListener(result -> {

                                if(result==true)
                                {
                                    new Handler(Looper.getMainLooper()).post(()-> {
                                        if (Setting.getBleName().equals(Setting.getPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME))) {
                                            BleDeviceInfo();
                                        } else {
                                            Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_NAME, Setting.getBleName());
                                            Setting.setPreference(mKocesPosSdk.getActivity(), Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                                            setBleInitializeStep();
                                        }
                                    });

                                }
                                else
                                {
                                    ReadyDialogHide();
                                }
                            });
                        }
                    }
//                    else if(Setting.g_PayDeviceType == Setting.PayDeviceType.LINES) {
//                        int mDCount = mKocesPosSdk.getUsbDevice().size();
//                        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//                        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
//                        if (mDCount <= 0 && !deviceList.isEmpty()) {
//
//                        } else {
////                            Setting.setIsUSBAutoMain2UI(true);
//                            mMain2Layout.setVisibility(View.VISIBLE);
//                            GoToFront();
//                        }
//                    } else {
////                        Setting.setIsUSBAutoMain2UI(true);
//                        mMain2Layout.setVisibility(View.VISIBLE);
//                        GoToFront();
//                    }

                    ReadToWork();
                }

            } else {
//                mMain2Layout.setVisibility(View.VISIBLE);
//                GoToFront();
                //퍼미션 권한을 받아오지 못했다.
                final String finalTmp = "권한요청을 사용자가 승인하지 않았습니다";
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mKocesPosSdk.getActivity(), finalTmp,Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(()->{
                            finishAndRemoveTask();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        },2000);
                    }
                });
            }
        }
    };


    /** 앱투앱으로 통한 실행이 아닌(앱투앱을 열면 거래를 하지 않아도 본앱을 일단 연다.) 본앱을 다이렉트로 열었을 경우 실행된다. */
    private void NotAppToApp()
    {
        //   if(!isAppIsInBackground()) {
        mtv_notice.setText("");
//        mtv_icreader.setText(""); //메인에서 사용하지 않는다. 다른 뷰에서 사용
        if (mAppToApp == 1) {
//            mAppToApp = 0;
//            MoveBackGround();
            moveTaskToBack(true);
        }
        if (Setting.g_PayDeviceType == Setting.PayDeviceType.LINES) {
//            if (mAppToApp == 1) {
//                mAppToApp = 0;
//            }
//            mKocesPosSdk.ResetSerial();

//            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                @Override
//                public void run() {
                    int mDCount = mKocesPosSdk.getUsbDevice().size();
                    UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                    if(mDCount<=0 || deviceList.isEmpty()) //연결된 장치가 없다면
                    {
//                        Setting.setIsUSBAutoMain2UI(true);
                        if (mAppToApp == 0) {
                            //메인2 가 아닌 메인에서 셋팅한다
//                            mMain2Layout.setVisibility(View.VISIBLE);
//                            GoToFront();
                            HideDialog();
                            ReadToWork();
                            Toast.makeText(mKocesPosSdk.getActivity(),Command.MSG_NO_SCAN_DEVICE,Toast.LENGTH_SHORT).show();
                        }else if(mAppToApp == 2) {
//                            mMain2Layout.setVisibility(View.VISIBLE);
//                            GoToFront();
                            HideDialog();
                            ReadToWork();
                            Toast.makeText(mKocesPosSdk.getActivity(),Command.MSG_NO_SCAN_DEVICE,Toast.LENGTH_SHORT).show();
                        }else {
                            //앱투앱에서 셋팅한다
                            mAppToApp = 0;
                            HideDialog();
                            ReadToWork();
                            Toast.makeText(mKocesPosSdk.getActivity(),Command.MSG_NO_SCAN_DEVICE,Toast.LENGTH_SHORT).show();

                        }

                        return;
                    }
//                    else {
//                        if (Setting.getIsAppForeGround() == 1 && mAppToApp == 0) {
//                            mMain2Layout.setVisibility(View.VISIBLE);
//                            GoToFront();
//                        }
//                    }
//                }
//            }, 1000);





            //TODO shin 일단 앱투앱에서 거래 도중 앱투앱을 껐다 다시 키고 바로 거래혹은 가맹점을 요청했을 때 문제가 발생했다. 따라서 앱투앱으로 들어왔을 경우 디바이스 컴패어를 일단 제껴본다.
            //
            if (mAppToApp == 0) {
                //메인2 가 아닌 메인에서 셋팅한다

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!Setting.g_AutoDetect) {
//                            Setting.setIsUSBAutoMain2UI(true);
//                            mMain2Layout.setVisibility(View.VISIBLE);
//                            GoToFront();
                            CompareDeviceList();
//                            mMain2Layout.setVisibility(View.VISIBLE);
//                            GoToFront();
                        }
                    }
                }, 2500);
            } else if(mAppToApp == 2) {
//                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
                        mAppToApp = 0;
//                        mMain2Layout.setVisibility(View.VISIBLE);
//                        GoToFront();
                        HideDialog();
                        ReadToWork();
//                    }
//                }, 1500);

            } else {
                //앱투앱에서 셋팅한다
//                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
                        mAppToApp = 0;
                        HideDialog();
                        ReadToWork();
//                    }
//                }, 1500);


            }
            //여기까지 추가 코드

//            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    if (!Setting.g_AutoDetect) {
//                        CompareDeviceList();
//                    }
//                }
//            }, 2500);


        }
        else if(Setting.g_PayDeviceType == Setting.PayDeviceType.BLE) {
//            Setting.setIsUSBAutoMain2UI(true);
            if (mAppToApp == 0) {
                readBleDevicesRecord();
            } else {
                mAppToApp = 0;
            }
            ReadToWork();
        } else if (Setting.g_PayDeviceType == Setting.PayDeviceType.NONE) {
//            Setting.setIsUSBAutoMain2UI(true);
            if (mAppToApp == 0) {
                //메인2 가 아닌 메인에서 셋팅한다

            } else {
                //메인2 가 아닌 메인에서 셋팅한다
                mAppToApp = 0;
            }
            ReadToWork();
            ShowDialog("환경설정에 장치 설정을 해야 합니다.");
            new Handler(Looper.getMainLooper()).postDelayed(()->{ReadyDialogHide();},3000);
        } else {
//            Setting.setIsUSBAutoMain2UI(true);
            ReadToWork();
            HideDialog();
        }



        //    }
    }

    //백그라운드에서 프론트로 이동
    private void GoToFront()
    {
        ActivityManager am = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);

    }

    /** 새 장치 또는 Usb를 뺏다가 다시 넣었을 때 장치인식 이 끝나고, 메인2엑티비티가 포그라운드에 있을 때 무결성검증을 실행하는 함수. */
    public void MainOnForeground()
    {
        if(!isAppIsInBackground() && Setting.g_PayDeviceType == Setting.PayDeviceType.LINES) {
            mtv_notice.setText("");
//            mtv_icreader.setText("");//메인에서 사용하지 않는다. 다른 뷰에서 사용
            if(CheckDeviceState(Command.TYPEDEFINE_ICCARDREADER)){b_detectSet=true;}
            else if(CheckDeviceState(Command.TYPEDEFINE_ICMULTIREADER)){b_detectSet=true;}
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    //카드 리더기 식별 번호 가져오기
                    ReadICReaderUniqueID();
//                    ClassfiCheck();
                }
            }, 1000);
        }
    }

    /** 연결된 장치가 카드리더기 또는 멀티카드리더기인지를 체크한다 */
    private boolean CheckDeviceState(int _DeviceType)
    {
        boolean tmp = false;
        if(_DeviceType == Command.TYPEDEFINE_ICCARDREADER && mKocesPosSdk.getUsbDevice().size()>0 && !mKocesPosSdk.getICReaderAddr().equals("") &&
                mKocesPosSdk.CheckConnectedUsbSerialState(mKocesPosSdk.getICReaderAddr()))
        {
            return true;
        }
        else if(_DeviceType == Command.TYPEDEFINE_ICMULTIREADER && mKocesPosSdk.getUsbDevice().size()>0 && !mKocesPosSdk.getMultiReaderAddr().equals("") &&
                mKocesPosSdk.CheckConnectedUsbSerialState(mKocesPosSdk.getMultiReaderAddr()))
        {
            return true;
        }
        return tmp;
    }

//    private void ShowToast()
//    {
//        Toast.makeText(this, getResources().getString(R.string.error_no_data_tid), Toast.LENGTH_SHORT).show();
//    }

    /** 저장되어있던 TID를 가져온다 */
    private String getTID()
    {
        return Setting.getPreference(this, Constants.TID);
    }

    /** 메인에서 무결성검증이 실패/성공 으로 함수결과값이 나올 때까지 막아두었던 다른 엑티비티 이동 등을 정상으로 돌리기 위한 함수 */
    private void ReadToWork()
    {
        Setting.setOnFirst(false);
        Setting.g_bDeviceScanOnOff =true;
        Setting.g_bMainIntegrity = true;
//        Setting.g_bfirstexecAppToApp =false;
        b_detectSet = false;
        _bleDeviceCheck = 0;
        if (mAppToApp == 0) {
            //본앱에서 실행한경우 여기서 처리
        } else {
            //앱투앱인경우. 앱투앱에서 처리
            mAppToApp = 0;
        }
        //
//        Setting.setIsAppForeGround(1);
        //
        HideDialog();
    }

    /** 현재 앱이 백그라운드 구동인지 아닌지를 체크한다 */
    public synchronized boolean isAppIsInBackground() {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(getPackageName())) {
                isInBackground = false;
            }
        }
        return isInBackground;
    }

    class Main2DetectTimer extends CountDownTimer
    {
        public Main2DetectTimer(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if(Count<mKocesPosSdk.getSerialDevicesBusList().length-1) {
                Count++;
                SendDetectData2();
            }
            else {
                /** 무결성검증이 장치정보요청 뒤로 순서가 밀림. 이유는 무결성검증을 안 할 수도 있기 때문 */
                ReadICReaderUniqueID();
//                ClassfiCheck();
                /** */
            }
        }
    }
}