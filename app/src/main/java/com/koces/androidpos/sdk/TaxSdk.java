package com.koces.androidpos.sdk;

import android.widget.TableRow;

import com.koces.androidpos.sdk.db.sqliteDbSdk;
import com.koces.androidpos.sdk.van.Constants;

import java.util.HashMap;

public class TaxSdk {
    private final static String TAG = "TAXSDK";
    private KocesPosSdk mKocesSdk;
    private static TaxSdk Instance;
    private boolean mUseVAT = true;
    private boolean museSVC = false;
    private int mVATmethod = 0; //0: 자동 1:통합
    private int mSVCmethod = 0; //0: 자동 1:수동
    private int mVATInclude = 0; //0: 포함 1:미포함
    private int mSVCInclude = 0; //0: 포함 1:미포함
    private int mVatRate = 10;  //세금 퍼센트
    private int mSvcRate = 0;   //봉사료 퍼센트
    private int mMinInstallmentAmount = 5;   //할부 최소 금액 설정
    private int mNoSignSettingAmount = 5;    //무서명 설정 금액

    private int Auto = 0;
    private int Manual = 0;
    private int Included = 0;
    private int NotIncluded = 0;

    /** define Hash String */
    public static String defVatUse = "vatUse";     //vat 적용
    public static String defVatMode = "vatMode";    //vat 자동,통합
    public static String defVatInclude = "vatInclude";  //부가세 포함,미포함
    public static String defVatRate = "vatRate";        //부가세율
    public static String defSvcUse = "svcUse";      //봉사료 적용
    public static String defSvcMdoe = "svcMode";    //봉사료 자동, 수동
    public static String defSvcInclude = "svcInclude";  //봉사료 포함,미포함
    public static String defSvcRate = "svcRate";    //봉사료율
    public static String defMinInstallMent = "minInstall";  //핧부 최소 금액
    public static String defMinNoSignAmount = "noSign"; //노서명 최소 금액

    private TaxSdk(){
        mKocesSdk = KocesPosSdk.getInstance();
        //DB에서 세금 설정을 읽어서 설정 한다.
        reloadTaxSettingOption();
    }

    public static TaxSdk getInstance(){
        if(Instance==null){
            Instance = new TaxSdk();
        }
        return Instance;
    }

    /**
     * 로컬에 저장되어 있는 세금 화면 설정 데이터를 다시 읽어들인다.
     * 22.01.26 kim.jy
     */
    private void reloadTaxSettingOption(){
        String Tid = Setting.getPreference(mKocesSdk.getActivity(), Constants.TID);
        if(Tid.equals("")){     //Tid가 저장 되어 있지 않다면 세금 설정을 기본으로 한다.
            return;
        }
        HashMap<String,String> info = mKocesSdk.getSqliteDB_TaxSettingInfo(Tid);
        if(info.size()==0){
            return;
        }
        //여기서 디비에 있는 데이터로 초기 설정을 한다.
        mUseVAT = info.get(defVatUse).equals("true")?true:false;
        mVATmethod = info.get(defVatMode).equals("0")?0:1;
        mVATInclude = info.get(defVatInclude).equals("0")?0:1;
        mVatRate = Integer.parseInt(info.get(defVatRate));
        museSVC = info.get(defSvcUse).equals("true")?true:false;
        mSVCmethod = info.get(defSvcMdoe).equals("0")?0:1;
        mSVCInclude = info.get(defSvcInclude).equals("0")?0:1;
        mSvcRate = Integer.parseInt(info.get(defSvcRate));
        mMinInstallmentAmount = Integer.parseInt(info.get(defMinInstallMent));
        mNoSignSettingAmount = Integer.parseInt(info.get(defMinNoSignAmount));
        return;
    }
    public HashMap<String,String> readTaxSettingDB(String Tid){
        HashMap<String,String> info = mKocesSdk.getSqliteDB_TaxSettingInfo(Tid);
        //데이터를 리턴 하지 전에 세금 설정 값을 다시 설정한다.
        if(info.size()==0){
            return info;
        }
        //DB에서 tax설정을 요청 할 적마다 저장 한다.
        mUseVAT = info.get(defVatUse).equals("true")?true:false;
        mVATmethod = info.get(defVatMode).equals("0")?0:1;
        mVATInclude = info.get(defVatInclude).equals("0")?0:1;
        mVatRate = Integer.parseInt(info.get(defVatRate));
        museSVC = info.get(defSvcUse).equals("true")?true:false;
        mSVCmethod = info.get(defSvcMdoe).equals("0")?0:1;
        mSVCInclude = info.get(defSvcInclude).equals("0")?0:1;
        mSvcRate = Integer.parseInt(info.get(defSvcRate));
        mMinInstallmentAmount = Integer.parseInt(info.get(defMinInstallMent));
        mNoSignSettingAmount = Integer.parseInt(info.get(defMinNoSignAmount));
        return info;
    }
    public void reloadTaxSettingOnDB(String Tid){
        HashMap<String,String> info = mKocesSdk.getSqliteDB_TaxSettingInfo(Tid);
    }

    public boolean getUseVAT(){return mUseVAT;}
    public boolean getUseSVC(){return museSVC;}
    public int getVATMode(){return mVATmethod;}
    public int getSVCMode(){return mSVCmethod;}
    public int getVATInclude(){return mVATInclude;}
    public int getSVCInclude(){return mSVCInclude;}
    public int getVATRate(){return mVatRate;}
    public int getSVCRate(){return mSvcRate;}
    public int getMinInstallmentAmount(){return mMinInstallmentAmount;}
    public int getMinNoSignAmount(){return mNoSignSettingAmount;}
    /**
     * 로컬에 세금 화면 설정 데이터를 저장 한다.
     * 22.01.26 kim.jy
     */
    public boolean setTaxSettingOption(String Tid,boolean UseVAT,int AutoVAT,int IncludeVAT,int VATRate,
                                       boolean UseSVC,int AutoSVC,int IncludeSVC,int SVCRate,int minInstallMentAmount,int NoSignAmount){
        mKocesSdk.setSqliteDB_SettingTax(Tid,UseVAT,AutoVAT,IncludeVAT,VATRate,UseSVC,AutoSVC,IncludeSVC,SVCRate,minInstallMentAmount,NoSignAmount);
        return true;
    }

public HashMap<String, Integer> TaxCalc(int _Money,int _TaxFree,int _ServiceCharge,Boolean UsebleOrSerial){
        int err = 0;
        int Money = _Money;
        HashMap<String,Integer> value = new HashMap<>();
        int originalMoney = Money;
        int PayMentMoney = 0;
        int VAT = 0;
        int SVC = 0;
        int TXF = _TaxFree;

        if(UsebleOrSerial){     //ble 또는 시리얼인 경우
            //제일 먼저 처리 할 것은 봉사료를 빼는 일이다.
            if(getUseSVC()){ //봉사료를 사용하는 것을
                if(getSVCMode()==Auto){ //봉사료가 자동인 경우에 비율에 따라서 금액을 처리 한다.
                    Double svcRt = (Double)(mSvcRate / 100.0);
                    SVC = (int)(Money * svcRt);
                    if(getSVCInclude()==Included){ //봉사료 원금 포함의 경우
                        originalMoney = originalMoney - SVC;
                    }
                }else{      //봉사료 수동 입력
                    if(getSVCInclude()==Included){    //봉사료 원금 포함의 경우
                        SVC = _ServiceCharge;
                        originalMoney = originalMoney - SVC;
                    }else{
                        SVC = _ServiceCharge;                    //봉사료 원금 미포함의 경우
                    }
                }
            }

            //세금 계산 부분
            if(getUseVAT()){
                Double vatRt = (Double)(mVatRate / 100.0);
                if(getVATInclude()==Included){
                    VAT =  (int)(originalMoney - (originalMoney / (1.0 + vatRt)));
                    originalMoney = originalMoney - VAT;
                }
                else{   //세금 미포함
                    VAT = (int)(originalMoney * vatRt);
                }
            }
            else{   //세금적용 안함.
                VAT = 0;
            }

            originalMoney = originalMoney + TXF;

            if(getUseSVC() && getSVCInclude()==NotIncluded){  //봉사료 적용, 봉사료 미포함
                //PayMentMoney = PayMentMoney + SVC
            }

            if(getUseVAT() && getVATInclude()==NotIncluded){  //세금 적용,세금 비포함
                //PayMentMoney = PayMentMoney + VAT
            }
        }
        else
        {   // CAT에 따른 계산
            if(getUseSVC()){  //봉사료 적용
            if (getSVCMode()==Auto){         //봉사료 방식이 자동인 경우
                Double svcRt = (Double)(mSvcRate / 100.0);
                SVC = (int)(Money * svcRt);
                if(getSVCInclude()==Included){ //봉사료 원금 포함의 경우
                    originalMoney = originalMoney - SVC;
                }
            }else{          //봉사료 방식이 수동인 경우
                if(getSVCInclude()==Included){   //봉사료가 원금 포함의 경우
                    SVC = _ServiceCharge;
                    originalMoney = originalMoney - SVC;
                }else{
                    //2021-08-17 kim.jy 이완재 과장님과 통화 후에 CAT봉사료 미포함 거래를 추가함
                    SVC = _ServiceCharge;                    //봉사료 원금 미포함의 경우
                }
            }
        }


            if(getUseVAT()){
            Double vatRt = (Double)(mVatRate / 100.0);
            if(getVATInclude()==Included){
                VAT = (int)(originalMoney - (originalMoney / (1.0 + vatRt)));
                originalMoney = originalMoney - VAT;
            }
                else{   //세금 미포함
                VAT = (int)(originalMoney * vatRt);
            }
        }
            else{   //세금적용 안함.
            VAT = 0;
        }

        }
        //value["Money"] = originalMoney + PayMentMoney //CAT에서는 원금에서 공급가액 + 세금 + 봉사료만 처리하고 비과세는 금액만 산정하여 리턴 한다.
        value.put("Money",new Integer(originalMoney));
        value.put("VAT",new Integer(VAT));
        value.put("SVC",new Integer(SVC));
        value.put("TXF",new Integer(TXF));
        value.put("Error",new Integer(err));
        return value;
    }
}
