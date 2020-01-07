package com.qlk.core.adapter.delegate

import android.support.annotation.MainThread
import android.support.v7.widget.RecyclerView
import com.qlk.core.adapter.delegate.data.IExpandDataDelegate
import com.qlk.core.adapter.delegate.data.IFlatDataDelegate
import com.qlk.core.adapter.delegate.data.OnDataDelegateChangedListener
import com.qlk.core.isInDebugMode

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-16.
 */
/**
 * Control the adapter's expand & collapse event<br/>
 *
 * All Not-Query methods should be called on Main Thread!
 */
interface OnExpandCollapseDelegateChangedListener<T> {
    /**
     * If you want close all expand groups if delegate changed, see setMaxExpandGroupCount(maxCount)
     */
    fun onExpandCollapseDelegateChanged(newDelegate: IExpandCollapseDelegate<T>?)
}

/**
 * Needs: DataDelegate
 */
interface IExpandCollapseDelegate<T> : OnDataDelegateChangedListener<T> {

    fun setMaxExpandGroupCount(maxCount: Int)

    fun addExpandedGroups(groups: List<T>)

    fun getExpandedGroups(): List<T>

    fun isGroupExpanded(item: T): Boolean

    /**
     * @return isExpanded after toggle
     */
    @MainThread
    fun toggleGroup(item: T): Boolean

    @MainThread
    fun expandGroup(item: T)

    @MainThread
    fun collapseGroup(item: T)

    @MainThread
    fun collapseAllExpandGroups() {
        getExpandedGroups().run {
            // If your implementation returns an snapshot list, synchronized unnecessarily
            synchronized(this) {
                forEach {
                    collapseGroup(it)
                }
            }
        }
    }
}

internal class ExpandCollapseDelegateImpl<T>(
    private val adapter: RecyclerView.Adapter<*>
) : IExpandCollapseDelegate<T> {
    private val expandedGroups by lazy { LinkedHashSet<T>() }

    private var maxSize = NOT_LIMIT
    private val allowExpand: Boolean get() = maxSize == NOT_LIMIT || maxSize > 0

    private var expandDataDelegate: IExpandDataDelegate<T>? = null

    override fun onDataDelegateChanged(newDelegate: IFlatDataDelegate<T>?) {
        if (expandDataDelegate == newDelegate || newDelegate !is IExpandDataDelegate) return
        expandDataDelegate = newDelegate
        expandedGroups.removeAll { !newDelegate.isItemExist(it) }
    }

    override fun setMaxExpandGroupCount(maxCount: Int) {
        maxSize = maxCount
        checkExpandedGroups()
    }

    override fun addExpandedGroups(groups: List<T>) {
        expandedGroups.addAll(groups)
        checkExpandedGroups()
    }

    override fun getExpandedGroups(): List<T> = expandedGroups.toList()

    override fun isGroupExpanded(item: T) = expandedGroups.contains(item)

    @MainThread
    override fun toggleGroup(item: T): Boolean {
        if (isGroupExpanded(item)) {
            collapseGroup(item)
        } else {
            expandGroup(item)
        }
        return isGroupExpanded(item)
    }

    @MainThread
    override fun expandGroup(item: T) {
        if (expandGroupInternal(item)) {
            expandedGroups.add(item)
        }
    }

    @MainThread
    override fun collapseGroup(item: T) {
        if (collapseGroupInternal(item)) {
            expandedGroups.remove(item)
        }
    }

    private fun expandGroupInternal(item: T): Boolean {
        if (allowExpand) {
            if (isGroupExpanded(item)) {
                moveExpandGroupToFront(item)    //avoid to be recycled by checkExpandGroups()
            } else {
                val dataDelegate = expandDataDelegate ?: return false
                val groupPosition = dataDelegate.getGroupItemPosition(item)
                if (groupPosition != -1) {
                    val childCount = dataDelegate.getChildItemCount(groupPosition)
                    if (childCount > 0) {
                        val adapterPosition = dataDelegate.getAdapterPosition(item)
                        if (adapterPosition != -1) {
                            adapter.notifyItemRangeInserted(adapterPosition + 1, childCount)
                        }
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun collapseGroupInternal(item: T): Boolean {
        val dataDelegate = expandDataDelegate
            ?: return true    //true: remove item from expandGroups to improve next checkExpandGroups() performance
        if (isGroupExpanded(item)) {
            val groupPosition = dataDelegate.getGroupItemPosition(item)
            if (groupPosition != -1) {
                val childCount = dataDelegate.getChildItemCount(groupPosition)
                if (childCount > 0) {
                    val adapterPosition = dataDelegate.getAdapterPosition(item)
                    if (adapterPosition != -1) {
                        // if you want remove its group simultaneously, mmm... how to do?
                        adapter.notifyItemRangeRemoved(adapterPosition + 1 /* exclude its group item */, childCount)
                    }
                    return true
                }
            }
        }
        return false
    }

    private fun moveExpandGroupToFront(item: T) {
        expandedGroups.remove(item)
        expandedGroups.add(item)
    }

    private fun checkExpandedGroups() {
        if (allowExpand) {
            if (maxSize != NOT_LIMIT) {
                val overflow = expandedGroups.size - maxSize
                if (overflow > 0) {
                    val iterator = expandedGroups.iterator()    //Can it execute in FIFO mode?
                    repeat(overflow) {
                        if (collapseGroupInternal(iterator.next())) {
                            iterator.remove()
                        }
                    }
                }
            }
            //internal method maybe change the list items
            expandedGroups.toList().forEach { expandGroupInternal(it) }  //check
        } else {
            expandedGroups.toList().forEach { collapseGroupInternal(it) }
            expandedGroups.clear()
        }
        if (isInDebugMode) println("ExpandCollapseDelegate#addExpandedGroups:205-> $expandedGroups")
    }

    companion object {
        const val NOT_LIMIT = -1
    }
}

