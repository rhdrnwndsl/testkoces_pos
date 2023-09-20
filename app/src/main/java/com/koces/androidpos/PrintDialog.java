package com.koces.androidpos;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.koces.androidpos.sdk.Setting;

import java.util.Objects;

public class PrintDialog extends Dialog {
    private DialogBoxListener m_listener;
    Context mCtx;
    TextView m_message,m_countTimer;
    String txt_msg;
    int m_count = 0;

    /** 메세지 박스 시간체크 */
    CountDownTimer countDownTimer;

    /**이 초기화 함수는 절대 사용 금지 **/
    public PrintDialog(){
        super(null);
    };
    public PrintDialog(@NonNull Context context){super(context);};

    public PrintDialog(@NonNull Context context,String _Message, int _Count, DialogBoxListener _listener) {
        super(context);
        mCtx = context;
        txt_msg = _Message;
        m_count = _Count;
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
        setContentView(R.layout.custom_print);
        init();
    }

    private void init()
    {
        m_message = findViewById(R.id.txt_msg_txt);
        m_message.setText(txt_msg);
        m_countTimer = (TextView)findViewById(R.id.txt_dlg_countTimer);
        if(m_message!=null)
        {
            m_message.setText(txt_msg);
        }
        if(countDownTimer!=null)
        {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if(m_count > 0)
        {
            if(countDownTimer==null) {
                countDownTimer = new CountDownTimer(m_count * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (m_countTimer != null) {
                            m_countTimer.setVisibility(View.VISIBLE);
                            m_countTimer.setText(String.valueOf((int)millisUntilFinished/1000));
                        }
                    }

                    @Override
                    public void onFinish() {
                        if (m_countTimer != null) {
                            m_countTimer.setVisibility(View.INVISIBLE);
                            m_countTimer.setText("");
                            String _command_cancel = m_message.getText().toString();
                            m_listener.onResult("타임아웃오류. 시간안에 프린트가 완료되지 않았습니다.");
                            DisMiss();
                        }
                    }
                };
                countDownTimer.start();
            }
        }
    }

    @Override
    public void onBackPressed() {

//        dismiss();
//        super.onBackPressed();
    }

    public void DisMiss()
    {
        if(countDownTimer!=null)
        {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        dismiss();
    }

    public void setListener(DialogBoxListener listener) { m_listener = listener; }

    public interface DialogBoxListener {
        void onResult(String _msg);              // 취소 버튼 클릭 시 호출
    }
}
