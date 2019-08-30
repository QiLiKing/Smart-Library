@file:JvmName("ViewExt")

package com.qlk.core.extensions

import android.view.View
import com.qlk.core.tasks.LiveTask
import com.qlk.core.tasks.ClickTask
import com.qlk.core.tasks.ViewClickTask
import com.qlk.core.Durations.Companion.VIOLENT_INTERVAL

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-15 08:58
 */
fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() = invisible(true)

fun View.gone() = gone(true)

fun View.invisible(invisible: Boolean) {
    visibility = if (invisible) View.INVISIBLE else View.VISIBLE
}

fun View.gone(gone: Boolean) {
    visibility = if (gone) View.GONE else View.VISIBLE
}

fun View.violentClick(interval: Long = VIOLENT_INTERVAL, task: LiveTask) {
    ViewClickTask.sharedInstance.attachViolentClick(this, interval, task)
}

fun View.listenViolentClick(action: ClickTask.ViolentBuilder<View>.() -> Unit) {
    ClickTask.ViolentBuilder(this).apply {
        action()
    }.build()
}

fun View.timesClick(threshold: Int, interval: Long = VIOLENT_INTERVAL, task: LiveTask) {
    ViewClickTask.sharedInstance.attachTimesClick(this, threshold, interval, task)
}

fun View.listenTimesClick(action: ClickTask.TimesBuilder<View>.() -> Unit) {
    ClickTask.TimesBuilder(this).apply {
        action()
    }.build()
}