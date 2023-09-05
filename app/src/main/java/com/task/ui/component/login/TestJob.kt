package com.task.ui.component.login

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by xiaoxiaoyu on 2023/6/5.
 */
object TestJob {
    fun test(){
        val job1 = GlobalScope.launch {
            Log.i("xiaoyumitest TestJob", "job1 running")
            delay(1000)
        }


        val job2 = GlobalScope.launch {
            job1.join()
            Log.i("xiaoyumitest TestJob", "job2 running")
        }

        job1.invokeOnCompletion{
            Log.i("xiaoyumitest TestJob ", "job1 invokeOnCompletion")
        }

        job2.invokeOnCompletion{
            Log.i("xiaoyumitest TestJob", "job2 invokeOnCompletion")
        }

        }
}