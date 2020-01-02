package com.qlk.core.cache

import android.util.LruCache

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-16 08:43
 */
internal class CachePoolImpl<V : Cacheable>(capacity: ByteSize) : LruCache<CacheTag, V>(capacity),
    ICachePool<V> {

    override fun putToPool(tag: CacheTag, value: V) {
        put(tag, value)
    }

    override fun getFromPool(tag: CacheTag): V? = get(tag)

    override fun currentSize(): ByteSize = size()

    override fun capacity(): ByteSize = maxSize()

    override fun getAllFromPool(): Map<CacheTag, V> = snapshot()

    override fun clearPool() {
        evictAll()
    }

    override fun sizeOf(key: CacheTag, value: V): Int {
        return value.byteCodes()
    }
}