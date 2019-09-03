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

/** use lazy waiting for [SmartRealm.initFactory] */
internal val realmExecutor: ExecutorService by lazy { Executors.newFixedThreadPool(SmartRealm.threadPoolSize) }
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
    fun <T : RealmModel> count(clazz: Class<T>, query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }): Long =
        read { count(clazz, query) } ?: 0

    @JvmStatic
    fun <T : RealmModel> findFirst(clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>): T? =
        read { copy(findFirst(clazz, query)) }

    @JvmStatic
    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>, transfer: T.() -> R?, query: (RealmQuery<T>) -> RealmQuery<T>
    ): R? = read { findFirst(clazz, query)?.transfer() }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> findAll(
        clazz: Class<T>, query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): List<T> = read { copyAll(findAll(clazz, query)) } ?: emptyList()

    /**
     * Remove the element if it is null after translation. So the results' elements are all nonnull.
     */
    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>, transfer: T.() -> R?, query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): List<R> = read { findAll(clazz, query).mapNotNull(transfer) } ?: emptyList()

    /* for Java */

    @JvmStatic
    fun readVoid(action: Executable<IReadScope>) = read { action.execute(this) }!!

    @JvmStatic
    fun writeVoid(action: Executable<IWriteScope>) = write { action.execute(this) }!!

    @JvmStatic
    fun readWriteVoid(action: Executable<IReadWriteScope>) = readWrite { action.execute(this) }!!


    internal var threadPoolSize: Int = 2
    private lateinit var factory: IRealmFactory

    /**
     * @param asyncThreadCount used for [SyncRealm] and [AsyncRealm]
     */
    @JvmStatic
    @JvmOverloads
    fun initFactory(factory: IRealmFactory, asyncThreadCount: Int = threadPoolSize) {
        SmartRealm.factory = factory
        threadPoolSize = asyncThreadCount
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
    fun <T : RealmModel> count(clazz: Class<T>, query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }): Long =
        runBlocking(realmDispatcher) { SmartRealm.count(clazz, query) }

    @JvmStatic
    fun <T : RealmModel> findFirst(clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>): T? =
        runBlocking(realmDispatcher) { SmartRealm.findFirst(clazz, query) }

    @JvmStatic
    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>, transfer: T.() -> R?, query: (RealmQuery<T>) -> RealmQuery<T>
    ): R? = runBlocking(realmDispatcher) { SmartRealm.translateFirst(clazz, transfer, query) }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> findAll(
        clazz: Class<T>, query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): List<T> = runBlocking(realmDispatcher) { SmartRealm.findAll(clazz, query) }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>, transfer: T.() -> R?, query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): List<R> = runBlocking(realmDispatcher) { SmartRealm.translateAll(clazz, transfer, query) }

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
        clazz: Class<T>, query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): FutureTracker<Long> = submit { SmartRealm.count(clazz, query) }

    @JvmStatic
    fun <T : RealmModel> findFirst(
        clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<T> = submit { SmartRealm.findFirst(clazz, query) }

    @JvmStatic
    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>, transfer: T.() -> R?, query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<R> = submit { SmartRealm.translateFirst(clazz, transfer, query) }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> findAll(
        clazz: Class<T>, query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): FutureTracker<List<T>> = submit { SmartRealm.findAll(clazz, query) }

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>, transfer: T.() -> R?, query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): FutureTracker<List<R>> = submit { SmartRealm.translateAll(clazz, transfer, query) }

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

    private fun <T> submit(action: () -> T?): FutureTracker<T> = FutureTracker<T>().apply {
        setJob(realmExecutor.submit job@{
            kotlin.runCatching { action() }
                .onSuccess { success(it) }
                .onFailure { failure(Reason().withException(it)) }
        })
    }

}

object LiveRealm {
    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> count(
        clazz: Class<T>,
        differ: Differ<T>? = null,
        query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): LiveData<Int> =
        CountRealmData(clazz, query, differ)

    @JvmStatic
    fun <T : RealmModel> findFirst(
        clazz: Class<T>,
        differ: Differ<T>? = null,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): LiveData<T> =
        FirstRealmData(clazz, query, differ)

    @JvmStatic
    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        differ: Differ<R>? = null,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): LiveData<R> = FirstRealmTransferData(clazz, query, transfer, differ)

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel> findAll(
        clazz: Class<T>,
        differ: Differ<T>? = null,
        query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): LiveData<List<T>> = MultiRealmData(clazz, query, differ)

    @JvmStatic
    @JvmOverloads
    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R,
        differ: Differ<R>? = null,
        query: ((RealmQuery<T>) -> RealmQuery<T>) = { it }
    ): LiveData<List<R>> = MultiRealmTransferData(clazz, query, transfer, differ)
}

//interface IRealmOperation {
//
//    fun <T> read(action: IReadScope.() -> T?): T?
//
//    fun <T> write(action: IWriteScope.() -> T?): T?
//
//    fun <T> readWrite(action: IReadWriteScope.() -> T?): T?
//
//    fun <T : RealmModel> count(clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>): Long
//
//    fun <T : RealmModel> findFirst(clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>): T?
//
//    fun <T : RealmModel, R : Any> translateFirst(
//        clazz: Class<T>, transfer: T.() -> R?, query: (RealmQuery<T>) -> RealmQuery<T>
//    ): R?
//
//    fun <T : RealmModel> findAll(
//        clazz: Class<T>, query: ((RealmQuery<T>) -> RealmQuery<T>)
//    ): List<T>
//
//    fun <T : RealmModel, R : Any> translateAll(
//        clazz: Class<T>, transfer: T.() -> R?, query: ((RealmQuery<T>) -> RealmQuery<T>)
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
//        clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>
//    ): FutureTracker<Long>
//
//    fun <T : RealmModel> findFirst(
//        clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>
//    ): FutureTracker<T>
//
//    fun <T : RealmModel, R : Any> translateFirst(
//        clazz: Class<T>, transfer: T.() -> R?, query: (RealmQuery<T>) -> RealmQuery<T>
//    ): FutureTracker<R>
//
//    fun <T : RealmModel> findAll(
//        clazz: Class<T>, query: ((RealmQuery<T>) -> RealmQuery<T>)
//    ): FutureTracker<List<T>>
//
//    fun <T : RealmModel, R : Any> translateAll(
//        clazz: Class<T>, transfer: T.() -> R?, query: ((RealmQuery<T>) -> RealmQuery<T>)
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