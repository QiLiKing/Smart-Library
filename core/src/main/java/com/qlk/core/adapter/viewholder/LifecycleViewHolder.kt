package com.qlk.core.adapter.viewholder

import android.arch.lifecycle.*
import android.support.annotation.CallSuper
import android.support.v7.widget.RecyclerView
import android.view.View
import com.easilydo.tools.performance.ISimpleLifecycle
import com.qlk.core.isInDebugMode
import com.qlk.core.lifecycle.MediatorLifecycle

/**
 * @see LazyLoadViewHolder
 */
abstract class LifecycleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
    LifecycleOwner,
    ISimpleLifecycle, LifecycleObserver {
    private var mediatorLifecycle: MediatorLifecycle? = null

    private val internalLifecycle: LifecycleRegistry by lazy {
        LifecycleRegistry(this).also { it.addObserver(this) }
    }

    @Deprecated(
        "It is inaccurate for mediator lifecycle.",
        ReplaceWith("onViewHolderActive"),
        DeprecationLevel.WARNING    //Java has issue if use HIDDEN
    )
    override fun onActive() {
        internalLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @Deprecated(
        "It is inaccurate for mediator lifecycle.",
        ReplaceWith("onViewHolderInactive"),
        DeprecationLevel.WARNING
    )
    override fun onInactive() {
        internalLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    @CallSuper
    override fun onRecycle() {
        internalLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        //should not remove, continue to observe for next bind.
//        internalLifecycle.removeObserver(internalObserver)
//        mediatorLifecycle?.removeObserver(internalObserver)
    }

    override fun getLifecycle(): Lifecycle = mediatorLifecycle ?: internalLifecycle

    /* Sources */

    fun addSource(lifecycleOwner: LifecycleOwner) {
        addSource(lifecycleOwner.lifecycle)
    }

    fun removeSource(lifecycleOwner: LifecycleOwner) {
        removeSource(lifecycleOwner.lifecycle)
    }

    fun addSource(lifecycle: Lifecycle) {
        val mediator = mediatorLifecycle ?: MediatorLifecycle(this).also {
            it.addSource(internalLifecycle)
            it.addObserver(this)
            internalLifecycle.removeObserver(this)
        }
        mediator.addSource(lifecycle)
    }

    fun removeSource(lifecycle: Lifecycle) {
        mediatorLifecycle?.removeSource(lifecycle)
    }

    /* internal observer */

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected fun onViewHolderActive() {
        if (isInDebugMode) println("LifecycleViewHolder#onViewHolderActive:73-> $adapterPosition")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    protected fun onViewHolderInactive() {
        if (isInDebugMode) println("LifecycleViewHolder#onViewHolderInactive:79-> $adapterPosition")
    }
}