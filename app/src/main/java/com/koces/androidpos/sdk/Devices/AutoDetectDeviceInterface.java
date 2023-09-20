package com.koces.androidpos.sdk.Devices;

/**
 * 장치 검색 응답 클래스
 */
public class AutoDetectDeviceInterface {
    /**
     * 장치 검색 응답 인터페이스
     */
    public interface DataListener {
        /**
         *  장치 응답 관련 리시브 함수
         * @param State 상태
         * @param _DeviceCount 장치 카운트
         * @param _Evt 이벤트 문자열
         */
        void onReceived(boolean State,int _DeviceCount,String _Evt);
    }
}