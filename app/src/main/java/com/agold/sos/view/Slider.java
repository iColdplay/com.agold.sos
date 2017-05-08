package com.agold.sos.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;


/**
 * Created by root on 17-4-17.
 */

public class Slider extends AppCompatSeekBar {

    private Drawable thumb;
    private SlideView.OnSlideCompleteListener listener;
    private SlideView slideView;

    public Slider(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public void setThumb(Drawable thumb) {
        this.thumb = thumb;
        super.setThumb(thumb);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        android.util.Log.i("ly20170418","Slider onTouchEvent");
        android.util.Log.i("ly20170418","Slider onTouchEvent v-->"+event.toString());
        //同一时间内 只允许单个组件滑动
        if(event.getPointerId(0) > 1){
            return false;
        }else {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (thumb.getBounds().contains((int) event.getX(), (int) event.getY())) {
                    super.onTouchEvent(event);
                } else {
                    return false;
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_POINTER_UP) {//双指事件 回调接口不一样
                //按照所长要求 减小回调接口的滑动长度要求
                if (getProgress() > 50) {
                    if (listener != null) listener.onSlideComplete(slideView);
                }
                setProgress(0);
            } else {
                super.onTouchEvent(event);
            }
        }
        return true;
    }

    void setOnSlideCompleteListenerInternal(SlideView.OnSlideCompleteListener listener, SlideView slideView) {
        this.listener = listener;
        this.slideView = slideView;
    }

    @Override
    public Drawable getThumb() {
        // getThumb method was added in SDK16 but our minSDK is 14
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return super.getThumb();
        } else {
            return thumb;
        }
    }
}
