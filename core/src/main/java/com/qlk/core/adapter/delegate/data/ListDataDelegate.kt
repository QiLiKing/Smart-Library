package com.qlk.core.adapter.delegate.data

import android.os.Looper
import android.support.annotation.MainThread
import android.support.v7.widget.RecyclerView
import com.easilydo.tools.performance.adapter.FlatPosition
import com.qlk.core.adapter.delegate.IDiffDelegate
import com.qlk.core.adapter.delegate.OnDiffDelegateChangedListener

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-16.
 */
//interface OnListDataDelegateChangedListener<T> {
//    fun onListDataDelegateChanged(newDelegate: IListDataDelegate<T>?)
//}

interface IListDataDelegate<T> : IFlatDataDelegate<T>, OnDiffDelegateChangedListener<T> {
    fun setList(newDatas: List<T>)
}

class ListDataDelegateImpl<T>(
    private val adapter: RecyclerView.Adapter<*>
) : FlatDataDelegateImpl<T>(), IListDataDelegate<T> {
    private var diffDelegate: IDiffDelegate<T>? = null

    private val datas = mutableListOf<T>()

    override fun getFlatItems(): List<T> = ArrayList(datas)

    override fun onDiffDelegateChanged(newDelegate: IDiffDelegate<T>?) {
        diffDelegate = newDelegate  //should stop old diff task first?
    }

    @MainThread
    override fun setList(newDatas: List<T>) {
        if (Looper.myLooper() != Looper.getMainLooper()) throw IllegalThreadStateException("method must be called on Main thread!")

        if (diffDelegate != null) {
            val oldDatas = getFlatItems()
            fillDatas(newDatas)
            diffDelegate?.diffUpdates(oldDatas, newDatas)
            return
        }
        fillDatas(datas)
        adapter.notifyDataSetChanged()
    }

    override fun getFlatItemCount(): Int = datas.size

    override fun getFlatItem(flatPosition: FlatPosition): T? =
        runCatching { datas[flatPosition] }.getOrNull()

    override fun getFlatItemPosition(item: T): FlatPosition = datas.indexOf(item)

    private fun fillDatas(newDatas: List<T>) {
        datas.clear()
        datas.addAll(newDatas)
    }
}