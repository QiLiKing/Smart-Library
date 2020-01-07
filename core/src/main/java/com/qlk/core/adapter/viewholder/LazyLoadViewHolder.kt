package com.qlk.core.adapter.viewholder

import android.view.View
import com.qlk.core.ILazyLoader
import com.qlk.core.isInDebugMode

/**
 *
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-17.
 *
 * @see LifecycleViewHolder
 */
abstract class LazyLoadViewHolder<T>(itemView: View) : LifecycleViewHolder(itemView),
    ILazyLoader<T> {
//    private val taskExecutor by lazy { OrderTask<Int>(this) }

    /**
     * Adapter stop scrolling, and you can do your heavy work now<br/>
     *
     * When working, you should check the "isActive" state to suspend your heavy work
     *
     * NOT be called if the ViewHolder has been detached from window before RecycleView stopping scroll
     */
    override fun onLazyLoad(t: T) {
//        if (this::taskExecutor.initialized) {
//            taskExecutor.runTasks()
//        }
        if (isInDebugMode) println("LazyLoadViewHolder#onLazyLoad:27-> t=$t pos=$adapterPosition")
    }

//    /**
//     * You should entrust your tasks in {@method onPrimaryLoad}, and they will be executed in {@method onLazyLoad} automatically.
//     *
//     * @param task you should check "isActive" state when trying to update UI. Or where before a long time task starts.
//     */
//    protected fun entrustLazyTask(order: Int, task: SimpleCoroutineTask) {
//        taskExecutor.setTask(order, task)
//    }

    /* Task Delegate */

//    private var taskPool: MutableList<ISimpleTask>? = null
//    private var suspendTasks: MutableList<ISimpleTask>? = null
//    private var runningTasks: HashMap<Job, ISimpleTask>? = null
//    private var nextTaskIndex = 0
//    private var maxThreadCount: Int = MAX_THREAD_COUNT
//
//    /**
//     * clear task pool and add new tasks to it.
//     *
//     * Entrust your background tasks, that should do in onLazyLoad() method, to parent.<br/>
//     *
//     * If some of the tasks are suspended by state, they will be continue executed when resumed.<br/>
//     *
//     * All tasks will be executed again every time when onPrimaryLoad() method called.
//     * Before do this, the tasks that wake up by upper step will be canceled firstly.
//     *
//     * @param tasks you should check "isActive" state when trying to update UI in each task. Or where before a long time task starts.
//     */
//    protected fun entrustLazyTasks(vararg tasks: ISimpleTask /* false to retry when resumed */) {
//        if (isActive) throw IllegalThreadStateException("You should not call entrustLazyTasks when isActive == true, use entrustLazyTask() instead ")
//        obtainTaskPool().run {
//            clear()
//            addAll(tasks)
//        }
//
//        suspendTasks?.clear()
//    }
//
//    protected fun setMaxThreadCount(
//        @IntRange(
//            from = MIN_THREAD_COUNT.toLong(),
//            to = MAX_THREAD_COUNT.toLong()
//        ) maxCount: Int
//    ) {
//        maxThreadCount = maxCount
//    }
//
//    /**
//     * be ready for next binding progress
//     */
//    private fun resetTasks() {
//        taskPool = null
//        suspendTasks = null
//        runningTasks = null // canceled by onInactive() method, so set to null directly
//
//        nextTaskIndex = 0
////        maxThreadCount = MAX_THREAD_COUNT //should keep this setting
//    }
//
//    private fun runTaskPool() {
//        val pool = taskPool ?: return
//        for (index in 0 until min(maxThreadCount(), pool.size)) {
//            nextTaskIndex++
//            val task = pool[index]
//            GlobalScope.launch {
//                task.invoke()
//            }.also { job ->
//                obtainRunningTasks()[job] = task
//                job.invokeOnCompletion { error ->
//                    if (error == null) {
//                        runningTasks?.remove(job)
//                    } else {
//                        obtainSuspendTasks().add(task)
//                    }
//                }
//            }
//        }
//    }
//
//    protected fun addTask(order: Int, task: ISimpleTask) {
//        val pool = taskPool ?: return
//        for (index in 0 until min(maxThreadCount(), pool.size)) {
//            nextTaskIndex++
//            val task = pool[index]
//            GlobalScope.launch {
//                task.invoke(v)
//            }.also { job ->
//                obtainRunningTasks()[job] = task
//                job.invokeOnCompletion { error ->
//                    if (error == null) {
//                        obtainRunningTasks().remove(job)
//                    } else {
//                        obtainSuspendTasks().add(task)
//                    }
//                }
//            }
//        }
//    }
//
//    private fun obtainTaskPool(): MutableList<ISimpleTask> =
//        taskPool ?: mutableListOf<ISimpleTask>().also { taskPool = it }
//
//    private fun obtainSuspendTasks(): MutableList<ISimpleTask> =
//        suspendTasks ?: mutableListOf<ISimpleTask>().also { suspendTasks = it }
//
//    private fun obtainRunningTasks(): HashMap<Job, ISimpleTask> =
//        runningTasks ?: HashMap<Job, ISimpleTask>().also { runningTasks = it }
//
//    companion object {
//        const val DEFAULT_ACTIVE = true
//        const val MIN_THREAD_COUNT = 1
//        const val MAX_THREAD_COUNT = 5
//    }
}