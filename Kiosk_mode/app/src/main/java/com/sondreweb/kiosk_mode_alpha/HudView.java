package com.sondreweb.kiosk_mode_alpha;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Created by sondre on 20-Apr-17.
 */

public class HudView extends ViewGroup {

    private Paint mLoadPaint;

    public HudView(Context context) {
        super(context);
        Toast.makeText(getContext(),"HUDView", Toast.LENGTH_LONG).show();

        mLoadPaint = new Paint();
        mLoadPaint.setAntiAlias(true);
        mLoadPaint.setTextSize(28);
        mLoadPaint.setARGB(255, 255, 0, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawText("Hello World", 100, 100, mLoadPaint);
        //int centerWidth = this.getWidth()/2;
        int centerHeigth = this.getHeight()/2;
        canvas.drawText("It seems you have exeeded the inteded area. Please return back inside where you came to contine",
                20,centerHeigth, mLoadPaint);

    }

    @Override
    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //return super.onTouchEvent(event);
        Toast.makeText(getContext(),"onTouchEvent", Toast.LENGTH_LONG).show();
        Log.d("HudView","onTouchEvent blabla");
        return super.onTouchEvent(event);
    }


}
