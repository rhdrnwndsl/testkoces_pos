package com.koces.androidpos.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.koces.androidpos.R;
import com.koces.androidpos.menu2Activity;
import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.DeviceSecuritySDK;
import com.koces.androidpos.sdk.Devices.AutoDetectDeviceInterface;
import com.koces.androidpos.sdk.Devices.AutoDetectDevices;
import com.koces.androidpos.sdk.Devices.Devices;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.SerialPort.ConnectFTP;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.ble.bleSdk;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.TcpInterface;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

/**
 * 환경 설정에서 장치 설정을 위한 클래스
 */
public class frag_device_line extends Fragment {
    /** Activity 의 onCreate() 의 setContentView()와 같은 관련 UI(xml) 를 화면에 구성하는 View */
    View view;
    /** menu2Activity.java 에서 가맹점설정 장치설정 관리자설정 무결성검증 등의 Fragment 가 속해있다 */
    menu2Activity m_menuActivity;
    //private Activity activity;
    /** 사용하지않음 */
    String[] SerialDeviceBusAddress;
    /** 앱 전체에 관련 권한 및 기능을 수행 하는 핵심 클래스로 장치설정을 불러오거나 저장하기 위해 사용한다 */
    KocesPosSdk mPosSdk;
    /** 화면에 구성되는 버튼 */
    Button m_btn_sign_connect_check,
            m_btn_cardreader_connect_check,
            m_btn_key_reset,
            m_btn_classfi_check,
            m_btn_upgrade_reader,
            m_btn_save,m_btn_autodetecting,
            m_btn_rescan_device;
    /** 제품 식별번호, 시리얼번호, 버전 */
    TextView m_txt_line_tvw_product,m_txt_line_tvw_serial, m_txt_line_tvw_version;

    ProgressDialog progressDoalog;

    /** 사용하지않음 카드리더기연결성공체크 */
    boolean mbReaderConnSuccess = false;
    /** 사용하지않음 사인패드연결성공체크 */
    boolean mbPadConnSuccess = false;
    /** 화면에 나타나는 여러 메뉴(사인패드구성메뉴, 카드리더기구성메뉴 등) */
    Spinner m_cbx_trade_option,m_cbx_card_reader,m_cbx_multi_signPad,m_cbx_signPad_size,m_cbx_signPad_type,m_cbx_signPad,m_cbx_card_reader_address;

    /** QR 읽는 장치 설정 카메라, 카드리더, 사인패드 */
    Spinner m_cbx_qr_reader;
    String mQrReaderType = "";

    /** 터치서명 사이즈 크기 */
    Spinner m_cbx_size_touchSignpad;
    String mTouchSignPadSize = "";

    /** linearlayoutGroup 장치에 따라서 세부 설정 여부를 설정하는 그룹을 보이기 또는 숨기 */
    LinearLayout m_llayout_line_group,m_llayout_ble_group,m_llayout_cat_group;

    /** usb 장치 타임아웃시간설정 */
    EditText m_edt_usb_timeout;

    byte[] multipadAuthNum = null;
    byte[] multipadSerialNum = null;
    String mDeviceVersion = "";
    String type = "",tid = "",version = "",serialNum = "",Ans_Cd = "",message = "",Secu_Key = "",
            Token_cs_Path = "",Data_Cnt = "",Protocol = "",Addr = "",Port = "",User_ID = "",Password = "",
            Data_Type = "",Data_Desc = "",File_Path_Name = "",File_Size = "",Check_Sum_Type = "",
            File_Check_Sum = "",File_Encrypt_key = "",Response_Time = "";
    byte[] mAesKey = null;
    byte[] mEesKey = null;
    int mPercent = 0;
    Context mCtx;
    bleSdk mbleSdk;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frag_device_line, container, false);
        init();
        return view;
    }
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        m_menuActivity = (menu2Activity) getActivity();
        mCtx = context;
        if(context instanceof Activity)
        {
            //activity = (Activity)context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        m_menuActivity = null;
        mPosSdk = null;
        mbleSdk = null;
    }

    /**
     * 변수 및 xml ui 초기화 함수
     */
    private void init()
    {
        mPosSdk = KocesPosSdk.getInstance();
        mbleSdk = bleSdk.getInstance();
        m_btn_rescan_device = (Button)view.findViewById(R.id.btn_f_recan_device);
        m_btn_sign_connect_check = (Button)view.findViewById(R.id.btn_f_connect_pad);
        m_btn_cardreader_connect_check =(Button)view.findViewById(R.id.btn_f_connect_ic);
        m_btn_key_reset = (Button)view.findViewById(R.id.btn_f_keyupdate);
        m_btn_classfi_check =(Button)view.findViewById(R.id.btn_f_classfi_check);
        //인증을 위해서 버튼을 숨긴다.
        m_btn_upgrade_reader = (Button)view.findViewById(R.id.btn_f_upgrade_reader);
        m_btn_save =(Button)view.findViewById(R.id.btn_f_device_save);

        //식별번호,시리얼번호,버전정보
        m_txt_line_tvw_product = (TextView)view.findViewById(R.id.frgline_tvw_product);
        m_txt_line_tvw_serial = (TextView)view.findViewById(R.id.frgline_tvw_serial);
        m_txt_line_tvw_version = (TextView)view.findViewById(R.id.frgline_tvw_ver);
//        m_txt_reader = (TextView)view.findViewById(R.id.txt_ICType);
//        m_txt_reader.setText("");

        //usb 타임아웃
        m_edt_usb_timeout = (EditText)view.findViewById(R.id.edit_usbTimeout);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(Setting.getPreference(mPosSdk.getActivity(),Constants.USB_TIME_OUT).equals(""))
                {
                    Setting.setPreference(mPosSdk.getActivity(),Constants.USB_TIME_OUT,"60");
                }
                m_edt_usb_timeout.setText(Setting.getPreference(mPosSdk.getActivity(),Constants.USB_TIME_OUT));
            }
        },200);

        //spinner
//        m_cbx_trade_option = (Spinner)view.findViewById(R.id.spinner_f_trade_option);    //거래 방식 선택
        m_cbx_card_reader = (Spinner)view.findViewById(R.id.spinner_f_ICType);   //카드 리더스 종료 선택
        m_cbx_signPad_type = (Spinner)view.findViewById(R.id.spinner_f_padType); //사인 패드 종류 선택

        m_cbx_signPad = (Spinner)view.findViewById(R.id.spinner_f_signpad);     //사인패드 연결할 디바이스 이름
        m_cbx_card_reader_address = (Spinner)view.findViewById(R.id.spinner_f_card_reader); //카드리더기 연결할 디바이스 이름

        m_llayout_line_group = (LinearLayout)view.findViewById(R.id.frg_dev_line_opt_group);
        //인증을 위해서 이 부분 주석 처리
        //m_cbx_multi_signPad = (Spinner)findViewById(R.id.spinner_multipad);
        //m_cbx_signPad_size = (Spinner)findViewById(R.id.spinner_padsize);

//        String[] SerialDeviceBusAddress = mPosSdk.getSerialDevicesBusList();
//
//        if(SerialDeviceBusAddress!=null) {
//            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
//                    m_menuActivity,
//                    R.layout.support_simple_spinner_dropdown_item,
//                    SerialDeviceBusAddress);
//            m_cbx_signPad.setAdapter(adapter);
//            m_cbx_card_reader_address.setAdapter(adapter);
//        }

        m_btn_sign_connect_check.setOnClickListener(BtnOnClickListener);
        m_btn_cardreader_connect_check.setOnClickListener(BtnOnClickListener);
        m_btn_key_reset.setOnClickListener(BtnOnClickListener);
        m_btn_classfi_check.setOnClickListener(BtnOnClickListener);
        m_btn_rescan_device.setOnClickListener(BtnOnClickListener);
        m_btn_upgrade_reader.setOnClickListener(BtnOnClickListener);
        m_btn_save.setOnClickListener(BtnOnClickListener);

        /* 현재 연결되어 있는 장치목록을 메뉴리스트로 불러온다 */
        if(mPosSdk.mDevicesList != null)
        {
            String[] deviceName = new String[mPosSdk.mDevicesList.size()+1];
            int mCount = 0;
            deviceName[0] = "";
            mCount++;
            for (Devices n: mPosSdk.mDevicesList) {
                deviceName[mCount] = n.getDeviceSerial();
                mCount++;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    m_menuActivity,
                    R.layout.spinner_item,
                    deviceName);
            m_cbx_signPad.setAdapter(adapter);
            m_cbx_card_reader_address.setAdapter(adapter);
        }

        //저장 정보 가져 와서 세팅 하기
        //거래방식 선택 - 카드리더기 현재 한개
        String SelectedtradeOption = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_TRADE_OPTION);
//        m_cbx_trade_option.setSelection(getIndex(m_cbx_trade_option,SelectedtradeOption));
        //카드리더기 선택 - 카드리더기, 멀티리더기
        String SelectedCardReaderOption = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_CARD_READER_OPTION);
        m_cbx_card_reader.setSelection(getIndex(m_cbx_card_reader,SelectedCardReaderOption));
        //서명패드 선택 - 선택안함, 서명패드, 멀티패드, 터치서명
        String SelectedSignPadOption = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_SIGN_PAD_OPTION);
        m_cbx_signPad_type.setSelection(getIndex(m_cbx_signPad_type,SelectedSignPadOption));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /* 현재 설정되어 있는 카드리더기(카드리더기OR멀티리더기)의 장치이름 */
                String SelectedCardReader= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL);
                String SelectedMultiReader= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL);
                if(SelectedCardReaderOption.equals("카드리더기"))
                {
                    m_cbx_card_reader_address.setSelection(getIndex(m_cbx_card_reader_address,SelectedCardReader));
//                    m_txt_reader.setText(Setting.mAuthNum);
                }
                else if(SelectedCardReaderOption.equals("멀티패드"))
                {
                    m_cbx_card_reader_address.setSelection(getIndex(m_cbx_card_reader_address,SelectedMultiReader));
//                    m_txt_reader.setText(Setting.mAuthNum);
                }

                if(mPosSdk.mDevicesList != null)
                {
                    String[] deviceName = new String[mPosSdk.mDevicesList.size()+1];
                    int mCount = 0;
                    deviceName[0] = "";
                    mCount++;
                    for (Devices n: mPosSdk.mDevicesList) {
                        deviceName[mCount] = n.getDeviceSerial();
                        if (n.getmType() == 1) {
                            if(n.getName() != null && n.getName().length()>16)
                            {
                                m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                            } else {
                                m_txt_line_tvw_product.setText(n.getName());
                            }

                            m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                            m_txt_line_tvw_version.setText(n.getVersion());
//                            m_txt_reader.setText(n.getName());
                        } else if(n.getmType() == 4) {
                            if(n.getName() != null && n.getName().length()>16)
                            {
                                m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                            } else {
                                m_txt_line_tvw_product.setText(n.getName());
                            }
                            m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                            m_txt_line_tvw_version.setText(n.getVersion());
//                            m_txt_reader.setText(n.getName());
                        } else if (n.getmType() == 0) {
//                            m_txt_reader.setText(n.getName());
                        }

                        mCount++;
                    }
                }

                /* 현재 설정되어 있는 서명패드(서명패드OR멀티서명패드)의 장치이름름 */
                String SelectedSignPad= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD_SERIAL);
                String SelectedMultiPad= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL);
                if(SelectedSignPadOption.equals("서명패드"))
                {
                    m_cbx_signPad.setSelection(getIndex(m_cbx_signPad,SelectedSignPad));
                }
                else if(SelectedSignPadOption.equals("멀티서명패드"))
                {
                    m_cbx_signPad.setSelection(getIndex(m_cbx_signPad,SelectedMultiPad));
                }
            }
        },200);


        /* 카드리더기메뉴선택 - 카드리더기 멀티패드(멀티카드리더기) */
        m_cbx_card_reader.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
 //               ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(view!=null) {
                    ((TextView) parent.getChildAt(0)).setTextSize(14);
                }
                Setting.setICReaderType(mPosSdk.getActivity(), position);
                setCardSignSelectedItemSwap(1,position);
                if(position==1)//멀티패드
                {
                    //카드리더기 => 멀티패드 선택시 사인패드 자동으로 사용안함 설정
                    Setting.setSignPadType(mPosSdk.getActivity(), 0);
                    m_cbx_signPad_type.setSelection(0);
                    m_cbx_signPad_type.setEnabled(false);
                    //서명패드 사용안함, 화면 터치 서명인 경우에는 시리얼을 표시 하지 않는다.
                    m_cbx_signPad.setVisibility(View.INVISIBLE);
                }
                else
                {
                    m_cbx_signPad_type.setEnabled(true);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        /* 거래방식메뉴선택 - 현재카드리더기1개 */
//        m_cbx_trade_option.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
////                ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
//                if(view!=null) {
//                    ((TextView) parent.getChildAt(0)).setTextSize(14);
//                }
//
//                setCardSignSelectedItemSwap(1,position);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });

        /* 서명패드메뉴선택 - 사용안함 서명패드 멀티서명패드 터치서명 */
        m_cbx_signPad_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //        ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(view!=null) {
                    ((TextView) parent.getChildAt(0)).setTextSize(14);
                }
                Setting.setSignPadType(mPosSdk.getActivity(), position);
                //서명패드 사용안함, 화면 터치 서명인 경우에는 시리얼을 표시 하지 않는다.
                if(position==0 || position==3){
                    m_cbx_signPad.setVisibility(View.INVISIBLE);
                }
                else
                {
                    m_cbx_signPad.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

//        m_btn_autodetecting = (Button)view.findViewById(R.id.btn_autodetect_device);        //장치 재검색 버튼
//        m_btn_autodetecting.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });

        /** qr 리더 설정 */
        m_cbx_qr_reader = view.findViewById(R.id.spinner_f_qrReader);
        mQrReaderType = Setting.getPreference(mPosSdk.getActivity(), Constants.LINE_QR_READER);
        m_cbx_qr_reader.setSelection(getIndex(m_cbx_qr_reader,mQrReaderType));
        m_cbx_qr_reader.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //               ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(view!=null) {
                    ((TextView) parent.getChildAt(0)).setTextSize(14);
                }
                switch (position)
                {
                    case 0://카메라
                        mQrReaderType = Constants.LineQrReader.Camera.toString();
                        break;
                    case 1://카드리더기
                        mQrReaderType = Constants.LineQrReader.CardReader.toString();
                        break;
                    case 2://사인패드
                        mQrReaderType = Constants.LineQrReader.SignPad.toString();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        /** 터치서명 사인패드 크기 설정 */
        m_cbx_size_touchSignpad = view.findViewById(R.id.spinner_f_size_signpad);
        mTouchSignPadSize = Setting.getPreference(mPosSdk.getActivity(), Constants.SIGNPAD_SIZE);
        m_cbx_size_touchSignpad.setSelection(getIndex(m_cbx_size_touchSignpad,mTouchSignPadSize));
        m_cbx_size_touchSignpad.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //               ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(view!=null) {
                    ((TextView) parent.getChildAt(0)).setTextSize(14);
                }
                switch (position)
                {
                    case 0://전체
                        mTouchSignPadSize = Constants.TouchSignPadSize.FullScreen.toString();
                        break;
                    case 1://보통
                        mTouchSignPadSize = Constants.TouchSignPadSize.Middle.toString();
                        break;
                    case 2://작게
                        mTouchSignPadSize = Constants.TouchSignPadSize.Small.toString();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * (사용하지 않음)<br>
     * 둘중에 한개의 spinner 값이 변화 되면 다른 spinner에는 다른 값이 설정 되게 처리 한다.
     * @param type 장치 종류
     * @param pos 위치
     */
    private void setCardSignSelectedItemSwap(int type,int pos)
    {
        //둘중에 한개의 spinner 값이 변화 되면 다른 spinner에는 다른 값이 설정 되게 처리 한다.

    }

    boolean mFirmwareOrReaderCheck = false; //리더기체크를 먼저해서 버전정보를 가져올 지 아니면 바로 펌웨어업데이트를 할지 체크
    //false = 리더기체크, true = 펌웨어업데이트
    Button.OnClickListener BtnOnClickListener = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.btn_f_connect_pad:
                    SignConnectCheck();
                    break;
                case R.id.btn_f_connect_ic:
                    CardConnectCheck();
                    break;
                case R.id.btn_f_keyupdate:
                    KeyReset();
                    break;
                case R.id.btn_f_classfi_check:
                    DeviceIntergrity();
                    break;
                case R.id.btn_f_recan_device:
                    DeviceReScan();
                    break;
                case R.id.btn_f_upgrade_reader:
                    if (mDeviceVersion.equals("")) {
                        mFirmwareOrReaderCheck = true;
                        CardConnectCheck();
                    } else {
                        mFirmwareOrReaderCheck = false;
                        UpgradeReader();
                    }


                    break;
                case R.id.btn_f_device_save:
                    Save();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 장치 재검색 함수
     */
    private void DeviceReScan()
    {
        /* 장치를 검색하여 현재 정상적으로 usb가 붙어있는 사용가능장비들을 체크한다 */
        final AutoDetectDevices autoDetectDevices = new AutoDetectDevices(m_menuActivity, new AutoDetectDeviceInterface.DataListener() {
                @Override
                public void onReceived(boolean State, int _DeviceCount, String _Evt) {
                    if(_DeviceCount>0)
                    {
                        String[] deviceName = new String[mPosSdk.mDevicesList.size()+1];
                        int mCount = 0;
                        deviceName[0] = "";
                        mCount++;
                        for (Devices n: mPosSdk.mDevicesList) {
                            deviceName[mCount] = n.getDeviceSerial();
                            mCount++;
                        }
                        if (deviceName.length > 1) {
                            if (deviceName[1].equals("")) {
                                ShowDialog("장치주소가 정상적이지 않습니다. 다시 검색해 주세요.");
                                mPosSdk.mSerial.Dispose();
                                mPosSdk.mSerial.init();
                                return;
                            }
                        } else {
                            ShowDialog("장치주소가 정상적이지 않습니다. 다시 검색해 주세요.");
                            mPosSdk.mSerial.Dispose();
                            mPosSdk.mSerial.init();
                            return;
                        }
                        /* 위의 설정된 장치리스트들의 이름을 spinner메뉴에 등록한다 */
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                m_menuActivity,
                                R.layout.spinner_item,
                                deviceName);
                        m_cbx_signPad.setAdapter(adapter);
                        m_cbx_card_reader_address.setAdapter(adapter);

                        String SelectedCardReaderOption = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_CARD_READER_OPTION);
                        String SelectedCardReader= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL);
                        String SelectedMultiReader= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL);
                        if(SelectedCardReaderOption.equals("카드리더기"))
                        {
                            m_cbx_card_reader_address.setSelection(getIndex(m_cbx_card_reader_address,SelectedCardReader));
//                            m_txt_reader.setText(Setting.mAuthNum);
                        }
                        else if(SelectedCardReaderOption.equals("멀티패드"))
                        {
                            m_cbx_card_reader_address.setSelection(getIndex(m_cbx_card_reader_address,SelectedMultiReader));
//                            m_txt_reader.setText(Setting.mAuthNum);
                        }

                        if(mPosSdk.mDevicesList != null)
                        {
                            String[] _deviceName = new String[mPosSdk.mDevicesList.size()+1];
                            int _mCount = 0;
                            _deviceName[0] = "";
                            _mCount++;
                            for (Devices n: mPosSdk.mDevicesList) {
                                _deviceName[_mCount] = n.getDeviceSerial();
                                if (n.getmType() == 1) {
                                    if(n.getName() != null && n.getName().length()>16)
                                    {
                                        m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                                    } else {
                                        m_txt_line_tvw_product.setText(n.getName());
                                    }
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if(n.getmType() == 4) {
                                    if(n.getName() != null && n.getName().length()>16)
                                    {
                                        m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                                    } else {
                                        m_txt_line_tvw_product.setText(n.getName());
                                    }
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if (n.getmType() == 0) {
//                                    m_txt_reader.setText(n.getName());
                                }

                                _mCount++;
                            }
                        }

                        String SelectedSignPad= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD_SERIAL);
                        String SelectedMultiPad= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL);

                        String SelectedSignPadOption = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_SIGN_PAD_OPTION);
                        if(SelectedSignPadOption.equals("서명패드"))
                        {
                            m_cbx_signPad.setSelection(getIndex(m_cbx_signPad,SelectedSignPad));
                        }
                        else if(SelectedSignPadOption.equals("멀티서명패드"))
                        {
                            m_cbx_signPad.setSelection(getIndex(m_cbx_signPad,SelectedMultiPad));
                        }
                    }
                }
            });
    }

    /**
     * 서명 패드 장치 연결 체크 함수
     */
    private void SignConnectCheck(){
        if(mPosSdk.getUsbDevice().size()!=0) {
//            mPosSdk.setSignPadAddress(m_cbx_signPad.getSelectedItem().toString());
            int _signPadType = Setting.getSignPadType(mPosSdk.getActivity());   /* 디바이스 설정에서 사인패드 콤보 박스에서 설정 값을 가져온다 */
            if(_signPadType!=Command.SIGNPAD_NONE && _signPadType!=Command.SIGNPAD_TOUCH && mPosSdk.mDevicesList!=null) {    //사용안함 = 0, 터치서명 =3
//                mPosSdk.setSignPadAddress(m_cbx_signPad.getSelectedItem().toString());
                if(m_cbx_signPad.getSelectedItem().toString().equals(""))
                {
                    ShowDialog("장치 설정 필요");
                    return;
                }
                for (Devices n: mPosSdk.mDevicesList)
                {
                    if(n.getDeviceSerial().equals(m_cbx_signPad.getSelectedItem().toString()))
                    {
                        if(_signPadType==Command.SIGNPAD_MULIT)
                        {
                            n.setmType(3);
                            n.setConnected(true);
                            mPosSdk.setMultiPadAddr(n.getmAddr());
                            break;
                        }
                        else if(_signPadType==Command.SIGNPAD_SIGN) {
                            n.setmType(2);
                            n.setConnected(true);
                            mPosSdk.setSignPadAddr(n.getmAddr());
                            break;
                        }

                    }
                }
                /* 위의 장치리스트에서 검색된 장비의 주소에 아래와 같은 내용의 메세지를 보낸다*/
                mPosSdk.__PadDisplayMessage("서명패드확인", "화면 글자 확인", "설정 완료", " ", "02",
                        mDataListener, new String[]{mPosSdk.getSignPadAddr(),mPosSdk.getMultiAddr()});
            }
            else
            {
                ShowDialog("장치의 시리얼 번호를 설정하거나 장치 재검색을 실행 하십시요");
                return;
            }
        }
        else
        {
            ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
            return;
        }
    }

    /**
     * IC Reader 장치 연결 확인 함수
     */
    private void CardConnectCheck()
    {
        if(mPosSdk.getUsbDevice().size()!=0 && mPosSdk.mDevicesList!=null )
        {
//          mLastCommnad = Command.CMD_INIT;
            //mPosSdk.setSignPadAddress(m_cbx_signPad.getSelectedItem().toString());
            if(m_cbx_card_reader_address.getSelectedItem()==null)
            {
                mFirmwareOrReaderCheck = false;
                ShowDialog("장치의 시리얼 번호를 설정하거나 장치 재검색을 실행 하십시요");
                return;
            }
            if(m_cbx_card_reader_address.getSelectedItem().toString().equals(""))
            {
                mFirmwareOrReaderCheck = false;
                ShowDialog("장치 설정 필요");
                return;
            }
            for (Devices n: mPosSdk.mDevicesList)
            {
                if(n.getDeviceSerial().equals(m_cbx_card_reader_address.getSelectedItem().toString()))
                {

                    if(m_cbx_card_reader.getSelectedItem().toString().equals("카드리더기")) {
                        mPosSdk.__PosInfo(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener() {
                            @Override
                            public void onReceived(byte[] _rev, int _type) {
                                if(_type==Command.PROTOCOL_TIMEOUT)
                                {
                                    mFirmwareOrReaderCheck = false;
                                    ShowDialog("장치 정보를 읽어오지 못했습니다.");
                                    return;
                                }
                                if(_rev[3]==(byte)0x15){
                                    //장비에서 NAK 올라 옮
                                    mFirmwareOrReaderCheck = false;
                                    ShowDialog("장치 NAK");
                                    return;
                                }
                                if(Setting.ICResponseDeviceType == 3)
                                {
                                    mFirmwareOrReaderCheck = false;
                                    ShowDialog("장치 오류");
                                    return;
                                }
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
                                mDeviceVersion = version;
                                String key = new String(KByteArray.CutToSize(2));
                                Setting.mLineHScrKeyYn = key;

                                Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                                //공백을 제거하여 추가 한다.
                                String tmp = authNum.trim();
                                Setting.mAuthNum = authNum.trim();
                                n.setmType(1);
                                n.setConnected(true);
                                n.setName(authNum);
                                n.setVersion(version);
                                n.setDeviceName(serialNum);
                                if (n.getmType() == 1) {
                                    if(n.getName() != null && n.getName().length()>16)
                                    {
                                        m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                                    } else {
                                        m_txt_line_tvw_product.setText(n.getName());
                                    }
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if(n.getmType() == 4) {
                                    if(n.getName() != null && n.getName().length()>16)
                                    {
                                        m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                                    } else {
                                        m_txt_line_tvw_product.setText(n.getName());
                                    }
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if (n.getmType() == 0) {
//                                    m_txt_reader.setText(n.getName());
                                }

                                mPosSdk.setICReaderAddr(n.getmAddr());
                                ShowDialog("연결에 성공하였습니다");
                                if(mFirmwareOrReaderCheck) {
                                    mFirmwareOrReaderCheck = false;
                                    UpgradeReader();
                                }
                            }
                        },new String[]{n.getmAddr()});
//                        mPosSdk.__CardStatusCheck( mDataListener,mPosSdk.getICReaderAddr2());   /* 카드리더기로 설정할 건지 멀티패드로 설정할건지를 정하여 셋팅한다 */
                    }else{

//                        mPosSdk.__CardStatusCheck( mDataListener,mPosSdk.getMultiReaderAddr2());    /* 카드리더기로 설정할 건지 멀티패드로 설정할건지를 정하여 셋팅한다 */
                        mPosSdk.__PosInfo(Utils.getDate("yyyyMMddHHmmss"), new SerialInterface.DataListener() {
                            @Override
                            public void onReceived(byte[] _rev, int _type) {
                                if(_type==Command.PROTOCOL_TIMEOUT)
                                {
                                    mFirmwareOrReaderCheck = false;
                                    return;
                                }
                                if(_rev[3]==(byte)0x15){
                                    //장비에서 NAK 올라 옮
                                    mFirmwareOrReaderCheck = false;
                                    return;
                                }
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
                                mDeviceVersion = version;
                                String key = new String(KByteArray.CutToSize(2));
                                Setting.mLineHScrKeyYn = key;

                                Setting.setPreference(mPosSdk.getActivity(),Constants.REGIST_DEVICE_SN,serialNum);
                                //공백을 제거하여 추가 한다.
                                String tmp = authNum.trim();
                                Setting.mAuthNum = authNum.trim();
                                n.setmType(4);
                                n.setName(authNum);
                                n.setVersion(version);
                                n.setDeviceName(serialNum);
                                n.setConnected(true);
                                if (n.getmType() == 1) {
                                    if(n.getName() != null && n.getName().length()>16)
                                    {
                                        m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                                    } else {
                                        m_txt_line_tvw_product.setText(n.getName());
                                    }
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if(n.getmType() == 4) {
                                    if(n.getName() != null && n.getName().length()>16)
                                    {
                                        m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                                    } else {
                                        m_txt_line_tvw_product.setText(n.getName());
                                    }
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if (n.getmType() == 0) {
//                                    m_txt_reader.setText(n.getName());
                                }
                                mPosSdk.setMultiReaderAddr(n.getmAddr());
                                ShowDialog("연결에 성공하였습니다");
                                if(mFirmwareOrReaderCheck) {
                                    mFirmwareOrReaderCheck = false;
                                    UpgradeReader();
                                }
                            }
                        },new String[]{n.getmAddr()});
                    }

                }
            }

        }
        else
        {
            mFirmwareOrReaderCheck = false;
            ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
            return;
        }
    }


    //private method of your class
    private int getIndex(Spinner spinner, String myString){
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                return i;
            }
        }
        return 0;
    }

    /**
     * 키 갱신 함수
     */
    private void KeyReset()
    {
//        if(getTID().equals(""))
//        {
//            Toast.makeText(mPosSdk.getActivity(), getResources().getString(R.string.error_no_data_tid), Toast.LENGTH_SHORT).show();
//            return;
//        }
        if(mPosSdk.mDevicesList == null || mPosSdk.mDevicesList.size()<1)
        {
            Toast.makeText(mPosSdk.getActivity(), getResources().getString(R.string.error_no_device), Toast.LENGTH_SHORT).show();
            return;
        }
        else if(m_txt_line_tvw_product.getText() == null || m_txt_line_tvw_product.getText().toString().equals(""))
        {
            Toast.makeText(mPosSdk.getActivity(), getResources().getString(R.string.error_no_device), Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            m_menuActivity.ReadyDialogShow(m_menuActivity, "키 갱신 중입니다", 0);
            if (m_txt_line_tvw_product.getText().toString().equals("################"))
            {
                DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(m_menuActivity, 1, "", mKeyUpdateListener); //초기화
                deviceSecuritySDK.Req_SecurityKeyUpdate(m_txt_line_tvw_product.getText().toString());  //보안 키 업데이트 요청
            } else {
                DeviceSecuritySDK deviceSecuritySDK = new DeviceSecuritySDK(m_menuActivity, 1, "", mKeyUpdateListener); //초기화
                deviceSecuritySDK.Req_SecurityKeyUpdate();  //보안 키 업데이트 요청
            }
        }
    }

    /**
     * 장치 무결성 검사 함수 <br>
     * 장치 무결성 검사 페이지로 이동
     */
    private void DeviceIntergrity(){
        Save();
        m_menuActivity.setFrag(4);
    }

    /**
     * 장치 펌웨어 업데이트 함수<br>
     * (미구현)
     */
    private void UpgradeReader()
    {
        ShowProgressBar();
//        m_menuActivity.ReadyDialogShow(m_menuActivity, "펌웨어 업데이트 중", 0);
        mPosSdk.__PosInit("99", null,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /* type = 0001:최신펌웨어, 0003:EMV Key*/
                mPosSdk.__authenticatoin_req("0001",new SerialInterface.DataListener() {
                    @Override
                    public void onReceived(byte[] _rev, int _type) {
                        Log.d("TAG", String.valueOf(_rev));
                        Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(_rev);

                        String mLog = "데이터 : " + Utils.bytesToHex_0xType(_rev);
                        mPosSdk.cout("CARD_READER_REC : 상호인증응답 0x53",Utils.getDate("yyyyMMddHHmmss"),mLog);

                        switch (protocolInfo.Command) {
                            case Command.ACK:
                                Log.d("TAG", String.valueOf(_rev));
                                break;
                            case Command.NAK:
//                                m_menuActivity.ReadyDialogHide();
                                progressDoalog.dismiss();
                                ShowDialog("상호인증 요청 실패");
                                Log.d("TAG", String.valueOf(_rev));
                                break;
                            case Command.CMD_MUTUAL_AUTHENTICATION_RES:
                                if(_rev.length < 220) {
                                    progressDoalog.dismiss();
                                    ShowDialog("업데이트 불가 데이터 길이이상");
//                                    Log.d("TAG", String.valueOf(_rev));
                                    return;
                                }
                                KByteArray bArray = new KByteArray(_rev);
                                bArray.CutToSize(1);    //STX + 길이 + CommandID 삭제
                                String length = new String(bArray.CutToSize(2));   //길이
                                bArray.CutToSize(1);    //CommandID
                                multipadAuthNum = bArray.CutToSize(32);  //멀티패드인증번호
                                multipadSerialNum = bArray.CutToSize(10);  //멀티패드시리얼번호
                                String revDataType = new String(bArray.CutToSize(4));  //요청데이타구분    0001:최신펌웨어, 0003:EMV Key
                                final byte[] data = bArray.CutToSize(90);  //보안키
                                final byte[] tmp = bArray.CutToSize(38);  //보안키에서 뒤에 버리는 부분
                                String emvKey = new String(bArray.CutToSize(10));  //EMV Key
                                byte[] tmsUpdateCheck = bArray.CutToSize(2);  //TMS 업데이트 가능 여부
                                if (tmsUpdateCheck[0] == (byte)0x30 && tmsUpdateCheck[1] == (byte)0x30) {

                                } else if (tmsUpdateCheck[0] == (byte)0x00 && tmsUpdateCheck[1] == (byte)0x00) {

                                } else if (tmsUpdateCheck[0] == (byte)0x20 && tmsUpdateCheck[1] == (byte)0x20) {

                                }  else {
                                    //                                    m_menuActivity.ReadyDialogHide();
                                    progressDoalog.dismiss();
                                    ShowDialog("업데이트 불가 제품입니다");
                                    return;
                                }
//                                String filler = new String(bArray.CutToSize(38));  //Filler
//                                bArray.CutToSize(2);    //ETX, LRC
                                TMS_Data_Down_Info(revDataType,multipadSerialNum,data);
                                break;
                        }

                    }
                    }, new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
            }
        },200);

//        ShowDialog("최신 버전 입니다.");
    }

    private void TMS_Data_Down_Info (String revDataType, byte[] multipadSerialNum, byte[] data) {
//        m_menuActivity.ReadyDialogHide();
        mPosSdk.___TMSDataDownInfo("9240", Setting.getPreference(mPosSdk.getActivity(), Constants.TID), mDeviceVersion,multipadSerialNum,
                revDataType, data, new TcpInterface.DataListener() {
                    @Override
                    public void onRecviced(byte[] _rev) {
                        Log.d("TAG", String.valueOf(_rev));
                        Log.d("TAG", String.valueOf(_rev));

                        String mLog = "데이터 : " + Utils.bytesToHex_0xType(_rev);
                        mPosSdk.cout("VAN_REC : TMS_단말기데이타다운로드정보응답 9250",Utils.getDate("yyyyMMddHHmmss"),mLog);

                        KByteArray b  = new KByteArray(_rev);
                        b.CutToSize(1); //STX
                        b.CutToSize(4); //전문총길이
                        b.CutToSize(4); //거래전문번호
                        b.CutToSize(4); //필드구분자정의
                        b.CutToSize(1); //fs
                        byte[] _type = b.CutToSize(1);//유무선구분
                        byte[] _tid = b.CutToSize(10); //단말기ID
                        byte[] _version = b.CutToSize(5); //현재 단말기 버전
                        byte[] _serialNum = b.CutToSize(10); //단말기 시리얼 번호
                        b.CutToSize(1); //fs
                        byte[] _Ans_Cd = b.CutToSize(4); //응답코드
                        b.CutToSize(1); //fs
                        byte[] _message = b.CutToSize(80); //응답메세지
                        b.CutToSize(1); //fs

                        int _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == Command.FS) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Secu_Key = b.CutToSize(_re); //접속보안키
                        b.CutToSize(1); //fs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == Command.FS) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Token_cs_Path = b.CutToSize(_re); //인증키 경로명
                        b.CutToSize(1); //fs
                        byte[] _Data_Cnt = b.CutToSize(4); //데이터 개수
                        b.CutToSize(1); //fs
                        byte[] _Protocol = b.CutToSize(5); //전송 프로토콜
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Addr = b.CutToSize(_re); //다운로드서버주소
                        b.CutToSize(1); //Gs
                        byte[] _Port = b.CutToSize(5); //다운로드서버포트
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _User_ID = b.CutToSize(_re); //계정번호
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Password = b.CutToSize(_re); //계정비밀번호
                        b.CutToSize(1); //Gs
                        byte[] _Data_Type = b.CutToSize(5); //버전 및 데이터 구분
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _Data_Desc = b.CutToSize(_re); //버전(데이터) 설명
                        b.CutToSize(1); //Gs
                        _re = 0;
                        for (int i=0; i<b.getlength(); i++) {
                            if(b.indexData(i) == (byte)0x1D) {
                                _re = i;
                                break;
                            }

                        }
                        byte[] _File_Path_Name = b.CutToSize(_re); //파일명
                        b.CutToSize(1); //Gs
                        byte[] _File_Size = b.CutToSize(10); //파일크기
                        b.CutToSize(1); //Gs
                        byte[] _Check_Sum_Type = b.CutToSize(5); //파일체크방식
                        b.CutToSize(1); //Gs
                        byte[] _File_Check_Sum = b.CutToSize(64); //파일 체크섬
                        byte[] _checkKey = Utils.hexStringToByteArray(new String(_File_Check_Sum));
                        b.CutToSize(1); //Gs
                        byte[] _File_Encrypt_key = b.CutToSize(32); //파일 암호화 키
                        byte[] _encKey = Utils.hexStringToByteArray(new String(_File_Encrypt_key));
                        mEesKey = _encKey;
                        b.CutToSize(1); //fs
                        byte[] _Response_Time = b.CutToSize(14); //응답시간
                        b.CutToSize(1); //ETX
                        b.CutToSize(1); //LRC

//                        type = "",tid = "",version = "",serialNum = "",Ans_Cd = "",message = "",Secu_Key = "",
//                                Token_cs_Path = "",Data_Cnt = "",Protocol = "",Addr = "",Port = "",User_ID = "",Password = "",
//                                Data_Type = "",Data_Desc = "",File_Path_Name = "",File_Size = "",Check_Sum_Type = "",
//                                File_Check_Sum = "",File_Encrypt_key = "",Response_Time = "";
                        try {
                            Data_Type = Utils.getByteToString_euc_kr(_Data_Type);
                            Data_Desc = Utils.getByteToString_euc_kr(_Data_Desc);
                            File_Path_Name = Utils.getByteToString_euc_kr(_File_Path_Name);
                            File_Size = Utils.getByteToString_euc_kr(_File_Size);
                            Check_Sum_Type = Utils.getByteToString_euc_kr(_Check_Sum_Type);
                            File_Check_Sum = Utils.getByteToString_euc_kr(_File_Check_Sum);
                            File_Encrypt_key = Utils.getByteToString_euc_kr(_File_Encrypt_key);
                            Response_Time = Utils.getByteToString_euc_kr(_Response_Time);

                            type = Utils.getByteToString_euc_kr(_type);
                            tid = Utils.getByteToString_euc_kr(_tid);
                            version = Utils.getByteToString_euc_kr(_version);
                            serialNum = Utils.getByteToString_euc_kr(_serialNum);
                            Ans_Cd = Utils.getByteToString_euc_kr(_Ans_Cd);
                            message = Utils.getByteToString_euc_kr(_message);
                            Secu_Key = Utils.getByteToString_euc_kr(_Secu_Key);
                            Token_cs_Path = Utils.getByteToString_euc_kr(_Token_cs_Path);
                            Data_Cnt = Utils.getByteToString_euc_kr(_Data_Cnt);
                            Protocol = Utils.getByteToString_euc_kr(_Protocol);
                            Addr = Utils.getByteToString_euc_kr(_Addr);
                            Port = Utils.getByteToString_euc_kr(_Port);
                            User_ID = Utils.getByteToString_euc_kr(_User_ID);
                            Password = Utils.getByteToString_euc_kr(_Password);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        byte [] secu = Utils.hexStringToByteArray(Secu_Key);

                        String finalResponse_Time = Response_Time;
                        String finalAns_Cd = Ans_Cd;
                        String finalMessage = message;
                        String finalData_Cnt = Data_Cnt;
                        String finalProtocol = Protocol;
                        String finalAddr = Addr;
                        String finalPort = Port;
                        String finalUser_ID = User_ID;
                        String finalPassword = Password;
                        String finalData_Type = Data_Type;
                        String finalData_Desc = Data_Desc;
                        String finalFile_Path_Name = File_Path_Name;
                        String finalFile_Size = File_Size;
                        String finalCheck_Sum_Type = Check_Sum_Type;
                        String finalFile_Check_Sum = File_Check_Sum;
                        String finalFile_Encrypt_key = File_Encrypt_key;
                        try {
                            Ans_Cd = Utils.getByteToString_euc_kr(_Ans_Cd);
                            message = Utils.getByteToString_euc_kr(_message);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
//                        if (!Ans_Cd.equals("0000")) {
//                            progressDoalog.dismiss();
//                            ShowDialog("펌웨어실패: "+ Ans_Cd + ","+ message);
//                            return;
//                        }

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mPosSdk.__authenticatoin_result_req(_Response_Time,multipadAuthNum,multipadSerialNum, _Ans_Cd, _message,
                                        secu, _Data_Cnt, _Protocol, _Addr, _Port, _User_ID, _Password, _Data_Type, _Data_Desc,
                                        _File_Path_Name, _File_Size, _Check_Sum_Type, _checkKey, _encKey,
                                        new SerialInterface.DataListener() {
                                    @Override
                                    public void onReceived(byte[] _rev, int _type) {
                                        Log.d("TAG", String.valueOf(_rev));
                                        Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(_rev);

                                        String mLog = "데이터 : " + Utils.bytesToHex_0xType(_rev);
                                        mPosSdk.cout("CARD_READER_REC : 상호인증정보결과응답 0x55",Utils.getDate("yyyyMMddHHmmss"),mLog);

                                        if (!Ans_Cd.equals("0000")) {
                                            progressDoalog.dismiss();
                                            ShowDialog("펌웨어실패: "+ Ans_Cd + ","+ message);
                                            return;
                                        }

                                        switch (protocolInfo.Command) {
                                            case Command.ACK:
                                                Log.d("TAG", String.valueOf(_rev));
                                                break;
                                            case Command.NAK:
//                                                m_menuActivity.ReadyDialogHide();
                                                progressDoalog.dismiss();
                                                ShowDialog("상호인증정보결과요청 실패");
                                                break;
                                            case Command.CMD_MUTUAL_AUTHENTICATION_RESULT_RES:
                                                Log.d("TAG", String.valueOf(_rev));
                                                Log.d("TAG", String.valueOf(_rev));
                                                KByteArray b  = new KByteArray(_rev);
                                                b.CutToSize(1); //STX
                                                b.CutToSize(2); //데이터길이
                                                b.CutToSize(1); //CommandID
                                                byte[] _response = b.CutToSize(2);//응답결과
                                                byte[] _message = b.CutToSize(20); //메세지
                                                try {
                                                    String response = Utils.getByteToString_euc_kr(_response);
                                                    String message = Utils.getByteToString_euc_kr(_message);
                                                    if (!response.equals("00")) {
//                                                        m_menuActivity.ReadyDialogHide();
                                                        progressDoalog.dismiss();
                                                        ShowDialog("결과요청 실패 :" + message);
                                                        return;
                                                    }
                                                } catch (UnsupportedEncodingException e) {
                                                    e.printStackTrace();
                                                }
                                                byte[] _fileDecKey = b.CutToSize(16);//파일 복호화 키
                                                byte[] _fileDecKeyTemp = b.CutToSize(16);//파일 복호화 키 남은 부분은 버린다
                                                mAesKey = _fileDecKey;

                                                byte[] _updateSize = b.CutToSize(10); //업데이트 크기
//                                                byte[] _secKey = b.CutToSize(128);//보안키
//                                                b.CutToSize(1); //etx
//                                                b.CutToSize(1);//lrc
                                                try {
                                                    String size = Utils.getByteToString_euc_kr(_updateSize);
                                                    size = size.replace(" ","");
                                                    defaultSize = Integer.valueOf(size);
                                                } catch (UnsupportedEncodingException e) {
                                                    e.printStackTrace();
                                                }
                                                ConnectFTP connectFTP = new ConnectFTP();
                                                new Thread() {
                                                    public void run() {
                                                        try {
                                                            String mLog = "FTP주소 : " + Addr+ "," +"FTP포트 : " + Port + "," + "계정 : " + User_ID+ "," +
                                                                    "비밀번호 : " + Password+ "," +"파일주소 : " + File_Path_Name + "," + "파일복호화키 : " + Utils.bytesToHex_0xType(mAesKey);
                                                            mPosSdk.cout("FTP_SERVER_SEND : Ftp서버에 요청",Utils.getDate("yyyyMMddHHmmss"),mLog);
//                                                            mAesKey, mEesKey
                                                            byte[] _testKey = {
                                                                    0x74, 0x71, 0x64, 0x6b, 0x79, 0x62, 0x70, 0x61, 0x78, 0x71, 0x38, 0x66, 0x30, 0x31, 0x70, 0x77
                                                            };
                                                            connectFTP.fileDownload(Addr, Integer.parseInt(Port), User_ID, Password, File_Path_Name, mAesKey, new ConnectFTP.FTPListener() {
                                                                @RequiresApi(api = Build.VERSION_CODES.O)
                                                                @Override
                                                                public void onSuccess(byte[] _result, byte[] _original) {
                                                                    try {
//                                                                        byte[] _b2 = Utils.Aes128Dec(_result,mAesKey);
//                                                                        TMS_Update_File_Send_First(_b2);
                                                                        KByteArray _r  = new KByteArray(_result);
                                                                        KByteArray _o  = new KByteArray(_original);
//                                                                        String _rFull = Utils.bytesToHex_0xType(_r.value());

                                                                        String _r100 = Utils.bytesToHex_0xType(_r.CopyToSize(100));
                                                                        Log.d("_r100", _r100);
                                                                        String _o100 = Utils.bytesToHex_0xType(_o.CopyToSize(100));
                                                                        String mLog = "복호화된 파일(100) : " + _r100+ "," +"서버에서받은 파일(100) : " + _o100;
                                                                        mPosSdk.cout("FTP_SERVER_REC : Ftp서버에서 받은데이터",Utils.getDate("yyyyMMddHHmmss"),mLog);

                                                                        TMS_Update_File_Send_First(_result);
                                                                    } catch (Exception e) {
                                                                        progressDoalog.dismiss();
                                                                        ShowDialog("FTP실패: " + e);
                                                                        e.printStackTrace();
                                                                    }

//                                                                    TMS_Update_File_Send_First(_result);
                                                                }

                                                                @Override
                                                                public void onFail(String _msg) {
                                                                    progressDoalog.dismiss();
//                                                                    m_menuActivity.ReadyDialogHide();
                                                                    ShowDialog("FTP실패: " + _msg);
                                                                }
                                                            });
//                                                            connectFTP.connect(Addr,Integer.parseInt(Port),User_ID,Password,File_Path_Name);
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }

                                                    }
                                                }.start();
                                                break;

                                        }

                                    }
                                }, new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
                            }
                        },200);
                    }
                });
    }

    int updateSize = 0; //총 업데이트 할 사이즈
    int recentSize = 0; //현재까지 업데이트 한 사이즈
    int defaultSize = 1024; //데이터를 한번에 보내는 기본사이즈.
    // ble는 이것보다 훨씬 적어야 한다 현재 ble는 한번에 보내는 양이 20으로 되어있다(mtu랑 별개임. ble내부에서 한번에 너무 많은 데이터가 들어오면 그거 전문대로 처리하는 것에서 문제가 생김.)
    private void TMS_Update_File_Send_First(byte[] _b) {
        updateSize = 0;
        recentSize = 0;

        String _size = String.valueOf(_b.length);
        updateSize = _b.length;

        KByteArray b = new KByteArray(_b);
        byte[] _sample = null;
        _sample = b.CutToSize(defaultSize);
        recentSize = defaultSize;

        mPercent = 0;
        progressDoalog.setProgress(mPercent);

        mPosSdk.__updatefile_transfer_req("0001", _size, String.valueOf(recentSize), defaultSize, _sample, new SerialInterface.DataListener() {
            @Override
            public void onReceived(byte[] _rev, int _type) {
                Log.d("TAG", String.valueOf(_rev));
                Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(_rev);

                String mLog = "데이터 : " + Utils.bytesToHex_0xType(_rev);
                mPosSdk.cout("CARD_READER_REC : 업데이트파일응답 0x57",Utils.getDate("yyyyMMddHHmmss"),mLog);

                switch (protocolInfo.Command) {
                    case Command.ACK:
                        Log.d("TAG", String.valueOf(_rev));
                        break;
                    case Command.NAK:
//                        m_menuActivity.ReadyDialogHide();
                        progressDoalog.dismiss();
                        ShowDialog("업데이트파일전송 실패");
                        Log.d("TAG", String.valueOf(_rev));
                        break;
                    case Command.CMD_SEND_UPDATE_DATA_RES:
                        try {
                            Log.d("TAG", String.valueOf(_rev));
                            KByteArray bArray = new KByteArray(_rev);
                            bArray.CutToSize(1);    //STX + 길이 + CommandID 삭제
                            String length = new String(bArray.CutToSize(2));   //길이
                            bArray.CutToSize(1);    //CommandID
                            String response =  Utils.getByteToString_euc_kr(bArray.CutToSize(2));  //응답결과
                            String message = Utils.getByteToString_euc_kr(bArray.CutToSize(20));  //정상 : 정상처리, 실패 : 실패사유

                            if(response.equals("01")) {
                                //실패
//                                m_menuActivity.ReadyDialogHide();
                                progressDoalog.dismiss();
                                ShowDialog("업데이트전송 실패: " + message);
                                Log.d("TAG", String.valueOf(_rev));
                                return;
                            }
                            bArray.CutToSize(2);    //ETX, LRC

                            TMS_Update_File_Send_loop(b.value());
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        return;
                }
            }
        }, new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
    }

    private void TMS_Update_File_Send_loop(byte[] _b) {
        KByteArray b = new KByteArray(_b);
        byte[] _sample = null;
        if (b.getlength() < defaultSize) {
            _sample = b.value();
        } else {
            _sample = b.CutToSize(defaultSize);
        }

        recentSize += defaultSize;

        int _percent = (int) (recentSize * 100)/updateSize;
        if (_percent >= 100) {
            _percent = 100;
        }
        Log.d("TAG", "퍼센트 : " +  String.valueOf(_percent));
        mPercent = _percent;
        progressDoalog.setProgress(mPercent);

        mPosSdk.__updatefile_transfer_req("0001", String.valueOf(updateSize), String.valueOf(recentSize),defaultSize, _sample, new SerialInterface.DataListener() {
            @Override
            public void onReceived(byte[] _rev, int _type) {
                Log.d("TAG", String.valueOf(_rev));
                Command.ProtocolInfo protocolInfo = new Command.ProtocolInfo(_rev);
                String mLog = "데이터 : " + Utils.bytesToHex_0xType(_rev);
                mPosSdk.cout("CARD_READER_REC : 업데이트파일응답 0x57",Utils.getDate("yyyyMMddHHmmss"),mLog);

                switch (protocolInfo.Command) {
                    case Command.ACK:
                        Log.d("TAG", String.valueOf(_rev));
                        break;
                    case Command.NAK:
//                        m_menuActivity.ReadyDialogHide();
                        progressDoalog.dismiss();
                        ShowDialog("업데이트파일전송 실패");
                        Log.d("TAG", String.valueOf(_rev));
                        break;
                    case Command.CMD_SEND_UPDATE_DATA_RES:
                        Log.d("TAG", String.valueOf(_rev));
                        KByteArray bArray = new KByteArray(_rev);
                        bArray.CutToSize(1);    //STX + 길이 + CommandID 삭제
                        String length = new String(bArray.CutToSize(2));   //길이
                        bArray.CutToSize(1);    //CommandID
                        String response = "";  //응답결과
                        String message = "";
                        try {
                            response = Utils.getByteToString_euc_kr(bArray.CutToSize(2));
                            message = Utils.getByteToString_euc_kr(bArray.CutToSize(20));  //정상 : 정상처리, 실패 : 실패사유
                            if(response.equals("01")) {
                                //실패
//                                m_menuActivity.ReadyDialogHide();
                                progressDoalog.dismiss();
                                ShowDialog("업데이트전송 실패: " + message);
                                Log.d("TAG", String.valueOf(_rev));
                                return;
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }


                        bArray.CutToSize(2);    //ETX, LRC

//                        recentSize += final_sample.length;
                        if (recentSize < updateSize) {
                            TMS_Update_File_Send_loop(b.value());
                            return;
                        } else {
                           //완료
//                            new Handler(Looper.getMainLooper()).postDelayed(()->{
//
//                                mPosSdk.__PosInit("99",null,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
//                                CardConnectCheck();
//                            },200);

                            new Handler(Looper.getMainLooper()).postDelayed(()->{
                                m_menuActivity.ReadyDialogHide();
                                progressDoalog.dismiss();
                            },10000);

                            new Handler(Looper.getMainLooper()).postDelayed(()->{
                                //완료하면 일단 진행한다
                                ShowDialog("펌웨어 업데이트 성공");
//                                mPosSdk.__PosInit("99",null,new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});
                                if (mPosSdk.getUsbDevice().size() > 0) {
                                    mPosSdk.mSerial.Dispose();
                                    mPosSdk.mSerial.init();
                                    ShowDialog("재검색 버튼을 눌러주세요");
                                } else {
                                    mPosSdk.mSerial.Dispose();
                                    mPosSdk.mSerial.init();
                                    ShowDialog("재검색 버튼을 눌러주세요");
                                    return;
                                }
                                if(mPosSdk.mDevicesList != null)
                                {
                                    String[] deviceName = new String[mPosSdk.mDevicesList.size()+1];
                                    int mCount = 0;
                                    deviceName[0] = "";
                                    mCount++;
                                    for (Devices n: mPosSdk.mDevicesList) {
                                        deviceName[mCount] = n.getDeviceSerial();
                                        if (n.getmType() == 1) {
                                            if(n.getName() != null && n.getName().length()>16)
                                            {
                                                m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                                            } else {
                                                m_txt_line_tvw_product.setText(n.getName());
                                            }

                                            m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                            m_txt_line_tvw_version.setText(n.getVersion());
//                            m_txt_reader.setText(n.getName());
                                        } else if(n.getmType() == 4) {
                                            if(n.getName() != null && n.getName().length()>16)
                                            {
                                                m_txt_line_tvw_product.setText(n.getName().substring(0,16));
                                            } else {
                                                m_txt_line_tvw_product.setText(n.getName());
                                            }
                                            m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                            m_txt_line_tvw_version.setText(n.getVersion());
//                            m_txt_reader.setText(n.getName());
                                        } else if (n.getmType() == 0) {
//                            m_txt_reader.setText(n.getName());
                                        }

                                        mCount++;
                                    }
                                }
                            },12000);


                            Log.d("TAG", String.valueOf(_rev));

                            return;
                        }

//                        break;
                }
            }
        }, new String[]{mPosSdk.getICReaderAddr(),mPosSdk.getMultiReaderAddr()});

    }

    /**
     * 장치 관련 정보를 저장 하는 함수
     */
    private void Save(){

        mPosSdk.BleIsConnected();
        if(Setting.getBleIsConnected())
        {
            mbleSdk = bleSdk.getInstance();
            mbleSdk.DisConnect();
            /** 정상적으로 연결을 해제한다면 기존 마지막 연결장비에 관한 데이터를 지운다 */
            Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_NAME, "");
            Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_ADDR, "");
        }

        if(m_cbx_signPad.getSelectedItem().toString().equals(m_cbx_card_reader_address.getSelectedItem().toString()))
        {
            if(m_cbx_signPad.getVisibility()==View.VISIBLE) {
                ShowDialog("한 개 포트에 두대의 장비를 설정 할 수 없습니다.");
                return;
            }
        }

        if(m_cbx_card_reader_address.getSelectedItem()==null)
        {
            ShowDialog("장치의 시리얼 번호를 설정하거나 장치 재검색을 실행 하십시요");
            return;
        }
        if(m_cbx_card_reader_address.getSelectedItem().toString().equals(""))
        {
            ShowDialog("장치 설정 필요");
            return;
        }

        if(m_txt_line_tvw_product.getText() == null || m_txt_line_tvw_product.getText().toString().equals(""))
        {
            ShowDialog("장치 연결 확인 필요");
            return;
        }

        if (!m_cbx_card_reader_address.getSelectedItem().toString().equals(m_txt_line_tvw_serial.getText().toString())) {
            ShowDialog("설정 한 리더기와 리더기 정보가 같지 않습니다");
            return;
        }

        if(m_cbx_signPad_type.getSelectedItemPosition() == 1| m_cbx_signPad_type.getSelectedItemPosition() == 2 ) {
            if (m_cbx_signPad.getSelectedItem() == null) {
                ShowDialog("장치의 시리얼 번호를 설정하거나 장치 재검색을 실행 하십시요");
                return;
            }
            if (m_cbx_signPad.getSelectedItem().toString().equals("")) {
                ShowDialog("장치 설정 필요");
                return;
            }
        }

        if (m_edt_usb_timeout.getText() == null || m_edt_usb_timeout.getText().toString().equals(""))
        {
            ShowDialog("카드대기시간 설정 필요");
            return;
        }
        else if (Integer.parseInt(m_edt_usb_timeout.getText().toString()) < 10 ||
                Integer.parseInt(m_edt_usb_timeout.getText().toString()) > 99)
        {
            ShowDialog("10 ~ 99초 이내 설정 가능");
            return;
        }

        Setting.setPreference(this.getContext(),Constants.USB_TIME_OUT,(m_edt_usb_timeout.getText().toString()));

        /** 유선 장비 사용하는 것을 판단하여 유선 장치를 결제 장치로 설정한다. */
        /** 2021.11.25 kim.jy */
        Setting.g_PayDeviceType = Setting.PayDeviceType.LINES;    //어플 전체에서 사용할 결제 방식
        Setting.setPreference(this.getContext(), Constants.APPLICATION_PAYMENT_DEVICE_TYPE,String.valueOf(Setting.g_PayDeviceType));

        /** QR 데이터를 읽는 장비를 어떤거로 할지 정한다 */
        Setting.setPreference(this.getContext(), Constants.LINE_QR_READER, mQrReaderType);

        /** 터치서명 크기를 어떻게 설정할지 정한다 */
        Setting.setPreference(this.getContext(), Constants.SIGNPAD_SIZE, mTouchSignPadSize);

        if(m_cbx_card_reader.getSelectedItemPosition()==1)  //사용하지않음.카드리더기가 멀티패드로 설정된 경우 사인패드를 사용하지 않는다(이미 위에서 셋팅되어 있는항목)
        {
            Setting.setSignPadType(mPosSdk.getActivity(), 0);
            m_cbx_signPad_type.setSelection(0);
            //서명패드 사용안함, 화면 터치 서명인 경우에는 시리얼을 표시 하지 않는다.
            m_cbx_signPad.setVisibility(View.INVISIBLE);
        }

        String tmpReader = "";
        if (mPosSdk.mDevicesList == null) {
            return;
        }
        for (Devices n: mPosSdk.mDevicesList)
        {
            if(n.getDeviceSerial().equals(m_cbx_card_reader_address.getSelectedItem().toString()))
            {
                if(Setting.getICReaderType(mPosSdk.getActivity())==Command.CARD_READER)
                {
                    n.setmType(1);
                    n.setConnected(true);
                    mPosSdk.setICReaderAddr(n.getmAddr());
                    mPosSdk.setMultiReaderAddr("");
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER, n.getmAddr());  //로컬 저장소에 카드리더기 주소값 저장
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL, n.getDeviceSerial());    //로컬 저장소에 카드리더기 주소값 저장
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER, "");  //로컬 저장소에 멀티리더기 값 초기화
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL, "");    //로컬 저장소에 멀티리더기값 초기화
                    tmpReader = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER);
                    for(Devices d:mPosSdk.mDevicesList)
                    {
                        if(d.getmType()==4)
                        {
                            d.setConnected(false);
                        }
                    }
                }
                else if(Setting.getICReaderType(mPosSdk.getActivity())==Command.MULTI_READER)
                {
                    n.setmType(4);
                    n.setConnected(true);
                    mPosSdk.setMultiReaderAddr(n.getmAddr());
                    mPosSdk.setICReaderAddr("");
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER, n.getmAddr());  //로컬 저장소에 멀티리더기 값 저장
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL, n.getDeviceSerial());    //로컬 저장소에 멀티리더기값 저장
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER, "");  //로컬 저장소에 카드리더기 값 초기화
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL, "");    //로컬 저장소에 카드리더기값 초기화
                    tmpReader = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER);
                    for(Devices d:mPosSdk.mDevicesList)
                    {
                        if(d.getmType()==1)
                        {
                            d.setConnected(false);
                        }
                    }
                }
            }
            else if(m_cbx_signPad.getVisibility()==View.VISIBLE && n.getDeviceSerial().equals(m_cbx_signPad.getSelectedItem().toString()))
            {
                if(Setting.getSignPadType(mPosSdk.getActivity())==Command.SIGNPAD_MULIT) {  //서명 패드를 멀티 패드로 설정한 경우
                    n.setmType(3);  //서명 패드를 멀티 패드로 설정한 경우
                    n.setConnected(true);
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD, n.getmAddr());   //로컬 저장소에 멀티서명패드값 저장
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL, n.getDeviceSerial());  //로컬 저장소에 멀티서명패드값 저장
                    mPosSdk.setMultiPadAddr(n.getmAddr());
                    mPosSdk.setSignPadAddr("");
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD, ""); //로컬저장소에 일반 서명 패드 초기화
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD_SERIAL, ""); //로컬저장소에 일반 서명 패드 초기화
                    for(Devices d:mPosSdk.mDevicesList)
                    {
                        if(d.getmType()==2)
                        {
                            d.setConnected(false);
                        }
                    }
                }
                else if(Setting.getSignPadType(mPosSdk.getActivity())==Command.SIGNPAD_SIGN)
                {
                    n.setmType(2);  //단순 서명 패드로 설정한 경우
                    n.setConnected(true);
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD, n.getmAddr()); //로컬저장소에 일반 서명 패드값 저장
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD_SERIAL, n.getDeviceSerial()); //로컬저장소에 일반 서명 패드값 저장
                    mPosSdk.setSignPadAddr(n.getmAddr());
                    mPosSdk.setMultiPadAddr("");
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD, "");  //로컬 저장소에 멀티서명패드값 초기화
                    Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL, "");  //로컬 저장소에 멀티서명패드값 초기화
                    for(Devices d:mPosSdk.mDevicesList)
                    {
                        if(d.getmType()==3)
                        {
                            d.setConnected(false);
                        }
                    }
                }


            }
        }

        /* 서명패드가 터치서명OR사용안함 인 경우 */
        if (m_cbx_signPad.getVisibility() == View.INVISIBLE) {
            mPosSdk.setSignPadAddr("");
            Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD, ""); //로컬저장소에 일반 서명 패드 초기화
            Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD_SERIAL, ""); //로컬저장소에 일반 서명 패드 초기화
            mPosSdk.setMultiPadAddr("");
            Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD, "");  //로컬 저장소에 멀티서명패드값 초기화
            Setting.setPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD_SERIAL, "");  //로컬 저장소에 멀티서명패드값 초기화
            for(Devices d:mPosSdk.mDevicesList)
            {
                if(d.getmType()==2)
                {
                    d.setConnected(false);
                }
                if(d.getmType()==3)
                {
                    d.setConnected(false);
                }
            }
        }
        Setting.setPreference(mPosSdk.getActivity(),Constants.SELECTED_TRADE_OPTION,"카드리더기");
        Setting.setPreference(mPosSdk.getActivity(),Constants.SELECTED_CARD_READER_OPTION,m_cbx_card_reader.getSelectedItem().toString());
        Setting.setPreference(mPosSdk.getActivity(),Constants.SELECTED_SIGN_PAD_OPTION,m_cbx_signPad_type.getSelectedItem().toString());

        /* 로그체크및 테스트를 위해 사용. 실제 데이터를 셋팅하고 저장하는 것과 아래는 무관 */
        String tmpPad = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_SIGN_PAD);
        String tmpMultiPad = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_SIGN_PAD);

        boolean noPadSaved = true;
        if(tmpPad.equals("") && tmpMultiPad.equals(""))
        {
            noPadSaved =true;
        }
        else
        {
            noPadSaved =false;
        }

        if(tmpReader.equals("") && noPadSaved)
        {
            ShowDialog("연결 확인 버튼을 눌러 장치 확인을 해야 합니다.");
            return;
        }
        if(!tmpReader.equals(tmpPad))
        {
            ShowDialog("디바이스 설정을 저장하였습니다.");
            return;
        }
        else
        {
            if(m_cbx_signPad.getVisibility()==View.VISIBLE) {
                ShowDialog("한 개 포트에 두대의 장비를 설정 할 수 없습니다.");
                return;
            }
        }
    }

    /**
     * 디바이스 응답 관련 interface callback 구현
     */
    private SerialInterface.DataListener mDataListener = new SerialInterface.DataListener()
    {
        @Override
        public void onReceived(byte[] _rev, int _type)
        {
            switch (_rev[3])
            {
                case Command.CMD_SEND_MSG_RES:  //(byte)0xB5; //메시지 전송 응답
                    //Command.ProtocolInfo proto = new Command.ProtocolInfo(_rev);
                    //KByteArray b = new KByteArray(proto.Contents);
//                    mPosSdk.setSignPadAddress(m_cbx_signPad.getSelectedItem().toString());
                    //mPosSdk.setICCardReaderAddress(m_cbx_card_reader.getSelectedItem().toString());
                    for (Devices n: mPosSdk.mDevicesList)
                    {
                        if(n.getDeviceSerial().equals(m_cbx_signPad.getSelectedItem().toString()))
                        {
                            mbPadConnSuccess = true;
                        }
                    }
                    showResultToast("정상연결");
                    break;
                case Command.CMD_IC_STATE_RES:  //(byte)0X19; //카드 상태 응답
                    //mPosSdk.setSignPadAddress(m_cbx_signPad.getSelectedItem().toString());
                    //mPosSdk.setICCardReaderAddress(m_cbx_card_reader.getSelectedItem().toString());
                    for (Devices n: mPosSdk.mDevicesList)
                    {
                        if(n.getDeviceSerial().equals(m_cbx_card_reader_address.getSelectedItem().toString()))
                        {
                            mbReaderConnSuccess = true;
                        }
                    }
                    showResultToast("정상연결");
                    break;
//                case Command.CMD_MUTUAL_AUTHENTICATION_RES:  //(byte)0X53; //상호인증 응답
//                    Log.d("상호인증응답", String.valueOf(_rev[4]));
//                    break;
                default:

                    showResultToast("연결실패");

                    break;
            }
        }
    };

    /**
     * 토스트 형태(하단에 한문장으로 짧게)로 메세지박스를 보여준다(ex.연결실패 정상연결)
     * @param _str
     */
    private void showResultToast(String _str)
    {
        Toast.makeText(m_menuActivity,_str,Toast.LENGTH_SHORT).show();
    }

    /**
     * 키업데이트 관련 interface callback 구현
     */
    private SerialInterface.KeyUpdateListener mKeyUpdateListener = new SerialInterface.KeyUpdateListener() {
        @Override
        public void result(String result, String Code,String state, HashMap<String, String> resultData) {
//            ReadyDialogHide();
            new Handler(Looper.getMainLooper()).postDelayed(()->{
                m_menuActivity.ReadyDialogHide();
                ShowDialog(result);
            },200);
        }
    };

    private void ShowProgressBar()
    {
        progressDoalog = new ProgressDialog(mCtx);
        progressDoalog.setMax(100);
        progressDoalog.setIndeterminate(false);
        progressDoalog.setMessage("업데이트 중입니다....");
        progressDoalog.setTitle("펌웨어 업데이트");
        progressDoalog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDoalog.show();
    }


    /**
     * 토스트 형태(하단에 한문장으로 짧게)로 메세지박스를 보여준다(ex.디바이스정보)
     * @param _str
     */
    private void ShowDialog(String _str)
    {
        new Handler(Looper.getMainLooper()).post(()->{
            Toast.makeText(m_menuActivity,_str,Toast.LENGTH_SHORT).show();
        });

    }
    /**
     * 사용안함.가맹점 설정을 하지 않은 경우에 거래를 할 수 없게 한다.
     */
    private void checkTid()
    {
        if(getTID().equals(""))
        {
            Toast.makeText(mPosSdk.getActivity(), getResources().getString(R.string.error_no_data_tid), Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(this, Main2Activity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(intent);
            return;
        }
    }

    /**
     * 안드로이드 sharedPreference에 저장되어 있는 TID를 가져 오는 함수
     * @return String
     */
    private String getTID()
    {
        return Setting.getPreference(mPosSdk.getActivity(),Constants.TID);
    }
}


