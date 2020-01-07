package com.qlk.core

/**
 * If your class contains tasks that needn't execute immediately, then LazyLoader is for you!<br/>
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-15 09:18
 */
interface ILazyLoader<T> {

    fun onPrimaryLoad(t: T){}

    fun onLazyLoad(t: T){}
}