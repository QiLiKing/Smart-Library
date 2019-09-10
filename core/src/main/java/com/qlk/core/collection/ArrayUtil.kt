package com.qlk.core.collection

import com.qlk.core.ITransfer

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-09-11.
 */
object ArrayUtil {
    @JvmStatic
    fun <T, R : Any> mapNotNull(data: Collection<T>, transfer: ITransfer<T, R>): List<R> {
        return data.mapNotNull { transfer.translate(it) }
    }
}