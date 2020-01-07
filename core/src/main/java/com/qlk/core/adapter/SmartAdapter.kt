package com.qlk.core.adapter

import android.support.annotation.CallSuper
import android.support.v7.widget.RecyclerView
import com.qlk.core.adapter.delegate.data.IFlatDataDelegate
import com.qlk.core.adapter.delegate.data.OnDataDelegateChangedListener
import com.qlk.core.adapter.delegate.*
import com.qlk.core.adapter.delegate.listener.*
import com.qlk.core.initialized
import com.qlk.core.isInDebugMode

/**
 * A switcher for you can open or close any function of adapter
 *
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-15.
 */
abstract class SmartAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>(),
    IFlatItemClickListenerOwner<T>,
    IFlatItemLongClickListenerOwner<T> {

    /* for outer */

    fun isItemExist(item: T): Boolean = dataDelegate?.isItemExist(item) == true

    /**
     * @return -1 not found
     */
    fun getItemPosition(item: T): Int = dataDelegate?.getFlatItemPosition(item) ?: -1

    fun getItem(position: Int): T? = dataDelegate?.getFlatItem(position)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        scrollStateDelegate?.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        scrollStateDelegate?.onDetachedFromRecyclerView(recyclerView)
    }

    @CallSuper
    override fun onBindViewHolder(holder: VH, position: Int) {
        if (isInDebugMode) println("SmartAdapter#onBindViewHolder()-> position=$position")
        lazyLoadDelegate?.onBindViewHolder(holder, position)
        itemTouchDelegate?.onBindViewHolder(holder, position)
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        lifecycleDelegate?.onViewAttachedToWindow(holder)
        lazyLoadDelegate?.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        lifecycleDelegate?.onViewDetachedFromWindow(holder)
        lazyLoadDelegate?.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        lifecycleDelegate?.onViewRecycled(holder)
        lazyLoadDelegate?.onViewRecycled(holder)
        itemTouchDelegate?.onViewRecycled(holder)
    }

    override fun getItemCount(): Int {
        val flatCount = dataDelegate?.getFlatItemCount() ?: 0
        val headerFooterCount = headerFooterDelegate?.run { getHeaderItemCount() + getFooterItemCount() }
            ?: 0
        if (isInDebugMode) println("SmartAdapter#getItemCount()-> itemCount=${flatCount + headerFooterCount}")
        return flatCount + headerFooterCount
    }

    /* Data */
    val dataChangedListeners by lazy { HashSet<OnDataDelegateChangedListener<T>>() }
    open var dataDelegate: IFlatDataDelegate<T>? = null
        set(value) {
            if (field == value) return
            field?.let { removeHeaderFooterListener(it) }
            value?.let { addHeaderFooterListener(it) }
            field = value
            if (this::dataChangedListeners.initialized) {
                dataChangedListeners.forEach { it.onDataDelegateChanged(value) }
            }
        }

    protected fun addDataListener(listener: OnDataDelegateChangedListener<T>) {
        dataChangedListeners.add(listener)
        listener.onDataDelegateChanged(dataDelegate)
    }

    protected fun removeDataListener(listener: OnDataDelegateChangedListener<T>) {
        if (this::dataChangedListeners.initialized) {
            dataChangedListeners.remove(listener)
        }
    }

    /* Scroll State */
    val scrollStateChangedListeners by lazy { HashSet<OnScrollStateDelegateChangedListener>() }
    var scrollStateDelegate: IScrollStateDelegate? = null
        set(value) {
            if (field == value) return
            field = value
            if (this::scrollStateChangedListeners.initialized) {
                scrollStateChangedListeners.forEach { it.onScrollStateDelegateChanged(value) }
            }
        }

    private fun addScrollStateListener(listener: OnScrollStateDelegateChangedListener) {
        scrollStateChangedListeners.add(listener)
        listener.onScrollStateDelegateChanged(scrollStateDelegate)
    }

    private fun removeScrollStateListener(listener: OnScrollStateDelegateChangedListener) {
        if (this::scrollStateChangedListeners.initialized) {
            scrollStateChangedListeners.remove(listener)
        }
    }

    /* Lifecycle */

    val lifecycleChangedListeners by lazy { HashSet<OnLifecycleDelegateChangedListener<VH>>() }
    var lifecycleDelegate: ILifecycleDelegate<VH>? = null
        set(value) {
            if (field == value) return
            field?.let { removeScrollStateListener(it) }
            value?.let { addScrollStateListener(it) }
            field = value
            if (this::lifecycleChangedListeners.initialized) {
                lifecycleChangedListeners.forEach { it.onLifecycleDelegateChanged(value) }
            }
        }

    private fun addLifecycleListener(listener: OnLifecycleDelegateChangedListener<VH>) {
        lifecycleChangedListeners.add(listener)
        listener.onLifecycleDelegateChanged(lifecycleDelegate)
    }

    private fun removeLifecycleListener(listener: OnLifecycleDelegateChangedListener<VH>) {
        if (this::lifecycleChangedListeners.initialized) {
            lifecycleChangedListeners.remove(listener)
        }
    }

    /* Lazy Load */

    val lazyLoadChangedListeners by lazy { HashSet<OnLazyLoadDelegateChangedListener<T, VH>>() }
    var lazyLoadDelegate: ILazyLoadDelegate<T, VH>? = null
        set(value) {
            if (field == value) return
            field?.let { removeDataListener(it); removeScrollStateListener(it) }
            value?.let { addDataListener(it); addScrollStateListener(it) }
            field = value
            if (this::lazyLoadChangedListeners.initialized) {
                lazyLoadChangedListeners.forEach { it.onLazyLoadDelegateChanged(value) }
            }
        }

    private fun addLazyLoadListener(listener: OnLazyLoadDelegateChangedListener<T, VH>) {
        lazyLoadChangedListeners.add(listener)
        listener.onLazyLoadDelegateChanged(lazyLoadDelegate)
    }

    private fun removeLazyLoadListener(listener: OnLazyLoadDelegateChangedListener<T, VH>) {
        if (this::lazyLoadChangedListeners.initialized) {
            lazyLoadChangedListeners.remove(listener)
        }
    }

    /* Differ */

    val diffChangedListeners by lazy { HashSet<OnDiffDelegateChangedListener<T>>() }
    var diffDelegate: IDiffDelegate<T>? = null
        set(value) {
            if (field == value) return
            field?.let { removeDataListener(it) }
            value?.let { addDataListener(it) }
            field = value
            if (this::diffChangedListeners.initialized) {
                diffChangedListeners.forEach { it.onDiffDelegateChanged(value) }
            }
        }

    protected fun addDiffListener(listener: OnDiffDelegateChangedListener<T>) {
        diffChangedListeners.add(listener)
        listener.onDiffDelegateChanged(diffDelegate)
    }

    protected fun removeDiffListener(listener: OnDiffDelegateChangedListener<T>) {
        if (this::diffChangedListeners.initialized) {
            diffChangedListeners.remove(listener)
        }
    }

    /* Listener */

    var itemTouchDelegate: IItemTouchDelegate<T, VH>? = null
        set(value) {
            if (field == value) return
            field?.let { removeDataListener(it) }
            value?.let { addDataListener(it) }
            field = value
        }

    override fun addOnFlatItemClickListener(listener: OnFlatItemClickListener<T>) {
        obtainItemClickDelegate().addOnFlatItemClickListener(listener)
    }

    override fun removeOnFlatItemClickListener(listener: OnFlatItemClickListener<T>) {
        obtainItemClickDelegate().removeOnFlatItemClickListener(listener)
    }

    override fun addOnFlatItemLongClickListener(listener: OnFlatItemLongClickListener<T>) {
        itemTouchDelegate?.addOnFlatItemLongClickListener(listener)
    }

    override fun removeOnFlatItemLongClickListener(listener: OnFlatItemLongClickListener<T>) {
        itemTouchDelegate?.removeOnFlatItemLongClickListener(listener)
    }

    protected fun obtainItemClickDelegate(): IItemTouchDelegate<T, VH> {
        if (itemTouchDelegate == null) {
            itemTouchDelegate = ItemClickDelegateImpl()
        }
        return itemTouchDelegate!!
    }

    /* Header & Footer */
    val headerFooterChangedListeners by lazy { HashSet<OnHeaderFooterDelegateChangedListener>() }
    var headerFooterDelegate: IHeaderFooterDelegate? = null
        set(value) {
            if (field == value) return
            field = value
            if (this::headerFooterChangedListeners.initialized) {
                headerFooterChangedListeners.forEach { it.onHeaderFooterDelegateChanged(value) }
            }
        }

    protected fun addHeaderFooterListener(listener: OnHeaderFooterDelegateChangedListener) {
        headerFooterChangedListeners.add(listener)
        listener.onHeaderFooterDelegateChanged(headerFooterDelegate)
    }

    protected fun removeHeaderFooterListener(listener: OnHeaderFooterDelegateChangedListener) {
        if (this::headerFooterChangedListeners.initialized) {
            headerFooterChangedListeners.remove(listener)
        }
    }

}