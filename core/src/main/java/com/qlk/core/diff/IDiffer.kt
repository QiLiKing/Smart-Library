package com.qlk.core.diff

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-30 11:01
 */
interface IDiffer<T> {
    fun areItemsTheSame(old: T, new: T): Boolean = old == new

    fun areContentsTheSame(old: T, new: T): Boolean = true
}