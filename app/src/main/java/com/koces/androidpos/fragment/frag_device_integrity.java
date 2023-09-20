package com.koces.androidpos.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.koces.androidpos.ListDeviceAdapter;
import com.koces.androidpos.R;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.DeviceSecuritySDK;
import com.koces.androidpos.sdk.Devices.Devices;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.ble.MyBleListDialog;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.van.Constants;

/**
 * 무결성 검증을 수동으로 진행한며, 최근10번의 수동 +자동(앱시작시 자동실행) 무결성검증 리스트를 불러온다
 *  frag_device_integrity.java 는 아래의 순서로 들어온다
 *  환경설정버튼클릭 Main2Activity.java
 *    ↓
 * 비밀번호입력 PrefpasswdActivity.java
 *    ↓
 * 장치설정 frag_devices.java
 *    ↓
 * 무결성검증 frag_device_integrity.java
 */
public class frag_device_integrity extends Fragment {
    /** Log를 위한 TAG 선언. 디버깅시 로그확인 용으로 만들었다 */
    private final static String TAG = frag_device_integrity.class.getSimpleName();
    /** Activity 의 onCreate() 의 setContentView()와 같은 관련 UI(xml) 를 화면에 구성하는 View */
    ViewGroup m_view;
    /** menu2Activity.java 에서 가맹점설정 장치설정 관리자설정 무결성검증 등의 Fragment 가 속해있다 */
    menu2Activity m_menuActivity;
    /** 장치설정화면이동버튼, 수동무결성검증버튼  */
    Button m_btn_gotoDevice, m_btn_classfi;
    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 sqlite 로 데이터를 저장및 불러오기 위해 사용한다 */
    KocesPosSdk mPosSdk;
    /** sqlite에 저장된 최근무결성검사 10개를 리스트로 화면에 구성한다 */
    ListView listview ;
    /** 위 listview 에 연계되는 adapter 해당 리스트들의 각각의 데이터들의 위치 리스트추가 등의 기능을 수행한다다*/
    ListDeviceAdapter adapter;

    bleSdk mbleSdk;

    /**
     * 디바이스 설정 화면에서 앱 무결성 검사를 위한 클래스
     */
    public frag_device_integrity() {
        // Required empty public constructor
    }

    /**
     * //이 메소드가 호출될떄는 프래그먼트가 엑티비티위에 올라와있는거니깐 getActivity메소드로 엑티비티참조가능
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        m_menuActivity = (menu2Activity) getActivity();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        m_view = (ViewGroup) inflater.inflate(R.layout.frag_device_clasifi, container, false);
        init();
        return m_view;
    }

    Button.OnClickListener BtnOnClickListener = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.btn_f_classfi:
                    /* 장치 정보를 읽어서 설정 하는 함수         */
                    String deviceType = Setting.getPreference(m_menuActivity, Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
                    if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
                        Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
                    }else
                    {
                        Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
                        Setting.g_PayDeviceType = _type;
                    }

                    switch (Setting.g_PayDeviceType)
                    {
                        case NONE:
                            Toast.makeText(m_menuActivity, "환경설정에서 장비설정을 해주십시오", Toast.LENGTH_SHORT).show();
                            return;
                        case BLE:       //BLE의 경우
                            CheckBLEIntegrity();
                            break;
                        case CAT:       //WIFI CAT의 경우
                            Toast.makeText(m_menuActivity, "CAT 은 지원하지 않습니다", Toast.LENGTH_SHORT).show();
                            break;
                        case LINES:     //유선장치의 경우
                            CheckIntegrity();
                            break;
                        default:
                            break;
                    }


                    break;
                case R.id.btn_f_gotoDevice:
                    GoDevice();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 변수 및 xml ui 초기화 함수
     */
    private void init(){

        m_btn_classfi = m_view.findViewById(R.id.btn_f_classfi);
        m_btn_gotoDevice = m_view.findViewById(R.id.btn_f_gotoDevice);
        m_btn_classfi.setOnClickListener(BtnOnClickListener);
        m_btn_gotoDevice.setOnClickListener(BtnOnClickListener);

        adapter = new ListDeviceAdapter();
        listview = (ListView) m_view.findViewById(R.id.list_f_date);
        listview.setAdapter(adapter);
        mPosSdk = KocesPosSdk.getInstance();
        mPosSdk.setFocusActivity(m_menuActivity,null);

        mbleSdk = bleSdk.getInstance();

        UpdateList();

        mPosSdk.BleConnectionListener(result -> {

            if(result==true)
            {
                Toast.makeText(mPosSdk.getActivity(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
                m_menuActivity.ReadyDialogHide();
                new Handler(Looper.getMainLooper()).post(()-> {
                    setBleInitializeStep();

                });
            }
            else
            {
                if( m_menuActivity != null) {
                    m_menuActivity.ReadyDialogHide();
                }
            }
        });
        mPosSdk.BleWoosimConnectionListener(result -> {

            if(result==true)
            {
                Toast.makeText(mPosSdk.getActivity(),"연결에 성공하였습니다", Toast.LENGTH_SHORT).show();
                m_menuActivity.ReadyDialogHide();
                new Handler(Looper.getMainLooper()).post(()-> {
                    setBleInitializeStep();

                });
            }
            else
            {
                if( m_menuActivity != null) {
                    m_menuActivity.ReadyDialogHide();
                }
            }
        });
    }

    /**
     * DB에서 무결성 검사 리스트를 가져와서 ListView 에 업데이트 시킨다.
     */
    private void UpdateList()
    {
        adapter = new ListDeviceAdapter();
        String [] data = mPosSdk.getSqliteDB_IntegrityTableInfo(); /* sqlite 에서 무결성 검사 데이터 가져오기 */

        if(data!=null)
        {
            for(String n:data)
            {
                String[] tmp = n.split("\\|");
                adapter.addItem(tmp[0],tmp[1],tmp[2]);
            }
        }

        listview.setAdapter(adapter);

    }

    /**
     * BLE 장치 무결성 검사를 진행 한다.
     */
    private void CheckBLEIntegrity()
    {
        mPosSdk.BleIsConnected();
        if(Setting.getBleIsConnected())
        {
            //이미 연결되어 있을 때
            setBleInitializeStep();
        } else {
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
            //연결되어 있지 않다면 BLE 장비를 찾아서 진행한다.
            MyBleListDialog myBleListDialog = new MyBleListDialog(this.getContext()) {
                @Override
                protected void onSelectedBleDevice(String bleName, String bleAddr) {
                    if (bleName.equals("") || bleAddr.equals(""))
                    {
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
                    Setting.setBleAddr(bleAddr);
                    Setting.setBleName(bleName);
                }

                @Override
                protected void onFindLastBleDevice(String bleName, String bleAddr) {
                    m_menuActivity.ReadyDialogShow(m_menuActivity, "장치에 연결중입니다", 0);
                    if (!mPosSdk.BleConnect(m_menuActivity,bleAddr, bleName)) {
                        Toast.makeText(mPosSdk.getContext(), "리더기 연결에 실패하였습니다", Toast.LENGTH_SHORT).show();
                    }
                    Setting.setBleAddr(bleAddr);
                    Setting.setBleName(bleName);
                }
            };

            myBleListDialog.show();
        }
    }



    /**
     * 장치 무결성 검사를 진행 한다.
     */
    private void CheckIntegrity()
    {
//        Setting.Showpopup(m_menuActivity,"무결성 검사 진행 중입니다.", null, null);
        m_menuActivity.ReadyDialogShow(m_menuActivity, "무결성 검사 진행 중입니다.",0);
        /* 연결된 장치가 있는지 체크 */
        if(mPosSdk.mDevicesList!=null) {
            for(Devices n: mPosSdk.mDevicesList)
            {
                if(n.getmType()==Command.TYPEDEFINE_ICCARDREADER)   /* 무결성검증 할 장치의 타입이 카드리더기인지 체크 */
                {
                    /* 무결성 검증. 초기화진행 */
                    DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(m_menuActivity, Command.TYPEDEFINE_ICCARDREADER, "", (result, Code, state, resultData) -> {

                        if (result.equals("00")) {
                            mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 0);  //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
                        } else {
                            mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 0);
                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                m_menuActivity.ReadyDialogHide();
//                                Setting.HidePopup();
                                UpdateList();
                            }
                        },1000);
//            m_menuActivity.ReadyDialogHide();

                    });

                    deviceSecuritySDK.Req_Integrity();   /* 무결성 검증. 키갱신 요청 */
                }
                else if(n.getmType()==Command.TYPEDEFINE_ICMULTIREADER) /* 무결성 검증 할 장치의 타입이 멀티리더기인지 체크 */
                {
                    DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(m_menuActivity, Command.TYPEDEFINE_ICMULTIREADER, "", (result, Code, state, resultData) -> {

                        if (result.equals("00")) {
                            mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 0);
                        } else {
                            mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 0);
                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                m_menuActivity.ReadyDialogHide();
//                                Setting.HidePopup();
                                UpdateList();
                            }
                        },1000);
//            m_menuActivity.ReadyDialogHide();

                    });

                    deviceSecuritySDK.Req_Integrity();
                }
            }
        }
        else
        {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    m_menuActivity.ReadyDialogHide();
//                    Setting.HidePopup();
                    mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 0);
                    UpdateList();
                }
            },1000);
        }

    }
    int _bleCount = 0;
    private void setBleInitializeStep()
    {
        m_menuActivity.ReadyDialogShow(m_menuActivity, "무결성 검증 중 입니다.",0);
        _bleCount += 1;
        /* 무결성 검증. 초기화진행 */
        DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(m_menuActivity, (result, Code, state, resultData) -> {

            if (result.equals("00")) {
                mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 1, 0);  //정상적으로 키갱신이 진행되었다면 sqlite 데이터 "성공"기록하고 비정상이라면 "실패"기록
                m_menuActivity.ReadyDialogHide();
                Setting.g_PayDeviceType = Setting.PayDeviceType.BLE;    //어플 전체에서 사용할 결제 방식
                String temp = String.valueOf(Setting.g_PayDeviceType);
                Setting.setPreference(this.getContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
            } else {
                mPosSdk.setSqliteDB_IntegrityTable(Utils.getDate(), 0, 0);
                m_menuActivity.ReadyDialogHide();
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
                            Toast.makeText(m_menuActivity, "네트워크 오류. 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
                            mbleSdk = bleSdk.getInstance();
                            mbleSdk.DisConnect();
                        }
                        else {
                            mbleSdk = bleSdk.getInstance();
                            mbleSdk.DisConnect();
                            new Handler().postDelayed(()->{
                                m_menuActivity.ReadyDialogShow(m_menuActivity, "무결성 검증 중 입니다.",0);
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

                    if(result.equals("9999"))
                    {
                        return;
                    }
                    UpdateList();
                }
            },200);
        });

        deviceSecuritySDK.Req_BLEIntegrity();   /* 무결성 검증. 키갱신 요청 */

    }

    /**
     * 장치 설정 화면으로 이동 한다
     */
    private void GoDevice(){
        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(m_menuActivity, Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }

        switch (Setting.g_PayDeviceType)
        {
            case NONE:
                m_menuActivity.setFrag(2);
                return;
            case BLE:       //BLE의 경우
                m_menuActivity.setFrag(6);
                break;
            case CAT:       //WIFI CAT의 경우
                m_menuActivity.setFrag(2);
                break;
            case LINES:     //유선장치의 경우
                m_menuActivity.setFrag(5);
                break;
            default:
                break;
        }

    }
}
