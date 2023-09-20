package com.koces.androidpos.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import android.Manifest;

import com.koces.androidpos.R;
import com.koces.androidpos.StoreInfoActivity;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.DeviceSecuritySDK;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.SerialPort.ConnectFTP;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.ble.bleSdkInterface;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.ble.MyBleListDialog;
import com.koces.androidpos.sdk.van.TcpInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_ADVERTISE;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
/**
 * 환경 설정 기타<br>
 * (현재는 사용하지 않음)
 */
public class frag_device_ble extends Fragment {
    final static String TAG = "Fragment_Ble";
    View view;
    /** menu2Activity.java 에서 가맹점설정 장치설정 관리자설정 무결성검증 등의 Fragment 가 속해있다 */
    menu2Activity m_menuActivity;
    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 가맹점등록다운로드를 위해 사용한다 */
    TextView mTxtBleStatus,mTxtProductNum,mTxtSerialNum,mTxtVer;
    Button mBtnConnect,mBtnDisConnect,mBtnSave,mBtnKeyDown,mBtnVerity,mBtnBlePower,mBtnFirmUpdate;
    KocesPosSdk mPosSdk;
    bleSdk mbleSdk;

    String mBleName,mBleAddr;
    String mStrNoConntected = "연결된 장치가 없습니다";
    String mStrConnected = "장치가 연결 되어 있습니다";

    /** ble 전원유지 시간 */
    Spinner m_cbx_ble_powermanage;
    String mBlePowerManage = "";
    byte mBlePowerManageByte = (byte)0x00;

    /** 터치서명 사이즈 크기 */
    Spinner m_cbx_size_touchSignpad;
    String mTouchSignPadSize = "";


    byte[] multipadAuthNum = null;
    byte[] multipadSerialNum = null;
    String mDeviceVersion = "";
    String type = "",tid = "",version = "",serialNum = "",Ans_Cd = "",message = "",Secu_Key = "",
            Token_cs_Path = "",Data_Cnt = "",Protocol = "",Addr = "",Port = "",User_ID = "",Password = "",
            Data_Type = "",Data_Desc = "",File_Path_Name = "",File_Size = "",Check_Sum_Type = "",
            File_Check_Sum = "",File_Encrypt_key = "",Response_Time = "";
    byte[] mAesKey = null;
    byte[] mEesKey = null;
    Context mCtx;

    ProgressDialog progressDoalog;
    int mPercent = 0;

    /**
     * //이 메소드가 호출될떄는 프래그먼트가 엑티비티위에 올라와있는거니깐 getActivity메소드로 엑티비티참조가능
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        m_menuActivity = (menu2Activity) getActivity();
        mCtx = context;
    }

    public frag_device_ble(){

    }
    @Override
    public void onDetach() {
        super.onDetach();
        m_menuActivity = null;
        mPosSdk = null;
        mbleSdk = null;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag_device_ble, container, false);
        init();
        return view;
    }

    private void init(){
        mPosSdk = KocesPosSdk.getInstance();
        mbleSdk = bleSdk.getInstance();

        if (mbleSdk == null) {
            Log.d("kim.jy","fragment blesdk 초기화 실패");
            return;
        }

        mBtnConnect = (Button)view.findViewById(R.id.frgBle_Connect);
        mBtnDisConnect = (Button)view.findViewById(R.id.frgBle_DisConnect);
        mBtnSave = (Button)view.findViewById(R.id.frgBle_Save);
        mBtnKeyDown = (Button)view.findViewById(R.id.frgble_btn_keydown);
        mBtnFirmUpdate = (Button)view.findViewById(R.id.frgble_btn_firmupdate);
        mBtnVerity = (Button)view.findViewById(R.id.frgble_btn_verity);

        mTxtBleStatus = (TextView) view.findViewById(R.id.frgBle_txt_status);   //ble연결 상태
        mTxtProductNum = (TextView) view.findViewById(R.id.frgble_tvw_product); //ble 제품식별 번호
        mTxtSerialNum = (TextView) view.findViewById(R.id.frgble_tvw_serial);   //ble 시리얼 번호
        mTxtVer = (TextView) view.findViewById(R.id.frgble_tvw_ver);            //ble 버전

        mBtnBlePower = (Button)view.findViewById(R.id.frgBle_power);
        m_cbx_ble_powermanage = view.findViewById(R.id.spinner_f_power_manage);

        mPosSdk.BleIsConnected();
        if(Setting.getBleIsConnected()) {
            mTxtBleStatus.setText("장치가 연결 되어 있습니다");
            setBleDeviceInfo(Setting.getPreference(m_menuActivity,Constants.REGIST_DEVICE_NAME),
                    Setting.getPreference(m_menuActivity,Constants.REGIST_DEVICE_SN),
                    Setting.getPreference(m_menuActivity,Constants.REGIST_DEVICE_VERSION));
        }else{
            mTxtBleStatus.setText("연결된 장치가 없습니다");

        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(Setting.getPreference(mPosSdk.getActivity(),Constants.BLE_TIME_OUT).equals(""))
                {
                    Setting.setPreference(mPosSdk.getActivity(),Constants.BLE_TIME_OUT,"20");
                }
            }
        },200);

        PermissionCheck();  //퍼미션을 체크한다

        mBtnSave.setOnClickListener( v -> {
            /** 2021.11.24 kim.jy */
            Setting.g_PayDeviceType = Setting.PayDeviceType.BLE;    //어플 전체에서 사용할 결제 방식
            String temp = String.valueOf(Setting.g_PayDeviceType);
            Setting.setPreference(this.getContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));

            /** 터치서명 크기를 어떻게 설정할지 정한다 */
            Setting.setPreference(this.getContext(), Constants.SIGNPAD_SIZE, mTouchSignPadSize);
            ShowDialog("장치(BLE) 설정을 저장하였습니다.");
        });

        mBtnConnect.setOnClickListener( v -> {
            mPosSdk.BleIsConnected();
            if (Setting.getBleIsConnected()) {
                Toast.makeText(this.getContext(), "이미 연결된 BLE가 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            /**
             * STATE_OFF = 10;
             * STATE_TURNING_ON = 11;
             * STATE_ON = 12;
             * STATE_TURNING_OFF = 13;
             * STATE_BLE_TURNING_ON = 14;
             * STATE_BLE_ON = 15;
             * STATE_BLE_TURNING_OFF = 16;
             */

            switch (mPosSdk.mblsSdk.GetBlueToothAdapter().getState())
            {
                case 10:
                case 13:
                case 16:
                    mTxtBleStatus.setText("연결된 장치없음");
                    Toast.makeText(this.getContext(),"블루투스, 위치 설정을 사용으로 해 주세요", Toast.LENGTH_SHORT).show();
                    m_menuActivity.ReadyDialogHide();
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
            MyBleListDialog myBleListDialog = new MyBleListDialog(this.getContext()) {
                @Override
                protected void onSelectedBleDevice(String bleName, String bleAddr) {
                    if (bleName.equals("") || bleAddr.equals(""))
                    {
                        mTxtBleStatus.setText("연결된 장치없음");
                        Toast.makeText(this.getContext(),"연결을 취소하였습니다.", Toast.LENGTH_SHORT).show();
                        m_menuActivity.ReadyDialogHide();
                        return;
                    }

                    m_menuActivity.ReadyDialogShow(m_menuActivity, "장치에 연결중입니다",0);
//                    ShowDialog(); //여기서 장치연결중이라는 메세지박스를 띄워주어야 한다. 만들지 않아서 일단 주석처리    21-11-24.진우
                    if(!mPosSdk.BleConnect(m_menuActivity,bleAddr,bleName))
                    {
                        Toast.makeText(this.getContext(), "리더기 연결 작업을 먼저 진행해야 합니다", Toast.LENGTH_SHORT).show();
                    }
                    Setting.setBleName(bleName);
                    Setting.setBleAddr(bleAddr);
//                    Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_NAME,bleName);
//                    Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_ADDR,bleAddr);
                }

                @Override
                protected void onFindLastBleDevice(String bleName, String bleAddr) {
                    m_menuActivity.ReadyDialogShow(m_menuActivity, "장치에 연결중입니다", 0);
                    if (!mPosSdk.BleConnect(m_menuActivity,bleAddr, bleName)) {
                        Toast.makeText(this.getContext(), "리더기 연결에 실패하였습니다", Toast.LENGTH_SHORT).show();
                    }
                    Setting.setBleAddr(bleAddr);
                    Setting.setBleName(bleName);
                }
            };

            myBleListDialog.show();
        });

        mBtnDisConnect.setOnClickListener(v -> {
            mPosSdk.BleIsConnected();
            if (!Setting.getBleIsConnected()) {
                Toast.makeText(this.getContext(), "BLE가 연결되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            mbleSdk = bleSdk.getInstance();
            mbleSdk.DisConnect();
            /** 정상적으로 연결을 해제한다면 기존 마지막 연결장비에 관한 데이터를 지운다 */
            Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_NAME, "");
            Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_ADDR, "");
            mTxtBleStatus.setText(mStrNoConntected);
            setBleDeviceInfo("","","");
        });

        //2021.12.13 kim.jy 추가
        mBtnVerity.setOnClickListener(v -> {
            //무결성 검사 보내는 페이지
            m_menuActivity.setFrag(4);
        });

        mBtnKeyDown.setOnClickListener(v -> {
            mPosSdk.BleIsConnected();
//            if(getTID().equals(""))
//            {
//                Toast.makeText(m_menuActivity, "저장된 TID 가 없습니다. 가맹점등록다운로드를 실행해 주세요", Toast.LENGTH_SHORT).show();
//                return;
//            }
            if (!Setting.getBleIsConnected()) {
                Toast.makeText(this.getContext(), "BLE가 연결되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            else {
                //TODO 2021.12.13 kim.ky 키 업데이트 구현 필요
                m_menuActivity.ReadyDialogShow(m_menuActivity, "키 갱신 중입니다", 0);
                DeviceSecuritySDK securitySDK = new DeviceSecuritySDK(this.getContext(),(result,Code,state,resultData) ->{
                    Log.d(TAG,result);
                    new Handler(Looper.getMainLooper()).post(()->{
                        m_menuActivity.ReadyDialogHide();
                        Toast.makeText(m_menuActivity,result,Toast.LENGTH_SHORT).show();
                    });
                });

                securitySDK.Req_BLESecurityKeyUpdate();    //무결성검증을 완료한다
            }
        });

        mBtnFirmUpdate.setOnClickListener(v -> {
            mPosSdk.BleIsConnected();
            if (!Setting.getBleIsConnected()) {
                Toast.makeText(this.getContext(), "BLE가 연결되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mDeviceVersion.equals("")) {
                Toast.makeText(this.getContext(), "BLE 정보를 가져오지 못했습니다. BLE를 다시 연결하여 정보를 가져와 주십시오.", Toast.LENGTH_SHORT).show();
                return;
            }
            ShowProgressBar();
//            m_menuActivity.ReadyDialogShow(m_menuActivity, "펌웨어 업데이트 중", 0);
            mPosSdk.__BLEauthenticatoin_req("0001", res ->{
                Log.d("__BLEauthenticatoin_req", String.valueOf(res));
                Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(res);

                String mLog = "데이터 : " + Utils.bytesToHex_0xType(res);
                mPosSdk.cout("BLE_READER_REC : 상호인증응답 0x53",Utils.getDate("yyyyMMddHHmmss"),mLog);

                switch (protocolInfo.Command) {
                    case Command.ACK:
                        Log.d("TAG", String.valueOf(res));
                        break;
                    case Command.NAK:
//                        m_menuActivity.ReadyDialogHide();
                        progressDoalog.dismiss();
                        ShowDialog("상호인증 요청 실패");
                        Log.d("TAG", String.valueOf(res));
                        break;
                    case Command.CMD_MUTUAL_AUTHENTICATION_RES:
                        if(res.length < 220) {
                            progressDoalog.dismiss();
                            ShowDialog("업데이트 불가 데이터 길이이상");
//                                    Log.d("TAG", String.valueOf(_rev));
                            return;
                        }
                        KByteArray bArray = new KByteArray(res);
                        bArray.CutToSize(1);    //STX + 길이 + CommandID 삭제
                        String length = new String(bArray.CutToSize(2));   //길이
                        bArray.CutToSize(1);    //CommandID
                        multipadAuthNum = bArray.CutToSize(32);  //멀티패드인증번호
                        multipadSerialNum = bArray.CutToSize(10);  //멀티패드시리얼번호
                        String revDataType = new String(bArray.CutToSize(4));  //요청데이타구분    0001:최신펌웨어, 0003:EMV Key
                        final byte[] data = bArray.CutToSize(90);  //보안키
                        final byte[] tmp = bArray.CutToSize(38);  //보안키에서 뒤에 버리는 부분
                        String emvKey = new String(bArray.CutToSize(10));  //EMV Key
                        byte[] tmsUpdateCheck = bArray.CutToSize(2);  //TMS 업데이트 가능 여부
                        if (tmsUpdateCheck[0] == (byte)0x30 && tmsUpdateCheck[1] == (byte)0x30) {

                        } else if (tmsUpdateCheck[0] == (byte)0x00 && tmsUpdateCheck[1] == (byte)0x00) {

                        } else if (tmsUpdateCheck[0] == (byte)0x20 && tmsUpdateCheck[1] == (byte)0x20) {

                        } else {
                            //                                    m_menuActivity.ReadyDialogHide();
                            progressDoalog.dismiss();
                            ShowDialog("업데이트 불가 제품입니다");
                            return;
                        }
//                        String filler = new String(bArray.CutToSize(38));  //Filler
//                        bArray.CutToSize(2);    //ETX, LRC
                        TMS_Data_Down_Info(revDataType,multipadSerialNum,data);
                        break;
                }
            });
        });


        mPosSdk.BleConnectionListener(result -> {

            if(result==true)
            {
//                Toast.makeText(mPosSdk.getContext(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
//                m_menuActivity.ReadyDialogHide();
                new Handler(Looper.getMainLooper()).post(()-> {
                    if(Setting.getBleName().equals(Setting.getPreference(m_menuActivity,Constants.BLE_DEVICE_NAME)))
                    {
                        BleDeviceInfo();
                    }
                    else
                    {
                        Setting.setPreference(m_menuActivity, Constants.BLE_DEVICE_NAME, Setting.getBleName());
                        Setting.setPreference(m_menuActivity, Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                        setBleInitializeStep();
                    }
//                    new Handler(Looper.getMainLooper()).postDelayed(()->{
//                        mPosSdk.__BLEPosInfo(Utils.getDate("yyyyMMddhhmmss"), res -> {
//                            Log.d("jiw", Utils.bytesToHex(res));
//                        });
//                    },500);
                });
            }
            else
            {
//                Toast.makeText(this.getContext(),"연결에 실패하였습니다", Toast.LENGTH_LONG).show();
                if( m_menuActivity != null) {
                    m_menuActivity.ReadyDialogHide();
                    mTxtBleStatus.setText(mStrNoConntected);
                    setBleDeviceInfo("","","");
                }

            }
        });
        mPosSdk.BleWoosimConnectionListener(result -> {

            if(result==true)
            {
//                Toast.makeText(mPosSdk.getContext(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
//                m_menuActivity.ReadyDialogHide();
                new Handler(Looper.getMainLooper()).post(()-> {
                    if(Setting.getBleName().equals(Setting.getPreference(m_menuActivity,Constants.BLE_DEVICE_NAME)))
                    {
                        BleDeviceInfo();
                    }
                    else
                    {
                        Setting.setPreference(m_menuActivity, Constants.BLE_DEVICE_NAME, Setting.getBleName());
                        Setting.setPreference(m_menuActivity, Constants.BLE_DEVICE_ADDR, Setting.getBleAddr());
                        setBleInitializeStep();
                    }
//                    new Handler(Looper.getMainLooper()).postDelayed(()->{
//                        mPosSdk.__BLEPosInfo(Utils.getDate("yyyyMMddhhmmss"), res -> {
//                            Log.d("jiw", Utils.bytesToHex(res));
//                        });
//                    },500);
                });
            }
            else
            {
//                Toast.makeText(this.getContext(),"연결에 실패하였습니다", Toast.LENGTH_LONG).show();
                if( m_menuActivity != null) {
                    m_menuActivity.ReadyDialogHide();
                    mTxtBleStatus.setText(mStrNoConntected);
                    setBleDeviceInfo("","","");
                }

            }
        });

        /** 터치서명 사인패드 크기 설정 */
        m_cbx_size_touchSignpad = view.findViewById(R.id.spinner_f_size_signpad);
        mTouchSignPadSize = Setting.getPreference(mPosSdk.getActivity(), Constants.SIGNPAD_SIZE);
        m_cbx_size_touchSignpad.setSelection(getIndex(m_cbx_size_touchSignpad,mTouchSignPadSize));
        m_cbx_size_touchSignpad.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //               ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(view!=null) {
                    ((TextView) parent.getChildAt(0)).setTextSize(14);
                }
                switch (position)
                {
                    case 0://전체
                        mTouchSignPadSize = Constants.TouchSignPadSize.FullScreen.toString();
                        break;
                    case 1://보통
                        mTouchSignPadSize = Constants.TouchSignPadSize.Middle.toString();
                        break;
                    case 2://작게
                        mTouchSignPadSize = Constants.TouchSignPadSize.Small.toString();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        /** Ble 페어링 해제 후 전원저장 */
        mBtnBlePower.setOnClickListener( v -> {
            mPosSdk.BlePowerManager(null,mBlePowerManageByte);
            switch (mBlePowerManage)
            {
                case "상시유지":
                    Setting.setPreference(mPosSdk.getActivity(),Constants.BLE_POWER_MANAGE,
                            Constants.BlePowerManager.AllWays.toString());

                    break;
                case "5분":
                    Setting.setPreference(mPosSdk.getActivity(),Constants.BLE_POWER_MANAGE,
                            Constants.BlePowerManager.FiveMinute.toString());

                    break;
                case "10분":
                    Setting.setPreference(mPosSdk.getActivity(),Constants.BLE_POWER_MANAGE,
                            Constants.BlePowerManager.TenMinute.toString());

                    break;
            }
           Toast.makeText(mPosSdk.getContext(),"단말기 전원 유지 시간은 " + mBlePowerManage + " 입니다.",Toast.LENGTH_LONG).show();
        });
        /** Ble 페어링 해제 후 전원 설정 */
        if(Setting.getPreference(mPosSdk.getActivity(), Constants.BLE_POWER_MANAGE).equals(""))
        {
            Setting.setPreference(mPosSdk.getActivity(),Constants.BLE_POWER_MANAGE, Constants.BlePowerManager.AllWays.toString());
        }
        mBlePowerManage = Setting.getPreference(mPosSdk.getActivity(), Constants.BLE_POWER_MANAGE);
        switch (mBlePowerManage)
        {
            case "상시유지":
                mBlePowerManageByte = Command.CMD_BLE_POWER_ALL;
                break;
            case "5분":
                mBlePowerManageByte = Command.CMD_BLE_POWER_05;
                break;
            case "10분":
                mBlePowerManageByte = Command.CMD_BLE_POWER_10;
                break;
        }
        m_cbx_ble_powermanage.setSelection(getIndex(m_cbx_ble_powermanage,mBlePowerManage));
        m_cbx_ble_powermanage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //               ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(view!=null) {
                    ((TextView) parent.getChildAt(0)).setTextSize(14);
                }
                switch (position)
                {
                    case 0://상시유지
                        mBlePowerManage = Constants.BlePowerManager.AllWays.toString();
                        mBlePowerManageByte = Command.CMD_BLE_POWER_ALL;
                        break;
                    case 1://5분
                        mBlePowerManage = Constants.BlePowerManager.FiveMinute.toString();
                        mBlePowerManageByte = Command.CMD_BLE_POWER_05;
                        break;
                    case 2://10분
                        mBlePowerManage = Constants.BlePowerManager.TenMinute.toString();
                        mBlePowerManageByte = Command.CMD_BLE_POWER_10;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    //private method of your class
    private int getIndex(Spinner spinner, String myString){
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                return i;
            }
        }
        return 0;
    }

    int _bleCount = 0;
    private void setBleInitializeStep()
    {
        mBleName = Setting.getPreference(this.getContext(), Constants.BLE_DEVICE_NAME);
//        mTxtName.setText(mBleName);
        mBleAddr = Setting.getPreference(this.getContext(),Constants.BLE_DEVICE_ADDR);




        m_menuActivity.ReadyDialogShow(m_menuActivity, "무결성 검증 중 입니다.",0);
        _bleCount += 1;
//        mTxtAddr.setText(mBleAddr);
//        mDlgBox = new DialogBox(ReaderActivity.this,
//                "장치 초기화",
//                "장치 초기화 작업중입니다",
//                "",
//                "",
//                true,
//                new DialogBox.DialogBoxListener() {
//                    @Override
//                    public void onClickCancel() {
//
//                    }
//
//                    @Override
//                    public void onClickConfirm() {
//
//                    }
//                });
//
//        mDlgBox.show();
//        if(mDlgBox!=null)
        {
            /* 무결성 검증. 초기화진행 */
            DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(m_menuActivity, (result, Code, state, resultData) -> {

                if (result.equals("00")) {
                    mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 1);  //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
//                    m_menuActivity.ReadyDialogHide();
                    Setting.g_PayDeviceType = Setting.PayDeviceType.BLE;    //어플 전체에서 사용할 결제 방식
                    String temp = String.valueOf(Setting.g_PayDeviceType);
                    Setting.setPreference(this.getContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                }  else if (result.equals("9999")) {
                    Log.d("frag_device_ble","무결성검증 타임아웃인 경우 db에 갱신하지 않음");
                    Setting.g_PayDeviceType = Setting.PayDeviceType.BLE;    //어플 전체에서 사용할 결제 방식
                    String temp = String.valueOf(Setting.g_PayDeviceType);
                    Setting.setPreference(this.getContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                } else {
                    mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1);
//                    m_menuActivity.ReadyDialogHide();
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (result.equals("00")) {
                            Toast.makeText(m_menuActivity, "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                        }
                        else if(result.equals("9999"))
                        {
                            mbleSdk = bleSdk.getInstance();
                            if(_bleCount >1)
                            {
                                _bleCount = 0;
                                mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 1);
                                Toast.makeText(m_menuActivity, "네트워크 오류. 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
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
                                    mPosSdk.BleConnect(m_menuActivity,
                                            Setting.getPreference(m_menuActivity,Constants.BLE_DEVICE_ADDR),
                                            Setting.getPreference(m_menuActivity,Constants.BLE_DEVICE_NAME));
                                },500);
                                return;
                            }



                        }
                        else {
                            Toast.makeText(m_menuActivity, "무결성 검증에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                        }

                    }
                },200);

                if(!result.equals("00"))
                {
                    return;
                }
                new Handler().postDelayed(()->{
                    //장치 정보 요청
                    Toast.makeText(m_menuActivity, "무결성 검증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                    BleDeviceInfo();
                },500);
            });

            deviceSecuritySDK.Req_BLEIntegrity();   /* 무결성 검증. 키갱신 요청 */

        }
    }

    /**
     * ble 리더기 식별번호 표시를 위한 장치 정보 요청 함
     */
    int _bleDeviceCheck = 0;
    private void BleDeviceInfo(){
        _bleDeviceCheck += 1;
        mPosSdk.__BLEPosInfo(Utils.getDate("yyyyMMddHHmmss"), res ->{
            m_menuActivity.ReadyDialogHide();
            Toast.makeText(m_menuActivity,"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
            mTxtBleStatus.setText(mStrConnected);
            if(res[3]==(byte)0x15){
                //장비에서 NAK 올라 옮
                return;
            }
            if (res.length == 6) {      //ACK가 6바이트 올라옴
                return;
            }
            if (res.length < 50) {
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

            Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_NAME,authNum);
            Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
            Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_VERSION,version);
            //공백을 제거하여 추가 한다.
            String tmp = authNum.trim();
//            Setting.mAuthNum = authNum.trim(); //BLE는 이것을 쓰지 않는다. 유선이 사용한다
//            mtv_icreader.setText(tmp);//메인에서 사용하지 않는다. 다른 뷰에서 사용
            //무결성 검사가 성공/실패 의 결과값이 어쨌든 나와야 한다
            setBleDeviceInfo(authNum,serialNum,version);
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(_bleDeviceCheck==1)
                {
                    mbleSdk = bleSdk.getInstance();
                    mbleSdk.DisConnect();
                    new Handler().postDelayed(()->{
                        m_menuActivity.ReadyDialogShow(mPosSdk.getActivity(), "장치 연결 중 입니다.", 0);
                    },200);

                    new Handler().postDelayed(()->{
//                                ShowDialog("네트워크 오류로 장치를 1회 재연결 합니다");
                        mPosSdk.BleConnect(mPosSdk.getActivity(),
                                Setting.getPreference(mPosSdk.getActivity(),Constants.BLE_DEVICE_ADDR),
                                Setting.getPreference(mPosSdk.getActivity(),Constants.BLE_DEVICE_NAME));
                    },500);
                    return;
                } else if (_bleDeviceCheck > 1)
                {
                    _bleDeviceCheck = 0;
                    mbleSdk = bleSdk.getInstance();
                    Toast.makeText(mPosSdk.getActivity(), "블루투스 통신 오류. 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
                    mbleSdk.DisConnect();
                    _bleDeviceCheck = 0;
                    return;
                }
                else if (_bleDeviceCheck == 0)
                {
                    //정상
                }

            }
        },2000);
    }

    private void ShowDialog(String _str)
    {
        Toast.makeText(m_menuActivity,_str,Toast.LENGTH_SHORT).show();
    }
    private void setBleDeviceInfo(String productNum,String SerialNum,String Ver){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (productNum.length()>16) {
                    mTxtProductNum.setText(productNum.substring(0,16));
                } else {
                    mTxtProductNum.setText(productNum);
                }
                mDeviceVersion = Ver;
                mTxtSerialNum.setText(SerialNum);
                mTxtVer.setText(Ver);
            }
        },200);

    }
    private void PermissionCheck() {
        int permissions_code = 42;
        String[] permissions = {Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.CALL_PHONE,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,};
        if(!hasPermissions(this.getContext(), permissions)){
            ActivityCompat.requestPermissions(this.getActivity(), permissions, permissions_code);
        }

        if (Build.VERSION.SDK_INT >= 30) {
            String[] BACKGROUND_LOCATION_PERMISSIONS = {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,

            };

            if(!hasPermissions(this.getContext(), permissions)){
                ActivityCompat.requestPermissions(this.getActivity(), BACKGROUND_LOCATION_PERMISSIONS, permissions_code);
            }
        }

    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private bleSdkInterface.ResDataListener keyDownloadresDataListener = res -> {
        final byte[] temp = res;
//        Command.CMDInfo cmdInfo = Command.make(temp);
        new Handler(Looper.getMainLooper()).post(()-> {
//            responseData(cmdInfo,res);
        });

    };

    /**
     * 안드로이드 sharedPreference에 저장되어 있는 TID를 가져 오는 함수
     * @return String
     */
    private String getTID()
    {
        return Setting.getPreference(mPosSdk.getActivity(),Constants.TID);
    }


    private void TMS_Data_Down_Info (String revDataType, byte[] multipadSerialNum, byte[] data) {
        mPosSdk.___TMSDataDownInfo("9240", Setting.getPreference(mPosSdk.getActivity(), Constants.TID), mDeviceVersion,multipadSerialNum,
                revDataType, data, new TcpInterface.DataListener() {
                    @Override
                    public void onRecviced(byte[] _rev) {
                        Log.d("TAG", String.valueOf(_rev));
                        Log.d("TAG", String.valueOf(_rev));
                        String mLog = "데이터 : " + Utils.bytesToHex_0xType(_rev);
                        mPosSdk.cout("VAN_REC : TMS_단말기데이타다운로드정보응답 9250",Utils.getDate("yyyyMMddHHmmss"),mLog);

                        KByteArray b  = new KByteArray(_rev);
                        b.CutToSize(1); //STX
                        b.CutToSize(4); //전문총길이
                        b.CutToSize(4); //거래전문번호
                        b.CutToSize(4); //필드구분자정의
                        b.CutToSize(1); //fs
                        byte[] _type = b.CutToSize(1);//유무선구분
                        byte[] _tid = b.CutToSize(10); //단말기ID
                        byte[] _version = b.CutToSize(5); //현재 단말기 버전
                        byte[] _serialNum = b.CutToSize(10); //단말기 시리얼 번호
                        b.CutToSize(1); //fs
                        byte[] _Ans_Cd = b.CutToSize(4); //응답코드
                        b.CutToSize(1); //fs
                        byte[] _message = b.CutToSize(80); //응답메세지
                        b.CutToSize(1); //fs
//                        try {
//                            Ans_Cd = Utils.getByteToString_euc_kr(_Ans_Cd);
//                            message = Utils.getByteToString_euc_kr(_message);
//                        } catch (UnsupportedEncodingException e) {
//                            e.printStackTrace();
//                        }
//                        if (!Ans_Cd.equals("0000")) {
//                            progressDoalog.dismiss();
//                            ShowDialog("펌웨어실패: "+ Ans_Cd + ","+ message);
//                            return;
//                        }
                        int _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == Command.FS) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Secu_Key = b.CutToSize(_re); //접속보안키
                        b.CutToSize(1); //fs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == Command.FS) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Token_cs_Path = b.CutToSize(_re); //인증키 경로명
                        b.CutToSize(1); //fs
                        byte[] _Data_Cnt = b.CutToSize(4); //데이터 개수
                        b.CutToSize(1); //fs
                        byte[] _Protocol = b.CutToSize(5); //전송 프로토콜
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Addr = b.CutToSize(_re); //다운로드서버주소
                        b.CutToSize(1); //Gs
                        byte[] _Port = b.CutToSize(5); //다운로드서버포트
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _User_ID = b.CutToSize(_re); //계정번호
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Password = b.CutToSize(_re); //계정비밀번호
                        b.CutToSize(1); //Gs
                        byte[] _Data_Type = b.CutToSize(5); //버전 및 데이터 구분
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Data_Desc = b.CutToSize(_re); //버전(데이터) 설명
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _File_Path_Name = b.CutToSize(_re); //파일명
                        b.CutToSize(1); //Gs
                        byte[] _File_Size = b.CutToSize(10); //파일크기
                        b.CutToSize(1); //Gs
                        byte[] _Check_Sum_Type = b.CutToSize(5); //파일체크방식
                        b.CutToSize(1); //Gs
                        byte[] _File_Check_Sum = b.CutToSize(64); //파일 체크섬
                        byte[] _checkKey = Utils.hexStringToByteArray(new String(_File_Check_Sum));
                        b.CutToSize(1); //Gs
                        byte[] _File_Encrypt_key = b.CutToSize(32); //파일 암호화 키
                        byte[] _encKey = Utils.hexStringToByteArray(new String(_File_Encrypt_key));
                        mEesKey = _encKey;
                        b.CutToSize(1); //fs
                        byte[] _Response_Time = b.CutToSize(14); //응답시간
                        b.CutToSize(1); //ETX
                        b.CutToSize(1); //LRC

//                        type = "",tid = "",version = "",serialNum = "",Ans_Cd = "",message = "",Secu_Key = "",
//                                Token_cs_Path = "",Data_Cnt = "",Protocol = "",Addr = "",Port = "",User_ID = "",Password = "",
//                                Data_Type = "",Data_Desc = "",File_Path_Name = "",File_Size = "",Check_Sum_Type = "",
//                                File_Check_Sum = "",File_Encrypt_key = "",Response_Time = "";
                        try {
                            Data_Type = Utils.getByteToString_euc_kr(_Data_Type);
                            Data_Desc = Utils.getByteToString_euc_kr(_Data_Desc);
                            File_Path_Name = Utils.getByteToString_euc_kr(_File_Path_Name);
                            File_Size = Utils.getByteToString_euc_kr(_File_Size);
                            Check_Sum_Type = Utils.getByteToString_euc_kr(_Check_Sum_Type);
                            File_Check_Sum = Utils.getByteToString_euc_kr(_File_Check_Sum);
                            File_Encrypt_key = Utils.getByteToString_euc_kr(_File_Encrypt_key);
                            Response_Time = Utils.getByteToString_euc_kr(_Response_Time);

                            type = Utils.getByteToString_euc_kr(_type);
                            tid = Utils.getByteToString_euc_kr(_tid);
                            version = Utils.getByteToString_euc_kr(_version);
                            serialNum = Utils.getByteToString_euc_kr(_serialNum);
                            Ans_Cd = Utils.getByteToString_euc_kr(_Ans_Cd);
                            message = Utils.getByteToString_euc_kr(_message);
                            Secu_Key = Utils.getByteToString_euc_kr(_Secu_Key);
                            Token_cs_Path = Utils.getByteToString_euc_kr(_Token_cs_Path);
                            Data_Cnt = Utils.getByteToString_euc_kr(_Data_Cnt);
                            Protocol = Utils.getByteToString_euc_kr(_Protocol);
                            Addr = Utils.getByteToString_euc_kr(_Addr);
                            Port = Utils.getByteToString_euc_kr(_Port);
                            User_ID = Utils.getByteToString_euc_kr(_User_ID);
                            Password = Utils.getByteToString_euc_kr(_Password);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        byte [] secu = Utils.hexStringToByteArray(Secu_Key);

                        String finalResponse_Time = Response_Time;
                        String finalAns_Cd = Ans_Cd;
                        String finalMessage = message;
                        String finalData_Cnt = Data_Cnt;
                        String finalProtocol = Protocol;
                        String finalAddr = Addr;
                        String finalPort = Port;
                        String finalUser_ID = User_ID;
                        String finalPassword = Password;
                        String finalData_Type = Data_Type;
                        String finalData_Desc = Data_Desc;
                        String finalFile_Path_Name = File_Path_Name;
                        String finalFile_Size = File_Size;
                        String finalCheck_Sum_Type = Check_Sum_Type;
                        String finalFile_Check_Sum = File_Check_Sum;
                        String finalFile_Encrypt_key = File_Encrypt_key;
                        try {
                            Ans_Cd = Utils.getByteToString_euc_kr(_Ans_Cd);
                            message = Utils.getByteToString_euc_kr(_message);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mPosSdk.__BLEauthenticatoin_result_req(_Response_Time,multipadAuthNum,multipadSerialNum, _Ans_Cd, _message,
                                        secu, _Data_Cnt, _Protocol, _Addr, _Port, _User_ID, _Password, _Data_Type, _Data_Desc,
                                        _File_Path_Name, _File_Size, _Check_Sum_Type, _checkKey, _encKey,
                                        res ->{
                                            Log.d("TAG", String.valueOf(res));
                                            String mLog = "데이터 : " + Utils.bytesToHex_0xType(res);
                                            mPosSdk.cout("BLE_READER_REC : 상호인증정보결과응답 0x55",Utils.getDate("yyyyMMddHHmmss"),mLog);

                                            if (!Ans_Cd.equals("0000")) {
                                                progressDoalog.dismiss();
                                                ShowDialog("펌웨어실패: "+ Ans_Cd + ","+ message);
                                                return;
                                            }

                                            Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(res);
                                            switch (protocolInfo.Command) {
                                                case Command.ACK:
                                                    Log.d("TAG", String.valueOf(res));
                                                    break;
                                                case Command.NAK:
//                                                    m_menuActivity.ReadyDialogHide();
                                                    progressDoalog.dismiss();
                                                    ShowDialog("상호인증정보결과요청 실패");
                                                    break;
                                                case Command.CMD_MUTUAL_AUTHENTICATION_RESULT_RES:
                                                    Log.d("TAG", String.valueOf(res));
                                                    Log.d("TAG", String.valueOf(res));
                                                    KByteArray b  = new KByteArray(res);
                                                    b.CutToSize(1); //STX
                                                    b.CutToSize(2); //데이터길이
                                                    b.CutToSize(1); //CommandID
                                                    byte[] _response = b.CutToSize(2);//응답결과
                                                    byte[] _message = b.CutToSize(20); //메세지
                                                    try {
                                                        String response = Utils.getByteToString_euc_kr(_response);
                                                        String message = Utils.getByteToString_euc_kr(_message);
                                                        if (!response.equals("00")) {
//                                                            m_menuActivity.ReadyDialogHide();
                                                            progressDoalog.dismiss();
                                                            ShowDialog("결과요청 실패 :" + message);
                                                            return;
                                                        }
                                                    } catch (UnsupportedEncodingException e) {
                                                        e.printStackTrace();
                                                    }
                                                    byte[] _fileDecKey = b.CutToSize(16);//파일 복호화 키
                                                    byte[] _fileDecKeyTemp = b.CutToSize(16);//파일 복호화 키 남은 부분은 버린다
                                                    mAesKey = _fileDecKey;

                                                    byte[] _updateSize = b.CutToSize(10); //업데이트 크기
//                                                    byte[] _secKey = b.CutToSize(128);//보안키
//                                                    b.CutToSize(1); //etx
//                                                    b.CutToSize(1);//lrc
                                                    try {
                                                        String size = Utils.getByteToString_euc_kr(_updateSize);
                                                        size = size.replace(" ","");
                                                        defaultSize = Integer.valueOf(size);
                                                    } catch (UnsupportedEncodingException e) {
                                                        e.printStackTrace();
                                                    }
                                                    ConnectFTP connectFTP = new ConnectFTP();
                                                    new Thread() {
                                                        public void run() {
                                                            try {
                                                                String mLog = "FTP주소 : " + Addr+ "," +"FTP포트 : " + Port + "," + "계정 : " + User_ID+ "," +
                                                                        "비밀번호 : " + Password+ "," +"파일주소 : " + File_Path_Name + "," + "파일복호화키 : " + Utils.bytesToHex_0xType(mAesKey);
                                                                mPosSdk.cout("FTP_SERVER_SEND : Ftp서버에 요청",Utils.getDate("yyyyMMddHHmmss"),mLog);

                                                                connectFTP.fileDownload(Addr, Integer.parseInt(Port), User_ID, Password, File_Path_Name, mAesKey, new ConnectFTP.FTPListener() {
                                                                    @RequiresApi(api = Build.VERSION_CODES.O)
                                                                    @Override
                                                                    public void onSuccess(byte[] _result, byte[] _original) {
                                                                        try {
//                                                                        byte[] _b2 = Utils.Aes128Dec(_result,mAesKey);
//                                                                        TMS_Update_File_Send_First(_b2);
                                                                            KByteArray _r  = new KByteArray(_result);
                                                                            KByteArray _o  = new KByteArray(_original);
                                                                            String _r100 = Utils.bytesToHex_0xType(_r.CopyToSize(100));
                                                                            Log.d("_r100", _r100);
                                                                            String _o100 = Utils.bytesToHex_0xType(_o.CopyToSize(100));
                                                                            String mLog = "복호화된 파일(100) : " + _r100+ "," +"서버에서받은 파일(100) : " + _o100;
                                                                            mPosSdk.cout("FTP_SERVER_REC : Ftp서버에서 받은데이터",Utils.getDate("yyyyMMddHHmmss"),mLog);

                                                                            TMS_Update_File_Send_First(_result);
                                                                        } catch (Exception e) {
                                                                            progressDoalog.dismiss();
                                                                            ShowDialog("FTP실패: " + e);
//                                                                            e.printStackTrace();
                                                                        }

//                                                                    TMS_Update_File_Send_First(_result);
                                                                    }

                                                                    @Override
                                                                    public void onFail(String _msg) {
//                                                                        m_menuActivity.ReadyDialogHide();
                                                                        progressDoalog.dismiss();
                                                                        ShowDialog("FTP실패: " + _msg);
                                                                    }
                                                                });
//                                                            connectFTP.connect(Addr,Integer.parseInt(Port),User_ID,Password,File_Path_Name);
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }

                                                        }
                                                    }.start();
                                                    break;

                                            }


                                        });
                            }
                        },200);
                    }
                });
    }

    int updateSize = 0; //총 업데이트 할 사이즈
    int recentSize = 0; //현재까지 업데이트 한 사이즈
    int defaultSize = 1024; //데이터를 한번에 보내는 기본사이즈.
    // ble는 이것보다 훨씬 적어야 한다 현재 ble는 한번에 보내는 양이 20으로 되어있다(mtu랑 별개임. ble내부에서 한번에 너무 많은 데이터가 들어오면 그거 전문대로 처리하는 것에서 문제가 생김.)
    private void TMS_Update_File_Send_First(byte[] _b) {
        updateSize = 0;
        recentSize = 0;

        String _size = String.valueOf(_b.length);
        updateSize = _b.length;

        KByteArray b = new KByteArray(_b);
        byte[] _sample = null;
        _sample = b.CutToSize(defaultSize);
        recentSize = defaultSize;

        mPercent = 0;
        progressDoalog.setProgress(mPercent);

        mPosSdk.__BLEupdatefile_transfer_req("0001", _size, String.valueOf(recentSize), defaultSize,_sample, res ->{
            Log.d("TAG", String.valueOf(res));
            String mLog = "데이터 : " + Utils.bytesToHex_0xType(res);
            mPosSdk.cout("BLE_READER_REC : 업데이트파일응답 0x57",Utils.getDate("yyyyMMddHHmmss"),mLog);

            Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(res);
            switch (protocolInfo.Command) {
                case Command.ACK:
                    Log.d("TAG", String.valueOf(res));
                    break;
                case Command.NAK:
//                    m_menuActivity.ReadyDialogHide();
                    progressDoalog.dismiss();
                    ShowDialog("업데이트파일전송 실패");
                    Log.d("TAG", String.valueOf(res));
                    break;
                case Command.CMD_SEND_UPDATE_DATA_RES:
                    try {
                        Log.d("TAG", String.valueOf(res));
                        KByteArray bArray = new KByteArray(res);
                        bArray.CutToSize(1);    //STX + 길이 + CommandID 삭제
                        String length = new String(bArray.CutToSize(2));   //길이
                        bArray.CutToSize(1);    //CommandID
                        String response =  Utils.getByteToString_euc_kr(bArray.CutToSize(2));  //응답결과
                        String message = Utils.getByteToString_euc_kr(bArray.CutToSize(20));  //정상 : 정상처리, 실패 : 실패사유

                        if(response.equals("01")) {
                            //실패
//                            m_menuActivity.ReadyDialogHide();
                            progressDoalog.dismiss();
                            ShowDialog("업데이트전송 실패: " + message);
                            Log.d("TAG", String.valueOf(res));
                            return;
                        }
                        bArray.CutToSize(2);    //ETX, LRC

                        TMS_Update_File_Send_loop(b.value());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    return;
            }
        });

    }

    private void TMS_Update_File_Send_loop(byte[] _b) {
        KByteArray b = new KByteArray(_b);
        byte[] _sample = null;
        if (b.getlength() < defaultSize) {
            _sample = b.value();
        } else {
            _sample = b.CutToSize(defaultSize);
        }

        recentSize += defaultSize;

        int _percent = (int) (recentSize * 100)/updateSize;
        if (_percent >= 100) {
            _percent = 100;
        }
        Log.d("TAG", "퍼센트 : " +  String.valueOf(_percent));
        mPercent = _percent;
        progressDoalog.setProgress(mPercent);

        mPosSdk.__BLEupdatefile_transfer_req("0001", String.valueOf(updateSize), String.valueOf(recentSize),defaultSize, _sample, res ->{
            Log.d("TAG", String.valueOf(res));
            String mLog = "데이터 : " + Utils.bytesToHex_0xType(res);
            mPosSdk.cout("BLE_READER_REC : 업데이트파일응답 0x57",Utils.getDate("yyyyMMddHHmmss"),mLog);

            Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(res);
            switch (protocolInfo.Command) {
                case Command.ACK:
                    Log.d("TAG", String.valueOf(res));
                    break;
                case Command.NAK:
//                    m_menuActivity.ReadyDialogHide();
                    progressDoalog.dismiss();
                    ShowDialog("업데이트파일전송 실패");
                    Log.d("TAG", String.valueOf(res));
                    break;
                case Command.CMD_SEND_UPDATE_DATA_RES:
                    Log.d("TAG", String.valueOf(res));
                    KByteArray bArray = new KByteArray(res);
                    bArray.CutToSize(1);    //STX + 길이 + CommandID 삭제
                    String length = new String(bArray.CutToSize(2));   //길이
                    bArray.CutToSize(1);    //CommandID
                    String response = "";  //응답결과
                    String message = "";
                    try {
                        response = Utils.getByteToString_euc_kr(bArray.CutToSize(2));
                        message = Utils.getByteToString_euc_kr(bArray.CutToSize(20));  //정상 : 정상처리, 실패 : 실패사유
                        if(response.equals("01")) {
                            //실패
//                            m_menuActivity.ReadyDialogHide();
                            progressDoalog.dismiss();
                            ShowDialog("업데이트전송 실패: " + message);
                            Log.d("TAG", String.valueOf(res));
                            return;
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    bArray.CutToSize(2);    //ETX, LRC

//                        recentSize += final_sample.length;
                    if (recentSize < updateSize) {
                        TMS_Update_File_Send_loop(b.value());
                        return;
                    } else {
                        //완료
                        m_menuActivity.ReadyDialogHide();
                        progressDoalog.dismiss();
                        new Handler(Looper.getMainLooper()).postDelayed(()->{
                            ShowDialog("펌웨어 업데이트 성공");
                            mPosSdk.BleDisConnect();

                        },1000);

                        return;
                    }

//                        break;
            }

        });

    }

    private void ShowProgressBar()
    {
        progressDoalog = new ProgressDialog(mCtx);
        progressDoalog.setMax(100);
        progressDoalog.setIndeterminate(false);
        progressDoalog.setMessage("업데이트 중입니다....");
        progressDoalog.setTitle("펌웨어 업데이트");
        progressDoalog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDoalog.show();
    }

}