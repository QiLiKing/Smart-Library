package com.qlk.core

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.support.annotation.NonNull
import android.view.TouchDelegate
import android.view.View

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-12-31
 */
object SmartView {
    @JvmStatic
    fun isPointInView(view: View, rx: Int, ry: Int): Boolean {
        val l = IntArray(2)
        view.getLocationOnScreen(l)
        val x = l[0]
        val y = l[1]
        val w = view.width
        val h = view.height
        return !(rx < x || ry < y || rx > x + w || ry > y + h)
    }

    /**
     * more efficient method than View's setAlpha.
     * setting alpha to a translucent value (0 < alpha < 1) can have significant performance implications, especially for large views.
     */
    @JvmStatic
    fun setViewAlpha(v: View?, alpha: Float) {
        if (v == null) {
            return
        }
        val drawable = v.background
        if (drawable is ColorDrawable) {
            val bgColor = drawable.color
            val newBgColor = Color.argb(
                (alpha * 255).toInt(),
                Color.red(bgColor),
                Color.green(bgColor),
                Color.blue(bgColor)
            )
            v.setBackgroundColor(newBgColor)
        } else {
            v.alpha = alpha
        }
    }

    /**
     * change the touch range of the target view, minus value to move left or top, plus value to move right or bottom
     *
     * @param view   the target view
     * @param left   left touch range change: "-" value move left;"+" value move right
     * @param top    top range change
     * @param right  right range change
     * @param bottom bottom range change
     */
    fun changeTouchRange(
        @NonNull view: View, left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        val parent = view.parent
        if (parent is View) {
            view.post {
                //使用post runnable的方式去设置Delegate区域大小的原因是，如该View师在Activity的OnCreate()或Fragment的OnCreateView()中绘制，此时UI界面尚未开始绘制，无法获得正确的坐标
                val rect = Rect()
                view.getHitRect(rect)
                rect.left += left
                rect.top += top
                rect.right += right
                rect.bottom += bottom
                (parent as View).touchDelegate = TouchDelegate(rect, view)
            }
        } else {
            throw RuntimeException("the view's parent is not a View!")
        }
    }

    /**
     * 还原View的触摸和点击响应范围,最小不小于View自身范围
     *
     * @param view
     */
    fun restoreViewTouchRange(@NonNull view: View) {
        val parent = view.parent
        if (parent is View) {
            view.post {
                //使用post runnable的方式去设置Delegate区域大小的原因是，如该View师在Activity的OnCreate()或Fragment的OnCreateView()中绘制，此时UI界面尚未开始绘制，无法获得正确的坐标
                val rect = Rect()
                rect.setEmpty()
                (parent as View).touchDelegate = TouchDelegate(rect, view)
            }
        } else {
            throw RuntimeException("the view's parent is not a View!")
        }
    }
}