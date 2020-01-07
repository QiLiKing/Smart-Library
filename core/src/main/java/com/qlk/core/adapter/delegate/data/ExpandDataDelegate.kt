package com.qlk.core.adapter.delegate.data

import android.support.annotation.MainThread
import android.support.v7.widget.RecyclerView
import com.easilydo.tools.performance.adapter.ChildPosition
import com.easilydo.tools.performance.adapter.FlatPosition
import com.easilydo.tools.performance.adapter.GroupPosition
import com.qlk.core.adapter.delegate.IDiffDelegate
import com.qlk.core.adapter.delegate.IExpandCollapseDelegate
import com.qlk.core.adapter.delegate.OnDiffDelegateChangedListener
import com.qlk.core.adapter.delegate.OnExpandCollapseDelegateChangedListener
import com.qlk.core.isInDebugMode

/**
 * Attention: If you want check item exist, USE (getGroupItemPosition(item) != -1 || getChildItemPosition(item) != -1) OR isItemExist(item), NOT dataDelegate.getFlatItemPosition(item) != -1
 */
//interface OnExpandDataDelegateChanged<T> {
//    fun onExpandDataChanged(newExpandData: IExpandDataDelegate<T>?)
//}

/**
 * Needs: DiffDelegate, HeaderFooterDelegate, ExpandCollapseDelegate
 */
interface IExpandDataDelegate<T> : IFlatDataDelegate<T>, OnDiffDelegateChangedListener<T>,
    OnExpandCollapseDelegateChangedListener<T> {
    val validGroupPositions: IntRange get() = IntRange(0, getGroupItemCount() - 1)
    val validChildPositions: (GroupPosition) -> IntRange get() = { IntRange(0, getChildItemCount(it) - 1) }

    fun setExpandList(groupDatas: List<T>? = null, childDatas: HashMap<T, List<T>>? = null)

    override fun isItemExist(item: T): Boolean =
        getGroupItemPosition(item) != -1 || getChildItemPosition(item) != -1

    fun getGroupItemCount(): Int

    fun getGroupItem(groupPosition: GroupPosition): T?

    fun getGroupItemPosition(item: T): GroupPosition

    fun getGroupPositionOfChild(item: T): GroupPosition

    fun getChildItemCount(groupPosition: GroupPosition): Int

    fun getChildItem(groupPosition: GroupPosition, childPosition: ChildPosition): T?

    fun getChildItemPosition(item: T): ChildPosition

    fun isValidGroupPosition(groupPosition: GroupPosition): Boolean =
        groupPosition >= 0 && groupPosition < getGroupItemCount()

    fun isValidChildPosition(groupPosition: GroupPosition, childPosition: ChildPosition): Boolean =
        childPosition >= 0 && childPosition < getChildItemCount(groupPosition)
}

class ExpandDataDelegateImpl<T> constructor(
    private val adapter: RecyclerView.Adapter<*>
) : FlatDataDelegateImpl<T>(), IExpandDataDelegate<T> {

    private val groupDatas = mutableListOf<T>()
    private val childDatas = hashMapOf<T, List<T>>()

    private var diffDelegate: IDiffDelegate<T>? = null
    private var expandCollapseDelegate: IExpandCollapseDelegate<T>? = null


    override fun onDiffDelegateChanged(newDelegate: IDiffDelegate<T>?) {
        this.diffDelegate = newDelegate
    }

    override fun onExpandCollapseDelegateChanged(newDelegate: IExpandCollapseDelegate<T>?) {
        this.expandCollapseDelegate = newDelegate
    }

    @MainThread
    override fun setExpandList(groupDatas: List<T>?, childDatas: HashMap<T, List<T>>?) {
//        if (!isOnMainThread) throw IllegalThreadStateException("method must be called on Main thread!")

        if (diffDelegate == null) {
            fillDatas(groupDatas, childDatas)
            adapter.notifyDataSetChanged()
        } else {
            val oldDatas = getFlatItems()
            fillDatas(groupDatas, childDatas)
            val newDatas = getFlatItems()
            diffDelegate?.diffUpdates(oldDatas, newDatas)
        }
    }

    /* Flats */

    /**
     * slowly method
     */
    override fun getFlatItems(): List<T> {
        val flatItems = mutableListOf<T>()
        for (groupPosition in validGroupPositions) {
            val groupItem = getGroupItem(groupPosition) ?: return emptyList()
            flatItems.add(groupItem)
            if (isInDebugMode) println("ExpandDataDelegate#getFlatItems:100-> $groupItem ${groupItem.hashCode()} ${expandCollapseDelegate?.getExpandedGroups()} ${expandCollapseDelegate?.isGroupExpanded(groupItem)}")
            if (expandCollapseDelegate?.isGroupExpanded(groupItem) == true) {
                for (childPosition in validChildPositions(groupPosition)) {
                    val childItem = getChildItem(groupPosition, childPosition) ?: return emptyList()
                    flatItems.add(childItem)
                }
            }
        }
        return flatItems
    }

    /**
     * slowly method
     */
    override fun getFlatItemCount(): Int {
        val count = getGroupItemCount()
        val offset = expandCollapseDelegate?.getExpandedGroups()?.sumBy {
            val groupPosition = getGroupItemPosition(it)
            if (groupPosition != -1) getChildItemCount(groupPosition) else 0
        } ?: 0
        return count + offset
    }

    /**
     * slowly method
     */
    override fun getFlatItem(flatPosition: FlatPosition): T? {
        var currentFlatPosition: FlatPosition = -1
        for (groupPosition in validGroupPositions) {
            currentFlatPosition++
            val groupItem = getGroupItem(groupPosition) ?: return null  //error
            if (currentFlatPosition == flatPosition) {
                return groupItem
            }
            if (expandCollapseDelegate?.isGroupExpanded(groupItem) == true) {
                for (childPosition in validChildPositions(groupPosition)) {
                    currentFlatPosition++
                    if (currentFlatPosition == flatPosition) {
                        return getChildItem(groupPosition, childPosition)
                    }
                }
            }
        }
        return null //not found
    }

    /**
     * slowly method
     */
    override fun getFlatItemPosition(item: T): FlatPosition {
        var currentFlatPosition: FlatPosition = -1
        for (groupPosition in validGroupPositions) {
            currentFlatPosition++
            val groupItem = getGroupItem(groupPosition) ?: return -1    //error
            if (groupItem == item) {
                return currentFlatPosition
            }
            if (expandCollapseDelegate?.isGroupExpanded(groupItem) == true) {
                for (childPosition in validChildPositions(groupPosition)) {
                    currentFlatPosition++
                    if (item == getChildItem(groupPosition, childPosition)) {
                        return currentFlatPosition
                    }
                }
            }
        }
        return -1 //not found
    }

    /* Groups */

    override fun getGroupItemCount() = groupDatas.size

    override fun getGroupItem(groupPosition: GroupPosition): T? =
        runCatching { groupDatas[groupPosition] }.getOrNull()

    override fun getGroupItemPosition(item: T): GroupPosition = groupDatas.indexOf(item)

    override fun getGroupPositionOfChild(item: T): GroupPosition {
        groupDatas.forEachIndexed { groupPosition, groupItem ->
            if (childDatas[groupItem]?.any { item == it } == true) {
                return groupPosition
            }
        }
        return -1
    }

    /* Child */

    override fun getChildItemCount(groupPosition: GroupPosition): Int {
        return getGroupItem(groupPosition)?.run { childDatas[this]?.size } ?: 0
    }

    override fun getChildItem(groupPosition: GroupPosition, childPosition: ChildPosition): T? {
        if (!isValidChildPosition(groupPosition, childPosition)) return null
        return getGroupItem(groupPosition)?.run {
            childDatas[this]?.run {
                this[childPosition]
            }
        }

        //or use the following codes
//      return runCatching { childDatas[groupDatas[groupPosition]!!]!![childPosition] }.getOrNull()
    }

    override fun getChildItemPosition(item: T): ChildPosition {
        childDatas.values.forEach {
            val index = it.indexOf(item)
            if (index != -1) {
                return index
            }
        }
        return -1
    }

    private fun fillDatas(groupDatas: List<T>?, childDatas: HashMap<T, List<T>>?) {
        if (groupDatas != null) {
            this.groupDatas.clear()
            this.groupDatas.addAll(groupDatas)
        }
        if (childDatas != null) {
            this.childDatas.clear()
            this.childDatas.putAll(childDatas)
        }
    }

}

