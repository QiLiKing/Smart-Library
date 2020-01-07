@file:Suppress("UNCHECKED_CAST")

package com.qlk.core.adapter.delegate.listener

import android.support.v7.widget.RecyclerView
import android.view.View
import com.easilydo.tools.performance.adapter.AdapterPosition
import com.easilydo.tools.performance.adapter.ChildPosition
import com.easilydo.tools.performance.adapter.FlatPosition
import com.easilydo.tools.performance.adapter.GroupPosition
import com.qlk.core.R
import com.qlk.core.adapter.delegate.data.IExpandDataDelegate
import com.qlk.core.adapter.delegate.data.IFlatDataDelegate
import com.qlk.core.adapter.delegate.data.OnDataDelegateChangedListener

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-16.
 */
interface IItemTouchDelegate<T, VH : RecyclerView.ViewHolder> : IFlatItemClickListenerOwner<T>,
    IExpandItemClickListenerOwner<T>, IFlatItemLongClickListenerOwner<T>,
    IExpandItemLongClickListenerOwner<T>,
    OnDataDelegateChangedListener<T> {
    fun onBindViewHolder(holder: VH, adapterPosition: AdapterPosition)

    fun onViewRecycled(holder: VH)
}

class ItemClickDelegateImpl<T, VH : RecyclerView.ViewHolder> : IItemTouchDelegate<T, VH> {

    private val clickListeners: HashSet<OnFlatItemClickListener<T>> by lazy { hashSetOf<OnFlatItemClickListener<T>>() }

    private val longClickListeners: HashSet<OnFlatItemLongClickListener<T>> by lazy { hashSetOf<OnFlatItemLongClickListener<T>>() }

    private var dataDelegate: IFlatDataDelegate<T>? = null

    override fun onDataDelegateChanged(newDelegate: IFlatDataDelegate<T>?) {
        this.dataDelegate = newDelegate
    }

    override fun onBindViewHolder(holder: VH, adapterPosition: Int) {
        if (dataDelegate?.isValidFlatPosition(adapterPosition) == true) {
            with(holder.itemView) {
                if (getListenerTag(this) == null) {
                    setListenerTag(this, Wrapper(holder))
                    setOnClickListener(internalItemClickListener)
                    setOnLongClickListener(internalItemLongClickListener)
                } else {
                    // set already or child class needs the tag place
                }
            }
        }
    }

    override fun onViewRecycled(holder: VH) {
        with(holder.itemView) {
            if (getListenerTag(this) is Wrapper<*>) {
                setListenerTag(this, null)
                setOnClickListener(null)
                setOnLongClickListener(null)
            }
        }
    }

    override fun addOnFlatItemClickListener(listener: OnFlatItemClickListener<T>) {
        synchronized(clickListeners) {
            clickListeners.add(listener)
        }
    }

    override fun removeOnFlatItemClickListener(listener: OnFlatItemClickListener<T>) {
        synchronized(clickListeners) {
            clickListeners.remove(listener)
        }
    }

    override fun addOnFlatItemLongClickListener(listener: OnFlatItemLongClickListener<T>) {
        synchronized(longClickListeners) {
            longClickListeners.add(listener)
        }
    }

    override fun removeOnFlatItemLongClickListener(listener: OnFlatItemLongClickListener<T>) {
        synchronized(longClickListeners) {
            longClickListeners.remove(listener)
        }
    }

    override fun addOnExpandItemClickListener(listener: OnExpandItemClickListener<T>) {
        synchronized(clickListeners) {
            clickListeners.add(listener)
        }
    }

    override fun removeOnExpandItemClickListener(listener: OnExpandItemClickListener<T>) {
        synchronized(clickListeners) {
            clickListeners.remove(listener)
        }
    }

    override fun addOnExpandItemLongClickListener(listener: OnExpandItemLongClickListener<T>) {
        synchronized(longClickListeners) {
            longClickListeners.add(listener)
        }
    }

    override fun removeOnExpandItemLongClickListener(listener: OnExpandItemLongClickListener<T>) {
        synchronized(longClickListeners) {
            longClickListeners.remove(listener)
        }
    }

    private val internalItemClickListener: View.OnClickListener by lazy {
        View.OnClickListener { v ->
            with(getListenerTag(v)) {
                if (this is Wrapper<*>) {
                    (holder as? VH)?.let { onItemClick(it) }
                }
            }
        }
    }

    private val internalItemLongClickListener: View.OnLongClickListener by lazy {
        View.OnLongClickListener { v ->
            var prevent = true
            with(getListenerTag(v)) {
                if (this is Wrapper<*>) {
                    (holder as? VH)?.let { prevent = onItemLongClick(it) }
                }
            }
            prevent
        }
    }

    private fun onItemClick(holder: VH) {
        if (clickListeners.isEmpty()) return
        val flatInfo = getFlatInfo(holder) ?: return
        val groupInfo by lazy { getGroupInfo(holder) }
        val childInfo by lazy { getChildInfo(holder) }
        clickListeners.forEach { flatClick ->
            flatClick.onFlatItemClick(holder.itemView, flatInfo.first, flatInfo.second)
            runCatching { flatClick as OnExpandItemClickListener<T> }.onSuccess { expandClick ->
                groupInfo?.run { expandClick.onGroupItemClick(holder.itemView, first, second) }
                childInfo?.run { expandClick.onChildItemClick(holder.itemView, first, second, third) }
            }
        }
    }

    private fun onItemLongClick(holder: VH): Boolean {
        if (longClickListeners.isEmpty()) return false
        val flatInfo = getFlatInfo(holder) ?: return false
        val groupInfo by lazy { getGroupInfo(holder) }
        val childInfo by lazy { getChildInfo(holder) }
        var prevent = false
        val checkPrevent: (Boolean?) -> Unit = { b -> if (b == true) prevent = true }
        longClickListeners.forEach { flatClick ->
            flatClick.onFlatItemLongClick(holder.itemView, flatInfo.first, flatInfo.second).also { checkPrevent(it) }
            runCatching { flatClick as OnExpandItemLongClickListener<T> }.onSuccess { expandClick ->
                groupInfo?.run { expandClick.onGroupItemLongClick(holder.itemView, first, second) }
                    .also { checkPrevent(it) }
                childInfo?.run { expandClick.onChildItemLongClick(holder.itemView, first, second, third) }
                    .also { checkPrevent(it) }
            }
        }
        return prevent
    }

    private data class Wrapper<VH : RecyclerView.ViewHolder>(val holder: VH)

    private fun getFlatInfo(vh: VH): Pair<FlatPosition, T>? {
        val flatPosition = vh.adapterPosition - (dataDelegate?.offset ?: 0)
        return dataDelegate?.getFlatItem(flatPosition)?.let {
            Pair(flatPosition, it)
        }
    }

    private fun getGroupInfo(vh: VH): Pair<GroupPosition, T>? {
        val delegate = runCatching { dataDelegate as IExpandDataDelegate<T> }.getOrNull() ?: return null
        val flatPosition = vh.adapterPosition - (dataDelegate?.offset ?: 0)
        return delegate.getFlatItem(flatPosition)?.let { item ->
            val groupPosition = delegate.getGroupItemPosition(item)
            if (groupPosition != -1) {
                Pair(groupPosition, item)
            } else null
        }
    }

    private fun getChildInfo(vh: VH): Triple<GroupPosition, ChildPosition, T>? {
        val delegate = runCatching { dataDelegate as IExpandDataDelegate<T> }.getOrNull() ?: return null
        val flatPosition = vh.adapterPosition - (dataDelegate?.offset ?: 0)
        delegate.getFlatItem(flatPosition)?.let { item ->
            val groupPosition = delegate.getGroupPositionOfChild(item)
            if (groupPosition != -1) {
                val childPosition = delegate.getChildItemPosition(item)
                if (childPosition != -1) {
                    return Triple(groupPosition, childPosition, item)
                }
            }
        }
        return null
    }

    private fun getListenerTag(view: View): Any? = view.getTag(R.id.listener_tag)
    private fun setListenerTag(view: View, tag: Any?) = view.setTag(R.id.listener_tag, tag)
}