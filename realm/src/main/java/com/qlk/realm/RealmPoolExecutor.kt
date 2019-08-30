package com.qlk.realm

import android.os.HandlerThread
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-08-27.
 */
internal class RealmPoolExecutor(poolSize: Int) : ThreadPoolExecutor(
    poolSize, poolSize,
    0, TimeUnit.MILLISECONDS,
    LinkedBlockingQueue<Runnable>(),
    object : ThreadFactory {
        private val poolNumber = AtomicInteger(1)
        private val threadNumber = AtomicInteger(1)
        private val namePrefix: String = "pool-" + poolNumber.getAndIncrement() + "-thread-"
        override fun newThread(r: Runnable): Thread {
            //use HandlerThread to avoid realm size growth.
            val t = HandlerThread(namePrefix + threadNumber.getAndIncrement(), Thread.NORM_PRIORITY)
            if (t.isDaemon)
                t.isDaemon = false
            //NOTE: the priority check is necessary!
            if (t.priority != Thread.NORM_PRIORITY)
                t.priority = Thread.NORM_PRIORITY
            return t
        }
    }) {

    override fun beforeExecute(t: Thread?, r: Runnable?) {
        super.beforeExecute(t, r)
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        super.afterExecute(r, t)
    }

}