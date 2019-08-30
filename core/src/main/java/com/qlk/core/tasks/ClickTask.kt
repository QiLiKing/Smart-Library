package com.qlk.core.tasks

import android.arch.lifecycle.LifecycleOwner
import android.view.View
import com.qlk.core.Durations.Companion.VIOLENT_INTERVAL

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-15 14:35
 */
open class ClickTask<K> @JvmOverloads constructor(lifecycleOwner: LifecycleOwner? = null) :
    SmartTask<K>(lifecycleOwner) {
    private val lastClickTime: HashMap<K, Long> = HashMap()
    private val clickTimes: HashMap<K, Int> = HashMap()

    fun click(k: K) {
        runTask(k)
        reset(k)
    }

    fun violentClick(k: K, interval: Long = VIOLENT_INTERVAL) {
        if (interval <= 0) click(k)

        val passed = System.currentTimeMillis() - getLastClickTime(k)
        if (passed > interval) {
            click(k)
        } else {
            violentPreventListener?.onViolentPrevented(k)
        }
    }

    fun timesClick(k: K, threshold: Int, interval: Long = VIOLENT_INTERVAL) {
        if (threshold < 1 || interval <= 0) click(k)

        val last = lastClickTime[k] ?: 0
        val now = System.currentTimeMillis()
        if (now - last > interval) {
            reset(k)
        } else {
            increaseRecorder(k)
        }
        if (getClickTimes(k) == threshold) {
            click(k)
        }
    }

    fun getLastClickTime(k: K) = lastClickTime[k] ?: 0L

    fun getClickTimes(k: K) = clickTimes[k] ?: 0

    fun reset(k: K) {
        val oldTimes = getClickTimes(k)
        lastClickTime.remove(k)
        clickTimes.remove(k)
        if (oldTimes != 0) {
            timesChangedListener?.onTimesChanged(k, 0)
        }
    }

    override fun removeTask(k: K) {
        super.removeTask(k)
        reset(k)
    }

    private fun increaseRecorder(k: K) {
        lastClickTime[k] = System.currentTimeMillis()
        val times = getClickTimes(k) + 1
        clickTimes[k] = times
        timesChangedListener?.onTimesChanged(k, times)
    }

    /* Listeners */

    private var timesChangedListener: OnTimesClickChangedListener<K>? = null
    private var violentPreventListener: OnViolentClickPreventListener<K>? = null

    fun setOnClickTimesChangedListener(listener: OnTimesClickChangedListener<K>) {
        this.timesChangedListener = listener
    }

    fun setOnViolentClickPreventListener(listener: OnViolentClickPreventListener<K>) {
        this.violentPreventListener = listener
    }

    abstract class Builder<K>(private val k: K) {
        var lifecycleOwner: LifecycleOwner? = null
        var interval: Long = VIOLENT_INTERVAL
        protected var task: LiveTask? = null
        protected open var times: Int? = 0
        protected var timesChangedListener: OnTimesClickChangedListener<K>? = null
        protected var violentPreventListener: OnViolentClickPreventListener<K>? = null

        fun whenClicked(task: LiveTask) {
            this.task = task
        }

        fun build(): ClickTask<K> {
            return ClickTask<K>(lifecycleOwner).apply {
                val threshold = times ?: 0
                if (threshold > 0) {
                    timesClick(k, threshold, interval)
                } else {
                    violentClick(k, interval)
                }
                if (task != null) {
                    setTask(k, task!!)
                }
            }
        }
    }

    class ViolentBuilder<K>(k: K) : Builder<K>(k) {

        fun whenPrevented(action: (K) -> Unit) {
            violentPreventListener = object : OnViolentClickPreventListener<K> {
                override fun onViolentPrevented(k: K) {
                    action.invoke(k)
                }
            }
        }
    }

    class TimesBuilder<K>(k: K) : Builder<K>(k) {
        override var times: Int? = 1

        fun whenTimesChanged(action: (Int) -> Unit) {
            timesChangedListener = object : OnTimesClickChangedListener<K> {
                override fun onTimesChanged(k: K, times: Int) {
                    action.invoke(times)
                }
            }
        }
    }
}

/**
 * You can implement functions like:
 *      Prevent user clicking too fast(attachViolentClick)
 *      Double click to exit application(attachTimesClick)
 *      Click several times to open a hidden function(e.g. android phone's development mode)
 *
 * Please note that violent mode and times mode are mutually exclusive.
 */
class ViewClickTask @JvmOverloads constructor(lifecycleOwner: LifecycleOwner? = null) :
    ClickTask<Int /* do NOT use "View" as type to avoid memory leak */>(lifecycleOwner) {

    fun attachClick(view: View, task: LiveTask) {
        setTask(view.hashCode(), task)
        view.setOnClickListener { click(it.hashCode()) }
    }

    fun attachViolentClick(view: View, interval: Long = VIOLENT_INTERVAL, task: LiveTask) {
        setTask(view.hashCode(), task)
        view.setOnClickListener { violentClick(it.hashCode(), interval) }
    }

    fun attachTimesClick(view: View, threshold: Int, interval: Long = VIOLENT_INTERVAL, task: LiveTask) {
        setTask(view.hashCode(), task)
        view.setOnClickListener { timesClick(it.hashCode(), threshold, interval) }
    }

    fun detach(view: View) {
        removeTask(view.hashCode())
    }

    companion object {
        val sharedInstance: ViewClickTask by lazy { ViewClickTask() }

        @JvmStatic
        fun getSharedTask(): ViewClickTask =
            sharedInstance
    }
}

interface OnTimesClickChangedListener<K> {
    fun onTimesChanged(k: K, times: Int)
}

interface OnViolentClickPreventListener<K> {
    fun onViolentPrevented(k: K)
}