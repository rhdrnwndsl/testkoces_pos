package com.koces.androidpos;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.input.InputManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.koces.androidpos.sdk.KByteArray;
import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.ConvertBitmapToMonchromeBitmap;
import com.koces.androidpos.sdk.KocesPosSdk;
import com.koces.androidpos.sdk.PaymentSdk;
import com.koces.androidpos.sdk.SerialPort.SerialInterface;
import com.koces.androidpos.sdk.Setting;
import com.koces.androidpos.sdk.TCPCommand;
import com.koces.androidpos.sdk.Utils;
import com.koces.androidpos.sdk.van.Constants;
import com.koces.androidpos.sdk.van.TcpInterface;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 사용안함
 * 작업 프로세스 절차
 * 금액 입력
 * IC 카드 삽입
 * 금액 확인(50000 원 이상 사인패드 요청 또는 화면 출력)
 *
 */
public class PaymentActivity extends BaseActivity {
//    /** log를 위한 TAG 설정 */
//    private final static String TAG = PaymentActivity.class.getSimpleName();
//    Button m_btn_credit, m_btn_cash, m_btn_card_insert, m_btn_card_delete, m_btn_card_eraser, m_btn_exit;
//    EditText m_TotalMoney, m_installment,m_tax,m_serviceCharge,m_taxfree;
//    //TextView m_approval_num,m_approval_date;
//    EditText m_approval_num,m_approval_date,m_cash_num;
//    RadioButton m_rad_private,m_rad_company,m_rad_etc;
//    RadioButton m_rad_card,m_rad_pinin;
//    int reciptTarget = 1;
//    int payMethod = 1;
//    KocesPosSdk mPosSdk;
//    PaymentSdk mPaymentSdk;
//    Spinner m_cbx_reason_cancel;
//    TextView m_txt_listner_msg;
//    boolean padInitReq;
//    int mMoney;
//
//    private int REQUEST_SIGNPAD = 10001;
//    String printReciptIDNumber = "";
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_payment);
//        init();
//        //CallSignPadActivity();
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if(requestCode==REQUEST_SIGNPAD)
//        {
//            if(resultCode==RESULT_OK && data.getStringExtra("signresult").equals("OK"))
//            {
//                //만일 서명패드가 없이 터치서명 또는 서명없음일 경우 _img 파일을 저장된 파일로 대체 한다. 저장된 파일이 없다면 이미지없음으로 처리한다.
//                if(Setting.getSignPadType(mPosSdk.getActivity()) == 1  || Setting.getSignPadType(mPosSdk.getActivity()) == 2){
//                    byte[] bytes = data.getByteArrayExtra("signdata");;
//                    try{
////                        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
////                        String fname = "signData" +".jpg";
////                        String imgpath = root + "/signfile/" + fname ;
////
////                        Bitmap bm = BitmapFactory.decodeFile(imgpath);
////                        KByteArrayOutputStream KByteArray = new KByteArrayOutputStream();
////                        bm.compress(Bitmap.CompressFormat.PNG, 100, KByteArray);
////                        //ConvertBitmapToMonchromeBitmap tmpBitmap = new ConvertBitmapToMonchromeBitmap();
////                        //final byte[] tmp = tmpBitmap.convertBitmap(bm);
////                        //bytes = new byte[tmp.length];
////                        //System.arraycopy(tmp,0,bytes,0,tmp.length);
////                        bytes = KByteArray.toKByteArray();
//                    }catch(Exception e){
//
//                    }
//                    mPaymentSdk.Req_tcp_Credit(getTID(), bytes,"","","");
////                    mPaymentSdk.Req_tcp_Credit(getTID(), bytes,data.getStringExtra("codeNver"),"","");
//                }
//                else{
//                    byte[] tmpSign = data.getByteArrayExtra("signdata");
//                    String CodeNversion = data.getStringExtra("codeNver");
//                    mPaymentSdk.Req_tcp_Credit(getTID(), tmpSign,"","","");
//                }
//                //mPaymentSdk.Req_tcp_Credit(null);
//            }
//            else if(resultCode==RESULT_CANCELED && data.getStringExtra("signresult").equals("CANCEL"))
//            {
//                //사인처리가 취소됨. 모든 정보 클리어 처리
//                mPaymentSdk.DeviceReset();
//                HideDialog();
//            }
//        }
//    }
//
//    private void init() {
//        m_btn_credit = findViewById(R.id.btn_credit);
//        m_btn_cash = findViewById(R.id.btn_cash);
//        //m_btn_card_insert = findViewById(R.id.btn_card_insert);
//        //m_btn_card_delete = findViewById(R.id.btn_card_delete);
//        m_btn_card_eraser = findViewById(R.id.btn_card_eraser);
//        m_btn_exit = findViewById(R.id.btn_main2_exit);
//
//        m_btn_credit.setOnClickListener(BtnOnClickListener);
//        m_btn_cash.setOnClickListener(BtnOnClickListener);
////        m_btn_card_insert.setOnClickListener(BtnOnClickListener);
////        m_btn_card_delete.setOnClickListener(BtnOnClickListener);
//        m_btn_card_eraser.setOnClickListener(BtnOnClickListener);
//        m_btn_exit.setOnClickListener(BtnOnClickListener);
//
//        //EditText
//        m_TotalMoney = (EditText) findViewById(R.id.edit_trade_money);
//        m_TotalMoney.setText(Command.ANGLE_VALUE);
//        m_installment = (EditText) findViewById(R.id.edit_installment);
//        m_installment.setText(Command.DEFAULT_VALUE);
//        m_tax = (EditText) findViewById(R.id.edit_tax1);
//        int tmp = Integer.parseInt(Command.ANGLE_VALUE) / 10;
//        m_tax.setText(String.valueOf(tmp));
//        m_serviceCharge = (EditText)findViewById(R.id.edit_serviceCharge);
//        m_serviceCharge.setText(Command.DEFAULT_VALUE);
//        m_taxfree = (EditText)findViewById(R.id.edit_taxFree);
//
//        mPosSdk = KocesPosSdk.getInstance();
//        mPosSdk.setFocusActivity(this,null);
//        Setting.setTopContext(this);
//        m_txt_listner_msg = (TextView)findViewById(R.id.txt_listner_msg);
//        m_cbx_reason_cancel = (Spinner)findViewById(R.id.spinner_reason_cancel);
//        m_rad_company = (RadioButton)findViewById(R.id.rab_company);
//        m_rad_private = (RadioButton)findViewById(R.id.rab_private);
//        m_rad_etc = (RadioButton)findViewById(R.id.rab_etc);
//
//        m_rad_card = (RadioButton)findViewById(R.id.rab_pay_card);
//        m_rad_pinin = (RadioButton)findViewById(R.id.rab_pay_pinin);
//        m_rad_private.setChecked(true);
//        m_rad_company.setChecked(false);
//        m_rad_etc.setChecked(false);
//
//        m_rad_card.setChecked(true);
//        m_rad_pinin.setChecked(false);
//
////        m_TotalMoney.setOnFocusChangeListener(new View.OnFocusChangeListener(){
////
////            @Override
////            public void onFocusChange(View v, boolean hasFocus) {
////                if(hasFocus)
////                {
////                    m_TotalMoney.setText("");
////                }
////            }
////        });
//        RadioGroup rab_payment_group = (RadioGroup)findViewById(R.id.rab_payment_group);
//        rab_payment_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                switch (checkedId)
//                {
//                    case R.id.rab_private:
//                        reciptTarget = 1;
//                        break;
//                    case R.id.rab_company:
//                        reciptTarget = 2;
//                        break;
//                    case R.id.rab_etc:
//                        reciptTarget = 3;
//                        break;
//                }
//            }
//        });
//        RadioGroup rab_payment_group2 = (RadioGroup)findViewById(R.id.rab_payment_group2);
//        rab_payment_group2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                switch (checkedId)
//                {
//                    case R.id.rab_pay_card:
//                        if(Setting.getPreference(Setting.getTopContext(),Constants.SELECTED_DEVICE_CARD_READER).equals(""))
//                        {
//                            payMethod = 4;
//                        }
//                        else
//                        {
//                            payMethod = 1;
//                        }
//
//                        break;
//                    case R.id.rab_pay_pinin:
//                        payMethod = 2;
//                        break;
//                }
//            }
//        });
//
//        m_approval_num = (EditText)findViewById(R.id.edit_approval_num);
//        m_approval_date = (EditText)findViewById(R.id.edit_approval_date);
//        m_cash_num = (EditText)findViewById(R.id.edit_cash_num);
//        //서명패드초기화
//        //mPosSdk.__PosInit("99", mDataListener);
//    }
//
//
//
//    Button.OnClickListener BtnOnClickListener = new Button.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            hideKeyboard();
//            switch (v.getId()) {
//                case R.id.btn_credit:
//                    Credit();
//                    break;
//                case R.id.btn_cash:
//                    reciptCash();
//                    break;
////                case R.id.btn_card_insert:
////                    CardInsert();
////                    break;
////                case R.id.btn_card_delete:
////                    CardDelete();
////                    break;
//                case R.id.btn_card_eraser:
//                    CardEraser();
//                    break;
//                case R.id.btn_main2_exit:
//                    GoMain();
//                    break;
//                default:
//                    break;
//            }
//            return;
//        }
//    };
//    private void hideKeyboard()
//    {
//        View view = this.getCurrentFocus();
//        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(view.getWindowToken(),0);
//    }
////    private void CallSignPadActivity()
////    {
////        Intent intent = new Intent(this,SignPadActivity.class);
////        intent.putExtra("Money",mMoney);
////        startActivityForResult(intent,REQUEST_SIGNPAD);
////    }
//
//    /** 현금 영수증 처리 함수 */
//    private void reciptCash()
//    {
//        ReadyDialog( "현금 영수증 처리중입니다.",30);
//        mPaymentSdk = new PaymentSdk(2,false,mPaymentListener);
//        String tmp = m_cbx_reason_cancel.getSelectedItem().toString();
//        String cancelReson ="";
//        String cancelInfo = "";
//        if(tmp.contains("거래취소"))
//        {
//            cancelReson = "1";
//            cancelInfo +="0";
//        }
//        else if(tmp.contains("오류발급"))
//        {
//            cancelReson = "2";
//            cancelInfo +="0";
//        }
//        else if(tmp.contains("기타"))
//        {
//            cancelReson = "3";
//            cancelInfo +="0";
//        }
//
//        //원승인 번호도 없고 원거래 일자도 없는데 취소를 누를 경우 처리 한다.
//        if(!cancelReson.equals(""))
//        {
//            if(m_approval_date.getText().toString().equals("") || m_approval_num.getText().equals(""))
//            {
//                HideDialog();
//                ShowDialog(getResources().getString(R.string.error_Check_approval_number_date));
//                return;
//            }
//        }
//        if(payMethod==2)
//        {
//            if(!m_cash_num.getText().toString().equals(""))
//            {
//                if(cancelReson.equals("")) {
//                    mPaymentSdk.CashReciptDirectInput(cancelReson, getTID(),"","",
//                            m_cash_num.getText().toString(),TCPCommand.CMD_CASH_RECEIPT_REQ,
//                            "", m_TotalMoney.getText().toString(),"",
//                            "","",String.valueOf(reciptTarget),
//                            "",false);
//                }
//                else
//                {
//                    mPaymentSdk.CashReciptDirectInput(cancelReson,getTID(),
//                            m_approval_date.getText().toString().substring(0, 6),
//                            m_approval_num.getText().toString(),m_cash_num.getText().toString(),
//                            TCPCommand.CMD_CASH_RECEIPT_CANCEL_REQ,"",
//                            m_TotalMoney.getText().toString(),"","",
//                            "",String.valueOf(reciptTarget),
//                            "",false);
//                }
//                return;
//            }
//
//        }
//        else if(payMethod==1 || payMethod==4)
//        {
//            if(mPosSdk.getUsbDevice().size()!=0) {
//                if (cancelReson.equals("")) {
//                    mPaymentSdk.CashRecipt(this, getTID(), Utils.StringAlignrightzero(m_TotalMoney.getText().toString(), 10), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_tax.getText().toString())),
//                            Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_serviceCharge.getText().toString())), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_taxfree.getText().toString())), reciptTarget, "0000", "",
//                            "","", cancelReson, "", "", "", "", "", payMethod);
//                } else {
//                    cancelInfo += m_approval_date.getText().toString().substring(0, 6);
//                    cancelInfo += m_approval_num.getText().toString();
//                    mPaymentSdk.CashRecipt(this, getTID(), Utils.StringAlignrightzero(m_TotalMoney.getText().toString(), 10), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_tax.getText().toString())),
//                            Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_serviceCharge.getText().toString())), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_taxfree.getText().toString())), reciptTarget, "0000", cancelInfo,
//                            m_approval_date.getText().toString(),"", cancelReson, "", "", "", "", "", payMethod);
//                }
//            }
//            else
//            {
//                HideDialog();
//                ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
//            }
//            return;
//        }
//
//        if(mPosSdk.getUsbDevice().size()!=0) {
//            int _getSignPadType = Setting.getSignPadType(mPosSdk.getActivity());
//            if(_getSignPadType==0 || _getSignPadType==3)    //서명패드 사용 안함, 터치서명인 경우
//            {
//                HideDialog();
//                ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
//            }
//            else
//            {
//                if (cancelReson.equals("")) {
//                    mPaymentSdk.CashRecipt(this, getTID(), Utils.StringAlignrightzero(m_TotalMoney.getText().toString(), 10), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_tax.getText().toString())),
//                            Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_serviceCharge.getText().toString())), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_taxfree.getText().toString())), reciptTarget, "0000", "",
//                            "","", cancelReson, "", "", "", "", "", payMethod);
//                } else {
//                    cancelInfo += m_approval_date.getText().toString().substring(0, 6);
//                    cancelInfo += m_approval_num.getText().toString();
//                    mPaymentSdk.CashRecipt(this, getTID(), Utils.StringAlignrightzero(m_TotalMoney.getText().toString(), 10), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_tax.getText().toString())),
//                            Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_serviceCharge.getText().toString())), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_taxfree.getText().toString())), reciptTarget, "0000", cancelInfo,
//                            m_approval_date.getText().toString(),"", cancelReson, "", "", "", "", "", payMethod);
//                }
//            }
//            return;
//
//        }
//        else
//        {
//            HideDialog();
//            ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
//        }
//
//    }
//
//    /**
//     * 신용 결제 함수
//     */
//    private void Credit() {
//
//        ShowDialog("IC를 요청 중입니다.");
////        ReadyDialogShow(this,"IC를 요청 중입니다.",30, true);
//        mPaymentSdk = new PaymentSdk(1,false,mPaymentListener);       //1은 신용 카드
//        String tmp = m_cbx_reason_cancel.getSelectedItem().toString();
//        String cancelInfo = "";
//        if(tmp.contains("거래취소"))
//        {
//            cancelInfo ="0";
//        }
//        else if(tmp.contains("오류발급"))
//        {
//            cancelInfo ="I";
//        }
//        else if(tmp.contains("기타"))
//        {
//            cancelInfo ="a";
//        }
//
//        if(mPosSdk.getUsbDevice().size()!=0) {
//            if(cancelInfo.equals(""))
//            {
//                mPaymentSdk.CreditIC(this,getTID(),Utils.StringAlignrightzero(m_TotalMoney.getText().toString(),10),Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_tax.getText().toString())),
//                        Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_serviceCharge.getText().toString())), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_taxfree.getText().toString())),
//                        m_installment.getText().toString(),"",cancelInfo, "", "", "","0");
//            }
//            else
//            {
//                if(!m_approval_num.getText().toString().equals(""))
//                {
//                    cancelInfo+=m_approval_date.getText().toString().substring(0,6);
//                    cancelInfo+=m_approval_num.getText().toString();
//                    //mPaymentSdk.Req_tcp_Credit(getTID(),null, "",cancelInfo);
//                    mPaymentSdk.CreditIC(this,getTID(),Utils.StringAlignrightzero(m_TotalMoney.getText().toString(),10),Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_tax.getText().toString())),
//                            Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_serviceCharge.getText().toString())), Integer.parseInt(Utils.CheckNullOrEmptyReturnZero(m_taxfree.getText().toString())),
//                            m_installment.getText().toString(),cancelInfo,m_approval_date.getText().toString(), "","","","0");
//                }
//                else
//                {
//                    ShowDialog("원승인번호오류");
//                }
//
//            }
//
//        }
//        else
//        {
//            HideDialog();
//            ShowDialog(getResources().getString(R.string.error_There_are_no_devices_connected_to_this_device));
//        }
//
//    }
//
//
//
//    private SerialInterface.PaymentListener mPaymentListener = new SerialInterface.PaymentListener() {
//        @Override
//        public void result(String result, String Code, HashMap<String,String> resultData)
//        {
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
//                    HideDialog();
//                    if(Code.equals("COMPLETE"))
//                    {
//                        String _result = "";
//                        if(resultData.get("AnsCode").equals("0000"))
//                            _result = "정상승인";
//                        else
//                            _result = "거래실패";
//                        String tmp = "거래확인 : " + _result + "\n" + resultData.get("Message");
//                        ShowDialog(tmp);
//                        m_approval_num.setText(resultData.get("AuNo"));
//                        m_approval_date.setText(resultData.get("date"));
//                        m_cash_num.setText("");
//                        Runtime.getRuntime().gc();
//                    }
//                    else if(Code.equals("COMPLETE_IC"))
//                    {
//                        String _result = "";
//                        if(resultData.get("AnsCode").equals("0000"))
//                            _result = "정상승인";
//                        else
//                            _result = "거래실패";
//                        String tmp = "거래확인 : " + _result + "\n" + resultData.get("Message");
//                        ShowDialog(tmp);
//                        m_approval_num.setText(resultData.get("AuNo"));
//                        m_approval_date.setText(resultData.get("TrdDate"));
//                        m_cash_num.setText("");
//                        Runtime.getRuntime().gc();
//                    }
//                    else if(Code.equals("SHOW"))
//                    {
//                        ReadyDialog(result,0);
//                    }
//                    else
//                    {
//                        ShowDialog(result);
//                        Runtime.getRuntime().gc();
//                    }
//                }
//            });
//
//        }
//    };
//
//    /**
//     * 아직 정확히 이 기능에 알 수 없어서 대략 예상으로 화면 지우기랑 메모리 지우기로 작성 한다.
//     */
//    private void CardEraser() {
//        mPaymentSdk = new PaymentSdk(0,false,mPaymentListener);
//        mPaymentSdk.Clear();
//
//        init();
//        m_TotalMoney.setText("0");
//        m_installment.setText("0");
//        m_approval_num.setText("");
//        m_approval_date.setText("");
//    }
//
//    private void GoMain() {
//
//        Intent intent = new Intent(this, Main2Activity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
//    }
//
//    public void showCardreaderState() {
//        // String tmp = mPosSdk.getCardReaderState()==true?"연결됨":"연결안됨";
//        //   Toast.makeText(m_menuActivity,tmp,Toast.LENGTH_SHORT).show();
//    }
//
//    private void Res_PosCardStatusCheck(Command.ProtocolInfo proto) {
//        if (proto.Command != Command.CMD_IC_STATE_RES) {
//            return;
//        }
//        KByteArray b = new KByteArray(proto.Contents);
//        String state = new String(b.CutToSize(2));
//
//        String tmp = "현재상태 : " + state;
//        ShowDialog(tmp);
//    }
//
//    //카드 넣기 빼기 때문에 있는 함수로 이 부분은 코세스랑 얘기 해서 삭제 하던가 한다.
//    private SerialInterface.DataListener mDataListener = new SerialInterface.DataListener() {
//        @Override
//        public void onReceived(byte[] _rev, int _type) {
//            readresponse(_rev,_type);
//        }
//    };
//    private void readresponse(byte[] _res, int _type)
//    {
//        switch (_res[3])
//        {
//            case Command.CMD_IC_STATE_RES:
//                byte[] rescode = new byte[2];
//                rescode[0] = _res[4];
//                rescode[1] = _res[5];
//                String result = new String(rescode);
//                if(result.equals("00")){
//                    ShowDialog("리더기 내부에 카드가 없습니다.");
//                }
//                else if(result.equals("01"))
//                {
//                    ShowDialog("카드가 리더기 입구에 있습니다");
//                }
//                else if(result.equals("02"))
//                {
//                    ShowDialog("카드가 리더기에 삽입 되어 있습니다.");
//                }
//                else if(result.equals("03"))
//                {
//                    ShowDialog("카드 장애가 발생 하였습니다.");
//                }
//                else
//                {
//                    ShowDialog("알수 없는 상태 입니다.");
//                }
//                break;
//            case Command.ESC:
//                ShowDialog("ESC 발생");
//                break;
//            case Command.ACK:
//                ShowDialog("ACT 수신");
//                break;
//        }
//    }
//
//    private void ReadyDialog(String _str,int _countTimer)
//    {
//        HideDialog();
//        ReadyDialogShow(this,_str,_countTimer);
////        Setting.Showpopup(this,_str,null,null);
//    }
//
//    private void ShowDialog(String _str) {
////        Intent intent = new Intent(this, PopupActivity.class);
////        intent.putExtra("contents", _str);
////        this.startActivity(intent);
//        HideDialog();
//        m_txt_listner_msg.setText("");
//        m_txt_listner_msg.setText(_str);
//    }
//
//    public void HideDialog() {
////        Setting.HidePopup();
//        ReadyDialogHide();
//    }
//
//    /**
//     * 가맹점 설정을 하지 않은 경우에 거래를 할 수 없게 한다.
//     */
//    private void checkTid()
//    {
//        if(getTID().equals(""))
//        {
//            ShowDialog(getResources().getString(R.string.error_cancel_trade));
//            Toast.makeText(mPosSdk.getActivity(), getResources().getString(R.string.error_no_data_tid), Toast.LENGTH_SHORT).show();
//            GoMain();
//            finish();
//        }
//    }
//    private String getTID()
//    {
//        return Setting.getPreference(mPosSdk.getActivity(),Constants.TID);
//    }
}