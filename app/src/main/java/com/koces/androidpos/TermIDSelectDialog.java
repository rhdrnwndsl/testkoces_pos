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

public class TermIDSelectDialog extends Dialog {
    private DialogBoxListener m_listener;
    private String mTid, mName;
    private boolean mAll = false;
    Button mBtn_cancel;
    Button[] mBtn_Tid;
    Context mCtx;

    /**이 초기화 함수는 절대 사용 금지 **/
    public TermIDSelectDialog(){
        super(null);
    };
    public TermIDSelectDialog(@NonNull Context context){super(context);};

    public TermIDSelectDialog(@NonNull Context context, boolean _all, DialogBoxListener _listener) {
        super(context);
        mCtx = context;
        mAll = _all;
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
        setContentView(R.layout.custom_tid_select);
        if (mAll) {
            initAll();
        } else {
            init();
        }

    }

    private void initAll()
    {
        mBtn_Tid = new Button[6];
        mBtn_Tid[0] = findViewById(R.id.btn_tidselect_1);
        mBtn_Tid[1] = findViewById(R.id.btn_tidselect_2);
        mBtn_Tid[2] = findViewById(R.id.btn_tidselect_3);
        mBtn_Tid[3] = findViewById(R.id.btn_tidselect_4);
        mBtn_Tid[4] = findViewById(R.id.btn_tidselect_5);
        mBtn_Tid[5] = findViewById(R.id.btn_tidselect_6);
        Button mall = findViewById(R.id.btn_tidselect_0);

        mall.setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm("",
                        "",
                        "",
                        "",
                        "",
                        "");
            }
            dismiss();
        });

        mBtn_Tid[0].setOnClickListener((View v)->{
            if (m_listener != null) {
//                m_listener.onClickConfirm("",
//                        "",
//                        "",
//                        "",
//                        "",
//                        "");
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID ),
                        Setting.getPreference(mCtx, Constants.STORE_NM ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR ),
                        Setting.getPreference(mCtx, Constants.STORE_NO ),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM ));
            }
            dismiss();
        });

        mBtn_Tid[1].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 1),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 1 ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR  + 1),
                        Setting.getPreference(mCtx, Constants.STORE_NO  + 1),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 1 ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 1 ));
            }
            dismiss();
        });
        mBtn_Tid[2].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 2),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 2 ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR + 2 ),
                        Setting.getPreference(mCtx, Constants.STORE_NO + 2 ),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 2 ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 2 ));
            }
            dismiss();
        });
        mBtn_Tid[3].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 3),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 3 ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR  + 3),
                        Setting.getPreference(mCtx, Constants.STORE_NO + 3 ),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 3 ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 3 ));
            }
            dismiss();
        });
        mBtn_Tid[4].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 4),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 4),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR + 4),
                        Setting.getPreference(mCtx, Constants.STORE_NO + 4),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 4),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 4));
            }
            dismiss();
        });
        mBtn_Tid[5].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 5),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 5 ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR + 5 ),
                        Setting.getPreference(mCtx, Constants.STORE_NO + 5 ),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 5 ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 5 ));
            }
            dismiss();
        });


        for (int i=0; i<6; i++)
        {
            mBtn_Tid[i].setVisibility(View.GONE);
        }

        mBtn_Tid[0].setVisibility(View.VISIBLE);
        mBtn_Tid[0].setText(Setting.getPreference(mCtx, Constants.TID) + " " + Setting.getPreference(mCtx, Constants.STORE_NM));

        for (int i=1; i<6; i++)
        {
            if(!(Setting.getPreference(mCtx, Constants.TID + i)).equals(""))
            {
                mBtn_Tid[i].setVisibility(View.VISIBLE);
                mBtn_Tid[i].setText(Setting.getPreference(mCtx, Constants.TID + i) + " " + Setting.getPreference(mCtx, Constants.STORE_NM + i));
            }

        }

        mBtn_cancel = findViewById(R.id.btn_tidselect_cancel);

        mBtn_cancel.setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickCancel("입력을 취소하였습니다");
            }
            dismiss();
        });
    }

    private void init()
    {
        mBtn_Tid = new Button[6];
        mBtn_Tid[0] = findViewById(R.id.btn_tidselect_1);
        mBtn_Tid[1] = findViewById(R.id.btn_tidselect_2);
        mBtn_Tid[2] = findViewById(R.id.btn_tidselect_3);
        mBtn_Tid[3] = findViewById(R.id.btn_tidselect_4);
        mBtn_Tid[4] = findViewById(R.id.btn_tidselect_5);
        mBtn_Tid[5] = findViewById(R.id.btn_tidselect_6);
        Button mall = findViewById(R.id.btn_tidselect_0);
//        mBtn_Tid[6] = findViewById(R.id.btn_tidselect_6);


        mBtn_Tid[0].setOnClickListener((View v)->{
            if (m_listener != null) {
//                m_listener.onClickConfirm("",
//                        "",
//                        "",
//                        "",
//                        "",
//                        "");
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID ),
                        Setting.getPreference(mCtx, Constants.STORE_NM ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR ),
                        Setting.getPreference(mCtx, Constants.STORE_NO ),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM ));
            }
            dismiss();
        });

        mBtn_Tid[1].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 1),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 1 ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR  + 1),
                        Setting.getPreference(mCtx, Constants.STORE_NO  + 1),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 1 ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 1 ));
            }
            dismiss();
        });
        mBtn_Tid[2].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 2),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 2 ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR + 2 ),
                        Setting.getPreference(mCtx, Constants.STORE_NO + 2 ),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 2 ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 2 ));
            }
            dismiss();
        });
        mBtn_Tid[3].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 3),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 3 ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR  + 3),
                        Setting.getPreference(mCtx, Constants.STORE_NO + 3 ),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 3 ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 3 ));
            }
            dismiss();
        });
        mBtn_Tid[4].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 4),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 4),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR + 4),
                        Setting.getPreference(mCtx, Constants.STORE_NO + 4),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 4),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 4));
            }
            dismiss();
        });
        mBtn_Tid[5].setOnClickListener((View v)->{
            if (m_listener != null) {
                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 5),
                        Setting.getPreference(mCtx, Constants.STORE_NM + 5 ),
                        Setting.getPreference(mCtx, Constants.STORE_ADDR + 5 ),
                        Setting.getPreference(mCtx, Constants.STORE_NO + 5 ),
                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 5 ),
                        Setting.getPreference(mCtx, Constants.OWNER_NM + 5 ));
            }
            dismiss();
        });
//        mBtn_Tid[6].setOnClickListener((View v)->{
//            if (m_listener != null) {
//                m_listener.onClickConfirm(Setting.getPreference(mCtx, Constants.TID + 5),
//                        Setting.getPreference(mCtx, Constants.STORE_NM + 5 ),
//                        Setting.getPreference(mCtx, Constants.STORE_ADDR + 5 ),
//                        Setting.getPreference(mCtx, Constants.STORE_NO + 5 ),
//                        Setting.getPreference(mCtx, Constants.STORE_PHONE + 5 ),
//                        Setting.getPreference(mCtx, Constants.OWNER_NM + 5 ));
//            }
//            dismiss();
//        });

//        if (!mAll)
//        {
//            mBtn_Tid[0].setVisibility(View.GONE);
//        }
        mall.setVisibility(View.GONE);
        for (int i=0; i<6; i++)
        {
            mBtn_Tid[i].setVisibility(View.GONE);
        }
//        Setting.getPreference(mCtx, Constants.TID);
//        Setting.getPreference(mCtx, Constants.STORE_NO);
//        Setting.getPreference(mCtx, Constants.STORE_ADDR);
//        Setting.getPreference(mCtx, Constants.OWNER_NM);
//        Setting.getPreference(mCtx, Constants.STORE_PHONE);
//        Setting.getPreference(mCtx, Constants.STORE_NM);

        mBtn_Tid[0].setVisibility(View.VISIBLE);
        mBtn_Tid[0].setText(Setting.getPreference(mCtx, Constants.TID) + " " + Setting.getPreference(mCtx, Constants.STORE_NM));

        for (int i=1; i<6; i++)
        {
            if(!(Setting.getPreference(mCtx, Constants.TID + i)).equals(""))
            {
                mBtn_Tid[i].setVisibility(View.VISIBLE);
                mBtn_Tid[i].setText(Setting.getPreference(mCtx, Constants.TID + i) + " " + Setting.getPreference(mCtx, Constants.STORE_NM + i));
            }

//            Setting.getPreference(mCtx, Constants.TID + i);
//            Setting.getPreference(mCtx, Constants.STORE_NO + i);
//            Setting.getPreference(mCtx, Constants.STORE_ADDR + i);
//            Setting.getPreference(mCtx, Constants.OWNER_NM + i);
//            Setting.getPreference(mCtx, Constants.STORE_PHONE + i);
//            Setting.getPreference(mCtx, Constants.STORE_NM + i);
        }

        mBtn_cancel = findViewById(R.id.btn_tidselect_cancel);

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
        void onClickConfirm(String _tid,String _storeName, String _storeAddr,
                            String _storeNumber, String _storePhone, String _Owner);              // 각 TID 클릭 시 호출
    }
}
