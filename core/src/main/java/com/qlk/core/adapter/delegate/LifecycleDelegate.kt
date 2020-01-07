package com.qlk.core.adapter.delegate

import android.support.annotation.CallSuper
import android.support.v7.widget.RecyclerView
import com.easilydo.tools.performance.ISimpleLifecycle
import com.easilydo.tools.performance.Recyclable
import com.qlk.core.tasks.SmartTask

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-15.
 */
interface OnLifecycleDelegateChangedListener<VH : RecyclerView.ViewHolder> {
    fun onLifecycleDelegateChanged(newDelegate: ILifecycleDelegate<VH>?)
}

/**
 * Need: ScrollStateDelegate
 */
interface ILifecycleDelegate<VH : RecyclerView.ViewHolder> : OnScrollStateDelegateChangedListener {

    fun onViewAttachedToWindow(holder: VH)

    fun onViewDetachedFromWindow(holder: VH)

    fun onViewRecycled(holder: VH)
}

open class LifecycleDelegateImpl<VH : RecyclerView.ViewHolder> : ILifecycleDelegate<VH> {
    private val smartTask: SmartTask<Int> by lazy { SmartTask<Int>() }

    private var scrollStateDelegate: IScrollStateDelegate? = null

    override fun onScrollStateDelegateChanged(newDelegate: IScrollStateDelegate?) {
        if (newDelegate == scrollStateDelegate) return

        scrollStateDelegate?.removeOnScrollStateChangedListener(scrollStateChangedListener)
        scrollStateDelegate = newDelegate
        scrollStateDelegate?.addOnScrollStateChangedListener(scrollStateChangedListener)
    }

    override fun onViewAttachedToWindow(holder: VH) {
        if (holder is ISimpleLifecycle) {
            if (scrollStateDelegate?.isScrolling() == true) {
                smartTask.setTask(holder.adapterPosition) {
                    if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                        activeViewHolder(holder)
                    }
                }
            } else {
                activeViewHolder(holder)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        if (holder is ISimpleLifecycle) {
            smartTask.removeTask(holder.adapterPosition)
            holder.onInactive()
        }
    }

    override fun onViewRecycled(holder: VH) {
        if (holder is Recyclable) {
            holder.onRecycle()
        }
    }

    @CallSuper
    protected open fun activeViewHolder(holder: VH) {
        (holder as? ISimpleLifecycle)?.onActive()
    }

    //If implement as the class interface, out caller may dirty the sate
    private val scrollStateChangedListener: OnScrollStateChangedListener by lazy {
        object : OnScrollStateChangedListener {
            override fun onScrollStateChanged(newState: Int) {
                if (scrollStateDelegate?.isScrolling() == false) {
                    smartTask.runTasks(oneOff = true)
                }
            }
        }
    }
}