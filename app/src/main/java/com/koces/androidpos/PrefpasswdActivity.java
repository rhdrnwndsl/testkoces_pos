package com.koces.androidpos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.Setting;

import java.security.PrivateKey;

public class PrefpasswdActivity extends BaseActivity {

    KocesPosSdk mPosSdk;
    Button mbtn_pref_exit,mbtn_btn_pref_ok;
    EditText mPasswd;
    private final static String Passwd = "3415";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prefpasswd);
        init();
    }

    private void init()
    {
        actAdd(this);
        mPosSdk = KocesPosSdk.getInstance();
        mPosSdk.setFocusActivity(this,null);
        Setting.setTopContext(this);
        mbtn_btn_pref_ok = (Button)findViewById(R.id.btn_btn_pref_ok);
        mbtn_pref_exit = (Button)findViewById(R.id.btn_pref_exit);
        mPasswd = (EditText)findViewById(R.id.prefpass_inputpasswd);
        mbtn_btn_pref_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPasswd.getText().toString().equals(Passwd))
                {
                    //Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                    Intent intent = new Intent(getApplicationContext(), menu2Activity.class); // for tabslayout
              //      actlist.remove(2);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else
                {
                    Showerror();
                }
            }
        });

        mbtn_pref_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
        //        actlist.remove(2);
                Intent intent = new Intent(getApplicationContext(), Main2Activity.class);
                //Intent intent = new Intent(getApplicationContext(), menu2Activity.class); // for tabslayout
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("AppToApp",2);
                startActivity(intent);
            }
        });

        Setting.setIsAppForeGround(1);
    }

    private void Showerror()
    {
        Toast.makeText(this,"패스워드가 일치 하지 않습니다.",Toast.LENGTH_SHORT).show();
    }
}
