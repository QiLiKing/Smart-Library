package com.qlk.core.tasks

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.annotation.CallSuper
import com.qlk.core.Durations.Companion.VISUAL_PAUSE
import com.qlk.core.lifecycle.ISimpleLifecycle
import com.qlk.core.extensions.isInitialized

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-06-07.
 */

typealias LiveTask = () -> Unit

/**
 * Store your future tasks, and you can execute them whenever you want.
 *
 * @param lifecycleOwner control your tasks automatically
 */
open class SmartTask<K> @JvmOverloads constructor(
    lifecycleOwner: LifecycleOwner? = null
) : ISimpleLifecycle, LifecycleObserver {

    protected val tasks: HashMap<K, LiveTask> = hashMapOf()
    @Volatile
    protected var isActive = lifecycleOwner == null
    private val suspendTasks: HashSet<K> by lazy { hashSetOf<K>() }

    init {
        lifecycleOwner?.lifecycle?.run {
            addObserver(this@SmartTask)
        }
    }

    fun containsTask(k: K): Boolean = tasks.containsKey(k)

    open fun setTask(k: K, task: LiveTask) {
        synchronized(tasks) {
            tasks[k] = task
        }
    }

    /**
     * @param oneOff true - remove the task at the same time
     */
    open fun runTask(k: K, oneOff: Boolean = false) {
        if (isActive) {
            tasks[k]?.run {
                invoke()
                if (oneOff) {
                    removeTask(k)
                }
            }
        } else {
            synchronized(suspendTasks) {
                suspendTasks.add(k)
            }
        }
    }

    /**
     * execute your tasks one by one
     */
    open fun runTasks(ks: List<K> = tasks.keys.toList(), oneOff: Boolean = false) {
        ks.forEach {
            runTask(it)
        }
    }

    open fun removeTask(k: K) {
        synchronized(tasks) {
            tasks.remove(k)
        }
        synchronized(suspendTasks) {
            suspendTasks.remove(k)
        }
    }

    fun removeTasks(ks: List<K> = tasks.keys.toList()) {
        ks.forEach {
            removeTask(it)
        }
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    override fun onActive() {
        isActive = true
        synchronized(suspendTasks) {
            suspendTasks.forEach {
                runTask(it)
            }
            suspendTasks.clear()
        }
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    override fun onInactive() {
        isActive = false
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    override fun onRecycle() {
        super.onRecycle()
        removeTasks()
    }

}

/**
 * execute tasks by their order: ASC & DESC
 */
open class OrderTask<T : Comparable<T>> @JvmOverloads constructor(
    lifecycleOwner: LifecycleOwner? = null,
    private val ascSort: Boolean = true /* sort by ASC or DESC */
) : SmartTask<T>(lifecycleOwner) {

    override fun runTasks(ks: List<T>, oneOff: Boolean) {
        if (ascSort) {
            super.runTasks(ks.sorted(), oneOff)
        } else {
            super.runTasks(ks.sortedDescending(), oneOff)
        }
    }
}

/**
 * If the initialization thread has a looper, [LazyTask] will post through the same looper, otherwise through a new looper of [ShortLifeHandlerThread]
 */
@Suppress("UNCHECKED_CAST")
open class LazyTask<K> @JvmOverloads constructor(
    lifecycleOwner: LifecycleOwner? = null,
    private val delay: Long = VISUAL_PAUSE,
    private val quickInitial: Boolean = true /* execute immediately for the first time */
) : SmartTask<K>(lifecycleOwner) {
    private val recorder = hashSetOf<K>()
    private var looper: Looper? = Looper.myLooper()

    private val handler by lazy {
        // This class is just a task dispatcher, very light weight, so "shared-handler-thread" is appropriate.
        object : Handler(looper ?: ShortLifeHandlerThread.getShared().bindLooper()) {
            override fun handleMessage(msg: Message) {
                runTask(msg.obj as K)
            }
        }
    }

    override fun setTask(k: K, task: LiveTask) {
        super.setTask(k, task)
        if (!quickInitial || recorder.contains(k)) {
            if (!handler.hasMessages(Lazy, k)) {
                handler.sendMessageDelayed(Message.obtain(handler,
                    Lazy, k), delay)
            }
        } else {
            recorder.add(k)
            task.invoke()
        }
    }

    override fun runTask(k: K, oneOff: Boolean) {
        super.runTask(k, oneOff)
        handler.removeMessages(Lazy, k)
    }

    override fun removeTask(k: K) {
        super.removeTask(k)
        handler.removeMessages(Lazy, k)
    }

    override fun onRecycle() {
        super.onRecycle()
        if (this::handler.isInitialized) {
            ShortLifeHandlerThread.getShared().quitSafely()
        }
    }

    companion object {
        const val Lazy = 0
    }
}

open class SimpleLazyTask @JvmOverloads constructor(
    lifecycleOwner: LifecycleOwner? = null,
    delay: Long = VISUAL_PAUSE,
    quickInitial: Boolean = true
) : LazyTask<Int>(lifecycleOwner, delay, quickInitial) {

    fun first(task: LiveTask) = setTask(FIRST, task)
    fun second(task: LiveTask) = setTask(SECOND, task)
    fun third(task: LiveTask) = setTask(THIRD, task)
    fun forth(task: LiveTask) = setTask(FORTH, task)
    fun fifth(task: LiveTask) = setTask(FIFTH, task)
    /* If your task more than five, you may consider to add another LazyRun instance to manager them. As too many tasks can easily make you confused! */
    fun sixth(task: LiveTask) = setTask(SIXTH, task)

    companion object {
        const val FIRST = 1
        const val SECOND = 2
        const val THIRD = 3
        const val FORTH = 4
        const val FIFTH = 5
        const val SIXTH = 6
    }
}

/**
 * Its lower priority task will be covered by higher one.
 */
class PriorityTask @JvmOverloads constructor(
    lifecycleOwner: LifecycleOwner? = null,
    delay: Long = VISUAL_PAUSE,
    quickInitial: Boolean = true
) : SimpleLazyTask(lifecycleOwner, delay, quickInitial) {

    override fun setTask(k: Int, task: LiveTask) {
        synchronized(tasks) {
            val iterator = tasks.iterator()
            while (iterator.hasNext()) {
                with(iterator.next()) {
                    if (key < k) {
                        iterator.remove()
                    } else if (key > k) {
                        if (containsTask(key)) {
                            return@synchronized
                        }
                    }
                }
            }
            super.setTask(k, task)
        }
    }

}

