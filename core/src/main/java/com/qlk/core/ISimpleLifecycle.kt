package com.easilydo.tools.performance

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-15.
 */
interface ISimpleLifecycle : Recyclable {
    /**
     * Namely onResume
     */
    fun onActive()

    /**
     * Namely onSuspend
     */
    fun onInactive()

    /**
     * Namely onDestroy
     */
    override fun onRecycle() {}
}