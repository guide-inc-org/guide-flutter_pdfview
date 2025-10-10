package io.endigo.plugins.pdfviewflutter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.ScrollHandle;
import com.github.barteksc.pdfviewer.util.Util;

public class CustomScrollbar extends View implements ScrollHandle {

    // iOS-style dimensions (in dp)
    private final static int SCROLLBAR_WIDTH = 4;
    private final static int SCROLLBAR_MARGIN = 2;
    private final static int SCROLLBAR_RADIUS = 2;
    private final static int SCROLLBAR_MIN_HEIGHT = 40;

    private float relativeHandlerMiddle = 0f;

    protected Context context;
    private boolean inverted;
    private PDFView pdfView;
    private float currentPos;

    private Paint paint;
    private RectF rect;
    private boolean isInitializing = true;

    private Handler handler = new Handler();
    private Runnable hidePageScrollerRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public CustomScrollbar(Context context) {
        this(context, false);
    }

    public CustomScrollbar(Context context, boolean inverted) {
        super(context);
        this.context = context;
        this.inverted = inverted;

        // Initialize paint for iOS-style scrollbar
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x59000000);
        paint.setStyle(Paint.Style.FILL);
        rect = new RectF();

        setVisibility(INVISIBLE);
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        int align;
        int width = SCROLLBAR_WIDTH;
        int height = SCROLLBAR_MIN_HEIGHT;

        // Determine handler position
        if (pdfView.isSwipeVertical()) {
            if (inverted) {
                align = RelativeLayout.ALIGN_PARENT_LEFT;
            } else {
                align = RelativeLayout.ALIGN_PARENT_RIGHT;
            }
        } else {
            if (inverted) {
                align = RelativeLayout.ALIGN_PARENT_TOP;
            } else {
                align = RelativeLayout.ALIGN_PARENT_BOTTOM;
            }
        }

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
            Util.getDP(context, width),
            Util.getDP(context, height)
        );
        lp.setMargins(
            Util.getDP(context, SCROLLBAR_MARGIN),
            Util.getDP(context, SCROLLBAR_MARGIN),
            Util.getDP(context, SCROLLBAR_MARGIN),
            Util.getDP(context, SCROLLBAR_MARGIN)
        );

        lp.addRule(align);
        pdfView.addView(this, lp);

        this.pdfView = pdfView;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radius = Util.getDP(context, SCROLLBAR_RADIUS);
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    @Override
    public void destroyLayout() {
        pdfView.removeView(this);
    }

    @Override
    public void setScroll(float position) {
        // Don't show on initial load
        if (isInitializing) {
            isInitializing = false;
        } else {
            if (!shown()) {
                show();
            } else {
                handler.removeCallbacks(hidePageScrollerRunnable);
            }
        }
        if (pdfView != null) {
            setPosition((pdfView.isSwipeVertical() ? pdfView.getHeight() : pdfView.getWidth()) * position);
        }
    }

    private void setPosition(float pos) {
        if (Float.isInfinite(pos) || Float.isNaN(pos)) {
            return;
        }

        float pdfViewSize;
        float handleSize;

        if (pdfView.isSwipeVertical()) {
            pdfViewSize = pdfView.getHeight();
            handleSize = getHeight();
        } else {
            pdfViewSize = pdfView.getWidth();
            handleSize = getWidth();
        }

        pos -= relativeHandlerMiddle;

        float margin = Util.getDP(context, SCROLLBAR_MARGIN);
        if (pos < margin) {
            pos = margin;
        } else if (pos > pdfViewSize - handleSize - margin) {
            pos = pdfViewSize - handleSize - margin;
        }

        if (pdfView.isSwipeVertical()) {
            setY(pos);
        } else {
            setX(pos);
        }

        calculateMiddle();
        invalidate();
    }

    private void calculateMiddle() {
        float pos, viewSize, pdfViewSize;
        if (pdfView.isSwipeVertical()) {
            pos = getY();
            viewSize = getHeight();
            pdfViewSize = pdfView.getHeight();
        } else {
            pos = getX();
            viewSize = getWidth();
            pdfViewSize = pdfView.getWidth();
        }
        relativeHandlerMiddle = ((pos + relativeHandlerMiddle) / pdfViewSize) * viewSize;
    }

    @Override
    public void hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, 1000);
    }

    @Override
    public void setPageNum(int pageNum) {
        // Calculate and set scrollbar height based on page count
        if (pdfView != null && pdfView.getPageCount() > 0) {
            int pageCount = pdfView.getPageCount();
            float contentRatio = 1.0f / pageCount;

            int newHeight;
            if (pdfView.isSwipeVertical()) {
                float availableHeight = pdfView.getHeight() - (2 * Util.getDP(context, SCROLLBAR_MARGIN));
                newHeight = (int) Math.max(Util.getDP(context, SCROLLBAR_MIN_HEIGHT), availableHeight * contentRatio);
            } else {
                float availableWidth = pdfView.getWidth() - (2 * Util.getDP(context, SCROLLBAR_MARGIN));
                newHeight = (int) Math.max(Util.getDP(context, SCROLLBAR_MIN_HEIGHT), availableWidth * contentRatio);
            }

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
            if (lp != null) {
                if (pdfView.isSwipeVertical()) {
                    lp.height = newHeight;
                } else {
                    lp.width = newHeight;
                }
                setLayoutParams(lp);
            }
        }
    }

    @Override
    public boolean shown() {
        return getVisibility() == VISIBLE;
    }

    @Override
    public void show() {
        setVisibility(VISIBLE);
        setAlpha(1.0f);
    }

    @Override
    public void hide() {
        // iOS-style fade out animation
        animate()
            .alpha(0.0f)
            .setDuration(300)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    setVisibility(INVISIBLE);
                }
            })
            .start();
    }

    public void setTextColor(int color) {
        // Not used in iOS-style scrollbar
    }

    public void setTextSize(int size) {
        // Not used in iOS-style scrollbar
    }

    private boolean isPDFViewReady() {
        return pdfView != null && pdfView.getPageCount() > 0 && !pdfView.documentFitsView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPDFViewReady()) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                isInitializing = false;  // User interaction should show scrollbar
                pdfView.stopFling();
                handler.removeCallbacks(hidePageScrollerRunnable);
                if (pdfView.isSwipeVertical()) {
                    currentPos = event.getRawY() - getY();
                } else {
                    currentPos = event.getRawX() - getX();
                }
            case MotionEvent.ACTION_MOVE:
                if (pdfView.isSwipeVertical()) {
                    setPosition(event.getRawY() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / (float) getHeight(), false);
                } else {
                    setPosition(event.getRawX() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / (float) getWidth(), false);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                hideDelayed();
                pdfView.performPageSnap();
                return true;
        }

        return super.onTouchEvent(event);
    }
}
