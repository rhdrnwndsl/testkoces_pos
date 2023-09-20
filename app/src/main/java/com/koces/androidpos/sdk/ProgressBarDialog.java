package com.koces.androidpos.sdk;

import androidx.annotation.NonNull;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koces.androidpos.R;

public class ProgressBarDialog extends Dialog {
     ProgressBar mProgressBar;
    TextView mTvwMsg1;
    Button mBtnOk;
    LinearLayout mLinearButton;
    String mStrMsg1 = "";           //TextView 텍스트
    String mBtnMsg1 = "";           //버튼 텍스트
    boolean mBtnOnOff = true;       //버튼 보이게 표시, 비표시
    public ProgressBarDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_progress_bar);
        init();

    }

    private void init(){
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar1);
        mTvwMsg1 = (TextView)findViewById(R.id.progress_tvw_text1);
        mBtnOk = (Button)findViewById(R.id.progress_btn_ok);
        mLinearButton = (LinearLayout)findViewById(R.id.progress_linear_button);
        mTvwMsg1.setText(mStrMsg1);
        mBtnOk.setText(mBtnMsg1);

        if(!mBtnOnOff){
            mLinearButton.setVisibility(View.GONE);
        }
        this.setCancelable(false);      // 외부 영역 클릭시 사라지지 않게 하기
    }
    public void ButtonSetOnClickListener(View.OnClickListener l){
        mBtnOk.setOnClickListener(l);
    }
    public void setText(String _str){
        if(Utils.isNullOrEmpty(_str)){
            mStrMsg1 = this.getContext().getResources().getString(R.string.txt_progress_now);
        }
        else{
            mStrMsg1 = _str;
        }
        mBtnOnOff = false;
    }
    public void setText(String _str,String _btnStr){
        if(Utils.isNullOrEmpty(_str)){
            mStrMsg1 = this.getContext().getResources().getString(R.string.txt_progress_now);
        }
        else{
            mStrMsg1 = _str;
        }

        if(Utils.isNullOrEmpty(_btnStr)){
            mBtnMsg1 = this.getContext().getResources().getString(R.string.txt_ok);
        }
        else{
           mBtnMsg1 = _btnStr;
        }

        mBtnOnOff = true;
    }
}