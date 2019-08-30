package com.qlk.core.cache

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-16 08:55
 */
interface Cacheable {
    /**
     * @return size of this instance in memory space
     */
    fun byteCodes(): ByteSize

}