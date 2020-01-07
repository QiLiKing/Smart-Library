package com.easilydo.tools.performance.adapter

import android.support.v7.widget.RecyclerView
import com.qlk.core.adapter.SmartAdapter
import com.qlk.core.adapter.delegate.ExpandCollapseDelegateImpl
import com.qlk.core.adapter.delegate.IExpandCollapseDelegate
import com.qlk.core.adapter.delegate.OnDiffDelegateChangedListener
import com.qlk.core.adapter.delegate.OnExpandCollapseDelegateChangedListener
import com.qlk.core.adapter.delegate.data.ExpandDataDelegateImpl
import com.qlk.core.adapter.delegate.data.IExpandDataDelegate
import com.qlk.core.adapter.delegate.data.IFlatDataDelegate
import com.qlk.core.adapter.delegate.listener.IExpandItemClickListenerOwner
import com.qlk.core.adapter.delegate.listener.IExpandItemLongClickListenerOwner
import com.qlk.core.adapter.delegate.listener.OnExpandItemClickListener
import com.qlk.core.adapter.delegate.listener.OnExpandItemLongClickListener
import com.qlk.core.initialized
import com.qlk.core.isInDebugMode

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-12.
 */
abstract class SmartExpandableAdapter<T, VH : RecyclerView.ViewHolder> :
    SmartAdapter<T, VH>(), IExpandItemClickListenerOwner<T>, IExpandItemLongClickListenerOwner<T> {

    /* for outer */

    fun setExpandList(groupDatas: List<T>? = null, childDatas: HashMap<T, List<T>>? = null) {
        if (isInDebugMode) println("ExpandableAdapter#setExpandList:27-> ${Thread.currentThread().name}")
        checkExpandCollapseDelegate()
        obtainExpandDataDelegate().setExpandList(groupDatas, childDatas)
    }

    fun isGroupExpanded(item: T): Boolean = expandCollapseDelegate?.isGroupExpanded(item) == true

    fun toggleGroup(item: T) {
        expandCollapseDelegate?.toggleGroup(item)
    }

    fun expandGroup(item: T) {
        expandCollapseDelegate?.expandGroup(item)
    }

    fun collapseGroup(item: T) {
        expandCollapseDelegate?.collapseGroup(item)
    }

    fun getExpandedGroups(): List<T> = expandCollapseDelegate?.getExpandedGroups() ?: emptyList()

    fun addExpandedGroups(groups: List<T>) {
        expandCollapseDelegate?.addExpandedGroups(groups)
    }

    /* Data */
    override var dataDelegate: IFlatDataDelegate<T>? = null
        set(value) {
            if (value == field || value !is IExpandDataDelegate<T>?) return
            field?.let {
                removeHeaderFooterListener(it)
                removeDiffListener(it as IExpandDataDelegate<T>)
                removeExpandCollapseListener(it)
            }
            value?.let {
                addHeaderFooterListener(it)
                addDiffListener(it as OnDiffDelegateChangedListener<T>)
                addExpandCollapseListener(it as OnExpandCollapseDelegateChangedListener<T>)
            }
            field = value
            if (this::dataChangedListeners.initialized) {
                dataChangedListeners.forEach { it.onDataDelegateChanged(value) }
            }
        }

    /* Expand & Collapse */
    val expandCollapseChangedListeners by lazy { HashSet<OnExpandCollapseDelegateChangedListener<T>>() }
    var expandCollapseDelegate: IExpandCollapseDelegate<T>? = null
        set(value) {
            if (value == field) return
            field?.let { removeDataListener(it) }
            value?.let { addDataListener(it) }
            field = value
            if (this::expandCollapseChangedListeners.initialized) {
                expandCollapseChangedListeners.forEach { it.onExpandCollapseDelegateChanged(value) }
            }
        }

    protected fun addExpandCollapseListener(listener: OnExpandCollapseDelegateChangedListener<T>) {
        expandCollapseChangedListeners.add(listener)
        listener.onExpandCollapseDelegateChanged(expandCollapseDelegate)
    }

    protected fun removeExpandCollapseListener(listener: OnExpandCollapseDelegateChangedListener<T>) {
        if (this::expandCollapseChangedListeners.initialized) {
            expandCollapseChangedListeners.remove(listener)
        }
    }

    private fun checkExpandCollapseDelegate() {
        if (expandCollapseDelegate == null) {
            expandCollapseDelegate = ExpandCollapseDelegateImpl(this)
        }
    }

    /* Listeners */

    override fun addOnExpandItemClickListener(listener: OnExpandItemClickListener<T>) {
        obtainItemClickDelegate().addOnExpandItemClickListener(listener)
    }

    override fun removeOnExpandItemClickListener(listener: OnExpandItemClickListener<T>) {
        itemTouchDelegate?.removeOnExpandItemClickListener(listener)
    }

    override fun addOnExpandItemLongClickListener(listener: OnExpandItemLongClickListener<T>) {
        obtainItemClickDelegate().addOnExpandItemLongClickListener(listener)
    }

    override fun removeOnExpandItemLongClickListener(listener: OnExpandItemLongClickListener<T>) {
        itemTouchDelegate?.removeOnExpandItemLongClickListener(listener)
    }


    private fun obtainExpandDataDelegate(): IExpandDataDelegate<T> {
        if (dataDelegate !is IExpandDataDelegate) {
            dataDelegate = ExpandDataDelegateImpl(this)
        }
        return dataDelegate as IExpandDataDelegate<T>
    }

}