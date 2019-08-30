package com.qlk.realm.livedata

import android.arch.lifecycle.LiveData
import com.qlk.core.diff.DiffOp
import com.qlk.core.diff.Differ
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-28.
 */
internal class CountRealmData<T : RealmModel>(
    clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>,
    differ: Differ<T>? = Differ<T>().ignoreDiffOps(DiffOp.Change, DiffOp.Move)
) : LiveData<Int>() {

    private var delegate = object : MultiRealmData<T>(clazz, query, differ) {
        override fun onChange(ts: RealmResults<T>) {
            this@CountRealmData.postValue(ts.size)
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