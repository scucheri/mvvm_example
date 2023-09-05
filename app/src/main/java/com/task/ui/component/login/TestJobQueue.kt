package com.task.ui.component.login

import android.util.Log
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by xiaoxiaoyu on 2023/6/5.
 */
object TestJobQueue {
    private val scope = MainScope()
    private val scope1 = GlobalScope

    //    private val queue = Channel<Job>(Channel.UNLIMITED)
    private val jobs: CopyOnWriteArrayList<MyJob> = CopyOnWriteArrayList()
    private var myContinuation: CancellableContinuation<Unit>? = null

    init { //        scope.launch(Dispatchers.Default) {
        //            for (job in queue) job.join()
        //        }

        Log.i("xiaoyumi TestJobQueue ", "init")

        scope.launch {
            while (true) {
                Log.i("xiaoyumi TestJobQueue ", "while  ${Thread.currentThread().name} ")
                suspendCancellableCoroutine<Unit> { continuation ->
                    myContinuation = continuation
                    if (jobs.size > 0) { // 这个必须要有，否则不会继续执行
                        myContinuation?.resume(Unit)
                    }
                    Log.i("xiaoyumi TestJobQueue ", "continuation ")
                }
                for (myJob in jobs) { // job在join之前或者运行结束之后，active状态都是false
                    Log.i(
                        "xiaoyumi TestJobQueue  ",
                        "active state  ${myJob.id}  ${myJob.job.isActive} ${Thread.currentThread().name}"
                    )
                }
                for (job in jobs) {
                    Log.i("xiaoyumi TestJobQueue ", "job.join  ${Thread.currentThread().name}")
                    job.job.join()
                    jobs.remove(job)
                }
            }
        }

    }

    fun test() {
        scope.launch {
            val job1 = submit(1) {
                Log.i("xiaoyumi TestJobQueue ", "submit 1 ${Thread.currentThread().name}")
                delay(1000)
            }
            val job2 = submit(2) {
                delay(1000)
                Log.i("xiaoyumi TestJobQueue ", "submit 2  ${Thread.currentThread().name}")
            }
            val job3 = submit(3) {
                Log.i("xiaoyumi TestJobQueue ", "submit 3  ${Thread.currentThread().name}")
            }

            val job4 = scope.launch {
                delay(5000)
                submit(4) {
                    Log.i("xiaoyumi TestJobQueue ", "submit 4  ${Thread.currentThread().name}")
                }
            }
            val job5 = submit(5, true) {
                Log.i("xiaoyumi TestJobQueue ", "submit 5  ${Thread.currentThread().name}")
            }
            val job6 = submit(6, true) {
                Log.i("xiaoyumi TestJobQueue ", "submit 6  ${Thread.currentThread().name}")
            }

            //        for (myJob in jobs) { // index and number
            //            if(myJob.id % 2 == 0){
            //                jobs.remove(myJob)
            //                if(myJob.job.isActive){
            //                    myJob.job.cancel()
            //                }
            //                Log.i("xiaoyumi TestJobQueue  ", "removeJob iterator ${myJob.id} ${Thread.currentThread().name}")
            //            }
            //        }
        }

    }


    fun submit(
        id: Int,
        insertToFirst: Boolean = false,
        block: suspend CoroutineScope.() -> Unit,
    ): MyJob {
        val job = scope.launch(
            Dispatchers.IO,
            CoroutineStart.LAZY,
            block
        ) // CoroutineStart.LAZY这个很重要，只有是这种模式，配合上面的join，才能实现任务队列 //        queue.trySend(job)
        val myJob = MyJob(id, job)
        if (insertToFirst) {
            jobs.add(0, myJob)
        } else {
            jobs.add(myJob)
        }
//        job.invokeOnCompletion {
//            Log.i(
//                "xiaoyumi TestJobQueue ",
//                "invokeOnCompletion remove   ${Thread.currentThread().name}   job.isActive: ${job.isActive}"
//            )
//            jobs.remove(myJob)
//        }
        try {
            myContinuation?.resume(Unit)
        } catch (e: Exception) {
            Log.i("xiaoyumi TestJobQueue exception ", e.message.toString())
        }
        return myJob
    }

    fun cancel() { //        queue.cancel()
        scope.cancel()
    }


    data class MyJob(val id: Int, val job: Job)
}