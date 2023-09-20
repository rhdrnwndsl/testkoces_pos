package com.koces.androidpos;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.van.Constants;

import java.util.HashMap;
import java.util.Objects;

public class StoreInfoDialog extends Dialog {
    private DialogBoxListener m_listener;
    private String mBsn, mName, mPhone, mAddr, mOwner;
    Button mBtn_OK, mBtn_cancel;
    EditText mEdit_bsn,mEdit_StoreName,mEdit_StorePhone,mEdit_StoreAddr,mEdit_StoreOwner;

    /**이 초기화 함수는 절대 사용 금지 **/
    public StoreInfoDialog(){
        super(null);
    };
    public StoreInfoDialog(@NonNull Context context){super(context);};

    /**
     * 버튼2개사용
     * @param context
     * @param _bsn
     * @param _name
     * @param _phone
     * @param _addr
     * @param _owner
     * @param _listener
     */
    public StoreInfoDialog(@NonNull Context context, String _bsn, String _name, String _phone, String _addr, String _owner, DialogBoxListener _listener) {
        super(context);
        mBsn = _bsn;
        mName = _name;
        mPhone = _phone;
        mAddr = _addr;
        mOwner = _owner;
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
        setContentView(R.layout.custom_store_info);
        init();
    }

    private void init()
    {
        mEdit_bsn = findViewById(R.id.edit_bsn);
        mEdit_StoreAddr = findViewById(R.id.edit_storeaddr);
        mEdit_StoreName = findViewById(R.id.edit_storename);
        mEdit_StoreOwner = findViewById(R.id.edit_storeowner);
        mEdit_StorePhone = findViewById(R.id.edit_storephone);

        mEdit_bsn.setText(mBsn);
        mEdit_StoreAddr.setText(mAddr);
        mEdit_StoreName.setText(mName);
        mEdit_StoreOwner.setText(mOwner);
        mEdit_StorePhone.setText(mPhone);

        mBtn_OK = findViewById(R.id.btn_ok_storeinfo);
        mBtn_cancel = findViewById(R.id.btn_cancel_storeinfo);

        mBtn_OK.setOnClickListener((View v)->{
            if (mEdit_bsn.getText().toString().replace(" ","").equals("")) {
                if (m_listener != null) {
                    m_listener.onClickCancel("사업자번호 입력 오류입니다");
                }
                dismiss();
                return;
            }
            if (mEdit_StoreAddr.getText().toString().replace(" ","").equals("")) {
                if (m_listener != null) {
                    m_listener.onClickCancel("가맹점주소 입력 오류입니다");
                }
                dismiss();
                return;
            }
            if (mEdit_StoreName.getText().toString().replace(" ","").equals("")) {
                if (m_listener != null) {
                    m_listener.onClickCancel("가맹점명 입력 오류입니다");
                }
                dismiss();
                return;
            }
            if (mEdit_StoreOwner.getText().toString().replace(" ","").equals("")) {
                if (m_listener != null) {
                    m_listener.onClickCancel("대표자명 입력 오류입니다");
                }
                dismiss();
                return;
            }
            if (mEdit_StorePhone.getText().toString().replace(" ","").equals("")) {
                if (m_listener != null) {
                    m_listener.onClickCancel("전화번호 입력 오류입니다");
                }
                dismiss();
                return;
            }

            if (m_listener != null) {
                m_listener.onClickConfirm(mEdit_bsn.getText().toString(),mEdit_StoreName.getText().toString(),
                        mEdit_StorePhone.getText().toString(),mEdit_StoreAddr.getText().toString(),mEdit_StoreOwner.getText().toString());
            }
            dismiss();
        });
        mBtn_cancel.setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickCancel("입력을 취소하였습니다");
            }
            dismiss();
        });
    }

    public void setListener(DialogBoxListener listener) { m_listener = listener; }

    public interface DialogBoxListener {
        void onClickCancel(String _msg);              // 취소 버튼 클릭 시 호출
        void onClickConfirm(String _bsn, String _name, String _phone, String _addr, String _owner);              // 확인 버튼 클릭 시 호출
    }
}
