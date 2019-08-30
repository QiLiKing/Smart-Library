package com.qlk.realm.livedata

import android.arch.lifecycle.LiveData
import com.qlk.core.diff.Differ
import com.qlk.core.diff.different
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmQuery

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-27.
 */
internal open class FirstRealmData<T : RealmModel>(
    private val clazz: Class<T>,
    private val query: (RealmQuery<T>) -> RealmQuery<T>,
    private val differ: Differ<T>? = null
) : LiveData<T>(), RealmChangeListener<T> {

    public override fun onActive() {
        super.onActive()
        LiveScope.bindFirstAsync(clazz, this, query)
    }

    public override fun onInactive() {
        super.onInactive()
        LiveScope.unbindAsync(this)
    }

    override fun onChange(t: T?) {
        LiveScope.copyAsync(t).whenSuccess {
            if (value != null && differ?.different(value!!, t) == false) return@whenSuccess
            postValue(it)
        }
    }

}