@file:JvmName("RealmUtils")

package com.qlk.realm

import android.arch.lifecycle.LiveData
import android.os.Looper
import io.realm.*
import io.realm.kotlin.addChangeListener
import io.realm.kotlin.isLoaded
import io.realm.kotlin.isManaged
import io.realm.kotlin.isValid
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-28.
 */

/* Notice: UnManaged object is always valid */

@UseExperimental(ExperimentalContracts::class)
fun <T : RealmModel> T?.handleByRealm(): Boolean {
    contract {
        returns(true) implies (this@handleByRealm != null)
    }
    return this != null && isManaged() && isValid()
}

@UseExperimental(ExperimentalContracts::class)
fun <T : RealmModel> List<T>?.handleByRealm(): Boolean {
//    contract {
//        returns(true) implies (this@listenable is RealmResults<T>)
//    }
    contract {
        returns(true) implies (this@handleByRealm != null)
    }
    val results = this as? RealmResults<T> ?: return false
    return results.isManaged && results.isValid
}

val <T : RealmModel> T.tableClass: Class<T>
    get() = ((javaClass.superclass as? Class<T>) ?: javaClass)

fun <T : RealmModel> List<T>.tableClass(): Class<T>? = firstOrNull()?.tableClass

fun <T : RealmModel> T.tryBind(listener: RealmChangeListener<T>) {
    if (isManaged()) {  //isValid is false until loaded
        addChangeListener(listener)
        if (isLoaded()) {
            listener.onChange(this)
        }
    }
}

fun <T : RealmModel> RealmResults<T>.tryBind(listener: RealmChangeListener<RealmResults<T>>) {
    if (isManaged) {
        addChangeListener(listener)
        if (isLoaded) {
            listener.onChange(this)
        }
    }
}

//can't refresh inside a transaction, if has looper, it's better to use a RealmChangeListener instead of this method.(see document)
fun Realm.tryRefresh() {
    if (!isInTransaction && Looper.myLooper() == null) refresh()
}

//Although we has invoked "beginTransaction", but the realm is not in transaction if we change nothing.
fun Realm.tryCommit() {
    if (isInTransaction) {
        try {
            commitTransaction()
        } catch (t: Throwable) {
            t.printStackTrace()
            cancelTransaction()
        }
    }
}

/**
 * see RealmObservableFactory#from(final Realm realm, final RealmResults<E> results)
 */
private fun <T : RealmModel> RealmResults<T>.asLiveData(realm: Realm): LiveData<List<T>> {
    val realmConfig = realm.configuration
    return object : LiveData<List<T>>() {
        var observeRealm: Realm? = null

        override fun onActive() {
            super.onActive()

        }

        override fun onInactive() {
            super.onInactive()
        }
    }.also {

    }
}


fun <T : RealmModel> RealmQuery<T>.include(field: String, vararg values: Int): RealmQuery<T> {
    if (values.isEmpty()) throw IllegalArgumentException("Nothing to include for filed:$field!")
    if (values.size == 1) {
        equalTo(field, values[0])
    } else {
        beginGroup()
        values.forEachIndexed { index, value ->
            if (index != 0) {
                or()
            }
            equalTo(field, value)
        }
        endGroup()
    }
    return this
}

fun <T : RealmModel> RealmQuery<T>.include(field: String, vararg values: String): RealmQuery<T> {
    if (values.isEmpty()) throw IllegalArgumentException("Nothing to include for filed:$field!")
    if (values.size == 1) {
        equalTo(field, values[0])
    } else {
        beginGroup()
        values.forEachIndexed { index, value ->
            if (index != 0) {
                or()
            }
            equalTo(field, value)
        }
        endGroup()
    }
    return this
}

fun <T : RealmModel> RealmQuery<T>.exclude(field: String, vararg values: Int): RealmQuery<T> {
    if (values.isEmpty()) throw IllegalArgumentException("Nothing to exclude for filed:$field!")
    values.forEach {
        notEqualTo(field, it)
    }
    return this
}

fun <T : RealmModel> RealmQuery<T>.exclude(field: String, vararg values: String): RealmQuery<T> {
    if (values.isEmpty()) throw IllegalArgumentException("Nothing to exclude for filed:$field!")
    values.forEach {
        notEqualTo(field, it)
    }
    return this
}