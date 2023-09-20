/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.koces.androidpos.sdk.ble;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String TRANS_TX ="0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String TRANS_RX ="0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String TRANS = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String NOTIFY = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_TRANS_RX = UUID.fromString(TRANS_RX);
    public static final UUID UUID_TRANS_TX = UUID.fromString(TRANS_TX);
    public static String UUID_CLIENT_CHARACTERISTIC_CONFIG ="00002902-0000-1000-8000-00805f9b34fb";
  //  public static final UUID UUID_CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
 //   public static final UUID UUID_TRANS_TX = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
    /* CBR Series */
    public  static  UUID SERVICE_ISSC_PROPRIETARY  = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    public  static  UUID CHR_CONNECTION_PARAMETER  = UUID.fromString("49535343-6DAA-4D02-ABF6-19569ACA69FE");
    public  static  UUID CHR_ISSC_TRANS_TX         = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");//
    public  static  UUID CHR_ISSC_TRANS_RX         = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
    public final static  UUID CHR_ISSC_MP               = UUID.fromString("49535343-ACA3-481C-91EC-D85E28A60318");
    public  static  UUID CHR_ISSC_TRANS_CTRL         = UUID.fromString("49535343-4C8A-39B3-2F49-511CFF073B7E");//KIS


    public static int mBleModelType = 0;
    public static UUID BLE_CHARACTERISTIC_CONFIG = null;
    public static UUID BLE_TX = null;
    public static UUID BLE_RX = null;
    public static UUID BLE_TRAN = null;
    public static UUID BLE_NOTIFY = null;

//    static {
//        // Sample Services.
//        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
//        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
//        // Sample Characteristics.
//        //attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
//        attributes.put(TRANS_TX, "TRANS DATA TX");
//        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
//    }


    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public final static byte STX = (byte)0x02;
    public final static byte ETX = (byte)0x03;
    public final static int SPEC_MIN_SIZE = 6;
    //inicis,KOCES의 경우
    public final static int SPEC_HEADER_SIZE = 4;
    //KOVAN의 경우
    //public final static int SPEC_HEADER_SIZE = 2;
}
