package com.koces.androidpos.sdk;

import java.io.FileOutputStream;
import android.util.Log;

/**
 * BMP file 정의 클래스
 */
public class BMPfile {

    private KByteArray mRawBitmapData;
    private final static int BITMAPFILEHEADER_SIZE = 14;
    private final static int BITMAPINFOHEADER_SIZE = 40;
    // --- Bitmap file header
    private byte bfType[] = { (byte) 'B', (byte) 'M' };
    private int bfSize = 0;
    private int bfReserved1 = 0;
    private int bfReserved2 = 0;
    private int bfOffBits = BITMAPFILEHEADER_SIZE + BITMAPINFOHEADER_SIZE + 8;
    // --- Bitmap info header
    private int biSize = BITMAPINFOHEADER_SIZE;
    private int biWidth = 0;
    private int biHeight = 0;
    private int biPlanes = 1;
    private int biBitCount = 1;
    private int biCompression = 0;
    private int biSizeImage = 0;
    private int biXPelsPerMeter = 0x0;
    private int biYPelsPerMeter = 0x0;
    private int biClrUsed = 2;
    private int biClrImportant = 2;

    // --- Bitmap raw data
    private byte bitmap[];
    // ---- Scanlinsize;
    int scanLineSize = 0;
    // -- Color Pallette to be used for pixels.
    private byte colorPalette[] = { 0, 0, 0, (byte) 255, (byte) 255,(byte) 255, (byte) 255, (byte) 255 };
    // --- Default constructor

    public BMPfile() {
        mRawBitmapData = new KByteArray();
    }

    public void saveBitmap(FileOutputStream fos, byte[] imagePix, int parWidth,int parHeight) {
        try {
            save(fos, imagePix, parWidth, parHeight);
        } catch (Exception saveEx) {
           saveEx.printStackTrace();
        }
    }
    public byte[] MakeMonoBitMap(byte[] imagePix, int parWidth,int parHeight) {
        byte[] tmp = null;
        try {
            tmp = save(imagePix, parWidth, parHeight);
        } catch (Exception saveEx) {
            saveEx.printStackTrace();
        }
        return tmp;
    }

    /*

     * The saveMethod is the main method of the process. This method will call
     * the convertImage method to convert the memory image to a byte array;
     * method writeBitmapFileHeader creates and writes the bitmap file header;
     * writeBitmapInfoHeader creates the information header; and writeBitmap
     * writes the image.
     */
    private void save(FileOutputStream fos, byte[] imagePix, int parWidth,int parHeight) {
        try {
            convertImage(imagePix, parWidth, parHeight);
            writeBitmapFileHeader(fos);
            writeBitmapInfoHeader(fos);
            writePixelArray(fos);
        } catch (Exception saveEx) {
            saveEx.printStackTrace();
        }
    }
    private byte[] save(byte[] imagePix, int parWidth,int parHeight) {
        try {
            convertImage(imagePix, parWidth, parHeight);
            writeBitmapFileHeader();
            writeBitmapInfoHeader();
            writePixelArray();
        } catch (Exception saveEx) {
            saveEx.printStackTrace();
        }

        return mRawBitmapData.value();
    }

    /*
     * convertImage converts the memory image to the bitmap format (BRG). It
     * also computes some information for the bitmap info header.
     */

    private boolean convertImage(byte[] imagePix, int parWidth, int parHeight) {
        bitmap = imagePix;
        bfSize = 62 + (((parWidth + 31) / 32) * 4 * parHeight);
        biWidth = parWidth;
        biHeight = parHeight;
        scanLineSize = ((parWidth * 1 + 31) / 32) * 4;
        return (true);
    }

    /*
     * writeBitmap converts the image returned from the pixel grabber to the
     * format required. Remember: scan lines are inverted in a bitmap file!
     *
     * Each scan line must be padded to an even 4-byte boundary.
     */

    /*
     * writeBitmapFileHeader writes the bitmap file header to the file.
     */
    private void writeBitmapFileHeader(FileOutputStream fos) {
        try {
            fos.write(bfType);
            fos.write(intToDWord(bfSize));
            fos.write(intToWord(bfReserved1));
            fos.write(intToWord(bfReserved2));
            fos.write(intToDWord(bfOffBits));
        } catch (Exception wbfh) {
            wbfh.printStackTrace();
        }
    }
    private void writeBitmapFileHeader() {
        try {
            mRawBitmapData.Add(bfType);
            mRawBitmapData.Add(intToDWord(bfSize));
            mRawBitmapData.Add(intToWord(bfReserved1));
            mRawBitmapData.Add(intToWord(bfReserved2));
            mRawBitmapData.Add(intToDWord(bfOffBits));
        } catch (Exception wbfh) {
            wbfh.printStackTrace();
        }
    }

    /*
     *
     * writeBitmapInfoHeader writes the bitmap information header to the file.
     */

    private void writeBitmapInfoHeader(FileOutputStream fos) {
        try {
            fos.write(intToDWord(biSize));
            fos.write(intToDWord(biWidth));
            fos.write(intToDWord(biHeight));
            fos.write(intToWord(biPlanes));
            fos.write(intToWord(biBitCount));
            fos.write(intToDWord(biCompression));
            fos.write(intToDWord(biSizeImage));
            fos.write(intToDWord(biXPelsPerMeter));
            fos.write(intToDWord(biYPelsPerMeter));
            fos.write(intToDWord(biClrUsed));
            fos.write(intToDWord(biClrImportant));
            fos.write(colorPalette);
        } catch (Exception wbih) {
            wbih.printStackTrace();
        }
    }
    private void writeBitmapInfoHeader() {
        try {
            mRawBitmapData.Add(intToDWord(biSize));
            mRawBitmapData.Add(intToDWord(biWidth));
            mRawBitmapData.Add(intToDWord(biHeight));
            mRawBitmapData.Add(intToWord(biPlanes));
            mRawBitmapData.Add(intToWord(biBitCount));
            mRawBitmapData.Add(intToDWord(biCompression));
            mRawBitmapData.Add(intToDWord(biSizeImage));
            mRawBitmapData.Add(intToDWord(biXPelsPerMeter));
            mRawBitmapData.Add(intToDWord(biYPelsPerMeter));
            mRawBitmapData.Add(intToDWord(biClrUsed));
            mRawBitmapData.Add(intToDWord(biClrImportant));
            mRawBitmapData.Add(colorPalette);
        } catch (Exception wbih) {
            wbih.printStackTrace();
        }
    }
    private void writePixelArray(FileOutputStream fos) {
        try {
            for (int i = biHeight; i > 0; i--) {
                for (int k = (i - 1) * (scanLineSize ); k < ((i - 1) * (scanLineSize ))
                        + (scanLineSize ); k++) {
                    fos.write(bitmap[k] & 0xFF);
                }
            }
        } catch (Exception e) {
            Log.e("BMPFile",e.toString());
        }
    }
    private void writePixelArray() {
        try {
            for (int i = biHeight; i > 0; i--) {
                for (int k = (i - 1) * (scanLineSize ); k < ((i - 1) * (scanLineSize ))+ (scanLineSize ); k++)
                {
                    mRawBitmapData.Add((byte)(bitmap[k] & 0xFF));
                }
            }
        } catch (Exception e) {
            Log.e("BMPFile",e.toString());
        }
    }

    /*
    *
     * intToWord converts an int to a word, where the return value is stored in
     * a 2-byte array.
     */
    private byte[] intToWord(int parValue) {
        byte retValue[] = new byte[2];
        retValue[0] = (byte) (parValue & 0x00FF);
        retValue[1] = (byte) ((parValue >> 8) & 0x00FF);
        return (retValue);
    }

    /*
     *
     * intToDWord converts an int to a double word, where the return value is
     * stored in a 4-byte array.
     */
    private byte[] intToDWord(int parValue) {
        byte retValue[] = new byte[4];
        retValue[0] = (byte) (parValue & 0x00FF);
        retValue[1] = (byte) ((parValue >> 8) & 0x000000FF);
        retValue[2] = (byte) ((parValue >> 16) & 0x000000FF);
        retValue[3] = (byte) ((parValue >> 24) & 0x000000FF);
        return (retValue);
    }
}