package com.qlk.realm

import android.arch.lifecycle.LiveData
import com.qlk.core.diff.Differ
import com.qlk.core.tasks.Executable
import com.qlk.core.tasks.FutureTracker
import com.qlk.core.tasks.Reason
import com.qlk.realm.livedata.*
import io.realm.RealmModel
import io.realm.RealmQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-27.
 */

internal val SmartTag = "SmartRealm"

/** use lazy waiting for [SmartRealm.initFactory] */
internal val realmExecutor: ExecutorService by lazy { Executors.newCachedThreadPool() } //如果固定数量，当任务太多时，会导致界面很久刷不出来。
//internal val realmExecutor: ExecutorService by lazy { RealmPoolExecutor(SmartRealm.threadPoolSize) }
internal val realmDispatcher: CoroutineDispatcher by lazy { realmExecutor.asCoroutineDispatcher() }

/**
 * I strongly recommend you using one of [SmartRealm], [SyncRealm], [AsyncRealm], [LiveRealm] instead of [AtomicRealm]
 */
class AtomicRealm : IRealmScope by RealmScopeImpl()

/**
 * Realm operations which will run into your current thread.
 */
object SmartRealm {
    /* Basic Operations [Managed]. If you want to use the scope data outside, do copy first, OR will cause an error (database closed automatically once the scope finished) */

    @JvmStatic
    fun <T> read(action: IReadScope.() -> T?): T? = ReadScopeImpl().use { action(it) }

    @JvmStatic
    fun <T> write(action: IWriteScope.() -> T?): T? = WriteScopeImpl().use { action(it) }

    @JvmStatic
    fun <T> readWrite(action: IReadWriteScope.() -> T?): T? =
        ReadWriteScopeImpl().use { action(it) }

    /* Extended Operations [UnManaged]. The following four methods will copy automatically, that means they are thread safe */

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> count(
        clazz: Class<T>,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): Long = read { queryBuilder(query(clazz)).count() }
        ?: 0

    @JvmStatic
    fun <T : RealmModel> getFirst(
        clazz: Class<T>,
        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
    ): T? =
        read { copy(queryBuilder(query(clazz)).findFirst()) }

    @JvmStatic
    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
    ): R? = read { getFirst(clazz, queryBuilder)?.transfer() }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> getAll(
        clazz: Class<T>,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): List<T> = read { copyAll(queryBuilder(query(clazz)).findAll()) }
        ?: emptyList()

    /**
     * Remove the element if it is null after translation. So the results' elements are all nonnull.
     */
    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R?,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): List<R> = read { getAll(clazz, queryBuilder).mapNotNull(transfer) }
        ?: emptyList()

    @JvmStatic
    fun insertOrUpdate(model: RealmModel) {
        write { insertOrUpdate(model) }
    }

    @JvmStatic
    fun insertOrUpdate(models: List<RealmModel>) {
        write { insertOrUpdate(models) }
    }
// I think it's not a good idead to offer a update method. And It is hard to use.
//    /**
//     * updated model count
//     */
//    @JvmStatic
//    fun <T : RealmModel> updateEach(
//        clazz: Class<T>,
//        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>,
//        updater: T.() -> Unit
//    ): Int {
//        return readWrite {
//            val results = queryBuilder(query(clazz)).findAll()
//            var count = 0
//            results.forEach {
//                updater(it)
//                count++
//            }
//            return@readWrite count
//        } ?: 0
//    }

    /* for Java */

    @JvmStatic
    fun readVoid(action: Executable<IReadScope>) = read { action.execute(this) }!!

    @JvmStatic
    fun writeVoid(action: Executable<IWriteScope>) = write { action.execute(this) }!!

    @JvmStatic
    fun readWriteVoid(action: Executable<IReadWriteScope>) = readWrite { action.execute(this) }!!

//    @JvmStatic
//    fun <T : RealmModel> updateEachVoid(
//        clazz: Class<T>,
//        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>,
//        updater: Executable<T>
//    ): Int = updateEach(clazz, queryBuilder) { updater.execute(this) }

    private lateinit var factory: IRealmFactory

    /**
     * @param asyncThreadCount used for [SyncRealm] and [AsyncRealm]
     */
    @JvmStatic
    fun initFactory(factory: IRealmFactory) {
        SmartRealm.factory = factory
    }

    internal fun getRealmFactory(): IRealmFactory = factory
}

/**
 * Realm operations which will run in their self database pool, and block your current thread.
 */
object SyncRealm {
    /* Basic Operations [Managed]. If you want to use the scope data outside, do copy first, OR will cause an error (database closed automatically once the scope finished) */

    @JvmStatic
    fun <T> read(action: IReadScope.() -> T?): T? =
        runBlocking(realmDispatcher) { SmartRealm.read(action) }

    @JvmStatic
    fun <T> write(action: IWriteScope.() -> T?): T? =
        runBlocking(realmDispatcher) { SmartRealm.write(action) }

    @JvmStatic
    fun <T> readWrite(action: IReadWriteScope.() -> T?): T? =
        runBlocking(realmDispatcher) { SmartRealm.readWrite(action) }

    /* Extended Operations [UnManaged]. The following four methods will copy automatically, that means they are thread safe */

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> count(
        clazz: Class<T>,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): Long = runBlocking(realmDispatcher) { SmartRealm.count(clazz, queryBuilder) }

    @JvmStatic
    fun <T : RealmModel> getFirst(
        clazz: Class<T>,
        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
    ): T? =
        runBlocking(realmDispatcher) { SmartRealm.getFirst(clazz, queryBuilder) }

    @JvmStatic
    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
    ): R? =
        runBlocking(realmDispatcher) { SmartRealm.translateFirst(clazz, transfer, queryBuilder) }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> getAll(
        clazz: Class<T>,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): List<T> = runBlocking(realmDispatcher) { SmartRealm.getAll(clazz, queryBuilder) }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R?,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): List<R> =
        runBlocking(realmDispatcher) { SmartRealm.translateAll(clazz, transfer, queryBuilder) }

    @JvmStatic
    fun insertOrUpdate(model: RealmModel) {
        runBlocking(realmDispatcher) { SmartRealm.write { insertOrUpdate(model) } }
    }

    @JvmStatic
    fun insertOrUpdate(models: List<RealmModel>) {
        runBlocking(realmDispatcher) { SmartRealm.write { insertOrUpdate(models) } }
    }

//    /**
//     * updated model count
//     */
//    @JvmStatic
//    fun <T : RealmModel> updateEach(
//        clazz: Class<T>,
//        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>,
//        updater: T.() -> Unit
//    ): Int = runBlocking(realmDispatcher) { SmartRealm.updateEach(clazz, queryBuilder, updater) }

    /* for Java */

    @JvmStatic
    fun readVoid(action: Executable<IReadScope>) =
        runBlocking(realmDispatcher) { SmartRealm.readVoid(action) }

    @JvmStatic
    fun writeVoid(action: Executable<IWriteScope>) =
        runBlocking(realmDispatcher) { SmartRealm.writeVoid(action) }

    @JvmStatic
    fun readWriteVoid(action: Executable<IReadWriteScope>) =
        runBlocking(realmDispatcher) { SmartRealm.readWriteVoid(action) }

//    @JvmStatic
//    fun <T : RealmModel> updateEachVoid(
//        clazz: Class<T>,
//        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>,
//        updater: Executable<T>
//    ): Int = updateEach(clazz, queryBuilder) { updater.execute(this) }
}

object AsyncRealm {
    @JvmStatic
    fun <T> read(action: IReadScope.() -> T?): FutureTracker<T> = submit { SmartRealm.read(action) }

    @JvmStatic
    fun <T> write(action: IWriteScope.() -> T?): FutureTracker<T> =
        submit { SmartRealm.write(action) }

    @JvmStatic
    fun <T> readWrite(action: IReadWriteScope.() -> T?): FutureTracker<T> =
        submit { SmartRealm.readWrite(action) }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> count(
        clazz: Class<T>,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): FutureTracker<Long> = submit { SmartRealm.count(clazz, queryBuilder) }

    @JvmStatic
    fun <T : RealmModel> getFirst(
        clazz: Class<T>,
        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<T> = submit { SmartRealm.getFirst(clazz, queryBuilder) }

    @JvmStatic
    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<R> = submit { SmartRealm.translateFirst(clazz, transfer, queryBuilder) }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> getAll(
        clazz: Class<T>,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): FutureTracker<List<T>> = submit { SmartRealm.getAll(clazz, queryBuilder) }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R?,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): FutureTracker<List<R>> = submit { SmartRealm.translateAll(clazz, transfer, queryBuilder) }

    @JvmStatic
    fun insertOrUpdate(model: RealmModel): FutureTracker<Unit> =
        submit { SmartRealm.insertOrUpdate(model) }

    @JvmStatic
    fun insertOrUpdate(models: List<RealmModel>): FutureTracker<Unit> =
        submit { SmartRealm.insertOrUpdate(models) }

//    /**
//     * updated model count
//     */
//    @JvmStatic
//    fun <T : RealmModel> updateEach(
//        clazz: Class<T>,
//        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>,
//        updater: T.() -> Unit
//    ): FutureTracker<Int> = submit { SmartRealm.updateEach(clazz, queryBuilder, updater) }

    /* for Java */
    @JvmStatic
    fun readVoid(action: Executable<IReadScope>): FutureTracker<Unit> =
        submit { SmartRealm.readVoid(action) }

    @JvmStatic
    fun writeVoid(action: Executable<IWriteScope>): FutureTracker<Unit> =
        submit { SmartRealm.writeVoid(action) }

    @JvmStatic
    fun readWriteVoid(action: Executable<IReadWriteScope>): FutureTracker<Unit> =
        submit { SmartRealm.readWriteVoid(action) }

//    @JvmStatic
//    fun <T : RealmModel> updateEachVoid(
//        clazz: Class<T>,
//        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>,
//        updater: Executable<T>
//    ): FutureTracker<Int> = updateEach(clazz, queryBuilder) { updater.execute(this) }

    private fun <T> submit(action: () -> T?): FutureTracker<T> = FutureTracker<T>().apply {
        setJob(realmExecutor.submit job@{
            kotlin.runCatching { action() }.onSuccess {
                success(it)
            }.onFailure {
                failure(Reason().withException(it))
            }
        })
    }

}

object LiveRealm {
    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> count(
        clazz: Class<T>,
        differ: Differ<T>? = null,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): LiveData<Int> = CountRealmData(clazz, queryBuilder, differ)

    @JvmStatic
    fun <T : RealmModel> getFirst(
        clazz: Class<T>,
        differ: Differ<T>? = null,
        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
    ): LiveData<T> = FirstRealmData(clazz, queryBuilder, differ)

    @JvmStatic
    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        differ: Differ<R>? = null,
        queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
    ): LiveData<R> = FirstRealmTransferData(clazz, queryBuilder, transfer, differ)

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> getAll(
        clazz: Class<T>,
        differ: Differ<T>? = null,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): LiveData<List<T>> = MultiRealmData(clazz, queryBuilder, differ)

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R,
        differ: Differ<R>? = null,
        queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): LiveData<List<R>> = MultiRealmTransferData(clazz, queryBuilder, transfer, differ)
}

//interface IRealmOperation {
//
//    fun <T> read(action: IReadScope.() -> T?): T?
//
//    fun <T> write(action: IWriteScope.() -> T?): T?
//
//    fun <T> readWrite(action: IReadWriteScope.() -> T?): T?
//
//    fun <T : RealmModel> count(clazz: Class<T>, queryBuilder: (RealmQuery<T>) -> RealmQuery<T>): Long
//
//    fun <T : RealmModel> findFirst(clazz: Class<T>, queryBuilder: (RealmQuery<T>) -> RealmQuery<T>): T?
//
//    fun <T : RealmModel, R : Any> translateFirst(
//        clazz: Class<T>, transfer: T.() -> R?, queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
//    ): R?
//
//    fun <T : RealmModel> findAll(
//        clazz: Class<T>, queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>)
//    ): List<T>
//
//    fun <T : RealmModel, R : Any> translateAll(
//        clazz: Class<T>, transfer: T.() -> R?, queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>)
//    ): List<R>
//
//    /* for Java */
//
//    fun readVoid(action: Executable<IReadScope>)
//
//    fun writeVoid(action: Executable<IWriteScope>)
//
//    fun readWriteVoid(action: Executable<IReadWriteScope>)
//}
//
//interface IAsyncRealmOperation {
//
//    fun <T> read(action: IReadScope.() -> T?): FutureTracker<T>
//
//    fun <T> write(action: IWriteScope.() -> T?): FutureTracker<T>
//
//    fun <T> readWrite(action: IReadWriteScope.() -> T?): FutureTracker<T>
//
//    fun <T : RealmModel> count(
//        clazz: Class<T>, queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
//    ): FutureTracker<Long>
//
//    fun <T : RealmModel> findFirst(
//        clazz: Class<T>, queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
//    ): FutureTracker<T>
//
//    fun <T : RealmModel, R : Any> translateFirst(
//        clazz: Class<T>, transfer: T.() -> R?, queryBuilder: (RealmQuery<T>) -> RealmQuery<T>
//    ): FutureTracker<R>
//
//    fun <T : RealmModel> findAll(
//        clazz: Class<T>, queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>)
//    ): FutureTracker<List<T>>
//
//    fun <T : RealmModel, R : Any> translateAll(
//        clazz: Class<T>, transfer: T.() -> R?, queryBuilder: ((RealmQuery<T>) -> RealmQuery<T>)
//    ): FutureTracker<List<R>>
//
//    /* for Java */
//
//    fun readVoid(action: Executable<IReadScope>): FutureTracker<Unit>
//
//    fun writeVoid(action: Executable<IWriteScope>): FutureTracker<Unit>
//
//    fun readWriteVoid(action: Executable<IReadWriteScope>): FutureTracker<Unit>
//