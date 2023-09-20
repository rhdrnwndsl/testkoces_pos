package com.koces.androidpos.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import android.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.koces.androidpos.R;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.van.Constants;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link frag_network#newInstance} factory method to
 * create an instance of this fragment.
 */
public class frag_network extends Fragment {
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

    public frag_network() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment frag_network.
     */
    // TODO: Rename and change types and number of parameters
    public static frag_network newInstance(String param1, String param2) {
        frag_network fragment = new frag_network();
        Bundle args = new Bundle();
        return fragment;
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
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        m_view = inflater.inflate(R.layout.frag_network, container, false);
        init();
        return m_view;
    }

    private void init(){
        mPosSdk = KocesPosSdk.getInstance();
        mPosSdk.setFocusActivity(m_menuActivity,null);
        m_btn_save = m_view.findViewById(R.id.frag_network_save);
        //초기화할때 ip, port 셋팅
        m_txt_ip = m_view.findViewById(R.id.frag_network_ip);
        m_txt_port = m_view.findViewById(R.id.frag_network_port);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                m_txt_ip.setText(Setting.getIPAddress(mPosSdk.getActivity()));
                m_txt_port.setText(Setting.getVanPORT(mPosSdk.getActivity()));
            }
        },200);


        m_btn_save.setOnClickListener(v->{
            Save();
        });

    }

    private void Save(){
        String tmpip = "";
        int tmpport = 0;
        if(!m_txt_port.getText().equals("")) {
            tmpport = Integer.parseInt(m_txt_port.getText().toString());

        } else {
            ShowDialog("PORT 설정이 잘못되었습니다");
            return;
        }
        if(!m_txt_ip.getText().equals("")) {
            tmpip = m_txt_ip.getText().toString();
        } else {
            ShowDialog("IP 설정이 잘못되었습니다");
            return;
        }

        if(!tmpip.equals("") && tmpport!=0)
        {
            Setting.setVanPORT(mPosSdk.getActivity(), m_txt_port.getText().toString());
            Setting.setIPAddress(mPosSdk.getActivity(), m_txt_ip.getText().toString());
            mPosSdk.resetTcpServerIPPort(tmpip,tmpport);
        } else {
            ShowDialog("IP/PORT 설정이 잘못되었습니다.");
            return;
        }
        ShowDialog("네트워크 설정을 저장하였습니다.");
    }
    private void ShowDialog(String _str)
    {
        Toast.makeText(m_menuActivity,_str,Toast.LENGTH_SHORT).show();
    }
}