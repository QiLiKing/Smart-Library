package com.qlk.core.adapter.delegate

import android.support.annotation.MainThread
import android.support.v7.widget.RecyclerView
import com.qlk.core.isInDebugMode

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-15.
 */
interface OnScrollStateDelegateChangedListener {
    fun onScrollStateDelegateChanged(newDelegate: IScrollStateDelegate?)
}

interface IScrollStateDelegate {
    /**
     * Sometimes that we maybe need full loading during user dragging(state = SCROLL_STATE_DRAGGING), like Gallery App, is when to override this method!
     */
    fun isScrolling(): Boolean = getCurrentScrollState() != RecyclerView.SCROLL_STATE_IDLE

    fun getCurrentScrollState(): Int

    fun onAttachedToRecyclerView(recyclerView: RecyclerView)

    fun onDetachedFromRecyclerView(recyclerView: RecyclerView)

    fun addOnScrollStateChangedListener(onScrollStateChangedListener: OnScrollStateChangedListener)

    fun removeOnScrollStateChangedListener(onScrollStateChangedListener: OnScrollStateChangedListener)

}

interface OnScrollStateChangedListener {
    @MainThread
    fun onScrollStateChanged(newState: Int)
}

class ScrollStateDelegateImpl : IScrollStateDelegate {
    private var scrollState: Int = RecyclerView.SCROLL_STATE_IDLE
    private val listeners: HashSet<OnScrollStateChangedListener> by lazy { HashSet<OnScrollStateChangedListener>() }

    override fun getCurrentScrollState(): Int = scrollState

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        scrollState = RecyclerView.SCROLL_STATE_IDLE
        recyclerView.addOnScrollListener(scrollListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.removeOnScrollListener(scrollListener)
//        listeners.clear() // if clear, user will not receive events after reattachToRecyclerView
    }

    override fun addOnScrollStateChangedListener(onScrollStateChangedListener: OnScrollStateChangedListener) {
        synchronized(listeners) {
            listeners.add(onScrollStateChangedListener)
        }
    }

    override fun removeOnScrollStateChangedListener(onScrollStateChangedListener: OnScrollStateChangedListener) {
        synchronized(listeners) {
            listeners.remove(onScrollStateChangedListener)
        }
    }

    private val scrollListener: RecyclerView.OnScrollListener by lazy {
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                scrollState = newState
                if (isInDebugMode) println("ScrollStateDelegate#onScrollStateChanged:79-> $newState $listeners")
                synchronized(listeners) {
                    listeners.forEach {
                        it.onScrollStateChanged(newState)
                    }
                }
            }
        }
    }
}