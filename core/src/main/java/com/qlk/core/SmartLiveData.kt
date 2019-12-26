package com.qlk.core

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.Looper
import java.util.*

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-11-08
 */
object SmartLiveData {
    @JvmStatic
    fun <T> emit(source: LiveData<T>, data: T) {
        if (source is MutableLiveData) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                source.value = data
            } else {
                source.postValue(data)
            }
        }
    }

    @JvmStatic
    fun <T> getOrEmpty(source: LiveData<List<T>>): List<T> {
        return source.value ?: Collections.emptyList()
    }

    @JvmStatic
    fun <T> getOrDefault(source: LiveData<List<T>>, default: List<T>): List<T> {
        return source.value ?: default
    }

    @JvmStatic
    fun <T> getOrDefault(source: LiveData<T>, default: T): T {
        return source.value ?: default
    }
}