package com.koces.androidpos.sdk.SerialPort;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.koces.androidpos.sdk.Command;
import com.koces.androidpos.sdk.Utils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class FTDISerial extends SerialBase {
    private final static String TAG = "FTDISerial";
    private static final int FTDI_SIO_RESET = 0;
    private static final int FTDI_SIO_MODEM_CTRL = 1;
    private static final int FTDI_SIO_SET_FLOW_CTRL = 2;
    private static final int FTDI_SIO_SET_BAUD_RATE = 3;
    private static final int FTDI_SIO_SET_DATA = 4;

    private static final int FTDI_REQTYPE_HOST2DEVICE = 0x40;

    private static final int FTDI_SIO_SET_DTR_MASK = 0x1;
    private static final int FTDI_SIO_SET_DTR_HIGH = (1 | (FTDI_SIO_SET_DTR_MASK << 8));
    private static final int FTDI_SIO_SET_DTR_LOW = (0 | (FTDI_SIO_SET_DTR_MASK << 8));
    private static final int FTDI_SIO_SET_RTS_MASK = 0x2;
    private static final int FTDI_SIO_SET_RTS_HIGH = (2 | (FTDI_SIO_SET_RTS_MASK << 8));
    private static final int FTDI_SIO_SET_RTS_LOW = (0 | (FTDI_SIO_SET_RTS_MASK << 8));

    public static final int FTDI_BAUDRATE_300 = 0x2710;
    public static final int FTDI_BAUDRATE_600 = 0x1388;
    public static final int FTDI_BAUDRATE_1200 = 0x09c4;
    public static final int FTDI_BAUDRATE_2400 = 0x04e2;
    public static final int FTDI_BAUDRATE_4800 = 0x0271;
    public static final int FTDI_BAUDRATE_9600 = 0x4138;
    public static final int FTDI_BAUDRATE_19200 = 0x809c;
    public static final int FTDI_BAUDRATE_38400 = 0xc04e;
    public static final int FTDI_BAUDRATE_57600 = 0x0034;
    public static final int FTDI_BAUDRATE_115200 = 0x001a;
    public static final int FTDI_BAUDRATE_230400 = 0x000d;
    public static final int FTDI_BAUDRATE_460800 = 0x4006;
    public static final int FTDI_BAUDRATE_921600 = 0x8003;

    /***
     *  Default Serial Configuration
     *  Baud rate: 9600
     *  Data bits: 8
     *  Stop bits: 1
     *  Parity: None
     *  Flow Control: Off
     */
    private static final int FTDI_SET_DATA_DEFAULT = 0x0008;
    private static final int FTDI_SET_MODEM_CTRL_DEFAULT1 = 0x0101;
    private static final int FTDI_SET_MODEM_CTRL_DEFAULT2 = 0x0202;
    private static final int FTDI_SET_MODEM_CTRL_DEFAULT3 = 0x0100;
    private static final int FTDI_SET_MODEM_CTRL_DEFAULT4 = 0x0200;
    private static final int FTDI_SET_FLOW_CTRL_DEFAULT = 0x0000;

    static final int USB_TIMEOUT = 5000;

    private int currentSioSetData = 0x0000;

    UsbDevice device;
    UsbDeviceConnection connection;

    private UsbInterface mInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;
    private UsbRequest requestIN;
    protected WorkerThread workerThread;
    protected WriteThread writeThread;

    protected SerialBuffer serialBuffer;

    FTDIUtilities ftdiUtilities;

    public FTDISerial(UsbDevice device, UsbDeviceConnection connection) {
        this.device = device;
        this.connection = connection;
        mInterface = device.getInterface(0);
        serialBuffer = new SerialBuffer(true);
        ftdiUtilities = new FTDIUtilities();
    }

    public boolean open() {
        boolean ret = openFTDI();

        if (ret) {
            // Initialize UsbRequest
            requestIN = new UsbRequest();
            requestIN.initialize(connection, inEndpoint);

            // Restart the working thread if it has been killed before and  get and claim interface
            restartWorkingThread();
            restartWriteThread();

            // Pass references to the threads
            setThreadsParams(requestIN, outEndpoint);

            return true;
        } else {
            return false;
        }
    }

    SerialReadCallback readCallback = null;

    public void read_start(SerialReadCallback cb) {
        readCallback = cb;
        workerThread.getUsbRequest().queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
    }

    public void close() {
        setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT3, 0, null);
        setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT4, 0, null);
        currentSioSetData = 0x0000;
        killWorkingThread();
        killWriteThread();
        connection.releaseInterface(mInterface);
    }

    public void write(byte[] buffer) {
        serialBuffer.putWriteBuffer(buffer);
    }


    public void setBaudRate(int baudRate) {
        int value = 0;
        if (baudRate >= 0 && baudRate <= 300)
            value = FTDI_BAUDRATE_300;
        else if (baudRate > 300 && baudRate <= 600)
            value = FTDI_BAUDRATE_600;
        else if (baudRate > 600 && baudRate <= 1200)
            value = FTDI_BAUDRATE_1200;
        else if (baudRate > 1200 && baudRate <= 2400)
            value = FTDI_BAUDRATE_2400;
        else if (baudRate > 2400 && baudRate <= 4800)
            value = FTDI_BAUDRATE_4800;
        else if (baudRate > 4800 && baudRate <= 9600)
            value = FTDI_BAUDRATE_9600;
        else if (baudRate > 9600 && baudRate <= 19200)
            value = FTDI_BAUDRATE_19200;
        else if (baudRate > 19200 && baudRate <= 38400)
            value = FTDI_BAUDRATE_38400;
        else if (baudRate > 19200 && baudRate <= 57600)
            value = FTDI_BAUDRATE_57600;
        else if (baudRate > 57600 && baudRate <= 115200)
            value = FTDI_BAUDRATE_115200;
        else if (baudRate > 115200 && baudRate <= 230400)
            value = FTDI_BAUDRATE_230400;
        else if (baudRate > 230400 && baudRate <= 460800)
            value = FTDI_BAUDRATE_460800;
        else if (baudRate > 460800 && baudRate <= 921600)
            value = FTDI_BAUDRATE_921600;
        else if (baudRate > 921600)
            value = FTDI_BAUDRATE_921600;
        else
            value = FTDI_BAUDRATE_9600;
        setControlCommand(FTDI_SIO_SET_BAUD_RATE, value, 0, null);
    }

    public void setDataBits(int dataBits) {
        switch (dataBits) {
            case SerialBase.DATA_BITS_5:
                currentSioSetData |= 1;
                currentSioSetData &= ~(1 << 1);
                currentSioSetData |= (1 << 2);
                currentSioSetData &= ~(1 << 3);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
            case SerialBase.DATA_BITS_6:
                currentSioSetData &= ~1;
                currentSioSetData |= (1 << 1);
                currentSioSetData |= (1 << 2);
                currentSioSetData &= ~(1 << 3);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
            case SerialBase.DATA_BITS_7:
                currentSioSetData |= 1;
                currentSioSetData |= (1 << 1);
                currentSioSetData |= (1 << 2);
                currentSioSetData &= ~(1 << 3);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
            case SerialBase.DATA_BITS_8:
            default:
                currentSioSetData &= ~1;
                currentSioSetData &= ~(1 << 1);
                currentSioSetData &= ~(1 << 2);
                currentSioSetData |= (1 << 3);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
        }
    }

    public void setStopBits(int stopBits) {
        switch (stopBits) {
            case SerialBase.STOP_BITS_1:
            case SerialBase.STOP_BITS_15:
                currentSioSetData |= (1 << 11);
                currentSioSetData &= ~(1 << 12);
                currentSioSetData &= ~(1 << 13);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
            case SerialBase.STOP_BITS_2:
                currentSioSetData &= ~(1 << 11);
                currentSioSetData |= (1 << 12);
                currentSioSetData &= ~(1 << 13);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
            default:
                currentSioSetData &= ~(1 << 11);
                currentSioSetData &= ~(1 << 12);
                currentSioSetData &= ~(1 << 13);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
        }
    }

    public void setParity(int parity) {
        switch (parity) {
            case SerialBase.PARITY_NONE:
            case SerialBase.PARITY_ODD:
                currentSioSetData |= (1 << 8);
                currentSioSetData &= ~(1 << 9);
                currentSioSetData &= ~(1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
            case SerialBase.PARITY_EVEN:
                currentSioSetData &= ~(1 << 8);
                currentSioSetData |= (1 << 9);
                currentSioSetData &= ~(1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
            case SerialBase.PARITY_MARK:
                currentSioSetData |= (1 << 8);
                currentSioSetData |= (1 << 9);
                currentSioSetData &= ~(1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
            case SerialBase.PARITY_SPACE:
                currentSioSetData &= ~(1 << 8);
                currentSioSetData &= ~(1 << 9);
                currentSioSetData |= (1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
            default:
                currentSioSetData &= ~(1 << 8);
                currentSioSetData &= ~(1 << 9);
                currentSioSetData &= ~(1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
                break;
        }

    }

    private boolean openFTDI() {
        if (connection.claimInterface(mInterface, true)) {
            Log.i("FTDISerial", "Interface succesfully claimed");
        } else {
            Log.i("FTDISerial", "Interface could not be claimed");
            return false;
        }

        // Assign endpoints
        int numberEndpoints = mInterface.getEndpointCount();
        for (int i = 0; i <= numberEndpoints - 1; i++) {
            UsbEndpoint endpoint = mInterface.getEndpoint(i);
            if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                inEndpoint = endpoint;
            } else {
                outEndpoint = endpoint;
            }
        }

        // Default Setup
        if (setControlCommand(FTDI_SIO_RESET, 0x00, 0, null) < 0)
            return false;
        if (setControlCommand(FTDI_SIO_SET_DATA, FTDI_SET_DATA_DEFAULT, 0, null) < 0)
            return false;
        currentSioSetData = FTDI_SET_DATA_DEFAULT;
        if (setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT1, 0, null) < 0)
            return false;
        if (setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT2, 0, null) < 0)
            return false;
        if (setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, 0, null) < 0)
            return false;
        if (setControlCommand(FTDI_SIO_SET_BAUD_RATE, FTDI_BAUDRATE_115200, 0, null) < 0)
            return false;

        // Flow control disabled by default
        // rtsCtsEnabled = false;
        // dtrDsrEnabled = false;

        return true;
    }

    private int setControlCommand(int request, int value, int index, byte[] data) {
        int dataLength = 0;
        if (data != null) {
            dataLength = data.length;
        }
        int response = connection.controlTransfer(FTDI_REQTYPE_HOST2DEVICE, request, value, mInterface.getId() + 1 + index, data, dataLength, USB_TIMEOUT);
        Log.i("FTDISerial", "Control Transfer Response: " + String.valueOf(response));
        return response;
    }


    protected void setThreadsParams(UsbRequest request, UsbEndpoint endpoint) {
        workerThread.setUsbRequest(request);
        writeThread.setUsbEndpoint(endpoint);
    }

    protected class WorkerThread extends Thread {
        private FTDISerial usbSerialDevice;

        private UsbRequest requestIN;
        private AtomicBoolean working;

        public WorkerThread(FTDISerial usbSerialDevice) {
            this.usbSerialDevice = usbSerialDevice;
            working = new AtomicBoolean(true);
        }

        @Override
        public void run() {
            while (working.get()) {
                UsbRequest request = connection.requestWait();
                if (request != null && request.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                        && request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN) {
                    byte[] data = serialBuffer.getDataReceived();

                    // FTDI devices reserves two first bytes of an IN endpoint with info about
                    // modem and Line.
                    //usbSerialDevice.ftdiUtilities.checkModemStatus(data); //Check the Modem status

                    serialBuffer.clearReadBuffer();

                    if (data.length > 2) {
                        final byte[] data1 = usbSerialDevice.ftdiUtilities.adaptArray(data);

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            public void run() {
                                if (readCallback != null) {
                                    readCallback.onReceivedData(data1,usbSerialDevice.device.getDeviceName());
                                }
                            }
                        });

                    }

                    requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
                }
            }
        }

        public void setUsbRequest(UsbRequest request) {
            this.requestIN = request;
        }

        public UsbRequest getUsbRequest() {
            return requestIN;
        }

        public void stopWorkingThread() {
            working.set(false);
        }


    }

    protected class WriteThread extends Thread {
        private UsbEndpoint outEndpoint;
        private AtomicBoolean working;

        public WriteThread() {
            working = new AtomicBoolean(true);
        }

        @Override
        public void run() {
            while (working.get()) {
                byte[] data = serialBuffer.getWriteBuffer();
                connection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);
            }
        }

        public void setUsbEndpoint(UsbEndpoint outEndpoint) {
            this.outEndpoint = outEndpoint;
        }

        public void stopWriteThread() {
            working.set(false);
        }
    }

    public class FTDIUtilities {
        // Special treatment needed to FTDI devices
        public byte[] adaptArray(byte[] ftdiData) {
            int length = ftdiData.length;
            if (length > 64) {
                int n = 1;
                int p = 64;
                // Precalculate length without FTDI headers
                while (p < length) {
                    n++;
                    p = n * 64;
                }
                int realLength = length - n * 2;
                byte[] data = new byte[realLength];
                copyData(ftdiData, data);
                return data;
            } else {
                return Arrays.copyOfRange(ftdiData, 2, length);
            }
        }

        public void checkModemStatus(byte[] data) {
            if (data.length == 0) // Safeguard for zero length arrays
                return;

            boolean cts = (data[0] & 0x10) == 0x10;
            boolean dsr = (data[0] & 0x20) == 0x20;

//            if(firstTime) // First modem status received
//            {
//                ctsState = cts;
//                dsrState = dsr;
//
//                if(rtsCtsEnabled && ctsCallback != null)
//                    ctsCallback.onCTSChanged(ctsState);
//
//                if(dtrDsrEnabled && dsrCallback != null)
//                    dsrCallback.onDSRChanged(dsrState);
//
//                firstTime = false;
//                return;
//            }
//
//            if(rtsCtsEnabled &&
//                    cts != ctsState && ctsCallback != null) //CTS
//            {
//                ctsState = !ctsState;
//                ctsCallback.onCTSChanged(ctsState);
//            }
//
//            if(dtrDsrEnabled &&
//                    dsr != dsrState && dsrCallback != null) //DSR
//            {
//                dsrState = !dsrState;
//                dsrCallback.onDSRChanged(dsrState);
//            }
//
//            if(parityCallback != null) // Parity error checking
//            {
//                if((data[1] & 0x04) == 0x04)
//                {
//                    parityCallback.onParityError();
//                }
//            }
//
//            if(frameCallback != null) // Frame error checking
//            {
//                if((data[1] & 0x08) == 0x08)
//                {
//                    frameCallback.onFramingError();
//                }
//            }
//
//            if(overrunCallback != null) // Overrun error checking
//            {
//                if((data[1] & 0x02) == 0x02)
//                {
//                    overrunCallback.onOverrunError();
//                }
//            }
//
//            if(breakCallback != null) // Break interrupt checking
//            {
//                if((data[1] & 0x10) == 0x10)
//                {
//                    breakCallback.onBreakInterrupt();
//                }
//            }
        }

        // Copy data without FTDI headers
        private void copyData(byte[] src, byte[] dst) {
            int i = 0; // src index
            int j = 0; // dst index
            while (i <= src.length - 1) {
                if (i != 0 && i != 1) {
                    if (i % 64 == 0 && i >= 64) {
                        i += 2;
                    } else {
                        dst[j] = src[i];
                        i++;
                        j++;
                    }
                } else {
                    i++;
                }
            }
        }
    }

    void killWorkingThread() {
        workerThread.stopWorkingThread();
        workerThread = null;
    }

    void restartWorkingThread() {
        workerThread = new WorkerThread(this);
        workerThread.start();
        while (!workerThread.isAlive()) {
        } // Busy waiting
    }

    void killWriteThread() {
        if (writeThread != null) {
            writeThread.stopWriteThread();
            writeThread = null;
            serialBuffer.resetWriteBuffer();
        }
    }

    void restartWriteThread() {
        if (writeThread == null) {
            writeThread = new WriteThread();
            writeThread.start();
            while (!writeThread.isAlive()) {
            } // Busy waiting
        }
    }
}
