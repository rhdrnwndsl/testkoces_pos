package com.koces.androidpos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.van.Constants;
public class ReceiptActivity extends BaseActivity{
    private final static String TAG = ReceiptActivity.class.getSimpleName();
    Button m_btn_send, m_btn_exit, m_btn_print;
    KocesPosSdk mPosSdk;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);
        init();
    }

    private void init(){
        m_btn_send = findViewById(R.id.txt_receipt_detail_sendBtn);
        m_btn_exit = findViewById(R.id.txt_receipt_cancelBtn);
        m_btn_print = findViewById(R.id.txt_receipt_detail_printBtn);

        m_btn_send.setOnClickListener(BtnOnClickListener);
        m_btn_exit.setOnClickListener(BtnOnClickListener);
        m_btn_print.setOnClickListener(BtnOnClickListener);

        mPosSdk = KocesPosSdk.getInstance();
    }

    Button.OnClickListener BtnOnClickListener = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.txt_receipt_detail_sendBtn:
                    Send();
                    break;
                case R.id.txt_receipt_detail_printBtn:
                    Print();
                    break;
                case R.id.txt_receipt_cancelBtn:
                    Exit();
                    break;
                default:
                    break;
            }
            return;
        }
    };

    private void Send()
    {

    }

    private void Print()
    {

    }

    private void Exit()
    {

    }
}
