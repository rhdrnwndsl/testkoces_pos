package com.koces.androidpos.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.koces.androidpos.R;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.van.Constants;

/**
 * 환경 설정 기타<br>
 * (현재는 사용하지 않음)
 */
public class frag_device_cat extends Fragment {
    /**
     * Log를 위한 TAG 설정
     */
    private final static String TAG = frag_manager.class.getSimpleName();
    /** Activity 의 onCreate() 의 setContentView()와 같은 관련 UI(xml) 를 화면에 구성하는 View */
    View m_view;
    /** menu2Activity.java 에서 가맹점설정 장치설정 관리자설정 무결성검증 등의 Fragment 가 속해있다 */
    menu2Activity m_menuActivity;
    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 가맹점등록다운로드를 위해 사용한다 */
    KocesPosSdk mPosSdk;

    EditText m_txt_ip, m_txt_port;
    Button m_btn_save;

    bleSdk mbleSdk;

    /** QR 읽는 장치 설정 카메라, 카드리더, 사인패드 */
    Spinner m_cbx_qr_reader;
    String mQrReaderType = "";

    /**
     * //이 메소드가 호출될떄는 프래그먼트가 엑티비티위에 올라와있는거니깐 getActivity메소드로 엑티비티참조가능
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        m_menuActivity = (menu2Activity) getActivity();
    }

    public frag_device_cat(){

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
        m_view = inflater.inflate(R.layout.frag_device_cat, container, false);
        init();
        return m_view;
    }

    private void init(){
        mPosSdk = KocesPosSdk.getInstance();
        mbleSdk = bleSdk.getInstance();
        mPosSdk.setFocusActivity(m_menuActivity,null);
        m_btn_save = m_view.findViewById(R.id.frag_cat_save);
        //초기화할때 ip, port 셋팅
        m_txt_ip = m_view.findViewById(R.id.frag_cat_ip);
        m_txt_port = m_view.findViewById(R.id.frag_cat_port);


        m_btn_save.setOnClickListener(BtnOnClickListener);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                m_txt_ip.setText(Setting.getCatIPAddress(mPosSdk.getActivity()));
                m_txt_port.setText(Setting.getCatVanPORT(mPosSdk.getActivity()));
                Setting.g_PayDeviceType = Setting.PayDeviceType.CAT;    //어플 전체에서 사용할 결제 방식
                Setting.setPreference(m_menuActivity, Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                if(Setting.getPreference(mPosSdk.getActivity(),Constants.CAT_TIME_OUT).equals(""))
                {
                    Setting.setPreference(mPosSdk.getActivity(),Constants.CAT_TIME_OUT,"30");
                }
//                Save();
            }
        },200);

        /** qr 리더 설정 */
        m_cbx_qr_reader = m_view.findViewById(R.id.spinner_f_qrReader);
        mQrReaderType = Setting.getPreference(mPosSdk.getActivity(), Constants.CAT_QR_READER);
        m_cbx_qr_reader.setSelection(getIndex(m_cbx_qr_reader,mQrReaderType));
        m_cbx_qr_reader.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //               ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(view!=null) {
                    ((TextView) parent.getChildAt(0)).setTextSize(14);
                }
                switch (position)
                {
                    case 0://카메라
                        mQrReaderType = Constants.CatQrReader.Camera.toString();
                        break;
                    case 1://CAT단말기
                        mQrReaderType = Constants.CatQrReader.CatReader.toString();
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

    Button.OnClickListener BtnOnClickListener = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.frag_cat_save:
                    Save();
                    break;
                default:
                    break;
            }
        }
    };

    private int getIndex(Spinner spinner, String myString){
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                return i;
            }
        }
        return 0;
    }

    private void Save(){
        String tmpip = "";
        int tmpport = 0;
        if(!m_txt_port.getText().equals("")) {
            Setting.setCatVanPORT(mPosSdk.getActivity(), m_txt_port.getText().toString());
            tmpport = Integer.parseInt(m_txt_port.getText().toString());

        }
        if(!m_txt_ip.getText().equals("")) {
            Setting.setCatIPAddress(mPosSdk.getActivity(), m_txt_ip.getText().toString());
            tmpip = m_txt_ip.getText().toString();
        }

        if(!tmpip.equals("") && tmpport!=0)
        {
            mPosSdk.resetCatServerIPPort(tmpip,tmpport);
        }
        mPosSdk.BleIsConnected();
        if(Setting.getBleIsConnected())
        {
            mbleSdk = bleSdk.getInstance();
            mbleSdk.DisConnect();
            /** 정상적으로 연결을 해제한다면 기존 마지막 연결장비에 관한 데이터를 지운다 */
            Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_NAME, "");
            Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_ADDR, "");
        }

        /** QR 데이터를 읽는 장비를 어떤거로 할지 정한다 */
        Setting.setPreference(this.getContext(), Constants.CAT_QR_READER, mQrReaderType);

        ShowDialog("장치(CAT) 설정을 저장하였습니다.");
    }
    private void ShowDialog(String _str)
    {
        Toast.makeText(m_menuActivity,_str,Toast.LENGTH_SHORT).show();
    }
}