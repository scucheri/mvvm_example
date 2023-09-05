package com.task.ui.component.login

import android.util.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Created by xiaoxiaoyu on 2023/5/10.
 */
object TestMultiSuspend {
    fun test(){
        MainScope().launch {
            testChild()
        }
    }
    suspend fun testChild() {
        Log.i("xiaoyumi TestMultiSuspend ","parent CoroutineScope")
    }
}