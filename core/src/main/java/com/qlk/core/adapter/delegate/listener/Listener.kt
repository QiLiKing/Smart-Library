package com.qlk.core.adapter.delegate.listener

import android.view.View
import com.easilydo.tools.performance.adapter.FlatPosition

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-16.
 */
interface OnFlatItemClickListener<T> {
    fun onFlatItemClick(itemView: View, flatPosition: FlatPosition, flatItem: T)
}

interface OnFlatItemLongClickListener<T> {
    fun onFlatItemLongClick(itemView: View, flatPosition: FlatPosition, flatItem: T): Boolean
}

interface OnExpandItemClickListener<T> : OnFlatItemClickListener<T> {

    override fun onFlatItemClick(itemView: View, flatPosition: FlatPosition, flatItem: T) {}

    fun onGroupItemClick(groupView: View, groupPosition: Int, groupItem: T)

    fun onChildItemClick(childView: View, groupPosition: Int, childPosition: Int, childItem: T)
}

interface OnExpandItemLongClickListener<T> : OnFlatItemLongClickListener<T> {

    override fun onFlatItemLongClick(itemView: View, flatPosition: FlatPosition, flatItem: T): Boolean = true

    fun onGroupItemLongClick(groupView: View, groupPosition: Int, groupItem: T): Boolean

    fun onChildItemLongClick(childView: View, groupPosition: Int, childPosition: Int, childItem: T): Boolean
}