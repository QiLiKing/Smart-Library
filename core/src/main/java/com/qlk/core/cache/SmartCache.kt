package com.qlk.core.cache

import com.qlk.core.isInDebugMode

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-16 08:40
 */
object SmartCache {
    private const val DEFAULT_POOL_CAPACITY = 5

    private val poolCapacity: HashMap<PoolName, ByteSize> by lazy { HashMap<PoolName, ByteSize>() }
    private val pools: HashMap<PoolName, ICachePool<*>> by lazy { HashMap<PoolName, ICachePool<*>>() }

    @JvmStatic
    fun setPoolCapacity(poolName: PoolName, capacity: ByteSize) {
        poolCapacity[poolName] = capacity
    }

    @JvmStatic
    fun <V : Cacheable> getAndRemove(poolName: PoolName, cacheTag: CacheTag): V? {
        return getOrNull<V>(poolName, cacheTag)?.also { clearPool(cacheTag) }
    }

    @JvmStatic
    fun <V : Cacheable> getOrNull(poolName: PoolName, cacheTag: CacheTag): V? {
        return obtainPool<V>(poolName).getFromPool(cacheTag)
    }

    /**
     * @param cacheTag recommend you to set it same as the value's simple class name
     *
     * @param creator createDefault new one if there is no cache
     */
    @JvmStatic
    fun <V : Cacheable> getOrCreate(poolName: PoolName, cacheTag: CacheTag, creator: () -> V): V {
        return getOrNull(poolName, cacheTag) ?: creator.invoke().also {
            if (isInDebugMode) println("CachePools#getOrCreate()-> put value into pool:$poolName cacheTag:$cacheTag capacity:${poolCapacity[poolName]}")
            put(poolName, cacheTag, it)
        }
    }

    @JvmStatic
    fun <V : Cacheable> put(poolName: PoolName, tag: CacheTag, value: V) {
        obtainPool<V>(poolName).run {
            putToPool(tag, value)
        }
    }

    @JvmStatic
    fun getAll(poolName: PoolName): Map<CacheTag, Cacheable> =
        pools[poolName]?.getAllFromPool() ?: hashMapOf()

    @JvmStatic
    fun clearPool(vararg poolNames: PoolName = pools.keys.toTypedArray()) =
        synchronized(pools) { poolNames.forEach { pools[it]?.clearPool() } }

    @JvmStatic
    fun currentSize(vararg poolNames: PoolName = pools.keys.toTypedArray()) =
        synchronized(pools) { poolNames.sumBy { pools[it]?.currentSize() ?: 0 } }

    @JvmStatic
    fun capacity(vararg poolNames: PoolName = pools.keys.toTypedArray()) =
        synchronized(pools) { poolNames.sumBy { pools[it]?.capacity() ?: 0 } }

    private fun <V : Cacheable> obtainPool(poolName: PoolName): ICachePool<V> {
        return synchronized(pools) {
            (pools[poolName] as? CachePoolImpl<V>)
                ?: CachePoolImpl<V>(obtainPoolCapacity(poolName)).also {
                    pools[poolName] = it
                    if (isInDebugMode) println("CachePools#obtainPool()-> create pool tag:$poolName capacity:${poolCapacity[poolName]}")
                }
        }
    }

    private fun obtainPoolCapacity(poolName: PoolName): ByteSize =
        poolCapacity[poolName] ?: DEFAULT_POOL_CAPACITY
}