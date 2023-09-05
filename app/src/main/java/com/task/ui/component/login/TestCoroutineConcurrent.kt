package com.task.ui.component.login

import android.util.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by xiaoxiaoyu on 2023/6/25.
 */
object TestCoroutineConcurrent {

    fun test(){
        MainScope().launch {
            Log.i("TestCoroutineConcurrent ", "test1 before delay")
            delay(1000)
            Log.i("TestCoroutineConcurrent ", "test1 after delay")
        }
        MainScope().launch {
            Log.i("TestCoroutineConcurrent ", "test2")
            MainScope().launch {
                Log.i("TestCoroutineConcurrent ", "test2 relaunch")
            }
        }
        MainScope().launch {
            Log.i("TestCoroutineConcurrent ", "test3")
        }
    }
}