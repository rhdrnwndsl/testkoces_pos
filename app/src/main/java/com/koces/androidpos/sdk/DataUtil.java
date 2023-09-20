package com.koces.androidpos.sdk;

public class DataUtil {
    public DataUtil() {
        }

        public static byte byteFromBits(byte[] p_baBits) {
            int nBits = p_baBits.length % 8;
            byte bByte = 0;
            if (nBits == 0) {
                nBits = 8;
            }

            for(int nInx = 0; nInx < nBits; ++nInx) {
                if (p_baBits[nInx] != 0) {
                    bByte |= (byte)(128 >> nInx);
                }
            }

            return bByte;
        }

        public static byte[] byteArrayFromBits(byte[] p_baBits) {
            int nFullBitsBytes = p_baBits.length / 8;
            int nRemindBits = p_baBits.length % 8;
            byte[] baByte = new byte[nFullBitsBytes + (nRemindBits == 0 ? 0 : 1)];
            int nInx = 0;

            int nInxT;
            byte[] baTmp;
            for(nInxT = 0; nInx < nFullBitsBytes; nInxT += 8) {
                baTmp = new byte[8];
                System.arraycopy(p_baBits, nInxT, baTmp, 0, 8);
                baByte[nInx] = byteFromBits(baTmp);
                ++nInx;
            }

            if (nRemindBits > 0) {
                baTmp = new byte[nRemindBits];
                System.arraycopy(p_baBits, nInxT, baTmp, 0, nRemindBits);
                baByte[nInx] = byteFromBits(baTmp);
            }

            return baByte;
        }

        public static byte[] regenByteArray(byte[] p_baData, int p_nLen) {
            byte[] baReGen = new byte[p_nLen];
            int nCopyLen = p_baData.length >= p_nLen ? p_nLen : p_baData.length;
            System.arraycopy(p_baData, 0, baReGen, 0, nCopyLen);
            return baReGen;
        }

        public static byte[] regenByteArray(byte[] p_baData, int p_nArrLen, int p_nLen) {
            byte[] baReGen = new byte[p_nArrLen];
            int nCopyLen = p_nArrLen > p_nLen ? p_nLen : p_nArrLen;
            if (p_baData.length < nCopyLen) {
                nCopyLen = p_baData.length;
            }

            System.arraycopy(p_baData, 0, baReGen, 0, nCopyLen);
            return baReGen;
        }

}
