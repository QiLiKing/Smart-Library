package com.qlk.realm.livedata

import android.arch.lifecycle.LiveData
import com.qlk.core.diff.Differ
import com.qlk.core.diff.different
import io.realm.RealmModel
import io.realm.RealmQuery

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-28.
 */
internal class FirstRealmTransferData<T : RealmModel, R : Any>(
    clazz: Class<T>,
    query: (RealmQuery<T>) -> RealmQuery<T>,
    private val transfer: T.() -> R?,
    differ: Differ<R>? = null
) : LiveData<R>() {
    private var delegate = object : FirstRealmData<T>(clazz, query) {
        override fun onChange(t: T?) {
            val r = t?.transfer()
            val cur = this@FirstRealmTransferData.value
            if (cur != null && differ?.different(cur, r) == false) return
            this@FirstRealmTransferData.postValue(r)
        }
    }

    override fun onActive() {
        super.onActive()
        delegate.onActive()
    }

    override fun onInactive() {
        super.onInactive()
        delegate.onInactive()
    }
}