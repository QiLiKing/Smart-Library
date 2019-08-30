@file:JvmName("DiffExt")

package com.qlk.core.diff

import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback


/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-30 10:38
 */
fun <T> IDiffer<T>.different(old: T?, new: T?): Boolean {
    if (old == null || new == null) return true
    return !areItemsTheSame(old, new) || !areContentsTheSame(old, new)
}

fun <T> IDiffer<T>.different(old: List<T>, new: List<T>, ignore: List<DiffOp>? = null): Boolean {
    val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun getOldListSize(): Int = old.size

        override fun getNewListSize(): Int = new.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            this@different.areItemsTheSame(old[oldItemPosition], new[newItemPosition])

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            this@different.areContentsTheSame(old[oldItemPosition], new[newItemPosition])
    })
    var different = false
    diffResult.dispatchUpdatesTo(object : ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            if (ignore?.contains(DiffOp.Change) == true) return
            different = true
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            if (ignore?.contains(DiffOp.Move) == true) return
            different = true
        }

        override fun onInserted(position: Int, count: Int) {
            if (ignore?.contains(DiffOp.Insert) == true) return
            different = true
        }

        override fun onRemoved(position: Int, count: Int) {
            if (ignore?.contains(DiffOp.Remove) == true) return
            different = true
        }
    })
    return different
}