package com.qlk.core

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-09-11.
 */
interface ITransfer<T, R> {
    fun translate(t: T?): R?
}