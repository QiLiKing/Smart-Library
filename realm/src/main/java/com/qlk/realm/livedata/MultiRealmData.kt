package com.qlk.realm.livedata

import android.arch.lifecycle.LiveData
import com.qlk.core.diff.Differ
import com.qlk.core.diff.different
import com.qlk.core.isInDebugMode
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-27.
 */
internal open class MultiRealmData<T : RealmModel>(
    private val clazz: Class<T>,
    private val query: (RealmQuery<T>) -> RealmQuery<T>,
    private val differ: Differ<T>? = null
) : LiveData<List<T>>(), RealmChangeListener<RealmResults<T>> {

    public override fun onActive() {
        super.onActive()
        LiveScope.bindAllAsync(clazz, this, query)
    }

    public override fun onInactive() {
        super.onInactive()
        LiveScope.unbindAsync(this)
    }

    override fun onChange(ts: RealmResults<T>) {
        if (isInDebugMode) println("MultiRealmData#onChange()-> $ts")
        LiveScope.copyAllAsync(ts).whenSuccess {
            if (value != null && differ?.different(value!!, ts, differ.ignoreOps) == false) return@whenSuccess
            postValue(it)
        }
    }
}