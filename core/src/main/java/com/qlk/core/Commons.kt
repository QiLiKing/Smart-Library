package com.qlk.core

import android.os.Looper
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-08-29 15:13
 */

val isInDebugMode = BuildConfig.DEBUG

sealed class Durations {
    companion object {
        const val SECOND = 1000L
        const val MINUTE = 60 * SECOND
        const val HOUR = 60 * MINUTE
        const val DAY = 24 * HOUR

        const val MEMORY_DURATION = 20 * MINUTE
        const val WAITING_DURATION = 9 * SECOND
        const val ANR_DURATION = 5 * SECOND
        const val VIOLENT_INTERVAL = 1500L
        const val TOLERATE_LIMIT = 800L
        const val VISUAL_PAUSE = 500L
        const val DEFAULT_DELAY = 300L
        const val FRAME_INTERVAL = 16L
    }
}

//https://cloud.tencent.com/developer/ask/121624
val <T> KProperty0<T>.initialized: Boolean
    get() {
        isAccessible = true
        return (getDelegate() as? Lazy<*>)?.isInitialized() ?: true
    }

val isOnMainThread = Looper.myLooper() == Looper.getMainLooper()