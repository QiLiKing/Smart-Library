package com.qlk.realm

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import com.qlk.core.tasks.Executable
import com.qlk.core.tasks.FutureTracker
import com.qlk.core.tasks.Reason
import com.qlk.core.diff.Differ
import com.qlk.core.extensions.isActive
import com.qlk.realm.livedata.CountRealmData
import com.qlk.realm.livedata.FirstRealmData
import com.qlk.realm.livedata.FirstRealmTransferData
import com.qlk.realm.livedata.MultiRealmData
import com.qlk.realm.livedata.MultiRealmTransferData
import io.realm.RealmModel
import io.realm.RealmQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.util.concurrent.*

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-27.
 */

/** use lazy waiting for [SmartRealm.initFactory] */
internal val realmExecutor: ExecutorService by lazy { Executors.newFixedThreadPool(SmartRealm.threadPoolSize) }
//internal val realmExecutor: ExecutorService by lazy { RealmPoolExecutor(SmartRealm.threadPoolSize) }
internal val realmDispatcher: CoroutineDispatcher by lazy { realmExecutor.asCoroutineDispatcher() }

private val syncRealm: ISyncRealm by lazy { SyncRealm() }

object SmartRealm : ISyncRealm by syncRealm {
    internal var threadPoolSize: Int = 2
    private lateinit var factory: IRealmFactory
    //    private val syncRealm: ISyncRealm by lazy { SyncRealm() }
    private val asyncRealm: IAsyncRealm by lazy { AsyncRealmImpl() }
    private val liveRealm: LiveRealm by lazy { LiveRealm() }

    @JvmStatic
    @JvmOverloads
    fun initFactory(factory: IRealmFactory, asyncThreadCount: Int = threadPoolSize) {
        SmartRealm.factory = factory
        threadPoolSize = asyncThreadCount
    }

    @JvmStatic
    fun sync(): ISyncRealm = syncRealm

    @JvmStatic
    @JvmOverloads
    fun async(lifecycleOwner: LifecycleOwner? = null): IAsyncRealm =
        if (lifecycleOwner == null) asyncRealm else AsyncRealmImpl(lifecycleOwner)

    @JvmStatic
    fun alive(): LiveRealm = liveRealm

    internal fun getRealmFactory(): IRealmFactory = factory
}

interface ISyncRealm {

    fun <T> read(action: IReadScope.() -> T?): T?

    fun <T> write(action: IWriteScope.() -> T?): T?

    fun <T> readWrite(action: IReadWriteScope.() -> T?): T?

    fun <T : RealmModel> count(clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>): Long

    fun <T : RealmModel> findFirst(clazz: Class<T>, query: (RealmQuery<T>) -> RealmQuery<T>): T?

    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): R?

    fun <T : RealmModel> findAll(
        clazz: Class<T>,
        query: ((RealmQuery<T>) -> RealmQuery<T>)
    ): List<T>

    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R?,
        query: ((RealmQuery<T>) -> RealmQuery<T>)
    ): List<R>

    /* for Java */

    fun readVoid(action: Executable<IReadScope>)

    fun writeVoid(action: Executable<IWriteScope>)

    fun readWriteVoid(action: Executable<IReadWriteScope>)
}

interface IAsyncRealm {

    fun <T> read(action: IReadScope.() -> T?): FutureTracker<T>

    fun <T> write(action: IWriteScope.() -> T?): FutureTracker<T>

    fun <T> readWrite(action: IReadWriteScope.() -> T?): FutureTracker<T>

    fun <T : RealmModel> count(
        clazz: Class<T>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<Long>

    fun <T : RealmModel> findFirst(
        clazz: Class<T>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<T>

    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<R>

    fun <T : RealmModel> findAll(
        clazz: Class<T>,
        query: ((RealmQuery<T>) -> RealmQuery<T>)
    ): FutureTracker<List<T>>

    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R?,
        query: ((RealmQuery<T>) -> RealmQuery<T>)
    ): FutureTracker<List<R>>

    /* for Java */

    fun readVoid(action: Executable<IReadScope>): FutureTracker<Unit>

    fun writeVoid(action: Executable<IWriteScope>): FutureTracker<Unit>

    fun readWriteVoid(action: Executable<IReadWriteScope>): FutureTracker<Unit>
}

interface ILiveRealm {
}

private class SyncRealm : ISyncRealm {
/* Basic Operations [Managed]. If you want to use the scope data outside, do copy first, OR will cause an error (database closed automatically once the scope finished) */

    override fun <T> read(action: IReadScope.() -> T?): T? =
        runBlocking(realmDispatcher) { ReadScopeImpl().use { action(it) } }

    override fun <T> write(action: IWriteScope.() -> T?): T? =
        runBlocking(realmDispatcher) { WriteScopeImpl().use { action(it) } }

    override fun <T> readWrite(action: IReadWriteScope.() -> T?): T? =
        runBlocking(realmDispatcher) { ReadWriteScopeImpl().use { action(it) } }

    override fun <T : RealmModel> count(
        clazz: Class<T>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): Long =
        runBlocking(realmDispatcher) { read { count(clazz, query) } ?: 0 }

    /* Extended Operations [UnManaged]. The following four methods will copy automatically, that means they are thread safe */

    override fun <T : RealmModel> findFirst(
        clazz: Class<T>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): T? =
        runBlocking(realmDispatcher) { read { copy(findFirst(clazz, query)) } }

    override fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): R? =
        runBlocking(realmDispatcher) { read { findFirst(clazz, query)?.transfer() } }

    override fun <T : RealmModel> findAll(
        clazz: Class<T>,
        query: ((RealmQuery<T>) -> RealmQuery<T>)
    ): List<T> =
        runBlocking(realmDispatcher) {
            read { copyAll(findAll(clazz, query)) }
                ?: emptyList()
        }

    override fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R?,
        query: ((RealmQuery<T>) -> RealmQuery<T>)
    ): List<R> =
        runBlocking(realmDispatcher) {
            read { findAll(clazz, query).mapNotNull(transfer) }
                ?: emptyList()
        }

    /* for Java */

    override fun readVoid(action: Executable<IReadScope>) =
        runBlocking(realmDispatcher) { read { action.execute(this) }!! }

    override fun writeVoid(action: Executable<IWriteScope>) =
        runBlocking(realmDispatcher) { write { action.execute(this) }!! }

    override fun readWriteVoid(action: Executable<IReadWriteScope>) =
        runBlocking(realmDispatcher) { readWrite { action.execute(this) }!! }
}

private class AsyncRealmImpl(private val lifecycleOwner: LifecycleOwner? = null) : IAsyncRealm {
    private val delegate: ISyncRealm = SyncRealm()

    override fun <T> read(action: IReadScope.() -> T?): FutureTracker<T> =
        submit { delegate.read(action) }

    override fun <T> write(action: IWriteScope.() -> T?): FutureTracker<T> =
        submit { delegate.write(action) }

    override fun <T> readWrite(action: IReadWriteScope.() -> T?): FutureTracker<T> =
        submit { delegate.readWrite(action) }

    override fun <T : RealmModel> count(
        clazz: Class<T>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<Long> =
        submit { delegate.count(clazz, query) }

    override fun <T : RealmModel> findFirst(
        clazz: Class<T>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<T> =
        submit { delegate.findFirst(clazz, query) }

    override fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<R> =
        submit { delegate.translateFirst(clazz, transfer, query) }

    override fun <T : RealmModel> findAll(
        clazz: Class<T>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<List<T>> =
        submit { delegate.findAll(clazz, query) }

    override fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R?,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): FutureTracker<List<R>> =
        submit { delegate.translateAll(clazz, transfer, query) }

    /* for Java */

    override fun readVoid(action: Executable<IReadScope>): FutureTracker<Unit> =
        submit { delegate.readVoid(action) }

    override fun writeVoid(action: Executable<IWriteScope>): FutureTracker<Unit> =
        submit { delegate.writeVoid(action) }

    override fun readWriteVoid(action: Executable<IReadWriteScope>): FutureTracker<Unit> =
        submit { delegate.readWriteVoid(action) }

    private fun <T> submit(action: () -> T?): FutureTracker<T> =
        FutureTracker<T>().apply {
            if (lifecycleOwner?.isActive == false) return@apply
            setJob(realmExecutor.submit job@{
                kotlin.runCatching {
                    action()
                }.onSuccess {
                    if (lifecycleOwner?.isActive == false) return@job
                    success(it)
                }.onFailure {
                    if (lifecycleOwner?.isActive == false) return@job
                    failure(Reason().withException(it))
                }
            })
        }

}

class LiveRealm : ILiveRealm {
    fun <T : RealmModel> count(
        clazz: Class<T>,
        differ: Differ<T>? = null,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): LiveData<Int> =
        CountRealmData(clazz, query, differ)

    @JvmOverloads
    fun <T : RealmModel> findFirst(
        clazz: Class<T>,
        differ: Differ<T>? = null,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): LiveData<T> =
        FirstRealmData(clazz, query, differ)

    @JvmOverloads
    fun <T : RealmModel, R : Any> translateFirst(
        clazz: Class<T>,
        transfer: T.() -> R?,
        differ: Differ<R>? = null,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): LiveData<R> =
        FirstRealmTransferData(clazz, query, transfer, differ)

    @JvmOverloads
    fun <T : RealmModel> findAll(
        clazz: Class<T>,
        differ: Differ<T>? = null,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): LiveData<List<T>> =
        MultiRealmData(clazz, query, differ)

    @JvmOverloads
    fun <T : RealmModel, R : Any> translateAll(
        clazz: Class<T>,
        transfer: T.() -> R,
        differ: Differ<R>? = null,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ): LiveData<List<R>> =
        MultiRealmTransferData(clazz, query, transfer, differ)
}