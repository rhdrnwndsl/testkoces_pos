package com.koces.androidpos;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.koces.androidpos.sdk.KocesPosSdk;

import java.util.Objects;

public class StartPermissionSetDialog extends Dialog {

    private DialogBoxListener m_listener;
    Button m_btn_confirm;

    /**이 초기화 함수는 절대 사용 금지 **/
    public StartPermissionSetDialog(){
        super(null);
    };
    public StartPermissionSetDialog(@NonNull Context context){super(context);};

    public StartPermissionSetDialog(@NonNull Context context, DialogBoxListener _listener) {
        super(context,android.R.style.Theme_Translucent_NoTitleBar);
        setListener(_listener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        this.setCancelable(false);
        Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        getWindow().setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.custom_permission_set);
        init();
    }

    private void init()
    {
        m_btn_confirm = findViewById(R.id.btn_permisson_check);
        m_btn_confirm.setOnClickListener(BtnOnClickListener);
    }

    Button.OnClickListener BtnOnClickListener = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.btn_permisson_check:
                    m_listener.onClickConfirm();
                    dismiss();
                    break;
                default:
                    break;
            }
        }
    };


    public void setListener(DialogBoxListener listener) { m_listener = listener; }

    public interface DialogBoxListener {
        void onClickConfirm();              // 확인 버튼 클릭 시 호출
    }
}
