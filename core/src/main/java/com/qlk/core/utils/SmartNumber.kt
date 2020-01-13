package com.qlk.core.utils

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2020-01-13
 */
object SmartNumber {
    @JvmStatic
    fun positiveOrNull(number: Number): String? {
        return if (number.toDouble() > 0) number.toString() else null
    }

    @JvmStatic
    fun positiveOrEmpty(number: Number): String {
        return if (number.toDouble() > 0) number.toString() else ""
    }

    @JvmStatic
    fun positiveOrDefault(number: Number, default: String): String {
        return if (number.toDouble() > 0) number.toString() else default
    }

    @JvmStatic
    fun negativeOrNull(number: Number): String? {
        return if (number.toDouble() < 0) number.toString() else null
    }

    @JvmStatic
    fun negativeOrEmpty(number: Number): String {
        return if (number.toDouble() < 0) number.toString() else ""
    }

    @JvmStatic
    fun negativeOrDefault(number: Number, default: String): String {
        return if (number.toDouble() < 0) number.toString() else default
    }
}