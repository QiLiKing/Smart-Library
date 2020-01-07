package com.qlk.core.adapter.delegate.listener

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-16.
 */
interface IFlatItemClickListenerOwner<T> {
    fun addOnFlatItemClickListener(listener: OnFlatItemClickListener<T>)

    fun removeOnFlatItemClickListener(listener: OnFlatItemClickListener<T>)
}

interface IExpandItemClickListenerOwner<T> {
    fun addOnExpandItemClickListener(listener: OnExpandItemClickListener<T>)

    fun removeOnExpandItemClickListener(listener: OnExpandItemClickListener<T>)
}

interface IFlatItemLongClickListenerOwner<T> {
    fun addOnFlatItemLongClickListener(listener: OnFlatItemLongClickListener<T>)

    fun removeOnFlatItemLongClickListener(listener: OnFlatItemLongClickListener<T>)
}

interface IExpandItemLongClickListenerOwner<T> {
    fun addOnExpandItemLongClickListener(listener: OnExpandItemLongClickListener<T>)

    fun removeOnExpandItemLongClickListener(listener: OnExpandItemLongClickListener<T>)
}