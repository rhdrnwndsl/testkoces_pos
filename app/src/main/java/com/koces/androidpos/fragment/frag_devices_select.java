package com.koces.androidpos.fragment;

import android.app.Fragment;
import android.content.Context;
import android.media.Image;
import android.os.Bundle;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.koces.androidpos.R;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.van.Constants;


/**
 * 결제 수단을 선택 하는 페이지 <br>
 */
public class frag_devices_select extends Fragment {
    View view;
    /** menu2Activity.java 에서 가맹점설정 장치설정 관리자설정 무결성검증 등의 Fragment 가 속해있다 */
    menu2Activity m_menuActivity;
    bleSdk mbleSdk;
    ImageView mImgLines,mImgBle,mImgCat;
    /** 선택 버튼 */
    Button mSelectedBtn;
    /** 장치 선택시 설정한 값
     * 1 -> serial
     * 2 -> ble
     * 3 -> cat
     */
    int mSelectdDevice = 0;

    /**
     * //이 메소드가 호출될떄는 프래그먼트가 엑티비티위에 올라와있는거니깐 getActivity메소드로 엑티비티참조가능
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        m_menuActivity = (menu2Activity) getActivity();
    }

    public frag_devices_select(){

    }
    @Override
    public void onDetach() {
        super.onDetach();
        m_menuActivity = null;
        mbleSdk = null;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag_device_select, container, false);
        init();
        return view;
    }

    private void init(){
        mbleSdk = bleSdk.getInstance();
        mSelectedBtn = (Button)view.findViewById(R.id.frag_seldev_btn_setok);

        mImgLines = (ImageView)view.findViewById(R.id.frag_seldev_imgbtn_lines);
        mImgBle = (ImageView)view.findViewById(R.id.frag_seldev_imgbtn_ble);
        mImgCat = (ImageView)view.findViewById(R.id.frag_seldev_imgbtn_cat);

        mImgLines.setOnClickListener(v-> {
            mSelectdDevice = 1;
            mImgLines.setBackgroundResource(R.drawable.main_selected_background);
            mImgBle.setBackgroundResource(R.drawable.rectangle);
            mImgCat.setBackgroundResource(R.drawable.rectangle);
        });
        mImgBle.setOnClickListener(v-> {
            mSelectdDevice = 2;
            mImgLines.setBackgroundResource(R.drawable.rectangle);
            mImgBle.setBackgroundResource(R.drawable.main_selected_background);
            mImgCat.setBackgroundResource(R.drawable.rectangle);
        });
        mImgCat.setOnClickListener(v->{
            mSelectdDevice = 3;
            mImgLines.setBackgroundResource(R.drawable.rectangle);
            mImgBle.setBackgroundResource(R.drawable.rectangle);
            mImgCat.setBackgroundResource(R.drawable.main_selected_background);
        });

        mSelectedBtn.setOnClickListener((v)->{
            switch (mSelectdDevice){
                case 0:
                    Toast.makeText(this.getContext(),"설정하려는 장치를 선택해 주세요",Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Setting.g_PayDeviceType = Setting.PayDeviceType.LINES;    //어플 전체에서 사용할 결제 방식
                    Setting.setPreference(this.getContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                    mbleSdk = bleSdk.getInstance();
                    mbleSdk.getConnected();
                    if(Setting.getBleIsConnected())
                    {
                        mbleSdk.DisConnect();
                    }
                    m_menuActivity.setFrag(5);
                    break;
                case 2:
                    Setting.g_PayDeviceType = Setting.PayDeviceType.BLE;    //어플 전체에서 사용할 결제 방식
                    Setting.setPreference(this.getContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                    m_menuActivity.setFrag(6);
                    break;
                case 3:
                    Setting.g_PayDeviceType = Setting.PayDeviceType.CAT;    //어플 전체에서 사용할 결제 방식
                    Setting.setPreference(this.getContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                    mbleSdk = bleSdk.getInstance();
                    mbleSdk.getConnected();
                    if(Setting.getBleIsConnected())
                    {
                        mbleSdk.DisConnect();
                    }
                    m_menuActivity.setFrag(7);
                    break;
            }
        });
    }
}