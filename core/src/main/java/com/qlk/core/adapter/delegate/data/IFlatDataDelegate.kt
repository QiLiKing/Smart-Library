package com.qlk.core.adapter.delegate.data

import com.easilydo.tools.performance.adapter.AdapterPosition
import com.easilydo.tools.performance.adapter.FlatPosition
import com.qlk.core.adapter.delegate.IHeaderFooterDelegate
import com.qlk.core.adapter.delegate.OnHeaderFooterDelegateChangedListener

interface OnDataDelegateChangedListener<T> {
    fun onDataDelegateChanged(newDelegate: IFlatDataDelegate<T>?)
}

interface IFlatDataDelegate<T> : OnHeaderFooterDelegateChangedListener {

    //convenient call of method
    val offset: Int get() = getAdapterPositionOfFirstFlatItem()

    val validFlatPositions: IntRange get() = IntRange(0, getFlatItemCount() - 1)

    fun getAdapterPositionOfFirstFlatItem(): AdapterPosition

    fun isItemExist(item: T): Boolean = getFlatItemPosition(item) != -1

    fun getAdapterPosition(item: T): AdapterPosition =
        getFlatItemPosition(item).let { if (it != -1) it + offset else -1 }

    fun getFlatItems(): List<T>

    fun getFlatItemCount(): Int

    fun getFlatItem(flatPosition: FlatPosition): T?

    /**
     * @return -1 not found
     */
    fun getFlatItemPosition(item: T): FlatPosition

    fun isValidFlatPosition(flatPosition: FlatPosition): Boolean =
        flatPosition >= 0 && flatPosition < getFlatItemCount()
}

abstract class FlatDataDelegateImpl<T> : IFlatDataDelegate<T> {
    private var headerCount = 0

    override fun onHeaderFooterDelegateChanged(newDelegate: IHeaderFooterDelegate?) {
        headerCount = newDelegate?.getHeaderItemCount() ?: 0
    }

    override fun getAdapterPositionOfFirstFlatItem(): AdapterPosition = headerCount
}

object EmptyFlatDataDelegateImpl : FlatDataDelegateImpl<Any>() {
    override fun getFlatItems(): List<Any> = emptyList()

    override fun getFlatItemCount(): Int = 0

    override fun getFlatItem(flatPosition: FlatPosition): Any? = null

    override fun getFlatItemPosition(item: Any): FlatPosition = 0
}