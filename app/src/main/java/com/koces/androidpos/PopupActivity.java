package com.koces.androidpos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.koces.androidpos.sdk.Setting;

public class PopupActivity extends BaseActivity {
    private final static String TAG = PopupActivity.class.getSimpleName();
    Button btn_exit;
    TextView txtText;
    String mData = "";
    String imgPath1 = "";
    String imgPath2 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);

        Setting.mPopupListener = mPoplistener;
        txtText = (TextView)findViewById(R.id.txt_popup_contents);

        //데이터 가져오기
        Intent intent = getIntent();
        mData = intent.getStringExtra("contents");
        imgPath1 = intent.getStringExtra("img1");
        imgPath2 = intent.getStringExtra("img2");


        txtText.setText(mData);
        if(!imgPath1.equals(""))
        {
            if(isExternalStorageReadable())
            {

            }
        }

        if(!imgPath2.equals(""))
        {
            if(isExternalStorageReadable())
            {

            }
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //바깥레이어 클릭시 안닫히게
        if(event.getAction()== MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //안드로이드 백버튼 막기
        return;
    }

    private void updateData(String _msg,String _img1,String _img2)
    {
        mData = _msg;
        txtText.setText(mData);
    }
    popupInterface.popupListener mPoplistener = new popupInterface.popupListener() {
        @Override
        public void onState(boolean _state, String _Message, String _imgPath1, String _imgPath2) {
            if(!_state)
            {
                Setting.mPopupListener = null;
                finish();
            }
            else
            {
                updateData(_Message,_imgPath1,_imgPath2);

            }
        }
    };

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

}
