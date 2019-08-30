package com.qlk.core.extensions

import com.qlk.core.Durations
import com.qlk.core.isInDebugMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

suspend fun waitUntilTrue(
    timeout: Long = Durations.VISUAL_PAUSE,
    period: Long = Durations.FRAME_INTERVAL,
    condition: () -> Boolean
) {
    waitUntil(true, timeout, period, condition)
}

suspend fun waitUntilFalse(
    timeout: Long = Durations.VISUAL_PAUSE,
    period: Long = Durations.FRAME_INTERVAL,
    condition: () -> Boolean
) {
    waitUntil(false, timeout, period, condition)
}

suspend fun <T> waitUntil(
    expected: T,
    timeout: Long = Durations.VISUAL_PAUSE,
    period: Long = Durations.FRAME_INTERVAL,
    condition: () -> T,
    action: (T.() -> Unit) = {}
) {
    waitAndExecute(expected, timeout, period, condition, action)
}

suspend fun <T, R> waitAndExecute(
    expected: T,
    timeout: Long = Durations.VISUAL_PAUSE,
    period: Long = Durations.FRAME_INTERVAL,
    condition: () -> T,
    action: T.() -> R
) {
    withTimeoutOrNull(timeout) {
        var t: T
        while (condition().also { t = it } != expected) {
            delay(period)
            if (isInDebugMode) println("PerformanceKt#waitAndExecute()-> expected:$expected timeout:$timeout period:$period")
        }
        action(t)
    } ?: if (isInDebugMode) println("PerformanceKt#waitAndExecute:56-> timeout $timeout ms")
}