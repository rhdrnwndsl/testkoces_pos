package com.koces.androidpos;


import static com.koces.androidpos.sdk.Setting.PayDeviceType;


import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.content.Context;

import com.koces.androidpos.sdk.CatPaymentSdk;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.van.Constants;

/**
 * 화면에 나타나는 메세지박스 커스텀다이얼로그
 */
public class CustomDialog extends Dialog {

    /** 메세지 박스 시간체크 */
    CountDownTimer countDownTimer;
    /** 취소버튼 클릭 시 AppToAppActivity 로 메세지를 보내기 위해서 */
    private final int RETURN_CANCEL = 1;

    TextView m_message,m_countTimer;
    String txt_msg;
    Button m_cancel;
    Context m_ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState){

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        if(Setting.getIsAppToApp()) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            // Set the content to appear under the system bars so that the
                            // content doesn't resize when the system bars hide and show.

                            // Hide the nav bar and status bar
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }


    private void init()
    {
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            getWindow().getInsetsController().hide(WindowInsets.Type.statusBars());
//        } else {
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }

        setContentView(R.layout.custom_dialog);
        m_message = findViewById(R.id.txt_msg_txt);
        m_message.setText(txt_msg);
        m_countTimer = (TextView)findViewById(R.id.txt_dlg_countTimer);
        m_cancel = findViewById(R.id.btn_cancel_dialog);
        m_cancel.setOnClickListener(BtnOnClickListener);
        m_cancel.setVisibility(View.INVISIBLE);

    }

    Button.OnClickListener BtnOnClickListener = new Button.OnClickListener(){
        @Override
        public void onClick(View v) {
            switch (v.getId())
            {
                case R.id.btn_cancel_dialog:
                    String _command_cancel = m_message.getText().toString();
                    if(countDownTimer!=null)
                    {
                        m_countTimer.setVisibility(View.INVISIBLE);
                        m_countTimer.setText("");
                        stopTimer();
                    }

                    if(m_ctx instanceof AppToAppActivity)
                    {
                        AppToAppActivity activity = (AppToAppActivity) m_ctx;
                        if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                            activity.mPosSdk.__PosInit("99",null,activity.mPosSdk.AllDeviceAddr());
                            activity.mPosSdk.DeviceReset();
                        }
                        else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                            activity.mPosSdk.__BLEPosinit("99",null);
                            activity.mPosSdk.__BLEReadingCancel(null);
                        }
                        else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                            activity.mPosSdk.mTcpClient.DisConnectCatServer();
                            CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                            if (mCatSdk != null)
                            {
                                mCatSdk.Cat_SendCancelCommandE(true);
                            }

                        }
                        activity.ReadyDialogHide();
                        activity.SendreturnData(RETURN_CANCEL,null,"사용자 취소, 다시 거래 해 주세요");
                    }
//                    else if(m_ctx instanceof PaymentActivity)
//                    {
//                        PaymentActivity activity = (PaymentActivity) m_ctx;
//                        if (Setting.g_PayDeviceType == PayDeviceType.LINES) { activity.mPosSdk.DeviceReset(); }
//                        else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {activity.mPosSdk.__BLEPosinit("99",null); activity.mPosSdk.__BLEReadingCancel(null);}
//                        else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {activity.mPosSdk.mTcpClient.DisConnectCatServer();}
//                        activity.ReadyDialogHide();
//                    }
                    else if(m_ctx instanceof Main2Activity)
                    {
                        Main2Activity activity = (Main2Activity) m_ctx;
                        if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
//                            activity.mKocesPosSdk.__PosInit("99",null,activity.mKocesPosSdk.AllDeviceAddr());
                            activity.mKocesPosSdk.DeviceReset();
                        }
                        else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {activity.mKocesPosSdk.__BLEPosinit("99",null); activity.mKocesPosSdk.__BLEReadingCancel(null);}
                        else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                            activity.mKocesPosSdk.mTcpClient.DisConnectCatServer();
                            CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                            if (mCatSdk != null)
                            {
                                mCatSdk.Cat_SendCancelCommandE(false);
                            }
                            }
                        activity.ReadyDialogHide();
                    }
                    else if(m_ctx instanceof menu2Activity)
                    {
                        menu2Activity activity = (menu2Activity) m_ctx;
                        if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                            activity.mKocesPosSdk.__PosInit("99",null,activity.mKocesPosSdk.AllDeviceAddr());
                            activity.mKocesPosSdk.DeviceReset();
                        }
                        else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {activity.mKocesPosSdk.__BLEPosinit("99",null);activity.mKocesPosSdk.__BLEReadingCancel(null);}
                        else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                            activity.mKocesPosSdk.mTcpClient.DisConnectCatServer();
                            CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                            if (mCatSdk != null)
                            {
                                mCatSdk.Cat_SendCancelCommandE(false);
                            }
                        }
                        activity.ReadyDialogHide();
                    }
                    else if(m_ctx instanceof EasyPayActivity)
                    {
                        EasyPayActivity activity = (EasyPayActivity) m_ctx;
                        if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                            activity.getMKocesSdk().__PosInit("99",null,activity.getMKocesSdk().AllDeviceAddr());
                            activity.getMKocesSdk().DeviceReset();
                        }
                        else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {activity.getMKocesSdk().__BLEPosinit("99",null);activity.getMKocesSdk().__BLEReadingCancel(null);}
                        else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                            activity.getMKocesSdk().mTcpClient.DisConnectCatServer();
                            CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                            if (mCatSdk != null)
                            {
                                mCatSdk.Cat_SendCancelCommandE(false);
                            }
                        }
                        activity.ReadyDialogHide();
                    }
                    else if(m_ctx instanceof ReceiptCreditActivity)
                    {
                        ReceiptCreditActivity activity = (ReceiptCreditActivity) m_ctx;
                        if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                            activity.getMKocesSdk().__PosInit("99",null,activity.getMKocesSdk().AllDeviceAddr());
                            activity.getMKocesSdk().DeviceReset();
                        }
                        else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {activity.getMKocesSdk().__BLEPosinit("99",null);activity.getMKocesSdk().__BLEReadingCancel(null);}
                        else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                            activity.getMKocesSdk().mTcpClient.DisConnectCatServer();
                            CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                            if (mCatSdk != null)
                            {
                                mCatSdk.Cat_SendCancelCommandE(false);
                            }
                        }
                        activity.ReadyDialogHide();
                    }
                    else if(m_ctx instanceof ReceiptCashActivity)
                    {
                        ReceiptCashActivity activity = (ReceiptCashActivity) m_ctx;
                        if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                            activity.getMKocesSdk().__PosInit("99",null,activity.getMKocesSdk().AllDeviceAddr());
                            activity.getMKocesSdk().DeviceReset();
                        }
                        else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {activity.getMKocesSdk().__BLEPosinit("99",null);activity.getMKocesSdk().__BLEReadingCancel(null);}
                        else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                            activity.getMKocesSdk().mTcpClient.DisConnectCatServer();
                            CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                            if (mCatSdk != null)
                            {
                                mCatSdk.Cat_SendCancelCommandE(false);
                            }
                        }
                        activity.ReadyDialogHide();
                    }
                    else if(m_ctx instanceof ReceiptEasyActivity)
                    {
                        ReceiptEasyActivity activity = (ReceiptEasyActivity) m_ctx;
                        if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                            activity.getMKocesSdk().__PosInit("99",null,activity.getMKocesSdk().AllDeviceAddr());
                            activity.getMKocesSdk().DeviceReset();
                        }
                        else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {activity.getMKocesSdk().__BLEPosinit("99",null);activity.getMKocesSdk().__BLEReadingCancel(null);}
                        else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                            activity.getMKocesSdk().mTcpClient.DisConnectCatServer();
                            CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                            if (mCatSdk != null)
                            {
                                mCatSdk.Cat_SendCancelCommandE(false);
                            }
                        }
                        activity.ReadyDialogHide();
                    }
                    break;
            }
        }
    };

    /**
     * 메세지박스의 취소버튼을 화면에 표시할지 감출지를 정한다
     * @param _hide
     */
    public void SetCancelBtn(boolean _hide)
    {
        if(_hide == true)
        {
            m_cancel.setVisibility(View.VISIBLE);
        }
        else
        {
            m_cancel.setVisibility(View.GONE);
        }
    }

    /**
     * 일반적인 메세지박스 생성
     * @param _ctx 해당 엑티비티
     * @param _Message 표시할 메세지
     * @param _imgPath1 이미지1(사용안함)
     * @param _imgPath2 이미지2(사용안함)
     * @param _Count 카운트다운(0초로 고정)
     */
    public CustomDialog(Context _ctx,String _Message,String _imgPath1,String _imgPath2,int _Count)
    {
        super(_ctx);
        m_ctx = _ctx;
        init();
        setDialog(_ctx,_Message,_imgPath1,_imgPath2,_Count,false);
    }

    /**
     * 결제관련하여 카운트다운을 해야 할 상황에서의 생성. 그 외에는 미사용
     * @param _ctx 해당 엑티비티
     * @param _Message 표시할 메세지
     * @param _imgPath1 이미지1(사용안함)
     * @param _imgPath2 이미지2(사용안함)
     * @param _Count 카운트다운
     * @param _visible 취소버튼을 표시할지를 체크
     */
    public CustomDialog(Context _ctx,String _Message,String _imgPath1,String _imgPath2,int _Count, boolean _visible)
    {
        super(_ctx);
        m_ctx = _ctx;
        init();
        setDialog(_ctx,_Message,_imgPath1,_imgPath2,_Count, _visible);
    }

    /** 메세지박스의 카운트다운을 멈춘다 */
    public void stopTimer()
    {
        if(countDownTimer!=null)
        {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    /**
     * 생성자를 통해 만들어진 메세지박스 다이얼로그를 정상적으로 화면에 출력
     * @param _ctx 표시할 엑티비티
     * @param _Message 표시할 메세지
     * @param _imgPath1 이미지1(사용안함)
     * @param _imgPath2 이미지2(사용안함)
     * @param _Count 카운트다운
     * @param _visible 취소버튼을 표시할지를 체크
     */
    public void setDialog(Context _ctx,String _Message,String _imgPath1,String _imgPath2,int _Count, boolean _visible)
    {
        m_ctx = _ctx;
        txt_msg = _Message;
        if(m_message!=null)
        {
            m_message.setText(txt_msg);
        }
        if(countDownTimer!=null)
        {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if(m_cancel != null)
        {
            m_cancel.setVisibility(View.GONE);
        }

        if(_Count > 0)
        {
            if(countDownTimer==null) {
                countDownTimer = new CountDownTimer(_Count * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (m_countTimer != null) {
                            m_countTimer.setVisibility(View.VISIBLE);
                            m_countTimer.setText(String.valueOf((int)millisUntilFinished/1000));
                            if(m_cancel!=null && m_cancel.getVisibility()==View.GONE && (int)_Count - ((int)millisUntilFinished/1000)>= 5)
                            {
                                if(_visible==true)
                                {
                                    m_cancel.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFinish() {
                        if (m_countTimer != null) {
                            m_countTimer.setVisibility(View.INVISIBLE);
                            m_countTimer.setText("");
                            SetCancelBtn(false);
                            String _command_cancel = m_message.getText().toString();

                            if(m_ctx instanceof AppToAppActivity)
                            {
                                AppToAppActivity activity = (AppToAppActivity) m_ctx;
                                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                                    activity.mPosSdk.__PosInit("99",null,activity.mPosSdk.AllDeviceAddr());
                                    activity.mPosSdk.DeviceReset();
                                    activity.mPosSdk.mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                                    activity.mPosSdk.__BLEPosinit("99",null);
                                    activity.mPosSdk.__BLEReadingCancel(null);
                                    activity.mPosSdk.mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                                    activity.mPosSdk.mTcpClient.DisConnectCatServer();
                                    CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                                    if (mCatSdk != null)
                                    {
                                        mCatSdk.Cat_SendCancelCommandE(true);
                                    }

                                }
                                activity.ReadyDialogHide();
                                activity.SendreturnData(RETURN_CANCEL,null,"대기시간 초과 다시 거래 해주세요");
                            }
//                            else if(m_ctx instanceof PaymentActivity)
//                            {
//                                PaymentActivity activity = (PaymentActivity) m_ctx;
//                                if (Setting.g_PayDeviceType == PayDeviceType.LINES) { activity.mPosSdk.DeviceReset(); }
//                                else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {activity.mPosSdk.__BLEPosinit("99",null); activity.mPosSdk.__BLEReadingCancel(null);}
//                                else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {activity.mPosSdk.mTcpClient.DisConnectCatServer();}
//                                activity.ReadyDialogHide();
//                            }
                            else if(m_ctx instanceof Main2Activity)
                            {
                                Main2Activity activity = (Main2Activity) m_ctx;
                                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                                    activity.mKocesPosSdk.__PosInit("99",null,activity.mKocesPosSdk.AllDeviceAddr());
                                    activity.mKocesPosSdk.DeviceReset();
                                    activity.mKocesPosSdk.mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                                    activity.mKocesPosSdk.__BLEPosinit("99",null);
                                    activity.mKocesPosSdk.__BLEReadingCancel(null);
                                    activity.mKocesPosSdk.mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                                    activity.mKocesPosSdk.mTcpClient.DisConnectCatServer();
                                    CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                                    if (mCatSdk != null)
                                    {
                                        mCatSdk.Cat_SendCancelCommandE(false);
                                    }
                                   }
                                activity.ReadyDialogHide();
                            }
                            else if(m_ctx instanceof menu2Activity)
                            {
                                menu2Activity activity = (menu2Activity) m_ctx;
                                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                                    activity.mKocesPosSdk.__PosInit("99",null,activity.mKocesPosSdk.AllDeviceAddr());
                                    activity.mKocesPosSdk.DeviceReset();
                                    activity.mKocesPosSdk.mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                                    activity.mKocesPosSdk.__BLEPosinit("99",null);
                                    activity.mKocesPosSdk.__BLEReadingCancel(null);
                                    activity.mKocesPosSdk.mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                                    activity.mKocesPosSdk.mTcpClient.DisConnectCatServer();
                                    CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                                    if (mCatSdk != null)
                                    {
                                        mCatSdk.Cat_SendCancelCommandE(false);
                                    }
                                   }
                                activity.ReadyDialogHide();
                            }
                            else if(m_ctx instanceof EasyPayActivity)
                            {
                                EasyPayActivity activity = (EasyPayActivity) m_ctx;
                                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                                    activity.getMKocesSdk().__PosInit("99",null,activity.getMKocesSdk().AllDeviceAddr());
                                    activity.getMKocesSdk().DeviceReset();
                                    activity.getMKocesSdk().mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                                    activity.getMKocesSdk().__BLEPosinit("99",null);
                                    activity.getMKocesSdk().__BLEReadingCancel(null);
                                    activity.getMKocesSdk().mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                                    activity.getMKocesSdk().mTcpClient.DisConnectCatServer();
                                    CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                                    if (mCatSdk != null)
                                    {
                                        mCatSdk.Cat_SendCancelCommandE(false);
                                    }
                                }
                                activity.ReadyDialogHide();
                            }
                            else if(m_ctx instanceof ReceiptCreditActivity)
                            {
                                ReceiptCreditActivity activity = (ReceiptCreditActivity) m_ctx;
                                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                                    activity.getMKocesSdk().__PosInit("99",null,activity.getMKocesSdk().AllDeviceAddr());
                                    activity.getMKocesSdk().DeviceReset();
                                    activity.getMKocesSdk().mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                                    activity.getMKocesSdk().__BLEPosinit("99",null);
                                    activity.getMKocesSdk().__BLEReadingCancel(null);
                                    activity.getMKocesSdk().mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                                    activity.getMKocesSdk().mTcpClient.DisConnectCatServer();
                                    CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                                    if (mCatSdk != null)
                                    {
                                        mCatSdk.Cat_SendCancelCommandE(false);
                                    }
                                }
                                activity.ReadyDialogHide();
                            }
                            else if(m_ctx instanceof ReceiptCashActivity)
                            {
                                ReceiptCashActivity activity = (ReceiptCashActivity) m_ctx;
                                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                                    activity.getMKocesSdk().__PosInit("99",null,activity.getMKocesSdk().AllDeviceAddr());
                                    activity.getMKocesSdk().DeviceReset();
                                    activity.getMKocesSdk().mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                                    activity.getMKocesSdk().__BLEPosinit("99",null);
                                    activity.getMKocesSdk().__BLEReadingCancel(null);
                                    activity.getMKocesSdk().mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                                    activity.getMKocesSdk().mTcpClient.DisConnectCatServer();
                                    CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                                    if (mCatSdk != null)
                                    {
                                        mCatSdk.Cat_SendCancelCommandE(false);
                                    }
                                }
                                activity.ReadyDialogHide();
                            }
                            else if(m_ctx instanceof ReceiptEasyActivity)
                            {
                                ReceiptEasyActivity activity = (ReceiptEasyActivity) m_ctx;
                                if (Setting.g_PayDeviceType == PayDeviceType.LINES) {
                                    activity.getMKocesSdk().__PosInit("99",null,activity.getMKocesSdk().AllDeviceAddr());
                                    activity.getMKocesSdk().DeviceReset();
                                    activity.getMKocesSdk().mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.BLE) {
                                    activity.getMKocesSdk().__BLEPosinit("99",null);
                                    activity.getMKocesSdk().__BLEReadingCancel(null);
                                    activity.getMKocesSdk().mTcpClient.Dispose();
                                }
                                else if (Setting.g_PayDeviceType == PayDeviceType.CAT) {
                                    activity.getMKocesSdk().mTcpClient.DisConnectCatServer();
                                    CatPaymentSdk mCatSdk = CatPaymentSdk.getInstance();
                                    if (mCatSdk != null)
                                    {
                                        mCatSdk.Cat_SendCancelCommandE(false);
                                    }
                                }
                                activity.ReadyDialogHide();
                            }
//                            if (Setting.g_PayDeviceType == PayDeviceType.BLE || Setting.g_PayDeviceType == PayDeviceType.CAT)
//                            {
//                                if (m_ctx instanceof AppToAppActivity) {
//                                    AppToAppActivity activity = (AppToAppActivity) m_ctx;
//                                    activity.ReadyDialogHide();
//                                    activity.SendreturnData(RETURN_CANCEL, null, "대기시간 초과 다시 거래 해주세요");
//                                } else if (m_ctx instanceof PaymentActivity) {
//                                    PaymentActivity activity = (PaymentActivity) m_ctx;
//                                    activity.ReadyDialogHide();
//                                } else if (m_ctx instanceof Main2Activity) {
//                                    Main2Activity activity = (Main2Activity) m_ctx;
//                                    activity.ReadyDialogHide();
//                                } else if (m_ctx instanceof menu2Activity) {
//                                    menu2Activity activity = (menu2Activity) m_ctx;
//                                    activity.ReadyDialogHide();
//                                }
//                                return;
//                            }
//
//                            if(_command_cancel.equals("PIN 번호를 입력해 주세요")) {
//                                if (m_ctx instanceof AppToAppActivity) {
//                                    AppToAppActivity activity = (AppToAppActivity) m_ctx;
//                                    activity.mPosSdk.DeviceReset();
//                                    activity.ReadyDialogHide();
//                                    activity.SendreturnData(RETURN_CANCEL, null, "카드 대기시간 초과 다시 거래 해주세요");
//                                } else if (m_ctx instanceof PaymentActivity) {
//                                    PaymentActivity activity = (PaymentActivity) m_ctx;
//                                    activity.mPosSdk.DeviceReset();
//                                    activity.ReadyDialogHide();
//                                } else if (m_ctx instanceof Main2Activity) {
//                                    Main2Activity activity = (Main2Activity) m_ctx;
//                                    activity.mKocesPosSdk.DeviceReset();
//                                    activity.ReadyDialogHide();
//                                } else if (m_ctx instanceof menu2Activity) {
//                                    menu2Activity activity = (menu2Activity) m_ctx;
//                                    activity.mKocesPosSdk.DeviceReset();
//                                    activity.ReadyDialogHide();
//                                }
//                            } else if(_command_cancel.equals("IC오류입니다. 마그네틱을 읽혀주세요"))
//                            {
//                                if (m_ctx instanceof AppToAppActivity) {
//                                    AppToAppActivity activity = (AppToAppActivity) m_ctx;
//                                    activity.mPosSdk.DeviceReset();
//                                    activity.ReadyDialogHide();
//                                    activity.SendreturnData(RETURN_CANCEL, null, "카드 대기시간 초과 다시 거래 해주세요");
//                                } else if (m_ctx instanceof PaymentActivity) {
//                                    PaymentActivity activity = (PaymentActivity) m_ctx;
//                                    activity.mPosSdk.DeviceReset();
//                                    activity.ReadyDialogHide();
//                                } else if (m_ctx instanceof Main2Activity) {
//                                    Main2Activity activity = (Main2Activity) m_ctx;
//                                    activity.mKocesPosSdk.DeviceReset();
//                                    activity.ReadyDialogHide();
//                                } else if (m_ctx instanceof menu2Activity) {
//                                    menu2Activity activity = (menu2Activity) m_ctx;
//                                    activity.mKocesPosSdk.DeviceReset();
//                                    activity.ReadyDialogHide();
//                                }
//                            }
                        }
                    }
                };
                countDownTimer.start();
            }
        }
        else
        {
            if(m_countTimer!=null) {
                m_countTimer.setVisibility(View.INVISIBLE);
                m_countTimer.setText("");
                SetCancelBtn(false);
            }
        }
    }

    @Override
    public void onBackPressed() {

//        dismiss();
//        super.onBackPressed();
    }

}
