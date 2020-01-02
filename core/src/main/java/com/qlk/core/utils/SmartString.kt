package com.qlk.core.utils

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2020-01-02
 */
object SmartString {
    @JvmStatic
    fun combine(split: String, vararg members: String?): String {
        var result = ""
        members.forEachIndexed { index, member ->
            result += member
            if (index != members.size - 1) {
                result += split
            }
        }
        return result
    }
}