package com.qlk.core.tasks

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.qlk.core.Durations
import java.util.concurrent.atomic.AtomicInteger

/**
 * A lightweight thread, you should not do long time tasks with it.
 */
class ShortLifeHandlerThread private constructor(
    name: String = defaultThreadName,
    private val duration: Long = Durations.MINUTE
) : HandlerThread(name) {

    private val quitHandler: Handler by lazy { Handler(looper) }
    private val outerBindCount: AtomicInteger = AtomicInteger(0)

    @Deprecated("Use safe quit instead.", ReplaceWith("quitSafely"), DeprecationLevel.HIDDEN)
    override fun quit(): Boolean {
        return super.quit()
    }

    /**
     * @hide
     */
    @Deprecated("Should not call this method externally.", ReplaceWith("bindLooper"), DeprecationLevel.WARNING)
    override fun getLooper(): Looper {
        return super.getLooper()
    }

    /**
     * @return null thread has been destroyed.
     */
    fun bindLooper(): Looper? = looper.also { outerBindCount.incrementAndGet() }

    override fun quitSafely(): Boolean {
        if (outerBindCount.decrementAndGet() <= 0) {
            if (isAlive) {
                if (duration > 0) {
                    quitHandler.postDelayed({ if (outerBindCount.get() <= 0) super.quitSafely() }, duration)
                } else {
                    super.quitSafely()
                }
            }
        }
        return true
    }

    companion object {
        private const val SHARE_NAME = "ShareHandlerThread"
        private var sharedThread: ShortLifeHandlerThread? = null
            get() {
                if (field == null || !field!!.isAlive /* destroyed */) {
                    field = ShortLifeHandlerThread(SHARE_NAME)
                        .also { it.start(); field = it }
                }
                return field
            }

        /**
         * You needn't call {@method start}, it's auto start.
         */
        @JvmStatic
        fun getShared(): ShortLifeHandlerThread = sharedThread!!

        @JvmStatic
        @JvmOverloads
        fun create(name: String, duration: Long = Durations.MINUTE, autoStart: Boolean = true): ShortLifeHandlerThread {
            return ShortLifeHandlerThread(name, duration).also {
                if (autoStart) it.start()
            }
        }
    }
}

val defaultThreadName: String get() = "Thread" + System.currentTimeMillis().toString()