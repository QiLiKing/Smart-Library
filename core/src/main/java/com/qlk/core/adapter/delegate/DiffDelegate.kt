package com.qlk.core.adapter.delegate

import android.support.annotation.MainThread
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.RecyclerView
import com.qlk.core.adapter.delegate.data.IFlatDataDelegate
import com.qlk.core.adapter.delegate.data.OnDataDelegateChangedListener
import com.qlk.core.diff.IDiffer
import com.qlk.core.isInDebugMode
import kotlinx.coroutines.*

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-15.
 */
interface OnDiffDelegateChangedListener<T> {
    fun onDiffDelegateChanged(newDelegate: IDiffDelegate<T>?)
}

/**
 * Needs: DataDelegate
 */
interface IDiffDelegate<T> : OnDataDelegateChangedListener<T> {
    @MainThread
    fun diffUpdates(oldData: List<T>, newData: List<T>)
}

internal class DiffDelegateImpl<T>(
    private val adapter: RecyclerView.Adapter<*>,
    private val differ: IDiffer<T>
) :
    IDiffDelegate<T> {

    private var job: Job? = null
    private var offset: Int = 0

    override fun onDataDelegateChanged(newDelegate: IFlatDataDelegate<T>?) {
        offset = newDelegate?.offset ?: 0
    }

    override fun diffUpdates(oldData: List<T>, newData: List<T>) {
        stopJob()
        job = GlobalScope.launch {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return differ.areItemsTheSame(
                        oldData[oldItemPosition],
                        newData[newItemPosition]
                    )
                }

                override fun getOldListSize() = oldData.size

                override fun getNewListSize() = newData.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return differ.areContentsTheSame(
                        oldData[oldItemPosition],
                        newData[newItemPosition]
                    )
                }
            })
            if (!isActive) {
                return@launch
            }
            //or use a delay()
//            delay(1)    //for cancel, CancellationException not cause runtime crash, need not to catch
            withContext(Dispatchers.Main) {
                //                if (offset == 0) {
//                    if (isInDebugMode) println("DiffDelegate#diffUpdates()-> offset=0")
//                    diffResult.dispatchUpdatesTo(adapter)
//                } else {
                if (isInDebugMode) println("DiffDelegate#diffUpdates()->oldSize=${oldData.size} newSize=${newData.size} offset=$offset")
                diffResult.dispatchUpdatesTo(object : ListUpdateCallback {
                    override fun onChanged(position: Int, count: Int, payload: Any?) {
                        if (isInDebugMode) println("DiffDelegate#onChanged()-> position=$position count=$count")
                        adapter.notifyItemRangeChanged(position + offset, count, payload)
                    }

                    override fun onMoved(fromPosition: Int, toPosition: Int) {
                        if (isInDebugMode) println("DiffDelegate#onMoved()-> fromPosition=$fromPosition toPosition=$toPosition")
                        adapter.notifyItemMoved(fromPosition + offset, toPosition + offset)
                    }

                    override fun onInserted(position: Int, count: Int) {
                        if (isInDebugMode) println("DiffDelegate#onInserted()-> position=$position count=$count")
                        adapter.notifyItemRangeInserted(position + offset, count)
                    }

                    override fun onRemoved(position: Int, count: Int) {
                        if (isInDebugMode) println("DiffDelegate#onRemoved()-> position$position count=$count")
                        adapter.notifyItemRangeRemoved(position + offset, count)
                    }

                })
//                }
            }
        }
    }

    private fun stopJob() {
        if (job?.isActive == true) {
            job!!.cancel()  //stop last diff task
        }
    }
}

