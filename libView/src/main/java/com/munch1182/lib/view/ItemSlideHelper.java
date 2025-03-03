package com.munch1182.lib.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;

public class ItemSlideHelper implements RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener {


    private static final String TAG = "ItemSwipeHelper";


    private final int DEFAULT_DURATION = 200;

    private View mTargetView;
    private View mMenuView;

    private final int mTouchSlop;
    private final int mMaxVelocity;
    private final int mMinVelocity;
    private int mLastX;
    private int mLastY;


    private boolean mIsDragging;

    private Animator mExpandAndCollapseAnim;

    private final GestureDetectorCompat mGestureDetector;

    private final Callback mCallback;

    public ItemSlideHelper(RecyclerView rv, SimpleCallback callback) {
        this(rv.getContext(), new Callback() {
            @Override
            public int getHorizontalRange(RecyclerView.ViewHolder holder) {
                View targetMenu = findTargetMenuView(holder);
                if (targetMenu != null) {
                    return targetMenu.getWidth();
                }
                return 0;
            }

            @Override
            public RecyclerView.ViewHolder getChildViewHolder(View childView) {
                return rv.getChildViewHolder(childView);
            }

            @Override
            public View findTargetItemView(float x, float y) {
                return rv.findChildViewUnder(x, y);
            }

            @Override
            public View findTargetMenuView(RecyclerView.ViewHolder holder) {
                return callback.findTargetMenuView(holder);
            }
        });
    }

    public ItemSlideHelper(Context context, Callback callback) {
        this.mCallback = callback;

        //手势用于处理fling
        mGestureDetector = new GestureDetectorCompat(context, this);

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaxVelocity = configuration.getScaledMaximumFlingVelocity();
        mMinVelocity = configuration.getScaledMinimumFlingVelocity();
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        int action = e.getAction();
        int x = (int) e.getX();
        int y = (int) e.getY();


        //如果RecyclerView滚动状态不是空闲targetView不是空
        if (rv.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            if (mTargetView != null) {
                //隐藏已经打开
                smoothHorizontalExpandOrCollapse((float) DEFAULT_DURATION / 2);
                mTargetView = null;
            }

            return false;
        }

        //如果正在运行动画 ，直接拦截什么都不做
        if (mExpandAndCollapseAnim != null && mExpandAndCollapseAnim.isRunning()) {
            return true;
        }

        boolean needIntercept = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:


                mLastX = (int) e.getX();
                mLastY = (int) e.getY();

                /*
                 * 如果之前有一个已经打开的项目，当此次点击事件没有发生在右侧的菜单中则返回TRUE，
                 * 如果点击的是右侧菜单那么返回FALSE这样做的原因是因为菜单需要响应Onclick
                 * */
                if (mTargetView != null) {
                    return !inView(x, y);
                }

                //查找需要显示菜单的view;
                mTargetView = mCallback.findTargetItemView(x, y);
                RecyclerView.ViewHolder holder = mCallback.getChildViewHolder(mTargetView);
                if (holder != null) {
                    mMenuView = mCallback.findTargetMenuView(holder);
                }

                break;
            case MotionEvent.ACTION_MOVE:

                int deltaX = (x - mLastX);
                int deltaY = (y - mLastY);

                if (Math.abs(deltaY) > Math.abs(deltaX)) return false;

                //如果移动距离达到要求，则拦截
                needIntercept = mIsDragging = mTargetView != null && Math.abs(deltaX) >= mTouchSlop;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /*
                 * 走这是因为没有发生过拦截事件
                 * */
                if (isExpanded()) {

                    if (inView(x, y)) {
                        // 如果走这那行这个ACTION_UP的事件会发生在右侧的菜单中
                        Log.d(TAG, "click item");
                    } else {
                        //拦截事件,防止targetView执行onClick事件
                        needIntercept = true;
                    }

                    //折叠菜单
                    smoothHorizontalExpandOrCollapse((float) DEFAULT_DURATION / 2);
                }

                mTargetView = null;
                break;
        }

        return needIntercept;
    }


    private boolean isExpanded() {
        return mTargetView != null && mTargetView.getScrollX() == getHorizontalRange();
    }

    private boolean isCollapsed() {

        return mTargetView != null && mTargetView.getScrollX() == 0;
    }

    /*
     * 根据targetView的scrollX计算出targetView的偏移，这样能够知道这个point
     * 是在右侧的菜单中
     * */
    private boolean inView(int x, int y) {

        if (mTargetView == null || mMenuView == null) return false;

        int scrollX = mTargetView.getScrollX();
        int left = mTargetView.getWidth() - scrollX - mMenuView.getWidth();
        int top = mTargetView.getTop();
        int right = left + mMenuView.getWidth();
        int bottom = mTargetView.getBottom();
        Rect rect = new Rect(left, top, right, bottom);
        return rect.contains(x, y);
    }

    private final PointF downPoint = new PointF();


    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, MotionEvent e) {
        if (mExpandAndCollapseAnim != null && mExpandAndCollapseAnim.isRunning() || mTargetView == null)
            return;

        //如果要响应fling事件设置将mIsDragging设为false
        if (mGestureDetector.onTouchEvent(e)) {
            mIsDragging = false;
            return;
        }

        int x = (int) e.getX();
        int y = (int) e.getY();
        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //RecyclerView 不会转发这个Down事件
                downPoint.set(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (mLastX - e.getX());
                if (mIsDragging) {
                    horizontalDrag(deltaX);
                }
                mLastX = x;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    if (!smoothHorizontalExpandOrCollapse(0) && isCollapsed()) mTargetView = null;

                    mIsDragging = false;
                }
                if (downPoint.x == x && downPoint.y == y && mCallback != null && mTargetView != null) {
                    if (inView(x, y)) {
                        if (mMenuView != null) {
                            mMenuView.performClick();
                        }
                    } else {
                        smoothHorizontalExpandOrCollapse((float) DEFAULT_DURATION / 2);
                    }
                }
                downPoint.set(-1f, -1f);

                break;
        }


    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    /**
     * 根据touch事件来滚动View的scrollX
     */
    private void horizontalDrag(int delta) {
        int scrollX = mTargetView.getScrollX();
        int scrollY = mTargetView.getScrollY();
        if ((scrollX + delta) <= 0) {
            mTargetView.scrollTo(0, scrollY);
            return;
        }


        int horRange = getHorizontalRange();
        scrollX += delta;
        if (Math.abs(scrollX) < horRange) {
            mTargetView.scrollTo(scrollX, scrollY);
        } else {
            mTargetView.scrollTo(horRange, scrollY);
        }


    }


    /**
     * 根据当前scrollX的位置判断是展开还是折叠
     *
     * @param velocityX 如果不等于0那么这是一次fling事件,否则是一次ACTION_UP或者ACTION_CANCEL
     */
    private boolean smoothHorizontalExpandOrCollapse(float velocityX) {

        int scrollX = mTargetView.getScrollX();
        int scrollRange = getHorizontalRange();

        if (mExpandAndCollapseAnim != null) return false;


        int to = 0;
        int duration = DEFAULT_DURATION;

        if (velocityX == 0) {
            //如果已经展一半，平滑展开
            if (scrollX > scrollRange / 2) {
                to = scrollRange;
            }
        } else {


            if (velocityX <= 0) {
                to = scrollRange;
            }

            duration = (int) ((1.f - Math.abs(velocityX) / mMaxVelocity) * DEFAULT_DURATION);
        }

        if (to == scrollX) return false;

        mExpandAndCollapseAnim = ObjectAnimator.ofInt(mTargetView, "scrollX", to);
        mExpandAndCollapseAnim.setDuration(duration);
        mExpandAndCollapseAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                mExpandAndCollapseAnim = null;
                if (isCollapsed()) mTargetView = null;

            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {
                //onAnimationEnd(animation);
                mExpandAndCollapseAnim = null;

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {
            }
        });
        mExpandAndCollapseAnim.start();

        return true;
    }


    public int getHorizontalRange() {
        if (mCallback == null || mTargetView == null) return 0;
        RecyclerView.ViewHolder viewHolder = mCallback.getChildViewHolder(mTargetView);
        return mCallback.getHorizontalRange(viewHolder);
    }


    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {

        if (Math.abs(velocityX) > mMinVelocity && Math.abs(velocityX) < mMaxVelocity) {
            if (!smoothHorizontalExpandOrCollapse(velocityX)) {
                if (isCollapsed()) mTargetView = null;
                return true;
            }
        }
        return false;
    }


    public interface Callback {

        int getHorizontalRange(RecyclerView.ViewHolder holder);

        RecyclerView.ViewHolder getChildViewHolder(View childView);

        @Nullable
        View findTargetItemView(float x, float y);

        View findTargetMenuView(RecyclerView.ViewHolder holder);
    }

    public abstract static class SimpleCallback {
        @Nullable
        public abstract View findTargetMenuView(RecyclerView.ViewHolder holder);
    }
}
