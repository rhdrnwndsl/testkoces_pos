package com.koces.androidpos;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.Devices.AutoDetectDeviceInterface;
import com.koces.androidpos.sdk.Devices.AutoDetectDevices;
import com.koces.androidpos.sdk.Devices.Devices;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.van.Constants;

import java.util.Objects;

public class LineSetDialog extends Dialog {
    private LineSetDialog.DialogBoxListener m_listener;
    KocesPosSdk mPosSdk;

    /** 화면에 구성되는 버튼 */
    Button m_btn_sign_connect_check,
            m_btn_cardreader_connect_check,
            m_btn_rescan_device,
            m_btn_save, m_btn_exit;

    /** 제품 식별번호, 시리얼번호, 버전 */
    TextView m_txt_line_tvw_product,m_txt_line_tvw_serial, m_txt_line_tvw_version;

    /** usb 장치 타임아웃시간설정 */
    EditText m_edt_usb_timeout;

    /** 화면에 나타나는 여러 메뉴(사인패드구성메뉴, 카드리더기구성메뉴 등) */
    Spinner m_cbx_trade_option,m_cbx_card_reader,m_cbx_signPad_type,m_cbx_signPad,m_cbx_card_reader_address;

    /** QR 읽는 장치 설정 카메라, 카드리더, 사인패드 */
    Spinner m_cbx_qr_reader;
    String mQrReaderType = "";

    /** 터치서명 사이즈 크기 */
    Spinner m_cbx_size_touchSignpad;
    String mTouchSignPadSize = "";

    /** linearlayoutGroup 장치에 따라서 세부 설정 여부를 설정하는 그룹을 보이기 또는 숨기 */
    LinearLayout m_llayout_line_group;

    /**이 초기화 함수는 절대 사용 금지 **/
    public LineSetDialog(){
        super(null);
    };
    public LineSetDialog(@NonNull Context context){super(context);};

    public LineSetDialog(@NonNull Context context, LineSetDialog.DialogBoxListener _listener) {
        super(context);
        mPosSdk = KocesPosSdk.getInstance();
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
        setContentView(R.layout.custom_line_set);
        init();
    }

    private void init()
    {
        m_btn_save =(Button)findViewById(R.id.btn_f_device_save);
        m_btn_exit=(Button)findViewById(R.id.btn_f_device_exit);
        m_btn_rescan_device = (Button)findViewById(R.id.btn_f_recan_device);
        m_btn_sign_connect_check = (Button)findViewById(R.id.btn_f_connect_pad);
        m_btn_cardreader_connect_check =(Button)findViewById(R.id.btn_f_connect_ic);

        //식별번호,시리얼번호,버전정보
        m_txt_line_tvw_product = (TextView)findViewById(R.id.frgline_tvw_product);
        m_txt_line_tvw_serial = (TextView)findViewById(R.id.frgline_tvw_serial);
        m_txt_line_tvw_version = (TextView)findViewById(R.id.frgline_tvw_ver);
//        m_txt_reader = (TextView)findViewById(R.id.txt_ICType);
//        m_txt_reader.setText("");

        //usb 타임아웃
        m_edt_usb_timeout = (EditText)findViewById(R.id.edit_usbTimeout);
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
//        m_cbx_trade_option = (Spinner)findViewById(R.id.spinner_f_trade_option);    //거래 방식 선택
        m_cbx_card_reader = (Spinner)findViewById(R.id.spinner_f_ICType);   //카드 리더스 종료 선택
        m_cbx_signPad_type = (Spinner)findViewById(R.id.spinner_f_padType); //사인 패드 종류 선택

        m_cbx_signPad = (Spinner)findViewById(R.id.spinner_f_signpad);     //사인패드 연결할 디바이스 이름
        m_cbx_card_reader_address = (Spinner)findViewById(R.id.spinner_f_card_reader); //카드리더기 연결할 디바이스 이름

        m_llayout_line_group = (LinearLayout)findViewById(R.id.frg_dev_line_opt_group);

        m_btn_save.setOnClickListener(BtnOnClickListener);
        m_btn_rescan_device.setOnClickListener(BtnOnClickListener);
        m_btn_sign_connect_check.setOnClickListener(BtnOnClickListener);
        m_btn_cardreader_connect_check.setOnClickListener(BtnOnClickListener);
        m_btn_exit.setOnClickListener(BtnOnClickListener);

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
                    mPosSdk.getActivity(),
                    R.layout.spinner_item,
                    deviceName);
            m_cbx_signPad.setAdapter(adapter);
            m_cbx_card_reader_address.setAdapter(adapter);
        }

        //저장 정보 가져 와서 세팅 하기
        //거래방식 선택 - 카드리더기 현재 한개
        String SelectedtradeOption = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_TRADE_OPTION);
//        m_cbx_trade_option.setSelection(getIndex(m_cbx_trade_option,SelectedtradeOption));
        //카드리더기 선택 - 카드리더기, 멀티리더기
        String SelectedCardReaderOption = Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_CARD_READER_OPTION);
        m_cbx_card_reader.setSelection(getIndex(m_cbx_card_reader,SelectedCardReaderOption));
        //서명패드 선택 - 선택안함, 서명패드, 멀티패드, 터치서명
        String SelectedSignPadOption = Setting.getPreference(mPosSdk.getActivity(),Constants.SELECTED_SIGN_PAD_OPTION);
        m_cbx_signPad_type.setSelection(getIndex(m_cbx_signPad_type,SelectedSignPadOption));

        /* 현재 설정되어 있는 카드리더기(카드리더기OR멀티리더기)의 장치이름 */
        String SelectedCardReader= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_CARD_READER_SERIAL);
        String SelectedMultiReader= Setting.getPreference(mPosSdk.getActivity(), Constants.SELECTED_DEVICE_MULTI_READER_SERIAL);
        if(SelectedCardReaderOption.equals("카드리더기"))
        {
            m_cbx_card_reader_address.setSelection(getIndex(m_cbx_card_reader_address,SelectedCardReader));
//            m_txt_reader.setText(Setting.mAuthNum);
        }
        else if(SelectedCardReaderOption.equals("멀티패드"))
        {
            m_cbx_card_reader_address.setSelection(getIndex(m_cbx_card_reader_address,SelectedMultiReader));
//            m_txt_reader.setText(Setting.mAuthNum);
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
                    m_txt_line_tvw_product.setText(n.getName());
                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                    m_txt_line_tvw_version.setText(n.getVersion());
//                            m_txt_reader.setText(n.getName());
                } else if(n.getmType() == 4) {
                    m_txt_line_tvw_product.setText(n.getName());
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

        /* 카드리더기메뉴선택 - 카드리더기 멀티패드(멀티카드리더기) */
        m_cbx_card_reader.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //               ((TextView)parent.getChildAt(0)).setTextColor(Color.BLACK);
                if(view!=null) {
                    ((TextView) parent.getChildAt(0)).setTextSize(14);
                }
                Setting.setICReaderType(mPosSdk.getActivity(), position);
//                setCardSignSelectedItemSwap(1,position);
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
////                setCardSignSelectedItemSwap(1,position);
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

        /** qr 리더 설정 */
        m_cbx_qr_reader = findViewById(R.id.spinner_f_qrReader);
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
        m_cbx_size_touchSignpad = findViewById(R.id.spinner_f_size_signpad);
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
//                case R.id.btn_f_keyupdate:
//                    KeyReset();
//                    break;
//                case R.id.btn_f_classfi_check:
//                    DeviceIntergrity();
//                    break;
                case R.id.btn_f_recan_device:
                    DeviceReScan();
                    break;
//                case R.id.btn_f_upgrade_reader:
//                    UpgradeReader();
//                    break;
                case R.id.btn_f_device_save:
                    Save();
                    break;
                case R.id.btn_f_device_exit:
                    DialogExit();
                    break;
                default:
                    break;
            }
        }
    };

    private int getIndex(Spinner spinner, String myString){
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                return i;
            }
        }
        return 0;
    }

    private void ShowDialog(String _str)
    {
        new Handler(Looper.getMainLooper()).post(()->{
            Toast.makeText(mPosSdk.getActivity(),_str,Toast.LENGTH_SHORT).show();
        });

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
                ShowDialog("장치의 시리얼 번호를 설정하거나 장치 재검색을 실행 하십시요");
                return;
            }
            if(m_cbx_card_reader_address.getSelectedItem().toString().equals(""))
            {
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
                                    ShowDialog("장치 정보를 읽어오지 못했습니다.");
                                    return;
                                }
                                if(_rev[3]==(byte)0x15){
                                    //장비에서 NAK 올라 옮
                                    ShowDialog("장치 NAK");
                                    return;
                                }
                                if(Setting.ICResponseDeviceType == 3)
                                {
                                    ShowDialog("장치 오류");
                                    return;
                                }
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
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
                                    m_txt_line_tvw_product.setText(n.getName());
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if(n.getmType() == 4) {
                                    m_txt_line_tvw_product.setText(n.getName());
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if (n.getmType() == 0) {
//                                    m_txt_reader.setText(n.getName());
                                }

                                mPosSdk.setICReaderAddr(n.getmAddr());
                                ShowDialog("연결에 성공하였습니다");
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
                                    return;
                                }
                                if(_rev[3]==(byte)0x15){
                                    //장비에서 NAK 올라 옮
                                    return;
                                }
                                KByteArray KByteArray = new KByteArray(_rev);
                                KByteArray.CutToSize(4);
                                String authNum = new String(KByteArray.CutToSize(32));//장비 인식 번호
                                String serialNum = new String(KByteArray.CutToSize(10));
                                String version = new String(KByteArray.CutToSize(5));
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
                                    m_txt_line_tvw_product.setText(n.getName());
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if(n.getmType() == 4) {
                                    m_txt_line_tvw_product.setText(n.getName());
                                    m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                    m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                                } else if (n.getmType() == 0) {
//                                    m_txt_reader.setText(n.getName());
                                }
                                mPosSdk.setMultiReaderAddr(n.getmAddr());
                                ShowDialog("연결에 성공하였습니다");
                            }
                        },new String[]{n.getmAddr()});
                    }

                }
            }

        }
        else
        {
            ShowDialog("연결된 장치를 찾을 수 없습니다");
            return;
        }
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
            ShowDialog("연결된 장치를 찾을 수 없습니다");
            return;
        }
    }

    /**
     * 장치 재검색 함수
     */
    private void DeviceReScan()
    {
        /* 장치를 검색하여 현재 정상적으로 usb가 붙어있는 사용가능장비들을 체크한다 */
        final AutoDetectDevices autoDetectDevices = new AutoDetectDevices(mPosSdk.getActivity(), new AutoDetectDeviceInterface.DataListener() {
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
                    /* 위의 설정된 장치리스트들의 이름을 spinner메뉴에 등록한다 */
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            mPosSdk.getContext(),
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
//                        m_txt_reader.setText(Setting.mAuthNum);
                    }
                    else if(SelectedCardReaderOption.equals("멀티패드"))
                    {
                        m_cbx_card_reader_address.setSelection(getIndex(m_cbx_card_reader_address,SelectedMultiReader));
//                        m_txt_reader.setText(Setting.mAuthNum);
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
                                m_txt_line_tvw_product.setText(n.getName());
                                m_txt_line_tvw_serial.setText(n.getDeviceSerial());
                                m_txt_line_tvw_version.setText(n.getVersion());
//                                    m_txt_reader.setText(n.getName());
                            } else if(n.getmType() == 4) {
                                m_txt_line_tvw_product.setText(n.getName());
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
//                            mbPadConnSuccess = true;
                        }
                    }
                    ShowDialog("정상연결");
                    break;
                case Command.CMD_IC_STATE_RES:  //(byte)0X19; //카드 상태 응답
                    //mPosSdk.setSignPadAddress(m_cbx_signPad.getSelectedItem().toString());
                    //mPosSdk.setICCardReaderAddress(m_cbx_card_reader.getSelectedItem().toString());
                    for (Devices n: mPosSdk.mDevicesList)
                    {
                        if(n.getDeviceSerial().equals(m_cbx_card_reader_address.getSelectedItem().toString()))
                        {
//                            mbReaderConnSuccess = true;
                        }
                    }
                    ShowDialog("정상연결");
                    break;
                default:

                    ShowDialog("연결실패");

                    break;
            }
        }
    };

    /** 연결하지 않고 종료 */
    private void DialogExit()
    {
        m_listener.onClickCancel("장치를 저장하지 않았습니다. 환경설정에서 장치셋팅을 완료해 주십시오.");
        dismiss();
        return;
    }

    /**
     * 장치 관련 정보를 저장 하는 함수
     */
    private void Save()
    {

//        if(Setting.getBleIsConnected())
//        {
//            mbleSdk.DisConnect();
//            /** 정상적으로 연결을 해제한다면 기존 마지막 연결장비에 관한 데이터를 지운다 */
//            Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_NAME, "");
//            Setting.setPreference(this.getContext(), Constants.BLE_DEVICE_ADDR, "");
//        }

        if (mPosSdk.mDevicesList == null) {
            m_listener.onClickCancel("검색된 장치가 없습니다.");
            dismiss();
            return;
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

        for (Devices n: mPosSdk.mDevicesList)
        {
            if(n.getDeviceSerial().equals(m_cbx_card_reader_address.getSelectedItem().toString()))
            {
                if(Setting.getICReaderType(mPosSdk.getActivity())== Command.CARD_READER)
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
//            ShowDialog("디바이스 설정을 저장하였습니다.");
            m_listener.onClickConfirm("디바이스 설정을 저장하였습니다.");
            dismiss();
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

    public void setListener(LineSetDialog.DialogBoxListener listener) { m_listener = listener; }

    public interface DialogBoxListener {
        void onClickCancel(String _msg);              // 취소 버튼 클릭 시 호출
        void onClickConfirm(String _msg);              // 확인 버튼 클릭 시 호출
    }
}
