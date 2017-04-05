package com.guohaiyang.swipelistviewlib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;

/**
 * Created by guohaiyang on 2017/3/30.
 * <p>
 * *限制item布局为LinerLayout横向，且前一个布局设置match_parent,后面的为删除按钮
 * <p>
 * 功能点：
 * 1.滑动删除：跟随手指滑动出现删除页面
 * 2.滑动回弹：超过3/5的距离则显示全部，反之关闭
 * 3.自动关闭：垂直滑动的时候自动关闭打开的界面，打开其他行的视图的时候自动关闭另一行的视图，确保只有一个
 * 4.删除动画：当删除的时候，视图会快速恢复状态并收缩，收缩完毕进行删除，并刷新页面，回调只需进行数据源的删除即可，其他的都无需去做
 */

public class SwipeListView extends ListView {
    //-------------TAG-----------------------
    private static final String TAG = "SwipeListView";

    //-------------参数-----------------------

    //用户滑动的最小距离
    private int touchSlop;
    //按下的x坐标
    private int xDown;
    //按下的y坐标
    private int yDown;
    //滑动时的x坐标
    private int xMove;
    //手指移动时的y坐标
    private int yMove;
    //x,y方向每次的变化量
    private int dx, dy;

    //-------------回调-----------------------

    //删除回调
    private onDeleteLisener deleteLisener;

    //-------------视图相关-----------------------

    //当前触摸的itemView(rootView)
    private ViewGroup mCurrentView;

    //手指触碰的位置
    private int mCurrentViewPos;

    //删除的视图
    private View deleteView;

    //删除视图的宽度
    private int scrollwidth; //需要滑动的距离

    //滑动工具
    private Scroller scroller;

    //滑动方向
    private int direction = Direction.NONE;

    //用户传入的adapter，用于刷新数据
    private BaseAdapter adapter;

    //删除动画
//    ValueAnimator deleteAnimator;

    public SwipeListView(Context context) {
        this(context, null);
    }

    public SwipeListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initConfig();
    }

    /**
     * 初始化基础配置
     */
    private void initConfig() {
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();//当前手机的最小滑动距离
        scroller = new Scroller(getContext());
    }

    /**
     * 获取用户设置的adapter
     *
     * @param adapter 用户传入的adapter
     */
    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        this.adapter = (BaseAdapter) adapter;
    }

    /**
     * 当前视图触摸事件的始发方法，用于设置触摸位置和判断滑动方向用于ontouch进行实际操作
     *
     * @param ev 触摸事件
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (action) {

            case MotionEvent.ACTION_DOWN:
                setDownPoint(x, y);//记录按下的点
                break;
            case MotionEvent.ACTION_MOVE:
                updateSlideState(x, y);//根据滑动判断滑动方向
                break;
        }
        return super.dispatchTouchEvent(ev);
    }


    /**
     * 初始化删除视图，更新宽度和点击事件
     */
    private void initDeleteView() {
        if (mCurrentView != null) {
            if (mCurrentView.getChildCount() >= 2) {
                deleteView = mCurrentView.getChildAt(1);
                scrollwidth = deleteView.getWidth();
                initDeleteViewClickLisner(deleteView);
            }

        }
    }

    /**
     * 设置删除视图的点击事件
     *
     * @param deleteView 删除视图
     */
    private void initDeleteViewClickLisner(View deleteView) {
        deleteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //首先判断是否有删除回调
                if (deleteLisener != null) {
                    //快速恢复视图的原状态（由于视图重用的问题，如果此处不恢复会导致下面的视图仍为展开状态）
                    quikResetAllView();
                    //回调删除接口，只用于数据的变化
                    deleteLisener.onSelectDelete(mCurrentViewPos);
                    //启动删除动画，删除完毕会notifyDatachange()
                    smoothDeleteAnim(mCurrentView);
                }
            }
        });
    }


    /**
     * 带动画的重置view状态方法
     */
    private void smoothResetAllView() {
        resetScroll();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view.getScrollX() != 0) {//只存在一个view是开启的
                smoothScroll(view, view.getScrollX(), 0);
                return;
            }
        }
    }

    /**
     * 不带动画的重置view方法
     */
    private void quikResetAllView() {
        resetScroll();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view.getScrollX() != 0) {//只存在一个view是开启的
                view.scrollTo(0, 0);
                return;
            }
        }
    }

    /**
     * 使view横向滑动（带动画）
     *
     * @param view   需要滑动的view
     * @param beigin 滑动的开始位置
     * @param end    滑动的结束位置
     */
    public void smoothScroll(final View view, int beigin, int end) {
        ValueAnimator animator = ValueAnimator.ofInt(beigin, end);
        animator.setDuration(Math.abs(beigin - end) * 2);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                view.scrollTo(value, 0);
            }
        });
        animator.start();


    }

    /**
     * 通过计算dx,dy来计算滑动的方向
     *
     * @param x 当前按下的点
     * @param y 当前按下的点
     */
    private void updateSlideState(int x, int y) {
        dx = x - xMove;
        dy = y - yMove;

        if (direction == Direction.NONE) { //当方向不确定的时候进行确定，否则一直按照上次的方向
            updateDirectionWhenNone(dx, dy);
        }

        setMovePoint(x, y);//更新新的位置
    }

    /**
     * 更新方向
     *
     * @param dx x方向滑动的距离
     * @param dy y方向滑动的距离
     */
    private void updateDirectionWhenNone(int dx, int dy) {
        if (Math.abs(dx) > Math.abs(dy)) { //横向距离大于纵向
            direction = Direction.HORIZONTAL;
            Log.v("scroll", "判定方向为横向");
            ViewGroup tempView = getPressView();//获取当前点击的view
            resetLastView(tempView);//重置上一次的view
            mCurrentView = tempView;//设置当前点击的view
            initDeleteView();//初始化删除视图
        } else {
            direction = Direction.VERTICAL;
            Log.v("scroll", "判定方向为纵向");
            smoothResetAllView();//滑动的时候重置view
        }

    }

    /**
     * 重置上一个view，如果当前的和之前的一致则不做处理
     *
     * @param temp
     */
    private void resetLastView(ViewGroup temp) {
        if (mCurrentView != null && !mCurrentView.equals(temp)) {//等同于上一个view则不做处理 或者没有上一个oldView
            smoothScroll(mCurrentView, mCurrentView.getScrollX(), 0);
        }
    }


    /**
     * 获取点击的视图
     *
     * @return
     */
    private ViewGroup getPressView() {
        mCurrentViewPos = pointToPosition(xDown, yDown);// 获取点击的位置
        if (mCurrentViewPos == -1) {
            Log.v("error", "mCurrentViewPos == -1");
            return null;
        }
        int viewIndex = mCurrentViewPos - getFirstVisiblePosition(); //获取可视范围内点击的view 的索引
        if (viewIndex < 0) {
            Log.v("error", "viewIndex < 0");
            return null;
        }
        // 获得当前手指按下时的item
        ViewGroup view = (ViewGroup) getChildAt(mCurrentViewPos - getFirstVisiblePosition()); //通过缓存的视图集合和视图id获取对应的视图
        Log.v("error", "有view");
        return view;
    }

    /**
     * 设置按下的位置信息
     *
     * @param x event.getX();
     * @param y event.getY();
     */
    public void setDownPoint(int x, int y) {
        xMove = xDown = x;
        yMove = yDown = y;
    }

    /**
     * 设置移动的最新位置信息
     *
     * @param x
     * @param y
     */
    public void setMovePoint(int x, int y) {
        xMove = x;
        yMove = y;
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        //判断方向：横向滑动
        if (direction == Direction.HORIZONTAL) {
            if (action == MotionEvent.ACTION_MOVE) {
                startScroll();//开始滑动
                Log.v("scroll", "横向滑动");
                return true; // 相应滑动期间屏幕itemClick事件，避免发生冲突
            }
        }
        //抬手
        if (action == MotionEvent.ACTION_UP) {
            //判断之前是什么状态，如果是滑动，则判断是否进行华东到最终距离反之滑动返回
            updateScrollWidth();
            //横向滑动的时候屏蔽点击
            if (direction == Direction.HORIZONTAL) {
                direction = Direction.NONE;
                return true;
            }
            //重置状态
            direction = Direction.NONE;
            Log.v("scroll", "清除滑动");
        }

        return super.onTouchEvent(ev);
    }

    /**
     * 更新滑动的距离：大于3/5 则滑动为全部，反之关闭
     */
    private void updateScrollWidth() {
        if (direction == Direction.HORIZONTAL) {
            //判断当前有没有view
            if (mCurrentView != null && deleteView != null) {
                //判断滑动的距离是否超过删除的长度一半，超过则将剩余的显示，反之返回
                if (mCurrentView.getScrollX() >= scrollwidth / 5 * 3) {//显示全部
                    resetScroll();
                    smoothScrollTo(scrollwidth - mCurrentView.getScrollX());
                    Log.v("update", "显示全部");
                } else {
                    resetScroll();
                    smoothScrollTo(-mCurrentView.getScrollX());
                    Log.v("update", "恢复：" + mCurrentView.getScrollX());
                }
            }
        }

    }

    /**
     * 滑动，跟随手指滑动
     */
    private void startScroll() {
        if (mCurrentView != null) {
            Log.v("start", mCurrentView.getScrollX() + ":" + dx);
            if (mCurrentView.getScrollX() - dx >= 0 && mCurrentView.getScrollX() <= scrollwidth + dx) {  //优化左右范围限制
                mCurrentView.scrollBy(-dx, 0);  //大于零往左走，小于往右走

            }
        }
    }

    /**
     * 当调用invalidate()-->ondraw()-->computeScroll()
     */
    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            childScrollTo(scroller.getCurrX());
            invalidate();
        }
        Log.v("test", "test");

    }

    /**
     * 对子视图进行滑动
     *
     * @param x
     */
    public void childScrollTo(int x) {
        if (mCurrentView != null) {
            mCurrentView.scrollTo(x, scroller.getCurrY());
            Log.v("childScorll", "成功");
        }
    }

    public void setDeleteLisener(onDeleteLisener deleteLisener) {
        this.deleteLisener = deleteLisener;
    }


    public interface onDeleteLisener {
        public void onSelectDelete(int position);
    }

    //方向类
    interface Direction {
        public static final int NONE = 2;
        public static final int VERTICAL = 0;
        public static final int HORIZONTAL = 1;
    }

    /**
     * 重置scroller 的状态
     */
    public void resetScroll() {
        if (scroller != null && !scroller.isFinished()) {
            scroller.abortAnimation();
        }
    }

    /**
     * 带动画的滑动
     *
     * @param delta
     */
    private void smoothScrollTo(int delta) {
        Log.v("smooth", "开始:" + delta);
        if (mCurrentView != null) {
            Log.v("smooth", "成功");
            // 缓慢滚动到指定位置
            int scrollX = mCurrentView.getScrollX();
            scroller.startScroll(scrollX, 0, delta, 0, Math.abs(delta) * 2);
            invalidate();
        }
    }

    /**
     * 删除动画
     */
    private void smoothDeleteAnim(final ViewGroup deleteTempView) {
        if (deleteTempView != null) {
            ValueAnimator valueAnimator = ValueAnimator.ofInt(deleteTempView.getHeight(), 0);
            valueAnimator.setDuration(500);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (int) animation.getAnimatedValue();
                    deleteTempView.getLayoutParams().height = value;
                    deleteTempView.requestLayout();
                }
            });

            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    super.onAnimationEnd(animation);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                        Log.v("end", "end");
                    }
                }
            });
            valueAnimator.start();
        }
    }


}
