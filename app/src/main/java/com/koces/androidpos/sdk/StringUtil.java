 package com.koces.androidpos.sdk;

import android.util.Log;

import org.json.JSONObject;
import org.w3c.dom.Node;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;
import static android.text.TextUtils.isEmpty;
 /**
  * 문자열 유틸리티
  *
  */
 public class StringUtil {

     /**
      * 사업자 번호 표기법으로 출력한다. (000-00-00000)
      *
      * @param storeNo 길이 10의 숫자 스트링
      * @return 000-00-00000 포멧으로 반환
      * <p>
      * created by hojun baek on 10/18/2016
      */
     public static String makeStoreNo(String storeNo) {
         if (storeNo.length() == 10) {
             return storeNo.substring(0, 3) + "-" + storeNo.substring(3, 5) + "-" + storeNo.substring(5, 10);
         } else {
             return storeNo;
         }
     }

     public static String getDate() {
         String date;
         Calendar c = Calendar.getInstance();
         int mYear = c.get(Calendar.YEAR);
         int mMonth = c.get(Calendar.MONTH);
         int mDay = c.get(Calendar.DAY_OF_MONTH);

         date = mYear + "";
         date += ((mMonth + 1) + "").length() == 1 ? "0" + (mMonth + 1)
                 : (mMonth + 1);
         date += (mDay + "").length() == 1 ? "0" + mDay : mDay;


         return date;
     }

     public static String safeString(Object orgStr, String convStr) {
         if (orgStr == null) return convStr;
         else return orgStr.toString();
     }

     public static String trim(Object s) {
         if (s == null) return "";
         else return s.toString().trim();
     }

     public static String safeString(String orgStr, String convStr) {
         if (orgStr == null) return convStr;
         else return orgStr;
     }

     /**
      * 대문자로 변경하는 안전 메서드
      * NULL이 들어와도 빈문자열로 반환한다.
      *
      * @param s 대문자로 변환이 필요한 문자
      * @return 대문자 데이터
      */
     public static String safeUpperCase(String s) {
         if (s == null) return "";
         else return s.toUpperCase();
     }

     /**
      * 문자열이 유효 (null이 아니고 공백이 아닐 경우)한지 검사한다.
      *
      * @param src 검사 할 문자열
      * @return 문자열 유효 여부
      */
     public static boolean isNull(String src) {
         return src == null || src.trim().length() == 0 || "null".equalsIgnoreCase(src.trim());
     }

     // 객체(String 또는 Integer)에서 integer 값을 조회하기
     public static int getInt(Object obj) {
         try {
             if (obj == null) return 0;

             if (String.class.isInstance(obj)) {
                 if (((String) obj).trim().length() == 0) return 0;

                 return Integer.parseInt((String) obj);
             } else if (Float.class.isInstance(obj)) {
                 return (int) (((Float) obj).floatValue());
             } else {
                 return ((Integer) obj).intValue();
             }
         } catch (Exception e) {
             return 0;
         }
     }

     // 객체(String 또는 Integer)에서 integer 값을 조회하기
     public static int getInt(String str) {
         if (str == null || str.length() == 0) return 0;

         return Integer.parseInt(str);
     }

     // 객체(String 또는 Integer)에서 String 값을 조회하기
     public static String getString(Object obj) {
         if (obj == null) return "";

         if (String.class.isInstance(obj)) {
             String tmp = (String) obj;
             if (StringUtil.isNull(tmp)) {
                 return "";
             }
             return tmp;
         } else if (Float.class.isInstance(obj)) {
             return "" + ((Float) obj).floatValue();
         } else if (Integer.class.isInstance(obj)) {
             return "" + ((Integer) obj).intValue();
         } else {
             return "" + obj;
         }
     }

     // 객체(String 또는 Integer)에서 String 값을 조회하기
     public static String getString(Object obj, String 기본값) {
         String tmp = getString(obj);
         if ("".equals(tmp)) {
             return 기본값;
         }
         return tmp;
     }

     public static String sqlInjection(String value) {
         if (value == null) return "";

         return value.replaceAll("'", "''").replaceAll(";", "");
     }

     // JSON 에서 Object 값을 빼낸다.
     public static Object getObjectFromJSONObject(JSONObject json, String key) {
         try {
             return json.get(key);
         } catch (Exception e) {
         }
         return null;
     }

     /**
      * JSON 에서 String 값을 빼낸다.
      *
      * @param json
      * @param key
      * @return
      */
     public static String getStringFromJSONObject(JSONObject json, String key) {
         try {
             return json.getString(key);
         } catch (Exception e) {

         }
         return "";
     }

     /**
      * JSON 에서 int 값을 빼낸다.
      *
      * @param json
      * @param key
      * @return
      */
     public static int getIntFromJSONObject(JSONObject json, String key) {
         try {
             return json.getInt(key);
         } catch (Exception e) {
         }
         return 0;
     }

     /**
      * JSON 에서 boolean 값을 빼낸다.
      *
      * @param json
      * @param key
      * @return
      */
     public static boolean getBoolFromJSONObject(JSONObject json, String key) {
         try {
             return json.getBoolean(key);
         } catch (Exception e) {
         }
         return false;
     }

     public static String makeStringWithCommaWithSimbol(Long value) {
         try {
             DecimalFormat format = new DecimalFormat("###,##0");
             String tmpStr = format.format(value);
             return tmpStr + " 원";
         } catch (Exception e) {
         }
         return "";
     }

     public static String makeStringWithComma(int value) {
         try {
             DecimalFormat format = new DecimalFormat("###,##0");
             String tmpStr = format.format(value);

             return tmpStr;
         } catch (Exception e) {
         }
         return "";
     }


     /**
      * 문자열에 통화적용을 위해 컴마를 표기하고 '원'을 붙여 반환한다.
      *
      * @param string 통화적용을 위한 문자열
      * @return 통화적용이 된 문자열
      */
     public static String makeStringWithCommaWithSimbol(String string) {
         String tmpStr = makeStringWithComma(string);
         if (tmpStr.length() == 0) {
             return "0 원";
         } else {
             return tmpStr + " 원";
         }
     }

     /**
      * 문자열에 통화적용을 위해 컴마를 표기한다.
      *
      * @param string 통화적용을 위한 문자열
      * @return 통화적용이 된 문자열
      */
     public static String makeStringWithComma(String string) {
         if (string != null)
             return makeStringWithComma(safeString(string.trim()), false);
         else
             return "";
     }

     /**
      * 콤마를 없앤다.
      *
      * @param source
      * @return
      */
     public static Number parseWonhwaToNumber(String source) {
         DecimalFormat format = new DecimalFormat("###,##0.00");
         Number number = null;
         try {
             number = format.parse(source.trim());
         } catch (ParseException e) {
             try {
                 number = format.parse("0");
             } catch (ParseException e1) {

             }
         }
         return number;
     }

     /**
      * 문자열에 통화적용을 위해 컴마를 표기한다.
      *
      * @param string     통화적용을 위한 문자열
      * @param ignoreZero 값이 0일 경우 공백을 리턴한다.
      * @return 통화적용이 된 문자열
      */
     public static String makeStringWithComma(String string, boolean ignoreZero) {
         if (string.length() == 0) {
             return "";
         }
         try {
             if (string.indexOf(".") >= 0) {
                 double value = Double.parseDouble(string);
                 if (ignoreZero && value == 0) {
                     return "";
                 }
                 DecimalFormat format = new DecimalFormat("###,##0.00");
                 return format.format(value);
             } else {
                 long value = Long.parseLong(string);
                 if (ignoreZero && value == 0) {
                     return "";
                 }
                 DecimalFormat format = new DecimalFormat("###,###");
                 return format.format(value);
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
         return string;
     }

     /**
      * 일자 형태로 변환한다.
      * <p>
      * 일자 적용을 위한 문자열
      *
      * @param shortYear 2자리 연도 여부
      * @return 일자 적용이 된 문자열
      */
     public static String makeDate(Calendar cal, boolean shortYear) {
         String result = "";
         if (cal != null) {
             int year = cal.get(Calendar.YEAR);
             result = "" + (shortYear ? (year % 100) : year);
             int month = cal.get(Calendar.MONTH) + 1;
             if (month < 10) result += "0";
             result += (cal.get(Calendar.MONTH) + 1);
             int day = cal.get(Calendar.DAY_OF_MONTH);
             if (day < 10) result += "0";
             result += cal.get(Calendar.DAY_OF_MONTH);
         }
         return result;
     }

     /**
      * 일자 형태로 변환한다.
      *
      * @param string    일자 적용을 위한 문자열
      * @param shortYear 2자리 연도 여부
      * @return 일자 적용이 된 문자열
      */
     public static String makeDate(String string, boolean shortYear) {
         String result = string;
         if (string.length() == 8) {
             result = shortYear ? string.substring(2, 4) : string
                     .substring(0, 4);
             result += "-";
             result += string.substring(4, 6);
             result += "-";
             result += string.substring(6, 8);
         } else if (string.length() == 16) {
             return makeDate(string.substring(0, 8), false);
         }
         return result;
     }

     /**
      * 시간 형태로 변환한다.
      *
      * @param string 시간 적용을 위한 문자열
      * @return 시간 적용이 된 문자열
      */
     public static String makeTime(String string) {
         String result = string;
         if (string.length() == 6) {
             result = string.substring(0, 2);
             result += ":";
             result += string.substring(2, 4);
             result += ":";
             result += string.substring(4, 6);
         }
         return result;
     }

     /**
      * 안전하게 인티저로 파싱한다.
      *
      * @param string 파싱할 문자열
      * @return 파싱된 인티저
      */
     public static int safeParseInterger(String string) {
         try {
             return Integer.parseInt(string);
         } catch (Exception e) {

         }
         return 0;
     }

     public static int safeParseInterger(Object o) {
         if (o == null) return 0;
         else return StringUtil.safeParseInterger(o.toString());
     }


     /**
      * 안전하게 더블로 파싱한다.
      *
      * @param string 파싱할 문자열
      * @return 파싱된 더블
      */
     public static double safeParseDouble(String string) {
         try {
             return Double.parseDouble(string);
         } catch (Exception e) {

         }
         return 0.0;
     }

     public static double safeParseDouble(Object o) {
         if (o == null) return 0.0;
         else return StringUtil.safeParseDouble(o.toString());
     }


     /**
      * 안전하게 롱으로 파싱한다.
      *
      * @param string 파싱할 문자열
      * @return 파싱된 롱
      */
     public static long safeParseLong(String string) {
         try {
             return Long.parseLong(string);
         } catch (Exception e) {

         }
         return 0;
     }

     public static long safeParseLong(Object string) {
         try {
             return Long.parseLong(string.toString());
         } catch (Exception e) {

         }
         return 0;
     }


     /**
      * 숫자만 있는지 판단한다.
      *
      * @param string 판단할 문자열
      * @return 숫자만 있는지에 대한 여부
      */
     public static boolean isDigit(String string) {
         try {
             if (string.indexOf('+') >= 0 || string.indexOf('-') >= 0
                     || string.indexOf('.') >= 0) {
                 return false;
             }
             Long.parseLong(string);
         } catch (Exception e) {
             return false;
         }
         return true;
     }

     /**
      * 비밀번호 형태로 반환한다.
      *
      * @param string 문자열
      * @return 비밀번호 형태
      */
     public static String toPassword(String string) {
         String ret = "";
         for (int i = 0; i < string.length(); i++) {
             ret += "*";
         }
         return ret;
     }

     /**
      * 지정한 길이의 비밀번호 형태를 만든다.
      *
      * @param length 비밀번호 형태 텍스트의 길이
      * @return 비밀번호 형태
      */
     public static String makePassword(int length) {
         String ret = "";
         for (int i = 0; i < length; i++) {
             ret += "*";
         }
         return ret;
     }

     /**
      * 문자열이 유효(null이 아니고 공백이 아닐 경우)한지 검사한다.
      *
      * @param src 검사 할 문자열
      * @return 문자열 유효 여부
      */
     public static boolean isEmpty(String src) {
         return src == null || src.length() == 0;
     }

     public static boolean isEmptys(String[] src) {
         if (src == null || src.length == 0) {
             return true;
         }

         int strLength = src.length;
         for (int i = 0; i < strLength; i++) {
             if (isEmpty(src[i])) {
                 return true;
             }
         }

         return false;
     }

     public static String safeString(Object str) {
         if (str != null)
             return StringUtil.isEmpty(str.toString()) ? "" : str.toString();
         else
             return "";
     }

     public static String safeString(String str) {
         if (str != null)
             return StringUtil.isEmpty(str) ? "" : str;
         else
             return "";
     }

     /**
      * @return String
      */
     public static String safeNode(Node node) {
         Node tmpNode = node.getFirstChild();

         if (tmpNode == null) return "";
         String tmpNodeVal = tmpNode.getNodeValue();

         return StringUtil.safeString(tmpNodeVal);
     }

     public static int safeNodeInt(Node node) {
         String tmpStr = StringUtil.safeNode(node);

         try {
             return Integer.parseInt(tmpStr.trim());
         } catch (Exception e) {
         }
         return 0;
     }

     /**
      * @param src
      * @param length
      * @return
      */
     public static boolean isEmptyAndLength(String src, int length) {
         return !isEmpty(src) && src.length() == length;
     }

     /**
      * 문자열이 같은지 비교한다.
      *
      * @param string       원본문자열
      * @param compareValue 비교할문자열
      * @return 문자열이 같으면 true
      */
     public static boolean compareString(String string, String compareValue) {
         return !isEmpty(string) && !isEmpty(compareValue)
                 && string.equals(compareValue);
     }

     /**
      * 문자열배열에 해당문자에 있는지 확인한다.
      *
      * @param src           원본문자열
      * @param compareValues 비교할문자열
      * @return
      */
     public static boolean compareString(String src, String[] compareValues) {
         if (!isEmpty(src) && (compareValues != null)) {
             for (String str : compareValues) {
                 if (src.equals(str)) {
                     return true;
                 }
             }
         }
         return false;
     }

     /**
      * 문자열이 숫자인지 검사한다.
      *
      * @param string 검사할 문자열
      * @return 숫자가 아니면 false를 리턴
      */
     public static boolean isDigitString(String string) {
         if (string == null) return false;
         int strLen = string.length();
         if (strLen == 0) return false;

         for (int i = 0; i < strLen; i++) {
             char c = string.charAt(i);
             if (c != ' ' && !Character.isDigit(c)) {
                 return false;
             }
         }
         return true;
     }

     /**
      * 문자열이 한글인지 검사한다.
      *
      * @param string 검사할 문자열
      * @return 한글이 아니면 false를 리턴
      */
     public static boolean isHangul(String string) {
         int strLen = string.length();
         for (int i = 0; i < strLen; i++) {
             char c = string.charAt(i);
             if (c != ' ' && Character.getType(c) != 5) {
                 return false;
             }
         }
         return true;
     }

     /**
      * 안전하게 byte로 파싱한다.
      *
      * @param string 파싱할 문자열
      * @return 파싱된 바이트
      */
     public static byte safeParseByte(String string) {
         if (!isEmpty(string)) {
             return Byte.parseByte(string);
         }
         return 0;
     }

     /**
      * Digit 바이트 배열
      */
     private static final byte BYTES[] = {'0', '1', '2', '3', '4', '5', '6',
             '7', '8', '9'};

     /**
      * 작은따옴표로 감싼 0 ~ 9
      *
      * @param string
      * @return
      */
     public static byte parseByteWrappedSingleQuot(String string) {
         return BYTES[safeParseByte(string)];
     }

     /**
      * 현재 날짜를 얻는다.
      *
      * @param pattern yyyy/MM/dd, yyyyMMdd
      * @return 문자열
      */
     public static String getCurrentDate(String pattern) {
         return new SimpleDateFormat(pattern).format(new Date());
     }

     public static String getCurrentDateType01() {
         Calendar cal = new GregorianCalendar();
         int mYear = cal.get(Calendar.YEAR);
         int mMonth = cal.get(Calendar.MONTH);
         int mDay = cal.get(Calendar.DAY_OF_MONTH);
         int mHour = cal.get(Calendar.HOUR_OF_DAY);
         //		int mMinute = cal.get(Calendar.MINUTE);
         //		int mSecond = cal.get(Calendar.SECOND);

         String monthStr = "" + mDay;
         String dayStr = "" + mHour;
         try {
             DecimalFormat format = new DecimalFormat("00");
             monthStr = format.format(mMonth + 1);
             dayStr = format.format(mDay);
         } catch (Exception e) {
         }

         return "(" + mYear + "." + monthStr + "." + dayStr + ")";
     }

     public static String getCurrentDateTime() {
         Calendar cal = new GregorianCalendar();
         int mYear = cal.get(Calendar.YEAR);
         int mMonth = cal.get(Calendar.MONTH);
         int mDay = cal.get(Calendar.DAY_OF_MONTH);
         int mHour = cal.get(Calendar.HOUR_OF_DAY);
         int mMinute = cal.get(Calendar.MINUTE);
         int mSecond = cal.get(Calendar.SECOND);

         String monthStr = "" + mDay;
         String dayStr = "" + mHour;
         try {
             DecimalFormat format = new DecimalFormat("00");
             monthStr = format.format(mMonth + 1);
             dayStr = format.format(mDay);
         } catch (Exception e) {
         }

         return mYear + monthStr + dayStr + " " +
                 ((mHour < 10) ? "0" + mHour : mHour) +
                 ":" +
                 ((mMinute < 10) ? "0" + mMinute : mMinute);
     }

     public static String getCurrentDateTime02() {
         Calendar cal = new GregorianCalendar();
         int mYear = cal.get(Calendar.YEAR);
         int mMonth = cal.get(Calendar.MONTH);
         int mDay = cal.get(Calendar.DAY_OF_MONTH);
         int mHour = cal.get(Calendar.HOUR_OF_DAY);
         int mMinute = cal.get(Calendar.MINUTE);
         int mSecond = cal.get(Calendar.SECOND);

         String monthStr = "" + mDay;
         String dayStr = "" + mHour;
         try {
             DecimalFormat format = new DecimalFormat("00");
             monthStr = format.format(mMonth + 1);
             dayStr = format.format(mDay);
         } catch (Exception e) {
         }

         return mYear + "/" + monthStr + "/" + dayStr + " " +
                 ((mHour < 10) ? "0" + mHour : mHour) +
                 ":" +
                 ((mMinute < 10) ? "0" + mMinute : mMinute);
     }

     public static String getCurrentDateNTime() {
         Calendar cal = new GregorianCalendar();
         //		int mYear = cal.get(Calendar.YEAR);
         int mMonth = cal.get(Calendar.MONTH);
         int mDay = cal.get(Calendar.DAY_OF_MONTH);
         int mHour = cal.get(Calendar.HOUR_OF_DAY);
         int mMinute = cal.get(Calendar.MINUTE);
         int mSecond = cal.get(Calendar.SECOND);

         return (mMonth + 1) + "-" + mDay + " " + mHour + ":" + mMinute + ":" + mSecond;
     }

     public static String getPreviousMonth() {
         Calendar cal = Calendar.getInstance();
         cal.setTime(new Date());
         cal.add(Calendar.MONTH, -1);
         SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMM");
         return simpleDateFormat.format(cal.getTime()).toString();
     }

     public static String getNextMonth(String pattern) {
         Calendar cal = Calendar.getInstance();
         cal.setTime(new Date());
         cal.add(Calendar.MONTH, +1);
         SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
         return simpleDateFormat.format(cal.getTime()).toString();
     }

     public static String getNextMonth(Date date, String pattern) {
         Calendar cal = Calendar.getInstance();
         cal.setTime(date);
         cal.add(Calendar.MONTH, +1);
         SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
         return simpleDateFormat.format(cal.getTime()).toString();
     }

     public static String getPreviousMonth(String pattern) {
         Calendar cal = Calendar.getInstance();
         cal.setTime(new Date());
         cal.add(Calendar.MONTH, -1);
         SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
         return simpleDateFormat.format(cal.getTime()).toString();
     }

     public static String getPreviousMonth(Date date, String pattern) {
         Calendar cal = Calendar.getInstance();
         cal.setTime(date);
         cal.add(Calendar.MONTH, -1);
         SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
         return simpleDateFormat.format(cal.getTime()).toString();
     }

     /*
      * 일주일 전 날짜를 얻는다.
      */
     public static String getPreviousWeek() {
         Calendar cal = Calendar.getInstance();
         cal.setTime(new Date());
         cal.add(Calendar.DATE, -7);
         SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMM");
         return simpleDateFormat.format(cal.getTime()).toString();
     }

     /**
      * 현재 날짜를 얻는다.
      *
      * @return 문자열
      */
     public static String getCurrentDate() {
         return getCurrentDate("yyyyMMdd");
     }

     /**
      * String타입을 Date로 파싱한다.
      *
      * @param pattern yyyy/MM/dd, yyyyMMdd
      * @param sDate
      * @return
      */
     public static Date parseDate(String pattern, String sDate) {
         Date date = null;
         try {
             date = new SimpleDateFormat(pattern).parse(sDate);
         } catch (ParseException e) {
             e.printStackTrace();
         }
         return date;
     }

     /**
      * 날짜를 얻는다.
      *
      * @param dateTime yyyyMMddHHmmss
      * @return yy. MM. dd.
      */
     public static String displayDate(String dateTime) {
         String ret = "";
         if (dateTime.length() > 8)
             ret = dateTime.substring(2, 4) + ". " + dateTime.substring(4, 6) + ". " + dateTime.substring(6, 8) + ".";

         return ret;
     }

     /**
      * 시간을 얻는다
      *
      * @param dateTime yyyyMMddHHmmss
      * @return HH:mm
      */
     public static String displayTime(String dateTime) {
         String ret = "";
         if (dateTime.length() >= 14)
             ret = dateTime.substring(8, 10) + ":" + dateTime.substring(10, 12);
         return ret;
     }

     /**
      * 전일 날짜를 얻는다.
      *
      * @param date yyyy/MM/dd, yyyyMMdd
      * @return
      */
     public static String getPreviousDate(Date date) {
         Calendar cal = Calendar.getInstance();
         cal.setTime(date);
         cal.add(Calendar.DATE, -1);
         SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
         return simpleDateFormat.format(cal.getTime()).toString();
     }

     /*
      * 몇일 전 날짜를 얻는다.
      *
      * @param data yyyy/MM/dd, int
      */
     public static String getPreviousDate(Date date, int priv) {
         Calendar cal = Calendar.getInstance();
         cal.setTime(date);
         cal.add(Calendar.DATE, -priv);
         SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
         return simpleDateFormat.format(cal.getTime()).toString();
     }

     @SuppressWarnings("finally")
     public static DateItem validDate(String sFromDate, String sToDate) {
         DateItem dateItem = new DateItem();
         try {
             SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

             Date fromDate = null;
             Date toDate = null;

             fromDate = simpleDateFormat.parse(sFromDate);
             toDate = simpleDateFormat.parse(sToDate);

             Calendar nextThreeMonth = Calendar.getInstance();
             Calendar fromCalendar = Calendar.getInstance();
             Calendar toCalendar = Calendar.getInstance();

             nextThreeMonth.setTime(fromDate);
             nextThreeMonth.add(Calendar.MONTH, +3);
             fromCalendar.setTime(fromDate);
             toCalendar.setTime(toDate);

             long limitedDay = nextThreeMonth.getTimeInMillis()
                     - fromCalendar.getTimeInMillis();
             long includedDay = toCalendar.getTimeInMillis()
                     - fromCalendar.getTimeInMillis();
             long excludedYear = toCalendar.get(Calendar.YEAR)
                     - fromCalendar.get(Calendar.YEAR);

             if (limitedDay <= includedDay || excludedYear > 0) {
                 dateItem.errMsg = "3개월을 초과 할 수 없습니다.";
                 dateItem.isValid = false;
                 return dateItem;
             }

             if (includedDay < 0 || excludedYear < 0) {
                 dateItem.errMsg = "시작날짜보다 작을 수 없습니다.";
                 dateItem.isValid = false;
                 return dateItem;
             }

             dateItem.errMsg = "성공";
             dateItem.isValid = true;
         } catch (ParseException e) {
             dateItem.errMsg = "ParseException";
             dateItem.isValid = false;
             Log.e("StringUtil", e.getMessage());
         } finally {
             return dateItem;
         }
     }

     @SuppressWarnings("finally")
     public static String getThreeMonthPrivDate() {
         String privDate = null;

         SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

         Date fromDate = null;
         Date toDate = null;

         //	fromDate = simpleDateFormat.parse(getCurrentDate());

         // toDate = simpleDateFormat.parse(sToDate);

         Calendar privThreeMonth = Calendar.getInstance();
         Calendar fromCalendar = Calendar.getInstance();
         Calendar toCalendar = Calendar.getInstance();

         //		privThreeMonth.setTime(fromDate);
         privThreeMonth.add(Calendar.MONTH, -3);

         privDate = simpleDateFormat.format(privThreeMonth.getTime());

         return privDate;

     }

     public static String getLastDay() {
         GregorianCalendar today = new GregorianCalendar();
         int maxday = today.getActualMaximum((today.DAY_OF_MONTH));
         return Integer.valueOf(maxday).toString();
     }


     public static class DateItem {
         public boolean isValid;
         public String errMsg;
     }

     /**
      * 소수점 이하 절삭
      *
      * @param d
      * @return
      */
     public static String trunc(double d) {
         return new BigDecimal(d).setScale(0, BigDecimal.ROUND_FLOOR).toString();
     }

     public static String twoPointDouble(double d) {
         String pattern = "####.##";
         DecimalFormat df = new DecimalFormat(pattern);
         return df.format(d);
     }

     /**
      * @param stringBuilder
      * @param length
      * @return
      */
     public static String makeSpace(StringBuilder stringBuilder, int length) {
         int _length = length - stringBuilder.length();
         for (int i = 0; i < _length; i++) {
             stringBuilder.append(" ");
         }
         return stringBuilder.toString();
     }

     /**
      * @param length
      * @return
      */
     public static String makeSpace(int length) {
         StringBuilder stringBuilder = new StringBuilder();
         for (int i = 0; i < length; i++) {
             stringBuilder.append(" ");
         }
         return stringBuilder.toString();
     }

     public static String checkMinus(String val) {
         //	double ret=0;
         String retStr = val.trim();
         int pos;
         pos = retStr.lastIndexOf("-");
         if (pos > 0) {
             retStr = "-" + retStr.substring(0, pos);
         }
         return retStr;
     }

     public static String makeRateType(Double val) {
         DecimalFormat format = new DecimalFormat("###,#");
         return format.format(val);

     }

     /**
      * <pre>
      * 정규표현식을 이용하여 메일을 검사한다.
      *
      * [_a-z0-9-]+ : '_', '-', 영어소문자, 숫자가 1개 이상
      * (.[_a-z0-9-]+)* : '.', '_', '-', 영어소문자, 숫자가 1개 이상 ( 입력시에만 평가한다. )
      * @[a-z]+ : '@', 영어소문자가 1개 이상
      * .[a-z]+ : '.', 영어소문자가 1개 이상 ( .com 형태는 반드시 있어야 함 )
      * (.[a-z]+)* : '.', 영어소문자가 1개 이상 ( .co.kr 같은 형태가 있을 수 있으므로 )
      * </pre>
      *
      * @param email
      * @return
      */
     public static boolean validateEmail(String email) {
         Pattern p = Pattern.compile("^[_a-z0-9-]+(.[_a-z0-9-]+)*@[a-z]+.[a-z]+(.[a-z]+)*$");
         return p.matcher(email).find();
     }

     /**
      * 주민번호를 000000-******* 형태로 보여준다.
      *
      * @param jumin
      * @return
      */
     public static String getSecretJumin(String jumin) {
         StringBuilder result = new StringBuilder();
         if (StringUtil.isEmptyAndLength(jumin, 13)) {
             result.append(jumin.substring(0, 6)).append("-").append("*******");
         } else {
             result.append(jumin);
         }
         return result.toString();
     }

     /**
      * 카드번호를 1234-****-****-1234 형태로 보여준다.
      *
      * @return
      */
     public static String getSecretCardNo(String cardNo) {
         StringBuilder result = new StringBuilder();

         if (StringUtil.isEmptyAndLength(cardNo, 16)) {
             result.append(cardNo.substring(0, 4)).append("-****-****-").append(cardNo.substring(12));
         } else {
             result.append(cardNo);
         }
         return result.toString();
     }


     /**
      * 카드번호를 1234-1234-1234-1234 형태로 보여준다.
      *
      * @return
      */
     public static String getCardTypeString(String orgNo) {
         String cardNo = "";

         if (orgNo.length() > 15) {
             cardNo = orgNo.substring(0, 4) + "-" + orgNo.substring(4, 8) + "-"
                     + orgNo.substring(8, 12) + "-" + orgNo.substring(12, 16);
         } else {

         }
         return cardNo;
     }

     /**
      * 금액으로 사용할 string에 ',' '원' 이 붙은경우 삭제후 리턴
      *
      * @param
      * @return
      */
     public static String initMoneyStr(String cost) {
         cost = cost.replace(",", "");
         cost = cost.replace("원", "");
         return cost;
     }

     public static String 자릿수맞추기(String strVal, String pattern) {
         int val = getInt(strVal);
         DecimalFormat format = new DecimalFormat(pattern);
         return format.format(val);
     }

     public static boolean lengthMerchantData(String mch) {

         boolean result = false;

         if (mch != null) {
             try {
                 if (mch.getBytes("EUC-KR").length <= 20) {
                     result = true;
                 } else {
                     result = false;
                 }
             } catch (Exception e) {
                 e.printStackTrace();
             }
         } else {
             result = false;
         }

         return result;
     }

     //     public static String SubStr(String str, int iFirst, int iLen) {
//         String strResult = "";
//         if (iFirst >= 0 && iLen >= 0) {
//             strResult = rightPad(str, " ", iFirst + iLen);
//             strResult = strResult.substring(iFirst, iFirst + iLen);
//         } else {
//             strResult = str;
//         }
//
//         return strResult;
//     }
//
//     public static String rightPad(String str, String fillChar, int length) {
//         if (fillChar.length() != 1) {
//             return "";
//         } else if (str.length() > length) {
//             return str;
//         } else {
//             String returnStr = str;
//
//             for(int i = str.length(); i < length; ++i) {
//                 returnStr += fillChar;
//             }
//
//             return returnStr;
//         }
//     }

     public static String repeat(char ch, int repeat) {
         if (repeat <= 0) {
             return "";
         } else {
             char[] buf = new char[repeat];

             for (int i = repeat - 1; i >= 0; --i) {
                 buf[i] = ch;
             }

             return new String(buf);
         }
     }

     public static String leftPadChar(String str, int size, char padChar) {
         if (str == null) {
             return null;
         } else {
             int pads = size - str.length();
             if (pads <= 0) {
                 return str;
             } else {
                 return pads > 8192 ? leftPad(str, String.valueOf(padChar), size) : repeat(padChar, pads).concat(str);
             }
         }
     }

     public static String leftPad(String str, String padStr, int size) {
         if (str == null) {
             return null;
         } else {
             if (isEmpty(padStr)) {
                 padStr = " ";
             }

             int padLen = padStr.length();
             int strLen = str.length();
             int pads = size - strLen;
             if (pads <= 0) {
                 return str;
             } else if (padLen == 1 && pads <= 8192) {
                 return leftPadChar(str, size, padStr.charAt(0));
             } else if (pads == padLen) {
                 return padStr.concat(str);
             } else if (pads < padLen) {
                 return padStr.substring(0, pads).concat(str);
             } else {
                 char[] padding = new char[pads];
                 char[] padChars = padStr.toCharArray();

                 for (int i = 0; i < pads; ++i) {
                     padding[i] = padChars[i % padLen];
                 }

                 return (new String(padding)).concat(str);
             }
         }
     }

     public static String rightPadChar(String str, int size, char padChar) {
         if (str == null) {
             return null;
         } else {
             int pads = size - str.length();
             if (pads <= 0) {
                 return str;
             } else {
                 return pads > 8192 ? rightPad(str, String.valueOf(padChar), size) : str.concat(repeat(padChar, pads));
             }
         }
     }

     public static String rightPad(String str, String padStr, int size) {
         if (str == null) {
             return null;
         } else {
             if (isEmpty(padStr)) {
                 padStr = " ";
             }

             int padLen = padStr.length();
             int strLen = str.length();
             int pads = size - strLen;
             if (pads <= 0) {
                 return str;
             } else if (padLen == 1 && pads <= 8192) {
                 return rightPadChar(str, size, padStr.charAt(0));
             } else if (pads == padLen) {
                 return str.concat(padStr);
             } else if (pads < padLen) {
                 return str.concat(padStr.substring(0, pads));
             } else {
                 char[] padding = new char[pads];
                 char[] padChars = padStr.toCharArray();

                 for (int i = 0; i < pads; ++i) {
                     padding[i] = padChars[i % padLen];
                 }

                 return str.concat(new String(padding));
             }
         }
     }
 }