package com.qlk.core.tasks

import android.os.Handler
import android.os.Looper
import com.qlk.core.cache.*
import com.qlk.core.isOnMainThread
import kotlinx.coroutines.Job
import java.util.concurrent.Future

/**
 * copy from kotlin.Result
 *
 * <br/>
 * QQ：1055329812<br/>
 * Created by QiLiKing on 2019-07-18 17:49
 */
sealed class Result<T>(protected var value: Any? = null) : Cacheable {

    @Suppress("UNCHECKED_CAST")
    fun getOrNull(): T? = when {
        successful -> runCatching { value as T }.getOrNull()
        else -> null
    }

    fun getReasonOrNull(): Reason? = kotlin.runCatching { value as Reason }.getOrNull()

    /**
     * not check null value, just rely on "isSuccess" flag
     */
    open fun onSuccess(action: (T?) -> Unit): Result<T> {
        if (successful) {
            action(getOrNull())
        }
        return this
    }

    open fun onSuccessVoid(executable: Executable<T>): Result<T> {
        return onSuccess { executable.execute(it) }
    }

    open fun onFailure(action: (Reason?) -> Unit): Result<T> {
        if (!successful) {
            action(getReasonOrNull())
        }
        return this
    }

    open fun onFailureVoid(executable: Executable<Reason>): Result<T> {
        return onFailure { executable.execute(it) }
    }

    override fun byteCodes(): ByteSize = 1

    companion object {
        @JvmStatic
        @JvmOverloads
        fun <T> success(value: T? = null): Result<T> =
            Success<T>(value)

        @JvmStatic
        @JvmOverloads
        fun <T> failure(reason: Reason? = null): Result<T> =
            Failure(reason)

        @JvmStatic
        fun <T> empty(): Result<T> = JobTracker()
    }
}

class Failure<T>(reason: Reason? = null) : Result<T>(reason)

class Success<T>(value: T? = null) : Result<T>(value)

open class MutableResult<T> : Result<T>() {
    private var success: Boolean? = null
    private var onComplete: (() -> Unit)? = null
    private var onSuccess: ((T?) -> Unit)? = null
    private var onFailure: ((Reason?) -> Unit)? = null
    private var onUIThread: Boolean = false

    /**
     * @return null the result has not been set to exact flag(false or true). Maybe returned from a async task.
     */
    fun getSuccessFlag(): Boolean? = success

    fun subscribeOnUI(): MutableResult<T> {
        onUIThread = true
        return this
    }

    fun success(value: T?) {
        this.success = true
        this.value = value
        if (onUIThread && !isOnMainThread) {
            handler.post {
                onComplete?.invoke()
                onSuccess?.invoke(value)
            }
        } else {
            onComplete?.invoke()
            onSuccess?.invoke(value)
        }
    }

    fun failure(reason: Reason? = null) {
        this.success = false
        this.value = reason
        if (onUIThread && !isOnMainThread) {
            handler.post {
                onComplete?.invoke()
                onFailure?.invoke(reason)
            }
        } else {
            onComplete?.invoke()
            onFailure?.invoke(reason)
        }
    }

    fun onComplete(action: () -> Unit): MutableResult<T> {
        this.onComplete = action
        if (success != null) {
            action.invoke() //already complete
        }
        return this
    }

    override fun onSuccess(action: (T?) -> Unit): MutableResult<T> {
        this.onSuccess = action
        if (success == true) {
            if (onUIThread && !isOnMainThread) {
                handler.post {
                    action.invoke(value as T?)
                }
            } else {
                action.invoke(value as T?)
            }
        }
        return this
    }

    override fun onSuccessVoid(executable: Executable<T>): MutableResult<T> {
        return onSuccess { executable.execute(it) }
    }

    override fun onFailure(action: (Reason?) -> Unit): MutableResult<T> {
        this.onFailure = action
        if (success == false) {
            if (onUIThread && !isOnMainThread) {
                handler.post {
                    action.invoke(value as Reason?)
                }
            } else {
                action.invoke(value as Reason?)
            }
        }
        return this
    }

    override fun onFailureVoid(executable: Executable<Reason>): MutableResult<T> {
        return onFailure { executable.execute(it) }
    }

    companion object {
        private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }
    }
}

/**
 * For kotlin coroutines
 *
 * Attention:
 *      You should switch to the original thread when calling back with {@method success} or {@method failure}
 */
class JobTracker<T> : MutableResult<T>() {
    private var suspend: Boolean = false
    private var job: Job? = null

    fun setJob(job: Job) {
        this.job = job
    }

    fun getJob(): Job? = job

    fun isCanceled(): Boolean = job?.isCancelled == true

    fun isActive(): Boolean = !isCanceled()

    fun cancelJob() {
        suspend = false
        job?.cancel()
    }

    fun isSuspending(): Boolean = suspend && isActive()

    fun suspendJob() {
        if (!suspend && isActive()) {
            suspend = true
        }
    }

    fun resumeJob() {
        if (suspend && isActive()) {
            job?.start() //not necessary
            suspend = false
        }
    }

    fun cache(cacheTag: CacheTag) {
        CachePools.put(TrackerPool, cacheTag, this)
    }

    companion object {
        val TrackerPool: PoolName = JobTracker::class.java.simpleName

        @JvmStatic
        fun <T> cancelJobFromCache(cacheTag: CacheTag) {
            CachePools.getAndRemove<JobTracker<T>>(TrackerPool, cacheTag)?.cancelJob()
        }

        @JvmStatic
        fun <T> resumeJobFromCache(cacheTag: CacheTag) {
            CachePools.getAndRemove<JobTracker<T>>(TrackerPool, cacheTag)?.resumeJob()
        }

        @JvmStatic
        fun <T> suspendJobFromCache(cacheTag: CacheTag) {
            CachePools.getAndRemove<JobTracker<T>>(TrackerPool, cacheTag)?.suspendJob()
        }
    }
}

class FutureTracker<T> : MutableResult<T>() {
    private var suspend: Boolean = false
    private var job: Future<*>? = null

    fun setJob(job: Future<*>) {
        this.job = job
    }

    fun getJob(): Future<*>? = job

    fun isCanceled(): Boolean = job?.isCancelled == true

    fun isActive(): Boolean = !isCanceled()

    fun cancelJob() {
        suspend = false
        job?.cancel(true)
    }

    fun isSuspending(): Boolean = suspend && isActive()

    fun suspendJob() {
        if (!suspend && isActive()) {
            suspend = true
        }
    }

    fun resumeJob() {
        if (suspend && isActive()) {
            suspend = false
        }
    }

    fun cache(cacheTag: CacheTag) {
        CachePools.put(TrackerPool, cacheTag, this)
    }

    companion object {
        val TrackerPool: PoolName = FutureTracker::class.java.simpleName

        @JvmStatic
        fun <T> cancelJobFromCache(cacheTag: CacheTag) {
            CachePools.getAndRemove<FutureTracker<T>>(TrackerPool, cacheTag)?.cancelJob()
        }

        @JvmStatic
        fun <T> resumeJobFromCache(cacheTag: CacheTag) {
            CachePools.getAndRemove<FutureTracker<T>>(TrackerPool, cacheTag)?.resumeJob()
        }

        @JvmStatic
        fun <T> suspendJobFromCache(cacheTag: CacheTag) {
            CachePools.getAndRemove<FutureTracker<T>>(TrackerPool, cacheTag)?.suspendJob()
        }
    }
}

val <T> Result<T>.completed: Boolean
    get() = when (this) {
        is JobTracker<T> -> getSuccessFlag() != null
        else -> true
    }

val <T> Result<T>.successful: Boolean
    get() = when (this) {
        is Failure -> false
        is Success -> true
        is MutableResult<T> -> getSuccessFlag() == true
    }

/**
 * helper for failure result. e.g. Result(Reason("this is an error message", -1))
 */
open class Reason @JvmOverloads constructor(
    val message: String? = null /* Usually message is most useful for user, so put at front */,
    val errCode: Int = 0
) {
    var throws: Throwable? = null

    fun withException(throws: Throwable): Reason {
        this.throws = throws
        return this
    }
}

/**
 * Only contains primitive key-value types
 */
typealias SimpleJson = String