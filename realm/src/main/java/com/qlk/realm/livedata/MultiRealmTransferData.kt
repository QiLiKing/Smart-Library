package com.qlk.realm.livedata

import android.arch.lifecycle.LiveData
import com.qlk.core.diff.Differ
import com.qlk.core.diff.different
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-28.
 */
internal class MultiRealmTransferData<T : RealmModel, R : Any>(
    clazz: Class<T>,
    query: (RealmQuery<T>) -> RealmQuery<T>,
    private val transfer: T.() -> R?,
    differ: Differ<R>? = null
) : LiveData<List<R>>() {

    private var delegate = object : MultiRealmData<T>(clazz, query) {
        override fun onChange(ts: RealmResults<T>) {
            val rs = ts.mapNotNull { it.transfer() }
            val cur = this@MultiRealmTransferData.value
            if (cur != null && differ?.different(cur, rs, differ.ignoreOps) == false) return
            this@MultiRealmTransferData.postValue(rs)
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