package com.qlk.realm

import com.qlk.core.IFastCopy
import com.qlk.core.extensions.isInitialized
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults
import java.io.Closeable
import java.util.*
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
}

/**
 * A scope must run in the same thread.
 *
 * If you want to use the result outside, must call [IReadScope.copy] or [IReadScope.copyAll]
 */
interface IReadScope {

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
    fun <T : RealmModel> copyAll(models: RealmResults<T>): List<T>
}

interface IWriteScope {

    fun insertOrUpdate(model: RealmModel?)

    fun insertOrUpdate(models: List<RealmModel>)

    /**
     * @return the new or updated RealmObject with all its properties backed by the Realm.
     *          You can change its values without caring transaction.
     */
    fun <T : RealmModel> copyToRealmOrUpdate(model: T): T

    fun deleteTable(table: Class<out RealmModel>)

    /**
     * It will be called at end automatically if you not do this.
     */
    fun commitTransaction();
}

/**
 * Auto wrap a transaction for every realm instance after it opened, before any operation
 *
 * see the document of BaseRealm#beginTransaction
 */
interface IReadWriteScope : IReadScope, IWriteScope

internal open class RealmScopeImpl : IRealmScope {

    private val openedRealms = HashMap<Class<out RealmModel>, Realm>()

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
}

internal open class ReadScopeImpl : RealmScopeImpl(), IReadScope {

    override fun <T : RealmModel> query(clazz: Class<T>): RealmQuery<T> {
        val realm = openTable(clazz)
        realm.tryRefresh()
        return realm.where(clazz)
    }

    override fun <T : RealmModel> copy(model: T?): T? {
        if (model as? IFastCopy<T> != null) {
            return model.fastCopy()
        }
        if (model.handleByRealm()) {
            val clazz = model.tableClass
            return openTable(clazz).copyFromRealm(
                model,
                SmartRealm.getRealmFactory().getCopyDeep(clazz)
            )
        }
        return null
    }

    override fun <T : RealmModel> copyAll(models: RealmResults<T>): List<T> {
        if (models.isEmpty() || !models.handleByRealm()) return emptyList()

        val first: T = models.first()!!
        if (first as? IFastCopy<T> != null) {
            return models.mapNotNull { (it as? IFastCopy<T>)?.fastCopy() }
        }
        val clazz = models.tableClass() ?: return emptyList()
        return openTable(clazz).copyFromRealm(
            models,
            SmartRealm.getRealmFactory().getCopyDeep(clazz)
        )
    }
}

internal open class WriteScopeImpl : RealmScopeImpl(), IWriteScope {

    //begin transaction by this scope, and should commit when completed.
    private val transactedRealms by lazy { HashSet<Realm>() }

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

    override fun commitTransaction() {
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
            if (transactedRealms.contains(realm) && realm.isInTransaction) { //do not cancel outer transaction
                realm.cancelTransaction()
            }
            throw t
        }
    }

    private fun transact(clazz: Class<out RealmModel>): Realm {
        val realm = openTable(clazz)
        if (!realm.isInTransaction) {
            realm.beginTransaction()
            transactedRealms.add(realm)
        }
        return realm
    }

}

internal class ReadWriteScopeImpl : WriteScopeImpl(), IReadWriteScope {

    private val reader by lazy { ReadScopeImpl() }

    override fun <T : RealmModel> query(clazz: Class<T>): RealmQuery<T> {
        return safeTransact(clazz) { reader.query(clazz) }
    }

    /* beginTransaction will refresh data first, so we needn't invoke again. */

    override fun <T : RealmModel> copy(model: T?): T? = reader.copy(model)

    override fun <T : RealmModel> copyAll(models: RealmResults<T>): List<T> =
        reader.copyAll(models)

    override fun close() {
        if (this::reader.isInitialized) { //maybe never use any read operation
            reader.close()
        }
        super.close()
    }
}