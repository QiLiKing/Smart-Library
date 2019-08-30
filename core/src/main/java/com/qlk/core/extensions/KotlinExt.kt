package com.qlk.core.extensions

import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

fun <T : Any> T.exception(message: String) = Exception("class:${javaClass.name} message:$message")

//https://cloud.tencent.com/developer/ask/121624
val <T> KProperty0<T>.isInitialized: Boolean
    get() {
        isAccessible = true
        return (getDelegate() as? Lazy<*>)?.isInitialized() ?: true
    }