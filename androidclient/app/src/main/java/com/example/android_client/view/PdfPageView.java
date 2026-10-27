package com.example.android_client.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

import java.io.IOException;

/**
 * 自定义PDF页面视图
 * 使用Android内置的PdfRenderer API渲染PDF页面
 */
public class PdfPageView extends View {

    private static final String TAG = "PdfPageView";

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private Bitmap pageBitmap;
    private Canvas pageCanvas;
    private Paint paint;

    private float scaleFactor = 1.0f;
    private float minScale = 1.0f;
    private float maxScale = 5.0f;

    private float translateX = 0f;
    private float translateY = 0f;

    private float lastTouchX;
    private float lastTouchY;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private OnPageChangeListener pageChangeListener;
    private int currentPageIndex = 0;
    private int totalPages = 0;

    public interface OnPageChangeListener {
        void onPageChanged(int pageIndex, int totalPages);
    }

    public PdfPageView(Context context) {
        super(context);
        init();
    }

    public PdfPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PdfPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setAntiAlias(true);

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    /**
     * 设置PDF渲染器
     */
    public void setPdfRenderer(PdfRenderer renderer) throws IOException {
        this.pdfRenderer = renderer;
        this.totalPages = renderer.getPageCount();

        if (currentPage != null) {
            currentPage.close();
        }

        if (totalPages > 0) {
            currentPage = pdfRenderer.openPage(0);
            currentPageIndex = 0;
            renderCurrentPage();
            notifyPageChange();
        }

        invalidate();
    }

    /**
     * 跳转到指定页面
     */
    public void goToPage(int pageIndex) {
        if (pdfRenderer == null || totalPages == 0) return;

        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= totalPages) pageIndex = totalPages - 1;

        if (pageIndex != currentPageIndex) {
            if (currentPage != null) {
                currentPage.close();
            }
            currentPage = pdfRenderer.openPage(pageIndex);
            currentPageIndex = pageIndex;
            resetTransform();
            renderCurrentPage();
            invalidate();
            notifyPageChange();
        }
    }

    /**
     * 下一页
     */
    public void nextPage() {
        if (currentPageIndex < totalPages - 1) {
            goToPage(currentPageIndex + 1);
        }
    }

    /**
     * 上一页
     */
    public void previousPage() {
        if (currentPageIndex > 0) {
            goToPage(currentPageIndex - 1);
        }
    }

    /**
     * 获取当前页码（从1开始）
     */
    public int getCurrentPageNumber() {
        return currentPageIndex + 1;
    }

    /**
     * 获取总页数
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * 设置页面变化监听器
     */
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.pageChangeListener = listener;
    }

    private void notifyPageChange() {
        if (pageChangeListener != null) {
            pageChangeListener.onPageChanged(currentPageIndex, totalPages);
        }
    }

    /**
     * 渲染当前页面
     */
    private void renderCurrentPage() {
        if (currentPage == null) return;

        int width = currentPage.getWidth();
        int height = currentPage.getHeight();

        // 创建Bitmap
        if (pageBitmap == null || pageBitmap.getWidth() != width || pageBitmap.getHeight() != height) {
            pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pageBitmap.eraseColor(Color.WHITE);
        }

        // 渲染PDF页面到Bitmap
        currentPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
    }

    /**
     * 重置缩放和平移
     */
    private void resetTransform() {
        scaleFactor = 1.0f;
        translateX = 0f;
        translateY = 0f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pageBitmap == null) return;

        canvas.save();

        // 应用变换
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        // 绘制PDF页面
        canvas.drawBitmap(pageBitmap, 0, 0, paint);

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);

        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }

    /**
     * 缩放手势监听器
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));
            invalidate();
            return true;
        }
    }

    /**
     * 手势监听器（滑动和点击）
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            // 单击切换UI或显示提示
            return true;
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            // 双击重置缩放
            if (scaleFactor > 1.5f) {
                resetTransform();
            } else {
                scaleFactor = 2.5f;
            }
            invalidate();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            if (scaleFactor > 1.0f) {
                // 缩放模式下可以平移
                translateX -= distanceX;
                translateY -= distanceY;
                invalidate();
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            // 快速滑动翻页
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                if (diffX > 0) {
                    previousPage();
                } else {
                    nextPage();
                }
                return true;
            }
            return false;
        }
    }

    /**
     * 关闭PDF渲染器
     */
    public void close() {
        if (currentPage != null) {
            currentPage.close();
            currentPage = null;
        }
        if (pdfRenderer != null) {
            pdfRenderer.close();
            pdfRenderer = null;
        }
        if (pageBitmap != null) {
            pageBitmap.recycle();
            pageBitmap = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        close();
    }
}
