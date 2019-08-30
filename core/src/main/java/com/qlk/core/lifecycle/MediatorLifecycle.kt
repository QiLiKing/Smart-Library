package com.qlk.core.lifecycle

import android.arch.lifecycle.*
import com.qlk.core.extensions.isActive

/**
 *
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-14.
 */
class MediatorLifecycle(lifecycleOwner: LifecycleOwner) : LifecycleRegistry(lifecycleOwner),
    LifecycleObserver {

    private val sources = HashSet<Lifecycle>()
    private var listener: ISimpleLifecycle? = null

    fun addSource(lifecycle: Lifecycle) {
        synchronized(sources) {
            if (sources.add(lifecycle)) {
                lifecycle.addObserver(this)
            }
        }
    }

    fun removeSource(lifecycle: Lifecycle) {
        synchronized(sources) {
            if (sources.remove(lifecycle)) {
                lifecycle.removeObserver(this)
            }
        }
    }

    @OnLifecycleEvent(Event.ON_START)
    fun onStart() {
        synchronized(sources) {
            val allActive = sources.all { it.isActive }
            if (allActive) {
                handleLifecycleEvent(Event.ON_START)
                listener?.onActive()
            }
        }
    }

    @OnLifecycleEvent(Event.ON_STOP)
    fun onStop() {
        if (listener != null && isActive) {
            listener?.onInactive()
        }
        handleLifecycleEvent(Event.ON_STOP)
    }

    @OnLifecycleEvent(Event.ON_DESTROY)
    fun onDestroy() {
        handleLifecycleEvent(Event.ON_DESTROY)
        synchronized(sources) {
            if (listener != null && sources.none { it.currentState == State.DESTROYED }) {
                listener?.onRecycle()
            }
            sources.forEach { lifecycle ->
                if (lifecycle.currentState == State.DESTROYED) {
                    removeSource(lifecycle)
                }
            }
        }
    }

    fun setOnLifecycleListener(listener: ISimpleLifecycle?) {
        this.listener = listener
    }
}