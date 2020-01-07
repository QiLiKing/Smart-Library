package com.qlk.core.adapter.delegate

import com.easilydo.tools.performance.adapter.FlatPosition

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-16 17:44
 */
interface OnHeaderFooterDelegateChangedListener {
    fun onHeaderFooterDelegateChanged(newDelegate: IHeaderFooterDelegate?)
}

interface IHeaderFooterDelegate {
    fun getHeaderItemCount(): Int

    fun getFooterItemCount(): Int

    // used to get itemViewType
    fun getHeaderItemPosition(position: FlatPosition): FlatPosition

    fun getFooterItemPosition(position: FlatPosition): FlatPosition
}

object EmptyHeaderFooterDelegate : IHeaderFooterDelegate {
    override fun getHeaderItemCount(): Int {
        return 0
    }

    override fun getFooterItemCount(): Int {
        return 0
    }

    override fun getHeaderItemPosition(position: FlatPosition): FlatPosition {
        return -1
    }

    override fun getFooterItemPosition(position: FlatPosition): FlatPosition {
        return -1
    }
}