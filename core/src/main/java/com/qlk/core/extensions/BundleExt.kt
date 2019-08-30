package com.qlk.core.extensions

import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import java.io.Serializable

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-21.
 */

/**
 * @param key empty - ignore
 * @param value null - remove
 */
fun Bundle.putExtra(key: String, value: Any?) {
    if (key.isEmpty()) return
    if (value == null) {
        remove(key)
    } else {
        putExtraInternal(this, key, value)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> Bundle.getExtra(key: String): T? = get(key) as? T

private fun putExtraInternal(extras: Bundle, key: String, value: Any) {
    with(extras) {
        when (value) {
            /* order by frequency of use  */
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Double -> putDouble(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Parcelable -> putParcelable(key, value)
            is Serializable -> putSerializable(key, value)
            is CharSequence -> putCharSequence(key, value)

            is Byte -> putByte(key, value)
            is Char -> putChar(key, value)
            is Short -> putShort(key, value)
            is Bundle -> putBundle(key, value)
            is Size -> putSize(key, value)
            is SizeF -> putSizeF(key, value)

            is Array<*> -> putArrayExtra(extras, key, value)
            is ArrayList<*> -> putArrayListExtra(extras, key, value)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun putArrayExtra(extras: Bundle, key: String, value: Any) {
    value as Array<*>
    if (value.isEmpty()) return

    with(extras) {
        when (value) {
            is ByteArray -> putByteArray(key, value)
            is CharArray -> putCharArray(key, value)
            is ShortArray -> putShortArray(key, value)
            is IntArray -> putIntArray(key, value)
            is LongArray -> putLongArray(key, value)
            is FloatArray -> putFloatArray(key, value)
            is DoubleArray -> putDoubleArray(key, value)
            is BooleanArray -> putBooleanArray(key, value)
            else -> {
                when (value[0]) {
                    is String -> putStringArray(key, value as Array<String>)
                    is CharSequence -> putCharSequenceArray(key, value as Array<CharSequence>)
                    is Parcelable -> putParcelableArray(key, value as Array<Parcelable>)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun putArrayListExtra(extras: Bundle, key: String, value: ArrayList<*>) {
    if (value.isEmpty()) return

    with(extras) {
        when (value[0]) {
            is String -> putStringArrayList(key, value as ArrayList<String>)
            is Parcelable -> putParcelableArrayList(key, value as ArrayList<Parcelable>)
            is CharSequence -> putCharSequenceArrayList(key, value as ArrayList<CharSequence>)
            is Int -> putIntegerArrayList(key, value as ArrayList<Int>)
        }
    }
}