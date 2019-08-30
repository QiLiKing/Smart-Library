package com.qlk.core.diff

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-30 12:12
 */
open class Differ<T> : IDiffer<T> {
    var ignoreOps: List<DiffOp>? = null

    /* override for java */

    override fun areItemsTheSame(old: T, new: T): Boolean = old == new

    override fun areContentsTheSame(old: T, new: T): Boolean = true

    fun setIgnoreDiffOps(ops: List<DiffOp>?): Differ<T> {
        this.ignoreOps = ops
        return this
    }

    fun ignoreDiffOps(vararg ops: DiffOp): Differ<T> {
        this.ignoreOps = ops.toList()
        return this
    }
}