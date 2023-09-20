package com.koces.androidpos.sdk.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.koces.androidpos.sdk.KByteArray
import com.koces.androidpos.sdk.Setting
import com.koces.androidpos.sdk.van.Constants
import com.koces.androidpos.sdk.van.Constants.CHARACTERISTIC_READ_C1_NEW_PRINT
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.disposables.Disposable
import java.util.*


class BleRepository {

    var rxBleConnection: RxBleConnection? = null
    private var mConnectSubscription: Disposable? = null
    private var connectionStateDisposable: Disposable? = null
    private var mNotificationSubscription: Disposable? = null
    private var mWriteSubscription: Disposable? = null
    private var mBleSdk: bleSdk = bleSdk.getInstance()
    var isRead = false
    var isName = ""
    var isAddr = ""
    private lateinit var isCtx: Context
    /**
     * Connect & Discover Services
     * @Saved rxBleConnection
     */
    fun connectDevice(addr: String, ctx: Context, name: String){
        Setting.setIsWoosim(false)
        isName = name
        isCtx = ctx
        isAddr = addr

        Connect()


    }

    fun Connect() {
        val rxBleClient: RxBleClient = RxBleClient.create(isCtx)

        val device: RxBleDevice = rxBleClient.getBleDevice(isAddr)

        // register connectionStateListener
        connectionStateDisposable = device.observeConnectionStateChanges()
            .subscribe(
                { connectionState ->
                    connectionStateListener(device, connectionState)
                }
            ) { throwable ->
                throwable.printStackTrace()
            }

        mConnectSubscription = device.establishConnection(false) // <-- autoConnect flag
            .flatMapSingle{ _rxBleConnection->
                // All GATT operations are done through the rxBleConnection.
                rxBleConnection = _rxBleConnection
                // Discover services
                _rxBleConnection.discoverServices()
            }.subscribe({ service ->
                // Services
                service.toString()
                Log.d("service", service.toString())
                mBleSdk.mConnectResult.onState(true);
                Setting.setBleIsConnected(true);
                Setting.setBleName(isName)
            },{throwable ->
                // Handle an error here
                throwable.printStackTrace()

            })
    }

    fun disconnectDevice(){
        mConnectSubscription?.dispose()
        mWriteSubscription?.dispose()
        connectionStateDisposable?.dispose()
        mNotificationSubscription?.dispose()
        isRead = false
    }

    fun getConnected(): Boolean {
        var _connected = false
        try {

            val bluetoothDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices
            for (bluetoothDevice in bluetoothDevices) {
                if (bluetoothDevice.name != null) {
                    if (bluetoothDevice.name.contains(Setting.getBleName()))
                    {
                        _connected = true
                    }
                }
            }
        } catch (e: Exception) {
            //블루투스 서비스 사용불가인 경우
//            _connected = false;
        }
        return _connected
    }

    private fun connectionStateListener(
        device: RxBleDevice,
        connectionState: RxBleConnection.RxBleConnectionState
    ){
        when(connectionState){
            RxBleConnection.RxBleConnectionState.CONNECTED -> {
                Log.d("RxBleConnectionState", "CONNECTED")
                Handler(Looper.getMainLooper()).postDelayed({
                    bleBonding()
                },500)
            }
            RxBleConnection.RxBleConnectionState.CONNECTING -> {
//                isConnecting.set(true)
                Log.d("RxBleConnectionState", "CONNECTING")
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTED -> {
                Log.d("RxBleConnectionState", "DISCONNECTED")
                mBleSdk.mConnectResult.onState(false)
                Setting.setBleIsConnected(false)
                mBleSdk.DisConnect()
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTING -> {
                Log.d("RxBleConnectionState", "DISCONNECTING")
                mBleSdk.mConnectResult.onState(false)
                Setting.setBleIsConnected(false)
                mBleSdk.DisConnect()
            }
        }
    }

    fun bleBonding() {
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bleSdk.getInstance().addr)
        if (device.bondState == BluetoothDevice.BOND_NONE ) {
            Log.d("RxBleConnectionState", "BOND_NONE")
            device.createBond()
        } else if (device.bondState == BluetoothDevice.BOND_BONDING) {
            Log.d("RxBleConnectionState", "BOND_BONDING")
        } else {
            Log.d("RxBleConnectionState", "CONNECTED")
//            mBleSdk.mConnectResult.onState(true);
//            Setting.setBleIsConnected(true);
//            Setting.setBleName(isName)
        }
    }

    fun bleSetNoti(notiuuid:String) {

        if (!isRead) {
            mNotificationSubscription = bleNotification(notiuuid)
                ?.subscribe({ bytes ->
                    // Given characteristic has been changes, here is the value.
//                readTxt.postValue(byteArrayToHex(bytes))
                    isRead = true
                    val str: String = byteArrayToHex(bytes)
                    Log.d("bleSetNoti", str)
                    mBleSdk.RecvDataUpdate(bytes)
                }, { throwable ->
                    // Handle an error here
                    throwable.printStackTrace()
//                    disconnectDevice()
                    isRead = false
                })
        }

    }

    fun writeData(data: ByteArray) {
        var sendByteData: ByteArray? = null
        var senduuid: String = ""

        sendByteData = data
        var size:Int = 20
        if (isName.contains(Constants.C1_KRE_NEW)) {
            senduuid = Constants.CHARACTERISTIC_WRITE_C1_NEW_PRINT
            bleSetNoti(Constants.CHARACTERISTIC_READ_C1_NEW_PRINT);
//            rxBleConnection!!.requestMtu(150)
//            size = 150
        } else if (isName.contains(Constants.C1_KRE_OLD_NOT_PRINT)) {
            senduuid = Constants.CHARACTERISTIC_WRITE_C1_OLD_NOT_PRINT
            bleSetNoti(Constants.CHARACTERISTIC_READ_C1_OLD_NOT_PRINT);
        } else if (isName.contains(Constants.C1_KRE_OLD_USE_PRINT)) {
            senduuid = Constants.CHARACTERISTIC_WRITE_C1_OLD_PRINT
            bleSetNoti(Constants.CHARACTERISTIC_READ_C1_OLD_PRINT);
        } else if (isName.contains(Constants.ZOA_KRE)) {
            senduuid = Constants.CHARACTERISTIC_WRITE_ZOA
            bleSetNoti(Constants.CHARACTERISTIC_READ_ZOA);
        } else if (isName.contains(Constants.KWANGWOO_KRE)) {
            senduuid = Constants.CHARACTERISTIC_WRITE_KWANGWOO
            bleSetNoti(Constants.CHARACTERISTIC_READ_KWANGWOO);
        }



        if (sendByteData != null) {
            var _byteData: KByteArray = KByteArray(sendByteData)
            var _fail:Boolean = false

            var _byte = _byteData.value()

            while(true) {
                if (_fail){
                    break
                }

                if (_byteData.getlength() > size) {
                    _byte = _byteData.CutToSize(size)
                    mWriteSubscription = writeData2(_byte, senduuid)?.subscribe({ writeBytes ->
                        // Written data.
                        val str: String = byteArrayToHex(writeBytes)
                        Log.d("writtenBytes", str)
                    }, { throwable ->
                        // Handle an error here.
                        _fail = true
                        throwable.printStackTrace()
                    })
                } else {
                    _byte = _byteData.CutToSize(_byteData.getlength())
                    mWriteSubscription = writeData2(_byte, senduuid)?.subscribe({ writeBytes ->
                        // Written data.
                        val str: String = byteArrayToHex(writeBytes)
                        Log.d("writtenBytes End", str)
                    }, { throwable ->
                        // Handle an error here.
                        _fail = true
                        throwable.printStackTrace()
                    })
                    break;
                }

            }
        }
    }

    private fun byteArrayToHex(a: ByteArray): String {
        val sb = java.lang.StringBuilder(a.size * 2)
        for (b in a) sb.append(String.format("%02x", b))
        return sb.toString()
    }

    /**
     * Notification
     */
    fun bleNotification(notiuuid: String) = rxBleConnection
        ?.setupNotification(UUID.fromString(notiuuid))
        ?.doOnNext { notificationObservable->
            // Notification has been set up
        }
        ?.flatMap { notificationObservable -> notificationObservable }

    /**
     * Read
     */
    fun bleRead() = rxBleConnection?.readCharacteristic(UUID.fromString(CHARACTERISTIC_READ_C1_NEW_PRINT))



    /**
     * Write Data
     */
    fun writeData2(sendByteData: ByteArray, senduuid: String) = rxBleConnection?.writeCharacteristic(
        UUID.fromString(senduuid),
        sendByteData
    )

}