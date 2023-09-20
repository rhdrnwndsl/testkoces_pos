package com.koces.androidpos.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.koces.androidpos.Main2Activity;
import com.koces.androidpos.R;
import com.koces.androidpos.SignPadActivity;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.van.Constants;

/**
 * 환경 설정 manager 클래스
 */
public class frag_manager extends Fragment {
    /**
     * Log를 위한 TAG 설정
     */
    private final static String TAG = frag_manager.class.getSimpleName();
    /** Activity 의 onCreate() 의 setContentView()와 같은 관련 UI(xml) 를 화면에 구성하는 View */
    View m_view;
    /** menu2Activity.java 에서 가맹점설정 장치설정 관리자설정 무결성검증 등의 Fragment 가 속해있다 */
    menu2Activity m_menuActivity;
    /** 화면에 구성되는 버튼 */
    Button m_btn_reset,
            m_btn_ic_set,
            m_btn_ms_set,
            m_btn_status_check,
            m_btn_request_sign,
            m_btn_request_passwd,
            m_btn_request_qr,
            m_btn_request_consumer,
            m_btn_save;
    EditText m_txt_ip, m_txt_port;
    /** 사용하지않음 */
    private byte mLastCommnad;
    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 장치설정을 불러오거나 저장하기 위해 사용한다 */
    KocesPosSdk mPosSdk;
    /** 사인패드를 intent 로 호출하여 사인패드의 결과값을 받을 때 지정하는 키값. 해당 키값으로 사인패드의 결과값을 확인한다*/
    private int REQUEST_SIGNPAD = 10001;

    public frag_manager() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //이 메소드가 호출될떄는 프래그먼트가 엑티비티위에 올라와있는거니깐 getActivity메소드로 엑티비티참조가능
        m_menuActivity = (menu2Activity) getActivity();
    }
    @Override
    public void onDetach() {
        super.onDetach();
        m_menuActivity = null;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        m_view = (ViewGroup) inflater.inflate(R.layout.frag_manager , container, false);
        init();
        return m_view;
    }

    /**
     * 초기화 변수 및 xml ui 초기화
     */
    private void init(){
        mPosSdk = KocesPosSdk.getInstance();
        mPosSdk.setFocusActivity(m_menuActivity,null);
        m_btn_reset = m_view.findViewById(R.id.btn_f_reset);
        m_btn_ic_set = m_view.findViewById(R.id.btn_f_ic_set);
        m_btn_ms_set = m_view.findViewById(R.id.btn_f_ms_set);
        m_btn_status_check = m_view.findViewById(R.id.btn_f_status_check);
        m_btn_request_sign = m_view.findViewById(R.id.btn_f_request_sign);
        m_btn_request_consumer = m_view.findViewById(R.id.btn_f_request_consumer);
        m_btn_request_qr = m_view.findViewById(R.id.btn_f_request_qr);
        m_btn_request_passwd = m_view.findViewById(R.id.btn_f_request_passwd);
        m_btn_save = m_view.findViewById(R.id.btn_f_manager_save);

        m_btn_reset.setOnClickListener(BtnOnClickListener);
        m_btn_ic_set.setOnClickListener(BtnOnClickListener);
        m_btn_ms_set.setOnClickListener(BtnOnClickListener);
        m_btn_request_consumer.setOnClickListener(BtnOnClickListener);
        m_btn_request_qr.setOnClickListener(BtnOnClickListener);
        m_btn_status_check.setOnClickListener(BtnOnClickListener);
        m_btn_request_sign.setOnClickListener(BtnOnClickListener);
        m_btn_request_passwd.setOnClickListener(BtnOnClickListener);
        m_btn_save.setOnClickListener(BtnOnClickListener);

        //초기화할때 ip, port 셋팅
        m_txt_ip = m_view.findViewById(R.id.txt_ip);
        m_txt_port = m_view.findViewById(R.id.txt_port);
        m_txt_ip.setText(Setting.getIPAddress(mPosSdk.getActivity()));
        m_txt_port.setText(Setting.getVanPORT(mPosSdk.getActivity()));
    }

    Button.OnClickListener BtnOnClickListener = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.btn_f_reset:
                    Reset();
                    break;
                case R.id.btn_f_ic_set:
                    IcSet();
                    break;
                case R.id.btn_f_ms_set:
                    MsSet();
                    break;
                case R.id.btn_f_request_qr:
                    RequestQr();
                    break;
                case R.id.btn_f_status_check:
                    StatusCheck();
                    break;
                case R.id.btn_f_request_sign:
                    RequestSign();
                    break;
                case R.id.btn_f_request_consumer:
                    RequestKey();
                    break;
                case R.id.btn_f_request_passwd:
                    RequestPasswd();
                    break;
                case R.id.btn_f_manager_save:
                    Save();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 모든 장치 초기화
     */
    private void Reset(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                mPosSdk.__PosInit("99",null,mPosSdk.AllDeviceAddr());

//                mPosSdk.__PosInit("99",null,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
//                mPosSdk.__PosInit("99",null,mPosSdk.getICReaderAddr2());
            }
        });
    }

    /**
     * IC 카드 테스트<br>
     *    (미구현)
     */
    private void IcSet(){

    }
    /**
     * MS 카드 테스트<br>
     *    (미구현)
     */
    private void MsSet(){

    }
    /**
     * QR 테스트<br>
     *    (미구현)
     */
    private  void RequestQr()
    {

    }
    /**
     * 카드 넣기 테스트<br>
     *    (미구현)
     */
    private void InsertCard(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mPosSdk.getUsbDevice().size()!=0) {
                    mLastCommnad = Command.CMD_IC_INSERT_REQ;

                    mPosSdk.__PosInit("99", mDataListener,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
                }
                else
                {
                    new Handler(Looper.getMainLooper()).post(()->{
                        ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
                    });
                    return;
                }
                mPosSdk.__Cardinsert(mDataListener,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
            }
        }).start();
    }
    /**
     * 카드 제거 테스트<br>
     *    (미구현)
     */
    private void DeleteCard(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mPosSdk.getUsbDevice().size()!=0) {
                    mLastCommnad = Command.CMD_IC_REMOVE_REQ;

                    mPosSdk.__PosInit("99", mDataListener,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
                }
                else
                {
                    new Handler(Looper.getMainLooper()).post(()->{
                        ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
                    });
                    return;
                }
                mPosSdk.__CardDelete(mDataListener,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
            }
        }).start();
    }

    /**
     * 현재 카드 상태 테스트<br>
     *    (미구현)
     */
    private void StatusCheck(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mPosSdk.getUsbDevice().size()!=0) {
                    mLastCommnad = Command.CMD_IC_STATE_REQ;

                    mPosSdk.__PosInit("99", mDataListener,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
                }
                else
                {
                    new Handler(Looper.getMainLooper()).post(()->{
                        ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
                    });

                    return;
                }
                mPosSdk.__CardStatusCheck(mDataListener,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
            }
        }).start();
    }
    /**
     * 서명 패드 테스트<br>
     *    (미구현)
     */
    private void RequestSign(){
        if(mPosSdk.getUsbDevice().size()==0 && mPosSdk.getSignPadAddr().equals("") && mPosSdk.getMultiAddr().equals("")) {
            Intent intent = new Intent(m_menuActivity, SignPadActivity.class);
            intent.putExtra("Money", 10000);
            startActivityForResult(intent, REQUEST_SIGNPAD);
        }
        else
        {
            ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
        }
    }

    /**
     * 키입력 화면 테스트<br>
     *    (미구현)
     */
    private void RequestKey(){
        if(getTID().equals(""))
        {
            Toast.makeText(mPosSdk.getActivity(), getResources().getString(R.string.error_no_data_tid), Toast.LENGTH_SHORT).show();
            m_menuActivity.GotoMain();
            return;
        }
        if(mPosSdk.getUsbDevice().size()!=0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String type = "06";
                    mPosSdk.__icreq(type, getTID(),"0000005000", Utils.getDate("yyyyMMddHHmmss"), "0", "0", type,
                            "0", "01", "40", Constants.WORKING_KEY_INDEX, Constants.WORKING_KEY, Constants.CASHIC_RANDOM_NUMBER, mDataListener,
                            new String[]{mPosSdk.getSignPadAddr(),mPosSdk.getMultiAddr(),mPosSdk.getMultiReaderAddr()});
                }
            }).start();
        }
        else
        {
            ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
        }
    }

    /**
     * 패스워드 입력 테스트<br>
     *    (미구현)
     */
    private void RequestPasswd(){
        if(mPosSdk.getUsbDevice().size()!=0) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    mPosSdk.__PosInit("99", mDataListener, new String[]{mPosSdk.getSignPadAddr(),mPosSdk.getMultiAddr()});

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mPosSdk.__PadRequestInputPasswd_no_encypt("0", "40", "금액123456789원", "비밀번호노출주의", "처리중", "잠시기다려주세요", "05",
                                    mDataListener,new String[]{mPosSdk.getSignPadAddr(),mPosSdk.getMultiAddr()});
                        }
                    }, 1000);
                }
            }).start();
        }else{
            ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
        }

    }

    /**
     * 정보 저장<br>
     * IP, Password <br>
     *    (미구현)
     */
    private void Save(){
        String tmpip = "";
        int tmpport = 0;
        if(!m_txt_port.getText().equals("")) {
            Setting.setVanPORT(mPosSdk.getActivity(), m_txt_port.getText().toString());
            tmpport = Integer.parseInt(m_txt_port.getText().toString());

        }
        if(!m_txt_ip.getText().equals("")) {
            Setting.setIPAddress(mPosSdk.getActivity(), m_txt_ip.getText().toString());
            tmpip = m_txt_ip.getText().toString();
        }

        if(!tmpip.equals("") && tmpport!=0)
        {
            mPosSdk.resetTcpServerIPPort(tmpip,tmpport);
        }
        ShowDialog("관리자 설정을 저장하였습니다.");
    }
    private void ShowDialog(String _str)
    {
        Toast.makeText(m_menuActivity,_str,Toast.LENGTH_SHORT).show();
    }

    private SerialInterface.DataListener mDataListener = new SerialInterface.DataListener() {
        @Override
        public void onReceived(byte[] _rev, int _type) {
            readresponse(_rev,_type);
        }
    };

    private void readresponse(byte[] _res, int _type)
    {
        switch (_res[3])
        {
            case Command.CMD_IC_STATE_RES:
                byte[] rescode = new byte[2];
                rescode[0] = _res[4];
                rescode[1] = _res[5];
                String result = new String(rescode);
                if(result.equals("00")){
                    ShowDialog("리더기 내부에 카드가 없습니다.");
                }
                else if(result.equals("01"))
                {
                    ShowDialog("카드가 리더기 입구에 있습니다");
                }
                else if(result.equals("02"))
                {
                    ShowDialog("카드가 리더기에 삽입 되어 있습니다.");
                }
                else if(result.equals("03"))
                {
                    ShowDialog("카드 장애가 발생 하였습니다.");
                }
                else
                {
                    ShowDialog("알수 없는 상태 입니다.");
                }
                break;
            case Command.ESC:
                ShowDialog("ESC 발생");
                break;
            case Command.ACK:
                //ShowDialog("ACT 수신");
                break;
            case Command.CMD_IC_RES:
                ShowDialog("IC(현금 영수증) 관련 데이터 수신 완료");
                break;
            default:
                ShowDialog("응답 관련 미 구현 또는 내부 코드 에러");
                break;
        }
    }
    /**
     * 가맹점 설정을 하지 않은 경우에 거래를 할 수 없게 한다.
     */
    private void checkTid()
    {
        if(getTID().equals(""))
        {
            Toast.makeText(mPosSdk.getActivity(), getResources().getString(R.string.error_no_data_tid), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(m_menuActivity, Main2Activity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
    /**
     * 안드로이드 sharedPreference에 저장되어 있는 TID를 가져 오는 함수
     * @return String
     */
    private String getTID()
    {
        return Setting.getPreference(mPosSdk.getActivity(),Constants.TID);
    }
}

