package com.koces.androidpos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.PorterDuff;

/**
 * 실제 화면에 그려지는 서명 뷰
 */
public class SignPad extends View {
    /** 터치서명 종료 시 화면에 그리는 것을 방지한다 */
    private boolean isDrawable = false;
    /** 화면에 그릴 때의 선 굵기 */
    public static int mStrokeWidth  = 5;
    /** 화면에 그리는 선의 색깔 */
    public static final int mPaintColor  = Color.BLACK;
    /** 화면에 그리는 배경 색깔 */
    public static final int mBackgroundColor = Color.WHITE;
    /** 비트맵으로 저장되는 이미지 */
    private Bitmap mCanvasBitmap = null;
    /** 화면에 그려지는 선의 경로 */
    public Path mDrawPath;
    /** 화면에 그려지는 주선(mDrawPaint), 선의 색을(그라디언트) 부드럽게 구현하기 위한(mCanvasPaint)*/
    private Paint mDrawPaint, mCanvasPaint;
    /** 화면에 그려지기 위한 창크기에 맞춘 Canvas 를 구현한다 */
    private Canvas mDrawCanvas;
    /** 화면에 그려질 좌표 x,y */
    private float mX=0, mY=0;

    /** 사용하지 않음 */
    private int mLastHeight;
    /** 사용하지 않음 */
    private int mLastWidth;

    public SignPad(Context context){
        super(context, null);
    }

    public SignPad(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    /** 서명을 그리기 위한 초기화 */
    private void init()
    {
        mDrawPath = new Path();
        mDrawPaint = new Paint();

        mDrawPaint.setColor(mPaintColor);
        mDrawPaint.setAntiAlias(true);
        mDrawPaint.setDither(true);
        mDrawPaint.setStrokeWidth(mStrokeWidth);
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setStrokeJoin(Paint.Join.ROUND);
        mDrawPaint.setStrokeCap(Paint.Cap.ROUND);
        mDrawPaint.setXfermode(null);
        mDrawPaint.setAlpha(0xff);
        mCanvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    /**
     * 안드로이드 화면터치를 통해 그릴지 말지를 셋팅
     * @param _bSet
     */
    public void setIsDrawable(boolean _bSet)
    {
        isDrawable = _bSet;
    }

    /** 사용하지 않음 */
    public void clear(){
        mDrawCanvas.drawColor(mBackgroundColor, PorterDuff.Mode.CLEAR);
        mCanvasBitmap.eraseColor(mBackgroundColor);
        invalidate();
    }

    /** 이미지를 비트맵으로 보내기 위한 곳 */
    public Bitmap getCanvasBitmap() {
        return mCanvasBitmap;
    }

    /** 사용하지 않음 */
    public void setCanvasBitmap(Bitmap mCanvasBitmap) {
        this.mCanvasBitmap = mCanvasBitmap;
    }

    @Override
    protected  void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawBitmap(mCanvasBitmap, 0, 0, mCanvasPaint);
        canvas.drawPath(mDrawPath, mDrawPaint);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mLastHeight = h;
        mLastWidth = w;
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mCanvasBitmap.eraseColor(mBackgroundColor);
        mDrawCanvas = new Canvas();
        mDrawCanvas.drawColor(mBackgroundColor, PorterDuff.Mode.CLEAR);
        mDrawCanvas.setBitmap(mCanvasBitmap);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(isDrawable) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    touchStart(x,y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touchMove(x,y);
                    break;
                case MotionEvent.ACTION_UP:
                    touchUp(x,y);
//                    SignPadActivity.isDisplaySignFinish = true;
//                    isDrawable = false;
                    break;
            }
        }
        return true;
    }

    /** 사인데이터를 SignPadActivity 로 보낸다 */
    public Bitmap saveImage(){
        Bitmap _bmp = getCanvasBitmap();
        _bmp = Bitmap.createScaledBitmap(_bmp,128,64,false);
        return  _bmp;
    }

    /**
     * 서명좌표를 최초로 입력받을 때 실행
     * @param x
     * @param y
     */
    public void touchStart(float x, float y)
    {
        SignPadActivity.mBtn_signpad_ok.setVisibility(VISIBLE);
        mDrawPath.moveTo(x,y);
        mX=x; mY=y;
        invalidate();
    }

    /**
     * 서명좌표대로 화면에 그린다
     * @param x
     * @param y
     */
    public void touchMove(float x, float y)
    {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if(dx >= 2 || dy>= 2){
            mDrawPath.quadTo(mX, mY, (x+mX)/2, (y+mY)/2);
            mX = (x+mX)/2;
            mY = (y+mY)/2;
        }
        invalidate();
    }

    /**
     * (터치서명제외) 사인입력을 마칠때 실행
     * @param x
     * @param y
     */
    public void UpdateDrawPt(float x, float y)
    {
        mDrawPath.lineTo(mX, mY);
        mDrawCanvas.drawPath(mDrawPath, mDrawPaint);
        mDrawPath.reset();
        invalidate();

        SignPadActivity.isDisplaySignFinish = true;
    }

    /**
     * 터치서명시 사인입력을 마칠때 실행
     * @param x
     * @param y
     */
    public void touchUp (float x, float y){
        mDrawPath.lineTo(mX, mY);
        mDrawCanvas.drawPath(mDrawPath,mDrawPaint);
        mDrawPath.reset();
        SignPadActivity.isDisplaySignFinish = true;
        invalidate();
     //   mPath = null;
    }

//    public void touchStart (float x, float y){
//        if(mPath == null){
//            mPath = new Path();
//        }
//        mX = x;
//        mY = y;
////        DrawBasic draw = new DrawBasic(currentColor, strokeWidth, mPath);
////        paths.add(draw);
//        mPath.moveTo(x,y);
//        invalidate();
//    }
//
//    public void touchMove (float x, float y){
//        //    if(mX == 0 && mY == 0)
//        //    {
//        //        touchStart(x,y);
//        //        return;
//        //    }
//        float dx = Math.abs(x - mX);
//        float dy = Math.abs(y - mY);
//
//        if(dx >= TOUCH_TOLERAMCE || dy>= TOUCH_TOLERAMCE){
//            mPath.quadTo(mX, mY, (x+mX)/2, (y+mY)/2);
//            mX = x;
//            mY = y;
//        }
//
//        invalidate();
//    }
//
//    public void touchUp (){
//        if(mX!=0 && mY!=0) {
//       //     mPath.lineTo(mX, mY);
//            mCanvas.drawPath(mPath,mPaint);
//            mPath.reset();
//        }
//        //
//     //   invalidate();
//     //   mPath = null;
//    }
//    public void initialise (DisplayMetrics displayMetrics){
//        int height = displayMetrics.heightPixels;
//        int width = displayMetrics.widthPixels;
//        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
////        mBitmap = Bitmap.createBitmap(1000, 500, Bitmap.Config.RGB_565);
////        mBitmap = Bitmap.createBitmap(384, 192, Bitmap.Config.RGB_565);
//        mCanvas = new Canvas(mBitmap);
//
//        currentColor = DEFAULT_COLOR;
//        strokeWidth = BRUSH_SIZE;
//    }

//    public void StartDrawPt(float x,float y)
//    {
//        mPath = new Path();
//        mPaint.setStrokeWidth(30f);
//        mCanvas.drawPoint(x,y,mPaint);
//    }
}
