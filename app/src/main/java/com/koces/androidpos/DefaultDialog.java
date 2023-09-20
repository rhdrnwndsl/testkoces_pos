package com.koces.androidpos;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.van.Constants;

import java.util.Objects;
public class DefaultDialog extends Dialog  {
    private DialogBoxListener m_listener;
    private String mMsg = "";
    TextView mTxt_msg;
    Button mBtn_cancel;
    Context mCtx;

    /**이 초기화 함수는 절대 사용 금지 **/
    public DefaultDialog(){
        super(null);
    };
    public DefaultDialog(@NonNull Context context){super(context);};

    public DefaultDialog(@NonNull Context context, String _msg, DialogBoxListener _listener) {
        super(context);
        mCtx = context;
        mMsg = _msg;
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
        setContentView(R.layout.custom_default_dialog);
        init();
    }

    private void init()
    {
        mTxt_msg = findViewById(R.id.txt_default_msg);
        String 부호 = mMsg.substring(0,1);
        if(부호.equals("0"))
        {
            부호 = "+";
        }
        else
        {
            부호 = "-";
        }

        int 잔액 = Integer.parseInt(mMsg.substring(1,13));
        int 출금가능금액 = Integer.parseInt(mMsg.substring(13));
        String msg = "잔액 : " + 부호 + String.valueOf(잔액) + "\n" +
                "출금가능금액 : " + String.valueOf(출금가능금액);
        mTxt_msg.setText(msg);
        mBtn_cancel = findViewById(R.id.btn_default_ok);
        mBtn_cancel.setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm("조회를 완료하였습니다");
            }
            dismiss();
        });
    }

    public void setListener(DialogBoxListener listener) { m_listener = listener; }

    public interface DialogBoxListener {
        void onClickConfirm(String _msg);              // 취소 버튼 클릭 시 호출

    }
}
