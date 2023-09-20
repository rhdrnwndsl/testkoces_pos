package com.koces.androidpos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.ConvertBitmapToMonchromeBitmap;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.van.Constants;

import java.io.UnsupportedEncodingException;

public class SignPadActivity extends BaseActivity {
    private final static String TAG = SignPadActivity.class.getSimpleName();
    /** 실제 화면에 그리는 뷰클래스 */
    SignPad _signView;
    /** 사인패드 레이아웃 */
    LinearLayout _linear_signpad, _linear_view;
    /** 실제 통신, 거래 등의 연동을 위한 곳 */
    KocesPosSdk mKocesPosSdk;
    /** 화면에 그려진 사인데이터를 비트맵이미지로 저장 */
    Bitmap mBMP;
    /** 사인패드를 통해 장비에 표시할 돈(세금+봉사료 포함) */
    int mMoney;
    /** 화면에 사인데이터가 그려질때를 체크. 이를 통해 사인을 종료시점을 정한다 */
    private int StartSign = 0;
    /** (터치서명일떄)해당 시간 동안 쓰레드를 슬립 시키고 사인 그리기 종료 */
    private int mSignDrawTimer = 2000;
    /** (터치서명제외)해당 시간 동안 쓰레드를 슬립 시키고 사인 시리얼 종료메세지 보냄 */
    private int mSignSerialTimer = 2000;
    /** (터치서명일떄)화면에 그리는 사인에 관해서 완료 여부를 검사 하기 위해서 이 코드를 작성한다 */
    public static boolean isDisplaySignFinish = false;
    /** 사인데이터 카운트다운. 사인입력을 기다리는 데 아무것도 하지 않을 시 종료를 위한 타이머 */
    CountDownTimer mSignTimer;
    /** 2회연속으로 같은커맨드가 들어오는 경우가 발생하여 체크한다 */
    private static byte LASTSIGNCOMMAND = (byte)0x00;
    /** (터치서명제외)사인이 끝났다는 것을 체크한다 */
    private boolean bfinishsign = false;
    /** (터치서명제외) 사인데이터가 최초로 화면에 그려지는 지 아닌지를 체크. 이를 통해 터치의 시작을 셋팅한다 */
    boolean _first=false;

    /** 사인패드 저장 및 취소가 연달아 들어올 수 있다. 버튼을 클릭 시 따라서 이를 방지한다 */
    boolean _signpadCount = false;
    public static Button mBtn_signpad_ok, mBtn_signpad_cancel;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            getWindow().getInsetsController().hide(WindowInsets.Type.statusBars());
//        } else {
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }

        setContentView(R.layout.activity_sign_pad);
        isDisplaySignFinish = false;
        actAdd(this);

        initSetting();
    }

    /** 변수들을 초기화및 셋팅하고 실제 사인데이터를 받아오기위해 통신을 요청한다 */
    private void initSetting()
    {
        mKocesPosSdk = KocesPosSdk.getInstance();
        Intent intent = getIntent();
        try {
            mMoney = intent.getExtras().getInt("Money");
        }
        catch (NullPointerException ex)
        {
            //Log.d(TAG,ex.toString());
        }
        _linear_signpad = findViewById(R.id.linear_signpad);

        _linear_view = findViewById(R.id.sign_view);

        //0=서명패드사용안함 1=사인패드 2=멀티사인패드 3=터치서명
        switch (Setting.getSignPadType(mKocesPosSdk.getActivity()))
        {
            case 3:
                // Gets the layout params that will allow you to resize the layout
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) _linear_signpad.getLayoutParams();

                DisplayMetrics metrics = getResources().getDisplayMetrics();
                String _size = Setting.getPreference(this,Constants.SIGNPAD_SIZE);
                float _padSize = 1f;
                switch (_size)
                {
                    case "전체":
                        _padSize = 1f;
                        break;
                    case "보통":
                        _padSize = 1.5f;
                        break;
                    case "작게":
                        _padSize = 2f;
                        break;
                }
                int widthPixels = (int)(metrics.widthPixels / _padSize);
                int heightPixels = (int)(metrics.heightPixels / _padSize);
                params.width = widthPixels;
                params.height = heightPixels;
                _linear_signpad.setLayoutParams(params);
                break;
        }
        

        _signView = findViewById(R.id.sign_signpad);
        mBtn_signpad_ok = findViewById(R.id.btn_signPad_ok);
        /** 연속클릭 방지 시간 */
        final long[] mPrintLastClickTime = {0L};
        mBtn_signpad_ok.setOnClickListener( v -> {
            if (SystemClock.elapsedRealtime() - mPrintLastClickTime[0] < 3000) {

                return;
            }
            mPrintLastClickTime[0] = SystemClock.elapsedRealtime();

            /* 장치 정보를 읽어서 설정 하는 함수         */
            String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
            if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
                Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
            }else
            {
                Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
                Setting.g_PayDeviceType = _type;
            }
            switch (Setting.g_PayDeviceType) {
                case NONE:
                    //아무 장비도 연결되지 않았는데 여기로 올 일이 없다.
                    break;
                case BLE:       //BLE의 경우

                    break;
                case CAT:       //WIFI CAT의 경우
                    //캣의 경우는 이곳에 올 일이 없다.
                    break;
                case LINES:     //유선장치의 경우
                    //아래쪽으로 그대로 진행한다.
                    //0=서명패드사용안함 1=사인패드 2=멀티사인패드 3=터치서명
//                    new Handler(Looper.getMainLooper()).postDelayed(()->{
//                        if(!bfinishsign)
//                        {
//                            switch (Setting.getSignPadType(mKocesPosSdk.getActivity()))
//                            {
//                                case 1:
//                                    if (mKocesPosSdk.getUsbDevice() != null) {
//                                        mKocesPosSdk.__PosInit("99", null, new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
//                                    }
//                                    break;
//                                case 2:
//                                    if (mKocesPosSdk.getUsbDevice() != null) {
//                                        mKocesPosSdk.__PosInit("99", null, new String[]{mKocesPosSdk.getMultiAddr()});
//                                    }
//                                    break;
//                            }
//                            resultCancel();
//                        }
//                    },3000);


                    return;
                default:
                    break;
            }

            SaveBitmap();
            SaveBitmapLocalDisk();
        });
        mBtn_signpad_ok.setVisibility(View.GONE);

        mBtn_signpad_cancel = findViewById(R.id.btn_signPad_cancel);
        mBtn_signpad_cancel.setOnClickListener( v -> {
            if (SystemClock.elapsedRealtime() - mPrintLastClickTime[0] < 3000) {

                return;
            }
            mPrintLastClickTime[0] = SystemClock.elapsedRealtime();
            /* 장치 정보를 읽어서 설정 하는 함수         */
            String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
            if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
                Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
            }else
            {
                Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
                Setting.g_PayDeviceType = _type;
            }
            switch (Setting.g_PayDeviceType) {
                case NONE:
                    //아무 장비도 연결되지 않았는데 여기로 올 일이 없다.
                    break;
                case BLE:       //BLE의 경우

                    break;
                case CAT:       //WIFI CAT의 경우
                    //캣의 경우는 이곳에 올 일이 없다.
                    break;
                case LINES:     //유선장치의 경우
                    //아래쪽으로 그대로 진행한다.
                    //0=서명패드사용안함 1=사인패드 2=멀티사인패드 3=터치서명
                    switch (Setting.getSignPadType(mKocesPosSdk.getActivity()))
                    {
                        case 1:
                            if (mKocesPosSdk.getUsbDevice() != null) {
                                mKocesPosSdk.__PosInit("99", null, new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
                            }
                            break;
                        case 2:
                            if (mKocesPosSdk.getUsbDevice() != null) {
                                mKocesPosSdk.__PosInit("99", null, new String[]{mKocesPosSdk.getMultiAddr()});
                            }
                            break;
                    }
                    break;
                default:
                    break;
            }

            resultCancel();
        });

        LASTSIGNCOMMAND = (byte)0x00;;

        if (Setting.getEashCheck())
        {
            EasyinitSetting();
            return;
        }

        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }
        switch (Setting.g_PayDeviceType) {
            case NONE:
                //아무 장비도 연결되지 않았는데 여기로 올 일이 없다.
                return;
            case BLE:       //BLE의 경우

                BleinitSetting();
                return;
            case CAT:       //WIFI CAT의 경우
                //캣의 경우는 이곳에 올 일이 없다.
                return;
            case LINES:     //유선장치의 경우
                //아래쪽으로 그대로 진행한다.
                break;
            default:
                break;
        }

        //0=서명패드사용안함 1=사인패드 2=멀티사인패드 3=터치서명
        switch (Setting.getSignPadType(mKocesPosSdk.getActivity()))
        {
            case 1:
                if (mKocesPosSdk.getUsbDevice() != null) {
                    mKocesPosSdk.__PosInit("99", mDataListener, new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
                }
                _signView.setIsDrawable(true);
                _signView.setVisibility(View.GONE);
                _linear_view.setVisibility(View.INVISIBLE);
                mBtn_signpad_ok.setVisibility(View.GONE);
                TextView _signText1 = findViewById(R.id.textView);
                _signText1.setText("서명패드 입력 대기 중");

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(mSignTimer!=null){ mSignTimer.cancel();}
                        mSignTimer = null;
                        int _time = Integer.valueOf(Setting.getPreference(mKocesPosSdk.getActivity(),Constants.USB_TIME_OUT));
                        mSignTimer = new CountDownTimer(_time * 1000, 1000) {
                            @Override
                            public void onTick(long l) {
                                Log.d(TAG, "SignTimer =  " + l/1000 );
                            }

                            @Override
                            public void onFinish() {
                                _signView.setIsDrawable(false);
                                resultCancel();
                            }
                        };
                        mSignTimer.cancel();
                        mSignTimer.start();
                    }
                });
                break;
            case 2:
                if (mKocesPosSdk.getUsbDevice() != null) {
                    if(Setting.ICResponseDeviceType ==1)
                    {
                        mKocesPosSdk.__PosInit("99", null, new String[]{mKocesPosSdk.getMultiAddr()});
                        LASTSIGNCOMMAND = Command.ACK;
                        String msg1 = "금액: " + String.valueOf(mMoney) + "원";
                        String msg2 = "서명하세요";
                        mKocesPosSdk.__PadRequestSign("0", "0", Constants.WORKING_KEY, msg1, msg2, " ", " ", mDataListener, new String[]{mKocesPosSdk.getSignPadAddr(), mKocesPosSdk.getMultiAddr()});
                        StartSign = 0;
                    }
                    else if(Setting.ICResponseDeviceType ==3)
                    {
                        LASTSIGNCOMMAND = Command.ACK;
                        String msg1 = "금액: " + String.valueOf(mMoney) + "원";
                        String msg2 = "서명하세요";
                        mKocesPosSdk.__PadRequestSign("0", "0", Constants.WORKING_KEY, msg1, msg2, " ", " ", mDataListener, new String[]{mKocesPosSdk.getSignPadAddr(), mKocesPosSdk.getMultiAddr()});
                        StartSign = 0;
                    }
                }
                _signView.setIsDrawable(true);
                _signView.setVisibility(View.GONE);
                _linear_view.setVisibility(View.INVISIBLE);
                mBtn_signpad_ok.setVisibility(View.GONE);
                TextView _signText2 = findViewById(R.id.textView);
                _signText2.setText("서명패드 입력 대기 중");

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(mSignTimer!=null){ mSignTimer.cancel();}
                        mSignTimer = null;
                        int _time = Integer.valueOf(Setting.getPreference(mKocesPosSdk.getActivity(),Constants.USB_TIME_OUT));
                        mSignTimer = new CountDownTimer(_time * 1000, 1000) {
                            @Override
                            public void onTick(long l) {
                                Log.d(TAG, "SignTimer =  " + l/1000 );
                            }

                            @Override
                            public void onFinish() {
                                _signView.setIsDrawable(false);
                                resultCancel();
                            }
                        };
                        mSignTimer.cancel();
                        mSignTimer.start();
                    }
                });
                break;
            case 0:
            case 3:
                if(Setting.getICReaderType(mKocesPosSdk.getActivity()) == 1)
                {
                    if (mKocesPosSdk.getUsbDevice() != null) {
                        LASTSIGNCOMMAND = Command.ACK;
                        String msg1 = "금액: " + String.valueOf(mMoney) + "원";
                        String msg2 = "서명하세요";
                        mKocesPosSdk.__PadRequestSign("0", "0", Constants.WORKING_KEY, msg1, msg2, " ", " ", mDataListener, mKocesPosSdk.getMultiReaderAddr2());
                        StartSign = 0;
                    }
                    _signView.setIsDrawable(true);
                    _signView.setVisibility(View.GONE);
                    _linear_view.setVisibility(View.INVISIBLE);
                }
                else
                {
                    TextView _signText = findViewById(R.id.textView);
                    _signText.setText("서명을 해 주세요");
                    _signView.setIsDrawable(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true)
                            {
                                try {
                                    Thread.sleep(mSignDrawTimer);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(isDisplaySignFinish)
                                {
                                    SaveBitmap();
                                    SaveBitmapLocalDisk();
                                    break;
                                }
                            }
                        }
                    }).start();
                }
                break;
        }


    }

    /**
     * 간편결제의 경우 이쪽으로 진행한다
     */
    private void EasyinitSetting()
    {

        /* 장치 정보를 읽어서 설정 하는 함수         */
        String deviceType = Setting.getPreference(this,Constants.APPLICATION_PAYMENT_DEVICE_TYPE);
        if (deviceType.isEmpty() || deviceType == ""){      //처음에 설정이 안되어 있는 경우에는 값이 없거나 ""로 되어 있을 수 있다.
            Setting.g_PayDeviceType = Setting.PayDeviceType.NONE;
        }else
        {
            Setting.PayDeviceType _type = Enum.valueOf(Setting.PayDeviceType.class, deviceType);
            Setting.g_PayDeviceType = _type;
        }
        switch (Setting.g_PayDeviceType) {
            case NONE:
                //아무 장비도 연결되지 않았는데 여기로 올 일이 없다.
                return;
            case BLE:       //BLE의 경우
            case CAT:       //WIFI CAT의 경우
                //캣의 경우는 이곳에 올 일이 없다.
                TextView _signText = findViewById(R.id.textView);
                _signText.setText("서명을 해 주세요");
                _signView.setIsDrawable(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true)
                        {
                            try {
                                Thread.sleep(mSignDrawTimer);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if(isDisplaySignFinish)
                            {
                                SaveBitmap();
                                SaveBitmapLocalDisk();
                                break;
                            }
                        }
                    }
                }).start();

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(mSignTimer!=null){ mSignTimer.cancel();}
                        mSignTimer = null;
                        mSignTimer = new CountDownTimer(30000, 1000) {
                            @Override
                            public void onTick(long l) {
                                Log.d(TAG, "SignTimer =  " + l/1000 );
                            }

                            @Override
                            public void onFinish() {
                                _signView.setIsDrawable(false);
                                resultCancel();
                            }
                        };
                        mSignTimer.cancel();
                        mSignTimer.start();
                    }
                });
                return;

            case LINES:     //유선장치의 경우
                //아래쪽으로 그대로 진행한다.
                break;
            default:
                break;
        }

        //0=서명패드사용안함 1=사인패드 2=멀티사인패드 3=터치서명
        switch (Setting.getSignPadType(mKocesPosSdk.getActivity()))
        {
            case 1:
                if (mKocesPosSdk.getUsbDevice() != null) {
                    mKocesPosSdk.__PosInit("99", mDataListener, new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
                }
                _signView.setIsDrawable(true);
                _signView.setVisibility(View.GONE);
                _linear_view.setVisibility(View.INVISIBLE);
                mBtn_signpad_ok.setVisibility(View.GONE);
                TextView _signText1 = findViewById(R.id.textView);
                _signText1.setText("서명패드 입력 대기 중");

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(mSignTimer!=null){ mSignTimer.cancel();}
                        mSignTimer = null;
                        int _time = Integer.valueOf(Setting.getPreference(mKocesPosSdk.getActivity(),Constants.USB_TIME_OUT));
                        mSignTimer = new CountDownTimer(_time * 1000, 1000) {
                            @Override
                            public void onTick(long l) {
                                Log.d(TAG, "SignTimer =  " + l/1000 );
                            }

                            @Override
                            public void onFinish() {
                                _signView.setIsDrawable(false);
                                resultCancel();
                            }
                        };
                        mSignTimer.cancel();
                        mSignTimer.start();
                    }
                });
                break;
            case 2:
                if (mKocesPosSdk.getUsbDevice() != null) {
                    if(Setting.ICResponseDeviceType ==1)
                    {
                        mKocesPosSdk.__PosInit("99", null, new String[]{mKocesPosSdk.getMultiAddr()});
                        LASTSIGNCOMMAND = Command.ACK;
                        String msg1 = "금액: " + String.valueOf(mMoney) + "원";
                        String msg2 = "서명하세요";
                        mKocesPosSdk.__PadRequestSign("0", "0", Constants.WORKING_KEY, msg1, msg2, " ", " ", mDataListener, new String[]{mKocesPosSdk.getSignPadAddr(), mKocesPosSdk.getMultiAddr()});
                        StartSign = 0;
                    }
                    else if(Setting.ICResponseDeviceType ==3)
                    {
                        LASTSIGNCOMMAND = Command.ACK;
                        String msg1 = "금액: " + String.valueOf(mMoney) + "원";
                        String msg2 = "서명하세요";
                        mKocesPosSdk.__PadRequestSign("0", "0", Constants.WORKING_KEY, msg1, msg2, " ", " ", mDataListener, new String[]{mKocesPosSdk.getSignPadAddr(), mKocesPosSdk.getMultiAddr()});
                        StartSign = 0;
                    }
                }
                _signView.setIsDrawable(true);
                _signView.setVisibility(View.GONE);
                _linear_view.setVisibility(View.INVISIBLE);
                mBtn_signpad_ok.setVisibility(View.GONE);
                TextView _signText2 = findViewById(R.id.textView);
                _signText2.setText("서명패드 입력 대기 중");

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(mSignTimer!=null){ mSignTimer.cancel();}
                        mSignTimer = null;
                        int _time = Integer.valueOf(Setting.getPreference(mKocesPosSdk.getActivity(),Constants.USB_TIME_OUT));
                        mSignTimer = new CountDownTimer(_time * 1000, 1000) {
                            @Override
                            public void onTick(long l) {
                                Log.d(TAG, "SignTimer =  " + l/1000 );
                            }

                            @Override
                            public void onFinish() {
                                _signView.setIsDrawable(false);
                                resultCancel();
                            }
                        };
                        mSignTimer.cancel();
                        mSignTimer.start();
                    }
                });
                break;
            case 0:
            case 3:
                if(Setting.getICReaderType(mKocesPosSdk.getActivity()) == 1)
                {
                    if (mKocesPosSdk.getUsbDevice() != null) {
                        LASTSIGNCOMMAND = Command.ACK;
                        String msg1 = "금액: " + String.valueOf(mMoney) + "원";
                        String msg2 = "서명하세요";
                        mKocesPosSdk.__PadRequestSign("0", "0", Constants.WORKING_KEY, msg1, msg2, " ", " ", mDataListener, mKocesPosSdk.getMultiReaderAddr2());
                        StartSign = 0;
                    }
                    _signView.setIsDrawable(true);
                    _signView.setVisibility(View.GONE);
                    _linear_view.setVisibility(View.INVISIBLE);
                }
                else
                {
                    TextView _signText = findViewById(R.id.textView);
                    _signText.setText("서명을 해 주세요");
                    _signView.setIsDrawable(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true)
                            {
                                try {
                                    Thread.sleep(mSignDrawTimer);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(isDisplaySignFinish)
                                {
                                    SaveBitmap();
                                    SaveBitmapLocalDisk();
                                    break;
                                }
                            }
                        }
                    }).start();
                }
                break;
        }


    }

    /**
     * BLE의 경우 이쪽으로 진행한다.
     */
    private void BleinitSetting()
    {
        // Gets the layout params that will allow you to resize the layout
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) _linear_signpad.getLayoutParams();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        String _size = Setting.getPreference(this,Constants.SIGNPAD_SIZE);
        float _padSize = 1f;
        switch (_size)
        {
            case "전체":
                _padSize = 1f;
                break;
            case "보통":
                _padSize = 1.5f;
                break;
            case "작게":
                _padSize = 2f;
                break;
        }
        int widthPixels = (int)(metrics.widthPixels / _padSize);
        int heightPixels = (int)(metrics.heightPixels / _padSize);
        params.width = widthPixels;
        params.height = heightPixels;
        _linear_signpad.setLayoutParams(params);

        TextView _signText = findViewById(R.id.textView);
        _signText.setText("서명을 해 주세요");
        _signView.setIsDrawable(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                {

                    try {
                        Thread.sleep(mSignDrawTimer);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(isDisplaySignFinish)
                    {
                        SaveBitmap();
                        SaveBitmapLocalDisk();
                        break;
                    }
                }
            }
        }).start();

//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                if(mSignTimer!=null){ mSignTimer.cancel();}
//                mSignTimer = null;
//                mSignTimer = new CountDownTimer(30000, 1000) {
//                    @Override
//                    public void onTick(long l) {
//                        Log.d(TAG, "SignTimer =  " + l/1000 );
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        _signView.setIsDrawable(false);
//                        resultCancel();
//                    }
//                };
//                mSignTimer.cancel();
//                mSignTimer.start();
//            }
//        });
    }

    /** (터치서명)일 경우 이미지를 비트맵으로 저장했기에 이를 모노크롬으로 변환하여 다시 바이트배열로 변환하여 저장한다 */
    private void SaveBitmapLocalDisk()
    {
        byte[] byArrayBitmapData = null;

        ConvertBitmapToMonchromeBitmap corvertBitmap = new ConvertBitmapToMonchromeBitmap();
        byArrayBitmapData = corvertBitmap.convertBitmap(mBMP);

        //'1' : KOCES , 'K' : K2VAN, 'B' : BMP String
        // 4:서명패드 미설치, 5:NoCVM(5만원이하), 6:전자서명사용 (주11)
        Setting.g_sDigSignInfo ="B";    //전자서명 사용여부

        resultOk(byArrayBitmapData,"");
    }

    /** 화면에 그려진 사인을 비트맵으로 저장한다 */
    private void SaveBitmap()
    {
        mBMP = _signView.saveImage();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        Rect dialogBounds = new Rect();
        getWindow().getDecorView().getHitRect(dialogBounds);
        if(!dialogBounds.contains((int)ev.getX(),(int)ev.getY()))
        {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    Thread CheckFinishSign = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true)
            {
                setfinishSign(true);
                try {
                    Thread.sleep(mSignSerialTimer);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(getfinishSign())
                {
                    //Log.d(TAG,"사인 종료");
                    if(Setting.getICReaderType(mKocesPosSdk.getActivity()) == 1)    //0=카드리더기 1=멀티패드리더기
                    {
                        mKocesPosSdk.__SignDataRequest(mDataListener,new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiReaderAddr()});
                    }
                    else
                    {
                        mKocesPosSdk.__SignDataRequest(mDataListener,new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
                    }

                    break;
                }
            }
        }
    });

    /**
     * (터치서명제외) 사인이 끝났는지 아닌지를 셋팅한다
     * @param _bSign
     */
    private synchronized void setfinishSign(boolean _bSign)
    {
        bfinishsign = _bSign;
    }

    /** (터치서명제외) 사인이 끝났는지를 확인한다 */
    private synchronized boolean getfinishSign()
    {
        return bfinishsign;
    }

    SerialInterface.DataListener mDataListener = new SerialInterface.DataListener() {
        @Override
        public void onReceived(byte[] _rev, int _type) {
            if(_type==0)
            {
                final byte[] b = _rev;
                if(LASTSIGNCOMMAND==b[3])
                {
                    return;
                }
                switch (b[3])
                {
                    case Command.ACK:
                        LASTSIGNCOMMAND = Command.ACK;
                        String msg1 =  "금액: " + String.valueOf(mMoney) + "원";
                        String msg2 =  "서명하세요";
                        mKocesPosSdk.__PadRequestSign("0","0", Constants.WORKING_KEY,msg1,msg2," "," ",mDataListener,new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
                        StartSign = 0;
                        break;
                    case Command.CMD_SIGN_SEND_RES:
//                        _signView.UpdateDrawPt(0,0);
//                        SaveBitmap();
                        if(Setting.getICReaderType(mKocesPosSdk.getActivity()) == 1)    //0=카드리더기 1=멀티패드리더기
                        {
                            mKocesPosSdk.__SendEOT(new String[]{mKocesPosSdk.getMultiReaderAddr()});
                        }
                        else
                        {
                            mKocesPosSdk.__SendEOT(new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
                        }
                        SignDataProc(b);
                        break;
                    case Command.CMD_SIGN_CANCEL_RES:
                        finishSign(b);
                        break;
                    default:
                        break;
                }
            }
            else
            {
                if(_rev[2]==(byte)0xEF && _rev[3]==(byte)0xEF)
                {
                    //resultOk(null);
                }
                else
                {
                    StartSign++;
                    if(StartSign==2)
                    {
                        //Log.d(TAG,"사인 시작" + String.valueOf(StartSign));
                        mBtn_signpad_ok.setVisibility(View.VISIBLE);
                        CheckFinishSign.start();
                    }
                    //recevied_signDataDrawPoint(_rev[2],_rev[3]);
//                    recevied_signData(_rev[2],_rev[3]);
                    setfinishSign(false);
                }
            }
        }
    };

    /**
     * 현재 사용하지 않음
     * @param _x
     * @param _y
     */
    private void recevied_signDataDrawPoint(byte _x,byte _y)
    {
        //최초로 받을때 터치스타트, 그다음부터는 터치무브로 이동
        //받는데이터가 없는경우 터치업 호출
        //터치업호출한 이후에 다시 받을경우 터치스타트부터 다시시작
        _signView.UpdateDrawPt(((float)(_x & 0xff)*3.0f),((float)(_y & 0xff)*3.0f));
    }

    /**
     * 장비를 통해 x,y 좌표를 받아와서 이를 SignPad 클래스로 보낸다
     * @param _x 좌표x
     * @param _y 좌표y
     */
    private void recevied_signData(byte _x,byte _y)
    {
        //최초로 받을때 터치스타트, 그다음부터는 터치무브로 이동
        //받는데이터가 없는경우 터치업 호출
        //터치업호출한 이후에 다시 받을경우 터치스타트부터 다시시작

        if(_first == false) {
            TextView _signText = findViewById(R.id.textView);
            _signText.setText("고객 서명 중입니다");
            _signView.touchStart(((float) (_x & 0xff)*3.5f), ((float) (_y & 0xff))*3.5f);
            _first = true;
        }
        else{
            _signView.touchMove(((float)(_x & 0xff)*3.5f),((float)(_y & 0xff)*3.5f));
        }
    }

    /**
     * 사인데이터 종료시 불러온다. 해당 데이터를 결과값으로 외부엑티비티(앱투앱or페이먼트)로 보낸다
     * @param _b
     */
    private void SignDataProc(byte[] _b)
    {
        //만약 서명데이터가 최대 사이즈(1536바이트)를 넘을 경우에는 CAT으로 NAK(0x15) 전송하고,
        //CAT은 해당거래를 종료 처리함
        KByteArray b = new KByteArray(_b);
        b.CutToSize(4);
        String codeAndVersion = "";
        try {
            codeAndVersion = Utils.getByteToString_euc_kr(b.CutToSize(16));
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        int SingDataSize = Utils.byteToInt(b.CutToSize(2));
        byte[] data = new byte[SingDataSize];
        data = b.CutToSize(SingDataSize);
//        System.arraycopy(b.CutToSize(SingDataSize),0,data,0,SingDataSize);
        b.CutToSize(1);
        String HashCode = String.valueOf(b.CutToSize(b.getlength()-2));
        if(HashCode==null)
        {

        }
        if(SingDataSize > 1536 || SingDataSize<10)
        {
            if(Setting.getICReaderType(mKocesPosSdk.getActivity()) == 1)    //0=카드리더기 1=멀티패드리더기
            {
                mKocesPosSdk.__SendNak(new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiReaderAddr()});
            }
            else
            {
                mKocesPosSdk.__SendNak(new String[]{mKocesPosSdk.getSignPadAddr(),mKocesPosSdk.getMultiAddr()});
            }

            resultCancel();
            return;
        }
        resultOk(data,codeAndVersion);
    }

    /**
     * 장치에서 사인데이터가 취소되었을 경우 종료한다
     * @param _b
     */
    private void finishSign(byte[] _b)
    {
        if(_b[3]==Command.CMD_SIGN_CANCEL_RES)  //서명 종료 응답 0xBC
        {

        }
        resultCancel();
    }

    /**
     * 사인데이터가 정상처리 되어 사인이미지가 만들어졌을 경우 처리한다
     * @param _signData 사인데이터값(byte)
     * @param codeNVersion (받아오지만 사용되지는 않음)
     */
    private void resultOk(byte[] _signData,String codeNVersion)
    {
        if(_signpadCount)
        {
            return;
        }
        _signpadCount = true;
        if(mSignTimer != null)
        {
            mSignTimer.cancel();
        }
        //TODO 사인데이터 파일로 저장
        //WriteSignFile(_signData);

        Intent intent = new Intent();
        intent.putExtra("signresult", "OK");
        intent.putExtra("signdata", _signData);
        intent.putExtra("codeNver", codeNVersion);
        setResult(RESULT_OK, intent);

        new Handler(Looper.getMainLooper()).postDelayed(()->{
            finishAndRemoveTask();
        },700);

    }

    /** 서명 요청이 취소된 경우. ex)장치에서 취소, 유저가 직접 취소버튼클릭, 시간 내에 그리지 않음 등 */
    private void resultCancel()
    {
        if(_signpadCount)
        {
            return;
        }
        _signpadCount = true;
        if(mSignTimer != null)
        {
            mSignTimer.cancel();
        }
        //서명 종료 요청
        Intent intent = new Intent();
        intent.putExtra("signresult", "CANCEL");

        setResult(RESULT_CANCELED, intent);
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            finishAndRemoveTask();
        },700);
    }

}
