package com.qlk.realm.livedata

import android.os.Handler
import android.os.HandlerThread
import com.qlk.core.tasks.JobTracker
import com.qlk.realm.ReadScopeImpl
import com.qlk.realm.handleByRealm
import com.qlk.realm.tryBind
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.kotlin.removeChangeListener

/**
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-08-02 10:11
 */
/**
 * LiveScope runs in a single thread, so mark its operations as async methods
 */
internal interface ILiveScope {
    fun <T : RealmModel> bindFirstAsync(
        clazz: Class<T>,
        listener: RealmChangeListener<T>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    )

    fun <T : RealmModel> bindAllAsync(
        clazz: Class<T>,
        listener: RealmChangeListener<RealmResults<T>>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    )

    fun unbindAsync(listener: RealmChangeListener<*>)

    fun <T : RealmModel> copyAsync(model: T?): JobTracker<T>

    fun <T : RealmModel> copyAllAsync(models: RealmResults<T>): JobTracker<MutableList<T>>
}

internal object LiveScope : ILiveScope {
    //Executor + Looper.prepare() does not work.
    private var executor: HandlerThread = HandlerThread("LiveScope").also { it.start() }
    private val handler = Handler(executor.looper)
    private val reader: ReadScopeImpl = ReadScopeImpl()

    private val activeDatas = ArrayList<ActiveData<*>>()

    override fun <T : RealmModel> bindFirstAsync(
        clazz: Class<T>,
        listener: RealmChangeListener<T>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ) {
        handler.post {
            val t = reader.openTable(clazz)
                .let { query(it.where(clazz)).findFirstAsync() } //If use findFirst(), you should notify the initial result manually.
            if (t == null) {
                reader.closeTable(clazz)
            } else {
                activeDatas.add(ActiveData(clazz, listener = listener, result = t).bind())
            }
        }
    }

    override fun <T : RealmModel> bindAllAsync(
        clazz: Class<T>,
        listener: RealmChangeListener<RealmResults<T>>,
        query: (RealmQuery<T>) -> RealmQuery<T>
    ) {
        handler.post {
            val ts = reader.openTable(clazz).let { query(it.where(clazz)).findAllAsync() }
            if (ts != null) {
                activeDatas.add(ActiveData(clazz, listeners = listener, results = ts).bind())
            }
        }
    }

    override fun unbindAsync(listener: RealmChangeListener<*>) {
        handler.post {
            activeDatas.forEachIndexed { index, activeData ->
                if (activeData.listener === listener || activeData.listeners === listener) {
                    activeData.unbind()
                    activeDatas.removeAt(index) //use activeDatas.remove(activeData) can cause exception
                    if (activeDatas.indexOfFirst { it.clazz == activeData.clazz } == -1) {
                        reader.closeTable(activeData.clazz)    //reference count--
                    }
                    return@post
                }
            }
        }
    }

    override fun <T : RealmModel> copyAsync(model: T?): JobTracker<T> {
        val tracker = JobTracker<T>()
        handler.post {
            kotlin.runCatching { reader.copy(model) }.onFailure {
                tracker.failure()
            }.onSuccess {
                tracker.success(it)
            }
        }
        return tracker
    }

    override fun <T : RealmModel> copyAllAsync(models: RealmResults<T>): JobTracker<MutableList<T>> {
        val tracker = JobTracker<MutableList<T>>()
        handler.post {
            kotlin.runCatching { reader.copyAll(models) }.onFailure {
                tracker.failure()
            }.onSuccess {
                tracker.success(it)
            }
        }
        return tracker
    }
}

internal data class ActiveData<T : RealmModel>(
    val clazz: Class<T>,
    val result: T? = null,
    val listener: RealmChangeListener<T>? = null,
    val results: RealmResults<T>? = null,
    val listeners: RealmChangeListener<RealmResults<T>>? = null
) {
    fun bind(): ActiveData<T> {
        if (listener != null && result != null) {
            result.tryBind(listener)
        }
        if (listeners != null && results != null) {
            results.tryBind(listeners)
        }

        return this
    }

    fun unbind(): ActiveData<T> {
        if (listener != null && result.handleByRealm()) {
            result.removeChangeListener(listener)
        }
        if (listeners != null && results.handleByRealm()) {
            results.removeChangeListener(listeners)
        }
        return this
    }
}