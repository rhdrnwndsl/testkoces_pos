package com.koces.androidpos.sdk.SerialPort;

/**
 * @brief 시리얼 드라이버 기본 구조
 */

public abstract class SerialBase {
    // Common values
    static final int DATA_BITS_5 = 5;
    static final int DATA_BITS_6 = 6;
    static final int DATA_BITS_7 = 7;
    static final int DATA_BITS_8 = 8;

    static final int STOP_BITS_1 = 1;
    static final int STOP_BITS_15 = 3;
    static final int STOP_BITS_2 = 2;

    static final int PARITY_NONE = 0;
    static final int PARITY_ODD = 1;
    static final int PARITY_EVEN = 2;
    static final int PARITY_MARK = 3;
    static final int PARITY_SPACE = 4;

//    int FLOW_CONTROL_OFF = 0;
//    int FLOW_CONTROL_RTS_CTS= 1;
//    int FLOW_CONTROL_DSR_DTR = 2;
//    int FLOW_CONTROL_XON_XOFF = 3;

    public interface SerialReadCallback
    {
        void onReceivedData(byte[] data,String busName);
    }

    // Common Usb Serial Operations (I/O Asynchronous)
    abstract boolean open();
    abstract void write(byte[] buffer);
    abstract void read_start(SerialReadCallback cb);
    abstract void close();

    abstract void setBaudRate(int baudRate);
    abstract void setDataBits(int dataBits);
    abstract void setStopBits(int stopBits);
    abstract void setParity(int parity);

    /* 국내 단말기 업계에서 이런 고급 기능을 쓰는 데는 없음. (LRC, CRC를 믿고 의존)

    abstract void setFlowControl(int flowControl);

    abstract void setRTS(boolean state);
    abstract void setDTR(boolean state);
    abstract void getCTS(UsbCTSCallback ctsCallback);
    abstract void getDSR(UsbDSRCallback dsrCallback);

    // Status methods
    abstract void getBreak(UsbBreakCallback breakCallback);
    abstract void getFrame(UsbFrameCallback frameCallback);
    abstract void getOverrun(UsbOverrunCallback overrunCallback);
    abstract void getParity(UsbParityCallback parityCallback);
     */
}
