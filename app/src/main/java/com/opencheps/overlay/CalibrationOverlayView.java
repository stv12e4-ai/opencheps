package com.opencheps.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class CalibrationOverlayView extends View {
    public interface Listener {
        void onConfirm(Rect bounds);
        void onCancel();
    }

    private final Paint paint = new Paint(3);
    private final RectF board = new RectF();
    private final RectF confirm = new RectF();
    private final RectF cancel = new RectF();
    private Listener listener;
    private int mode = 0; // 0 move, 1 resize
    private float downX, downY;
    private RectF start = new RectF();
    private final float density;

    public CalibrationOverlayView(Context context, Rect initial, Listener listener) {
        super(context);
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        if (initial != null && initial.width() > 0) board.set(initial);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private float dp(float v) { return v * density; }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawColor(0x66000000);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(dp(16));
        paint.setFakeBoldText(true);
        c.drawText("Position the box over the chessboard", dp(16), dp(34), paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(dp(12));
        c.drawText("Drag inside to move · drag a corner to resize", dp(16), dp(56), paint);

        paint.setColor(0x2200E676);
        c.drawRect(board, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(0xFF00E676);
        c.drawRect(board, paint);
        paint.setStyle(Paint.Style.FILL);
        float h = dp(12);
        float[][] pts = {{board.left,board.top},{board.right,board.top},
                {board.left,board.bottom},{board.right,board.bottom}};
        for (float[] p : pts) c.drawCircle(p[0], p[1], h, paint);

        float margin = dp(16), bh = dp(48), gap = dp(12);
        float y = getHeight() - margin - bh;
        float half = (getWidth() - margin * 2 - gap) / 2f;
        cancel.set(margin, y, margin + half, y + bh);
        confirm.set(margin + half + gap, y, getWidth() - margin, y + bh);
        paint.setColor(0xFF424242);
        c.drawRoundRect(cancel, dp(10), dp(10), paint);
        paint.setColor(0xFF087F23);
        c.drawRoundRect(confirm, dp(10), dp(10), paint);
        paint.setColor(0xFFFFFFFF);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(15));
        c.drawText("CANCEL", cancel.centerX(), cancel.centerY() + dp(5), paint);
        c.drawText("CONFIRM", confirm.centerX(), confirm.centerY() + dp(5), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(), y=e.getY();
        if (e.getAction()==MotionEvent.ACTION_DOWN) {
            if (confirm.contains(x,y) || cancel.contains(x,y)) return true;
            downX=x; downY=y; start.set(board);
            float handle=dp(30);
            mode = (near(x,y,board.left,board.top,handle)
                    || near(x,y,board.right,board.top,handle)
                    || near(x,y,board.left,board.bottom,handle)
                    || near(x,y,board.right,board.bottom,handle)) ? 1 : 0;
            return true;
        }
        if (e.getAction()==MotionEvent.ACTION_MOVE) {
            float dx=x-downX, dy=y-downY;
            if (mode==0) {
                float w=start.width(), h=start.height();
                float l=clamp(start.left+dx, 0, getWidth()-w);
                float t=clamp(start.top+dy, dp(72), getHeight()-dp(80)-h);
                board.set(l,t,l+w,t+h);
            } else {
                float side=Math.max(dp(160), Math.max(start.width()+dx,start.height()+dy));
                side=Math.min(side, Math.min(getWidth()-start.left,getHeight()-dp(80)-start.top));
                board.set(start.left,start.top,start.left+side,start.top+side);
            }
            invalidate(); return true;
        }
        if (e.getAction()==MotionEvent.ACTION_UP) {
            if (confirm.contains(x,y)) {
                if (listener!=null) listener.onConfirm(new Rect(Math.round(board.left),
                        Math.round(board.top),Math.round(board.right),Math.round(board.bottom)));
            } else if (cancel.contains(x,y)) {
                if (listener!=null) listener.onCancel();
            }
            return true;
        }
        return true;
    }

    private boolean near(float x,float y,float a,float b,float d) {
        return Math.abs(x-a)<=d && Math.abs(y-b)<=d;
    }
    private float clamp(float v,float min,float max) { return Math.max(min,Math.min(max,v)); }
}
