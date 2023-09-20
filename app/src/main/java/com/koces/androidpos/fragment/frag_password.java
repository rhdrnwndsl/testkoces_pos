package com.koces.androidpos.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.koces.androidpos.CashActivity2;
import com.koces.androidpos.CreditActivity;
import com.koces.androidpos.EasyPayActivity;
import com.koces.androidpos.PaymentActivity;
import com.koces.androidpos.R;
import com.koces.androidpos.ReceiptCreditActivity;
import com.koces.androidpos.TradeListActivity;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.KocesPosSdk;

public class frag_password extends Fragment {

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

    Button mbtn_pref_exit,mbtn_btn_pref_ok;
    EditText mPasswd;
    private final static String Passwd = "3415";
    private final static String PasswdTestMode = "2457";
    private final static String cashActivity = "1005";
    private final static String CreditActivity = "1004";
    private final static String EasyPayActivity = "1006";
    private final static String PayActivity = "2001";
    private final static String tradeActivity = "9000";
    private final static String RipCreditActivity = "3000";

    /**
     * //이 메소드가 호출될떄는 프래그먼트가 엑티비티위에 올라와있는거니깐 getActivity메소드로 엑티비티참조가능
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        m_menuActivity = (menu2Activity) getActivity();
    }

    public frag_password(){

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        m_view = inflater.inflate(R.layout.frag_password, container, false);
        init();
        return m_view;
    }

    private void init(){
        mPosSdk = KocesPosSdk.getInstance();
        mPosSdk.setFocusActivity(m_menuActivity,null);
        mbtn_btn_pref_ok = m_view.findViewById(R.id.btn_btn_pref_ok);
        mbtn_pref_exit = m_view.findViewById(R.id.btn_pref_exit);
        mPasswd = m_view.findViewById(R.id.prefpass_inputpasswd);
        mPasswd.setText("");

        mbtn_btn_pref_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mPasswd.getText().toString().equals(Passwd))
                {
                    mPasswd.setText("");
                    m_menuActivity.setFrag(9);
                    return;

                } else if(mPasswd.getText().toString().equals(PasswdTestMode)){ //22.01.26 kim.jy 테스트를 위해서 추가
                    mPasswd.setText("");
                    m_menuActivity.setFrag(frag_tax.fragMentNumber);
                }else if(mPasswd.getText().toString().equals(cashActivity)){    //22.02.10 kim.jy 테스트위해서 추가 현금영수증페이지
                    Intent intent = new Intent(mPosSdk.getActivity(), CashActivity2.class);
                    startActivity(intent);
                }else if(mPasswd.getText().toString().equals(CreditActivity)){    //22.02.10 kim.jy 테스트위해서 추가 신용페이지
                    Intent intent = new Intent(mPosSdk.getActivity(), CreditActivity.class);
                    startActivity(intent);
                }else if(mPasswd.getText().toString().equals(EasyPayActivity)) {    //22.02.10 kim.jy 테스트위해서 추가 간편결제페이지
                    Intent intent = new Intent(mPosSdk.getActivity(), EasyPayActivity.class);
                    startActivity(intent);
                }else if(mPasswd.getText().toString().equals(EasyPayActivity)) {
                    Intent intent = new Intent(mPosSdk.getActivity(), PaymentActivity.class);   //22.02.11 kim.jy 기존의 페이먼트 activity를 표시 한다.
                    startActivity(intent);
                }else if(mPasswd.getText().toString().equals(tradeActivity)) {
                    Intent intent = new Intent(mPosSdk.getActivity(), TradeListActivity.class);   //22.02.18 kim.jy 기존의 거래내역페이지
                    startActivity(intent);
                }else if(mPasswd.getText().toString().equals(RipCreditActivity)){
                    Intent intent = new Intent(mPosSdk.getActivity(), ReceiptCreditActivity.class); //22.02.22 kim.jy 신용카드 영수증 화면
                }else{
                    mPasswd.setText("");
                    ShowDialog("비밀번호를 잘못 입력하였습니다");
                    m_menuActivity.tabs.selectTab(m_menuActivity.tabs.getTabAt(0));
                    m_menuActivity.setFrag(0);
                    return;
                }
            }
        });

        mbtn_pref_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPasswd.setText("");
                ShowDialog("비밀번호 입력을 취소하였습니다");
                m_menuActivity.tabs.selectTab(m_menuActivity.tabs.getTabAt(0));
                m_menuActivity.setFrag(0);
                return;
            }
        });
    }

    private void ShowDialog(String _str)
    {
        Toast.makeText(m_menuActivity,_str,Toast.LENGTH_SHORT).show();
    }

}
