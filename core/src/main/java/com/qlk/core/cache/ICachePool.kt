package com.qlk.core.cache

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-16 09:02
 */
interface ICachePool<V : Cacheable> {

    fun putToPool(tag: CacheTag, value: V)

    fun getFromPool(tag: CacheTag): V?

    fun getAllFromPool(): Map<CacheTag, V>

    fun clearPool()

    fun currentSize(): ByteSize

    fun capacity(): ByteSize
}