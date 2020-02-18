package com.qlk.realm

import android.util.Log
import com.qlk.core.extensions.isInitialized
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults
import java.io.Closeable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * In the scope, database keeps open, no copy of RealmModel
 *
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-27.
 */

interface IRealmScope : Closeable {
    /**
     * make our operations be compatible with [Realm.getInstance] error.
     */
    fun openTable(table: Class<out RealmModel>): Realm

    fun closeTable(table: Class<out RealmModel>)

    /**
     * wrap "close" method with try-catch
     */
    fun closeSafety()
}

/**
 * A scope must run in the same thread.
 *
 * If you want to use the result outside, must call [IReadScope.copy] or [IReadScope.copyAll]
 */
interface IReadScope : IRealmScope {

    fun <T : RealmModel> query(clazz: Class<T>): RealmQuery<T>

    /**
     * @return null if [model] is not a managed and valid [RealmModel]
     * @see handleByRealm
     */
    fun <T : RealmModel> copy(model: T?): T?

    /**
     * @return empty if [models] is not a managed and valid [RealmResults]
     * @see handleByRealm
     */
    fun <T : RealmModel> copyAll(models: List<T>): MutableList<T>
}

interface IWriteScope : IRealmScope {

    fun insertOrUpdate(model: RealmModel?)

    fun insertOrUpdate(models: List<RealmModel>)

    /**
     * @return the new or updated RealmObject with all its properties backed by the Realm.
     *          You can change its values without caring transaction.
     */
    fun <T : RealmModel> copyToRealmOrUpdate(model: T): T

    fun deleteTable(table: Class<out RealmModel>)
}

/**
 * Auto wrap a transaction for every realm instance after it opened, before any operation
 *
 * see the document of BaseRealm#beginTransaction
 */
interface IReadWriteScope : IReadScope, IWriteScope {
    /**
     * Realm document suggests us that:"It is therefore recommended to query for the items that should be modified from inside the transaction".
     * So I invoke "beginTransaction" automatically when you try to read something.
     * If you do NOT want to do so, set "auto" false.
     * @see Realm.beginTransaction
     * @param auto default true.
     */
    fun autoTransactionWhenQuery(auto: Boolean = true)

    /**
     * transaction will be committed automatically at end of the scope.
     * You needn't invoke it manually at most of time.
     * This method will set "autoTransactionWhenQuery" to false after
     */
    fun commitTransaction()
}

internal open class RealmScopeImpl : IRealmScope {
    private val openedRealms = HashMap<Class<out RealmModel>, Realm>()  //needn't synchronize

    override fun close() {
        openedRealms.forEach {
            it.value.close()
        }
        openedRealms.clear()
    }

    override fun openTable(table: Class<out RealmModel>): Realm {
        return openedRealms[table]?.takeUnless { it.isClosed } //outer close abnormally
            ?: SmartRealm.getRealmFactory().open(table).also { openedRealms[table] = it }
    }

    /**
     * Its database may not close due to the reference count.
     */
    override fun closeTable(table: Class<out RealmModel>) {
        openedRealms.remove(table)?.close()
    }

    override fun closeSafety() {
        try {
            close()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}

internal open class ReadScopeImpl : RealmScopeImpl(), IReadScope {

    override fun <T : RealmModel> query(clazz: Class<T>): RealmQuery<T> {
        val realm = openTable(clazz)
        realm.tryRefresh()
        return realm.where(clazz)
    }

    override fun <T : RealmModel> copy(model: T?): T? {
        if (model.handleByRealm()) {
            val clazz = model.tableClass
            return openTable(clazz).copyFromRealm(
                model,
                SmartRealm.getRealmFactory().getCopyDeep(clazz)
            )
        }
        return null
    }

    override fun <T : RealmModel> copyAll(models: List<T>): MutableList<T> {
        if (models.isEmpty() || !models.handleByRealm()) return ArrayList()
        val clazz = models.tableClass() ?: return ArrayList()
        return openTable(clazz).copyFromRealm(
            models,
            SmartRealm.getRealmFactory().getCopyDeep(clazz)
        )
    }
}

internal open class WriteScopeImpl : RealmScopeImpl(), IWriteScope {

    //begin transaction by this scope, and should commit when completed.
    private val transactedRealms by lazy { HashSet<Realm>() }   //needn't synchronize

    override fun insertOrUpdate(model: RealmModel?) {
        if (model == null) return
        safeTransact(model.javaClass) {
            insertOrUpdate(model)
        }
    }

    override fun insertOrUpdate(models: List<RealmModel>) {
        if (models.isEmpty()) return
        val clazz = models[0].javaClass
        safeTransact(clazz) {
            insertOrUpdate(models)
        }
    }

    override fun <T : RealmModel> copyToRealmOrUpdate(model: T): T = safeTransact(model.javaClass) {
        copyToRealmOrUpdate(model)
    }

    override fun deleteTable(table: Class<out RealmModel>) {
        safeTransact(table) {
            delete(table)
        }
    }

    open fun commitTransaction() {
        try {
            transactedRealms.forEach {
                it.tryCommit()
            }
        } catch (t: Throwable) {
            transactedRealms.forEach {
                if (!it.isClosed && it.isInTransaction) {
                    it.cancelTransaction()
                }
            }
            throw t
        } finally {
            transactedRealms.clear()
        }
    }

    override fun close() {
        try {
            commitTransaction()
        } finally {
            super.close()
        }
    }

    protected fun <R> safeTransact(clazz: Class<out RealmModel>, action: Realm.() -> R): R {
        val realm = transact(clazz)
        return try {
            action(realm)
        } catch (t: Throwable) {
            if (transactedRealms.contains(realm) && realm.isInTransaction) {
                realm.cancelTransaction()
            }
            throw t
        }
    }

    private fun transact(clazz: Class<out RealmModel>): Realm {
        val realm = openTable(clazz)
//        if (!realm.isInTransaction) {     //I think we should throw exceptions
        if (!transactedRealms.contains(realm)) {
            realm.beginTransaction()
            transactedRealms.add(realm)
        }
//        else {
//            if (BuildConfig.DEBUG && !transactedRealms.contains(realm)) {
//                Log.w(SmartTag, "Realm is already in transaction! clazz=$clazz")
//            }
//        }
        return realm
    }

}

internal class ReadWriteScopeImpl : WriteScopeImpl(), IReadWriteScope {
    private var autoTransaction: Boolean = true

    private val reader by lazy { ReadScopeImpl() }

    override fun autoTransactionWhenQuery(auto: Boolean) {
        this.autoTransaction = auto
    }

    override fun <T : RealmModel> query(clazz: Class<T>): RealmQuery<T> {
        return if (autoTransaction) {
            safeTransact(clazz) { reader.query(clazz) }
        } else {
            reader.query(clazz)
        }
    }

    /* beginTransaction will refresh data first, so we needn't invoke again. */

    override fun <T : RealmModel> copy(model: T?): T? = reader.copy(model)

    override fun <T : RealmModel> copyAll(models: List<T>): MutableList<T> =
        reader.copyAll(models)

    override fun commitTransaction() {
        super.commitTransaction()
        autoTransactionWhenQuery(false)
    }

    override fun close() {
        if (this::reader.isInitialized) { //maybe never use any read operation
            reader.close()
        }
        super.close()
    }
}