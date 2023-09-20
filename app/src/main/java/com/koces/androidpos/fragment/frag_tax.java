package com.koces.androidpos.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.koces.androidpos.Main2Activity;
import com.koces.androidpos.R;
import com.koces.androidpos.StoreMenuActivity;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.TaxSdk;
import com.koces.androidpos.sdk.van.Constants;

import java.util.HashMap;

public class frag_tax extends Fragment {

    /**
     * Log를 위한 TAG 설정
     */
    private final static String TAG = frag_manager.class.getSimpleName();
    public final static int fragMentNumber = 201; //22.01.26 kim.jy 고유의프래그먼트 넘버
    /** Activity 의 onCreate() 의 setContentView()와 같은 관련 UI(xml) 를 화면에 구성하는 View */
    View m_view;
    /** menu2Activity.java 에서 가맹점설정 장치설정 관리자설정 무결성검증 등의 Fragment 가 속해있다 */
    StoreMenuActivity m_storeMenuActivity;
    KocesPosSdk mPosSdk;
    TaxSdk mTaxSdk;
    /**
     * //이 메소드가 호출될떄는 프래그먼트가 엑티비티위에 올라와있는거니깐 getActivity메소드로 엑티비티참조가능
     * @param context
     */
    private Button mBtnSave;
    private Switch mSwtVAT,mSwtSVC;
    private RadioGroup mRdbGroupVatMethod,mRdbGroupVatInclude,mRdbGroupSvcMethod,mRdbGroupSvcInclude, mRdbGroupFallbackUse;
    private RadioButton mRdbVatAuto,mRdbVatManual,mRdbVatInclude,mRdbVatNotInclude,mRdbSvcAuto,mRdbSvcManual,mRdbSvcInclude,mRdbSvcNotInclude,
            mRdbFallbackInclude,mRdbFallbackNotInclude;
    private EditText mEdtVatRate,mEdtSvcRate,mEdtInstallment,mEdtSignMoney;    //부가세율, 봉사료율, 할부최소 금액, 사인표시 금액
    private LinearLayout mLayoutVat,mLayoutSvc;

    /** tax set init value */
    private boolean mvatUse = true;         //vat 사용/미사용
    private boolean mVatMode = true;        //vat mode auto:true, 통합:false
    private boolean mvatInclude = true;     //vat 포함:true 미포함:false
    private boolean mSvcUse = false;         //svc 사용/미사용
    private boolean msvcMode = true;        //svc mode auto:true, manual:false
    private boolean msvcInclude = true;    //svc 포함:true 미포함:false
    private boolean mFallbackUse = true;    //fallback 사용유무
    private int mvatRate = 10;
    private int msvcRate = 0;
    private int mInstallMin = 5;
    private int mSignMin = 5;

    /** read tax setting data */
    HashMap<String,String> mTaxInfo = null;
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        m_storeMenuActivity = (StoreMenuActivity) getActivity();
    }

    public frag_tax(){
    }
    @Override
    public void onDetach() {
        super.onDetach();
        m_storeMenuActivity = null;
        mPosSdk = null;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        m_view = inflater.inflate(R.layout.frag_tax, container, false);
        init();
        return m_view;
    }

    private void init(){
        mPosSdk = KocesPosSdk.getInstance();
        mTaxSdk = TaxSdk.getInstance();
        mPosSdk.setFocusActivity(m_storeMenuActivity,null);
        String tid = Setting.getPreference(mPosSdk.getActivity(),Constants.TID);


        /**화면 UI 연결*/
        //폴백사용유무
        mRdbGroupFallbackUse = m_view.findViewById(R.id.frgtax_rdb_group_fallback_use); //폴백사용유무그룹
        mRdbFallbackInclude = (RadioButton)m_view.findViewById(R.id.frgtax_rdb_fallback_include);
        mRdbFallbackNotInclude = (RadioButton)m_view.findViewById(R.id.frgtax_rdb_fallback_notinclude);
        //세금 설정 부분
        mSwtVAT = (Switch)m_view.findViewById(R.id.frgtax_switch_vat_use);
        mLayoutVat = (LinearLayout)m_view.findViewById(R.id.frgtax_vat_linearlayout); //세금 설정 패널
        mRdbGroupVatMethod = (RadioGroup)m_view.findViewById(R.id.frgtax_rdb_group_vat_method);     //방식
        mRdbGroupVatInclude = (RadioGroup)m_view.findViewById(R.id.frgtax_rdb_group_vat_include);   //포함, 미포함
        mRdbVatAuto = (RadioButton)m_view.findViewById(R.id.frgtax_rdb_vat_auto);
        mRdbVatManual =(RadioButton)m_view.findViewById(R.id.frgtax_rdb_vat_manual);
        mRdbVatInclude = (RadioButton)m_view.findViewById(R.id.frgtax_rdb_vat_include);
        mRdbVatNotInclude = (RadioButton)m_view.findViewById(R.id.frgtax_rdb_vat_notinclude);
        mEdtVatRate = (EditText)m_view.findViewById(R.id.frgtax_edt_vatrate);   //부가세율
        //봉사료 설정 부분
        mSwtSVC = (Switch)m_view.findViewById(R.id.frgtax_switch_svc_use);
        mLayoutSvc = (LinearLayout)m_view.findViewById(R.id.frgtax_svc_linearlayout);   //봉사료 설정 패널
        mRdbGroupSvcMethod = (RadioGroup)m_view.findViewById(R.id.frgtax_rdb_group_svc_method);     //방식
        mRdbGroupSvcInclude = (RadioGroup)m_view.findViewById(R.id.frgtax_rdb_group_svc_include);   //포함,미포함
        mRdbSvcAuto = (RadioButton) m_view.findViewById(R.id.frgtax_rdb_svc_auto);
        mRdbSvcManual= (RadioButton) m_view.findViewById(R.id.frgtax_rdb_svc_manual);
        mRdbSvcInclude= (RadioButton) m_view.findViewById(R.id.frgtax_rdb_svc_include);
        mRdbSvcNotInclude= (RadioButton) m_view.findViewById(R.id.frgtax_rdb_svc_notinclude);
        mEdtSvcRate = (EditText)m_view.findViewById(R.id.frgtax_edt_svcrate);   //봉사료율

        //할부 최소 금액
        mEdtInstallment = (EditText)m_view.findViewById(R.id.frgtax_edt_installment);
        //서명패드 최소 금액
        mEdtSignMoney = (EditText)m_view.findViewById(R.id.frgtax_edt_nosign);
        //
        mBtnSave = (Button) m_view.findViewById(R.id.frgtax_btn_save);
        mBtnSave.setOnClickListener(onClickListener);

        //TID 가져오기
        if (!tid.equals("")){
            if(getTaxInfo()){ //db에 세금 설정 정보가 있음

            }else{  //세금 설정 정보가 없으면 초기 설정을 진행 한다.
                initsetting();
            }
        }
        else{   //220204 kim.jy
                //맨 처음 어플 기동 하게 가맹점 설정 안하고 세금 설정 들어온 경우
            //Todo:TID 없을 때도 처리가능하도록 수정. 이유는 CAT은 TID 없이도 거래가 진행되기 때문이다
            if(getTaxInfo()){ //db에 세금 설정 정보가 있음

            }else{  //세금 설정 정보가 없으면 초기 설정을 진행 한다.
                initsetting();
            }
        }

        if (Setting.getPreference(m_view.getContext(),Constants.FALLBACK_USE).equals("0"))  //0=사용
        {
            mRdbFallbackInclude.setChecked(true);   //폴백사용유무 설정
            mRdbFallbackNotInclude.setChecked(false);
        }
        else
        {
            mRdbFallbackInclude.setChecked(false);   //폴백사용유무 설정
            mRdbFallbackNotInclude.setChecked(true);
        }

        /** 부가세 적용, 미적용 스위치 */
        mSwtVAT.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG,"테스트 부가세 스위치 ");
            if (isChecked){
                mLayoutVat.setVisibility(View.VISIBLE);
                mvatUse = true;
            }else{
                mLayoutVat.setVisibility(View.INVISIBLE);
                mvatUse = false;
            }
        });

        /** 부가세 방식 자동,통합 */
        mRdbGroupVatMethod.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId){
                case R.id.frgtax_rdb_vat_auto:
                    mVatMode = true;
                    break;
                case R.id.frgtax_rdb_vat_manual:
                    mVatMode = false;
                    break;
            }
        });
        /** 부가세 포함, 미포함 설정 */
        mRdbGroupVatInclude.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId){
                case R.id.frgtax_rdb_vat_include:
                    mvatInclude =true;
                    break;
                case R.id.frgtax_rdb_vat_notinclude:
                    mvatInclude=false;
                    break;
            }
        });

        /** 봉사료 적용, 미적용 스위치 */
        mSwtSVC.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG,"테스트 봉사료 스위치 ");
            if(isChecked){
                mLayoutSvc.setVisibility(View.VISIBLE);
                mSvcUse = true;
            }else{
                mLayoutSvc.setVisibility(View.INVISIBLE);
                mSvcUse = false;
            }
        });
        /** 봉사료 입력 방식, 자동, 수동 */
        mRdbGroupSvcMethod.setOnCheckedChangeListener((group,checkedId)->{
            switch (checkedId){
                case R.id.frgtax_rdb_svc_auto:
                    msvcMode = true;
                    break;
                case R.id.frgtax_rdb_svc_manual:
                    msvcMode = false;
                    break;
            }
        });
        /** 봉사료 포함, 미포함 */
        mRdbGroupSvcInclude.setOnCheckedChangeListener(((group, checkedId) -> {
            switch (checkedId){
                case R.id.frgtax_rdb_svc_include:
                    msvcInclude = true;
                    break;
                case R.id.frgtax_rdb_svc_notinclude:
                    msvcInclude = false;
                    break;
            }
        }));

        /** 폴백사용유무 */
        mRdbGroupFallbackUse.setOnCheckedChangeListener(((group, checkedId) -> {
            switch (checkedId){
                case R.id.frgtax_rdb_fallback_include:
                    mFallbackUse = true;
                    break;
                case R.id.frgtax_rdb_fallback_notinclude:
                    mFallbackUse = false;
                    break;
            }
        }));
    }
    private void initsetting(){     //220204 kim.jy
        mSwtVAT.setChecked(mvatUse);   //초기에는 부가세 설정을 true로 한다.
        mLayoutVat.setVisibility(View.VISIBLE); // 세금 패널 표시
        mRdbVatAuto.setChecked(true);   //세금 자동으로 설정
        mRdbVatManual.setChecked(false);    //세금 통합을 설정하지 않음
        mRdbVatInclude.setChecked(true);    //세금은 포함으로 설정
        mRdbVatNotInclude.setChecked(false);
        mSwtSVC.setChecked(mSvcUse);  //초기에는 봉사료 설정을 false로 한다.
        mLayoutSvc.setVisibility(View.INVISIBLE);   //봉사료 패널 숨기기
        mRdbSvcAuto.setChecked(true);
        mRdbSvcManual.setChecked(false);
        mRdbSvcInclude.setChecked(true);
        mRdbSvcNotInclude.setChecked(false);
        mEdtVatRate.setText(String.valueOf(mvatRate));
        mEdtSvcRate.setText(String.valueOf(msvcRate));
        mEdtInstallment.setText(String.valueOf(mInstallMin));
        mEdtSignMoney.setText(String.valueOf(mSignMin));

    }
    //세금 설정을 db로 부터 읽어 온다.
    private boolean getTaxInfo(){
        mTaxInfo = mTaxSdk.readTaxSettingDB(getTID());
        if(mTaxInfo.size() == 0){   //22.02.07 kim.jy 올라오는 데이터가 null 이 아니라 0이 올라옵니다
            return false;
        }


        setRadioButtonVatMode(mTaxSdk.getUseVAT(),mTaxSdk.getVATMode(),mTaxSdk.getVATInclude());
        mEdtVatRate.setText(String.valueOf(mTaxSdk.getVATRate()));
        setRadioButtonSvcMode(mTaxSdk.getUseSVC(),mTaxSdk.getSVCMode(),mTaxSdk.getSVCInclude());
        mEdtSvcRate.setText(String.valueOf(mTaxSdk.getSVCRate()));

        mEdtInstallment.setText(String.valueOf(mTaxSdk.getMinInstallmentAmount()));
        mEdtSignMoney.setText(String.valueOf(mTaxSdk.getMinNoSignAmount()));
        return true;
    }
    private void setRadioButtonVatMode(boolean useValue,int modeValue,int includeValue){
        mLayoutVat.setVisibility(useValue==true?View.VISIBLE:View.INVISIBLE);
        mSwtVAT.setChecked(useValue);
        mvatUse = useValue;
        mVatMode = modeValue==0?true:false;
        if(modeValue==0){
            mRdbVatAuto.setChecked(true);
            mRdbVatManual.setChecked(false);
        }else{
            mRdbVatAuto.setChecked(false);
            mRdbVatManual.setChecked(true);
        }
        mvatInclude = includeValue==0?true:false;
        if(includeValue==0){
            mRdbVatInclude.setChecked(true);
            mRdbVatNotInclude.setChecked(false);
        }else{
            mRdbVatInclude.setChecked(false);
            mRdbVatNotInclude.setChecked(true);
        }
    }
    private void setRadioButtonSvcMode(boolean useValue, int modeValue,int includeValue){
        mLayoutSvc.setVisibility(useValue==true?View.VISIBLE:View.INVISIBLE);
        mSwtSVC.setChecked(useValue);
        mSvcUse = useValue;
        msvcMode = modeValue==0?true:false;
        if(modeValue==0){
            mRdbSvcAuto.setChecked(true);
            mRdbSvcManual.setChecked(false);
        }else{
            mRdbSvcAuto.setChecked(false);
            mRdbSvcManual.setChecked(true);
        }
        msvcInclude = includeValue==0?true:false;
        if(includeValue==0){
            mRdbSvcInclude.setChecked(true);
            mRdbSvcNotInclude.setChecked(false);
        }else{
            mRdbSvcInclude.setChecked(false);
            mRdbSvcNotInclude.setChecked(true);
        }
    }
    //저장 버튼 클릭 이벤트
    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String tid = Setting.getPreference(mPosSdk.getActivity(),Constants.TID);
            /** check error */

//            if (tid.equals("")){    //가맹점 정보가 없는 경우
//                ShowDialog("가맹점 정보가 없습니다. 가맹점 등록 정보 확인 필요");
//                return;
//            }

            //empty value
            if (mEdtInstallment.getText().toString().equals("") || mEdtSignMoney.getText().toString().equals("") ||
                    mEdtVatRate.getText().toString().equals("") || mEdtSvcRate.getText().toString().equals("")){
                ShowDialog("빈 값을 사용 할 수 없습니다");
                return;
            }

            /** 부가세율, 봉사료율은 50%를 넘을 수 없다 */
            char[] tmpVAT = mEdtVatRate.getText().toString().toCharArray();
            char[] tmpSVC = mEdtSvcRate.getText().toString().toCharArray();

            if(tmpVAT!=null){
                if(tmpVAT.length==2 && tmpVAT[0]>0x34){
                    ShowDialog("부가세는 50%를 넘을 수 없습니다");
                    return;
                }
            }

            if(tmpSVC!=null){
                if(tmpSVC.length==2 && tmpSVC[0]>0x34){
                    ShowDialog("봉사료 50%를 넘을 수 없습니는다");
                    return;
                }
            }

            //값의 범위 체크
            SaveTaxSettingInfo();

        }
    };

    private void SaveTaxSettingInfo(){

        if(mEdtVatRate.getText().toString().equals("") || mEdtVatRate.getText().toString().equals(" "))
        {
            ShowDialog("금액이 잘못입력되었습니다");
            return;
        }
        if(mEdtSvcRate.getText().toString().equals("") || mEdtSvcRate.getText().toString().equals(" "))
        {
            ShowDialog("금액이 잘못입력되었습니다");
            return;
        }
        if(mEdtInstallment.getText().toString().equals("") || mEdtInstallment.getText().toString().equals(" "))
        {
            ShowDialog("금액이 잘못입력되었습니다");
            return;
        }
        if(mEdtSignMoney.getText().toString().equals("") || mEdtSignMoney.getText().toString().equals(" "))
        {
            ShowDialog("금액이 잘못입력되었습니다");
            return;
        }

        mvatRate = Integer.parseInt(mEdtVatRate.getText().toString());
        msvcRate = Integer.parseInt(mEdtSvcRate.getText().toString());
        mInstallMin = Integer.parseInt(mEdtInstallment.getText().toString());
        mSignMin = Integer.parseInt(mEdtSignMoney.getText().toString());
        //String Tid,boolean UseVAT,int AutoVAT,int IncludeVAT,boolean UseSVC,
        //int AutoSVC,int IncludeSVC,int minInstallMentAmount,int NoSignAmount
        mPosSdk.setSqliteDB_SettingTax(getTID(),
                mvatUse,
                mVatMode==true?0:1,
                mvatInclude==true?0:1,
                mvatRate,
                mSvcUse,
                msvcMode==true?0:1,
                msvcInclude==true?0:1,
                msvcRate,
                mInstallMin,
                mSignMin);
        Setting.setPreference(m_view.getContext(),Constants.UNSIGNED_SETMONEY,String.valueOf(mSignMin * 10000));
        if(mFallbackUse) {
            Setting.setPreference(m_view.getContext(), Constants.FALLBACK_USE, "0");    //0=사용 1=미사용
        } else {
            Setting.setPreference(m_view.getContext(), Constants.FALLBACK_USE, "1");
        }
        ShowDialog("세금설정을 저장하였습니다");
    }
    private void ShowDialog(String _str)
    {
        Toast.makeText(m_storeMenuActivity,_str,Toast.LENGTH_SHORT).show();
    }
    private String getTID()
    {
        return Setting.getPreference(mPosSdk.getActivity(),Constants.TID);
    }
}
