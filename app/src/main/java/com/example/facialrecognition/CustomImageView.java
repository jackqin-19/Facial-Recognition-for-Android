package com.example.facialrecognition;

import androidx.appcompat.widget.AppCompatImageView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 自定义ImageView类，用于更好的图片显示控制和支持触摸操作
 */
public class CustomImageView extends AppCompatImageView {
    public CustomImageView(Context context) {
        super(context);
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    @Override
    public boolean performClick() {
        // 实现performClick方法以支持可访问性
        super.performClick();
        return true;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 确保触摸事件处理正确
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // 对于点击事件，确保调用performClick以支持可访问性
            performClick();
        }
        return super.onTouchEvent(event);
    }
}