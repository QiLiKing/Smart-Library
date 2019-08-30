package com.qlk.core.extensions

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner


fun Lifecycle?.notNullAndActive(): Boolean {
    return this?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: false
}

val LifecycleOwner.isActive get() = lifecycle.isActive

val Lifecycle.isActive get() = currentState.isAtLeast(Lifecycle.State.STARTED)