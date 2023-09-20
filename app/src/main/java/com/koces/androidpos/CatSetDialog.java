package com.koces.androidpos;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.van.Constants;

import java.util.Objects;

public class CatSetDialog extends Dialog {
    private CatSetDialog.DialogBoxListener m_listener;
    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 가맹점등록다운로드를 위해 사용한다 */
    KocesPosSdk mPosSdk;

    EditText m_txt_ip, m_txt_port;
    Button m_btn_save;

    /** QR 읽는 장치 설정 카메라, 카드리더, 사인패드 */
    Spinner m_cbx_qr_reader;
    String mQrReaderType = "";

    /**이 초기화 함수는 절대 사용 금지 **/
    public CatSetDialog(){
        super(null);
    };
    public CatSetDialog(@NonNull Context context){super(context);};

    public CatSetDialog(@NonNull Context context, CatSetDialog.DialogBoxListener _listener) {
        super(context);
        mPosSdk = KocesPosSdk.getInstance();
        setListener(_listener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);
        this.setCancelable(false);
        Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        getWindow().setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.custom_cat_set);
        init();
    }

    private void init(){
        m_btn_save = findViewById(R.id.frag_cat_save);
        //초기화할때 ip, port 셋팅
        m_txt_ip = findViewById(R.id.frag_cat_ip);
        m_txt_port = findViewById(R.id.frag_cat_port);


        m_btn_save.setOnClickListener(BtnOnClickListener);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                m_txt_ip.setText(Setting.getCatIPAddress(mPosSdk.getActivity()));
                m_txt_port.setText(Setting.getCatVanPORT(mPosSdk.getActivity()));
                Setting.g_PayDeviceType = Setting.PayDeviceType.CAT;    //어플 전체에서 사용할 결제 방식
                Setting.setPreference(mPosSdk.getContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
                if(Setting.getPreference(mPosSdk.getActivity(),Constants.CAT_TIME_OUT).equals(""))
                {
                    Setting.setPreference(mPosSdk.getActivity(),Constants.CAT_TIME_OUT,"30");
                }
//                Save();
            }
        },200);

        /** qr 리더 설정 */
        m_cbx_qr_reader = findViewById(R.id.spinner_f_qrReader);
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

        if (m_txt_ip.getText() == null || m_txt_ip.getText().toString().equals(""))
        {
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
            Setting.setPreference(mPosSdk.getActivity(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
            Setting.setCatVanPORT(mPosSdk.getActivity(),"");
            Setting.setCatIPAddress(mPosSdk.getActivity(), "");
            m_listener.onClickCancel("CAT IP 설정이 잘못되었습니다");
            dismiss();
            return;
        }
        if (m_txt_port.getText() == null || m_txt_port.getText().toString().equals(""))
        {
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
            Setting.setPreference(mPosSdk.getActivity(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));
            Setting.setCatVanPORT(mPosSdk.getActivity(),"");
            Setting.setCatIPAddress(mPosSdk.getActivity(), "");
            m_listener.onClickCancel("CAT PORT 설정이 잘못되었습니다");
            dismiss();
            return;
        }

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

        /** QR 데이터를 읽는 장비를 어떤거로 할지 정한다 */
        Setting.setPreference(this.getContext(), Constants.CAT_QR_READER, mQrReaderType);

        m_listener.onClickConfirm("CAT 설정을 저장하였습니다.");
        dismiss();
        return;
    }

    public void setListener(CatSetDialog.DialogBoxListener listener) { m_listener = listener; }

    public interface DialogBoxListener {
        void onClickCancel(String _msg);              // 취소 버튼 클릭 시 호출
        void onClickConfirm(String _msg);              // 확인 버튼 클릭 시 호출
    }

}
