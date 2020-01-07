package com.qlk.core.adapter.delegate

import android.support.v7.widget.RecyclerView
import com.easilydo.tools.performance.adapter.AdapterPosition
import com.qlk.core.ILazyLoader
import com.qlk.core.adapter.delegate.data.IFlatDataDelegate
import com.qlk.core.adapter.delegate.data.OnDataDelegateChangedListener

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-17.
 */
interface OnLazyLoadDelegateChangedListener<T, VH : RecyclerView.ViewHolder> {
    fun onLazyLoadDelegateChanged(newDelegate: ILazyLoadDelegate<T, VH>?)
}

/**
 * Needs: DataDelegate, ScrollStateDelegate
 */
interface ILazyLoadDelegate<T, VH : RecyclerView.ViewHolder> : ILifecycleDelegate<VH>,
    OnDataDelegateChangedListener<T> {

    fun onBindViewHolder(holder: VH, adapterPosition: AdapterPosition)

}

class LazyLoadDelegateImpl<T, VH : RecyclerView.ViewHolder> : LifecycleDelegateImpl<VH>(),
    ILazyLoadDelegate<T, VH> {
    private var dataDelegate: IFlatDataDelegate<T>? = null

    override fun onDataDelegateChanged(newDelegate: IFlatDataDelegate<T>?) {
        this.dataDelegate = newDelegate
    }

    override fun onBindViewHolder(holder: VH, adapterPosition: AdapterPosition) {
        val item: T = dataDelegate?.run {
            getFlatItem(adapterPosition - offset)
        } ?: return
        (holder as? ILazyLoader<T>)?.onPrimaryLoad(item)
    }

    override fun activeViewHolder(holder: VH) {
        super.activeViewHolder(holder)
        val item: T = dataDelegate?.run {
            getFlatItem(holder.adapterPosition - offset)
        } ?: return
        (holder as? ILazyLoader<T>)?.onLazyLoad(item)
    }
}