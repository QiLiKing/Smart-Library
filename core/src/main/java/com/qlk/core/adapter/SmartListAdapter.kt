package com.qlk.core.adapter

import android.support.v7.widget.RecyclerView
import com.qlk.core.adapter.delegate.OnDiffDelegateChangedListener
import com.qlk.core.adapter.delegate.data.IFlatDataDelegate
import com.qlk.core.adapter.delegate.data.IListDataDelegate
import com.qlk.core.adapter.delegate.data.ListDataDelegateImpl
import com.qlk.core.initialized

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-16 17:19
 */
abstract class SmartListAdapter<T, VH : RecyclerView.ViewHolder> : SmartAdapter<T, VH>() {

    /* Data */
    override var dataDelegate: IFlatDataDelegate<T>? = null
        set(value) {
            if (value == field || value !is IListDataDelegate?) return
            field?.let { removeHeaderFooterListener(it); removeDiffListener(it as IListDataDelegate) }
            value?.let { addHeaderFooterListener(it); addDiffListener(it as OnDiffDelegateChangedListener<T>) }
            field = value
            if (this::dataChangedListeners.initialized) {
                dataChangedListeners.forEach { it.onDataDelegateChanged(value) }
            }
        }

    fun setList(newDatas: List<T>) {
        if (dataDelegate !is IListDataDelegate) dataDelegate = ListDataDelegateImpl(this)
        (dataDelegate as? IListDataDelegate<T>)?.setList(newDatas)
    }
}