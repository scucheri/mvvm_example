package com.task.ui.component.login

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by xiaoxiaoyu on 2023/5/10.
 */
object TestScopeCancelLeak {
    fun test(viewModelScope: CoroutineScope) {
//        testLeak(viewModelScope)
        testNotLeak(viewModelScope)
    }

    private fun testNotLeak(viewModelScope: CoroutineScope) {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                suspendLeak1(viewModelScope)
                suspendLeak2(viewModelScope)
                suspendLeak3(viewModelScope)
            }
        }
    }

    /**
     * 这是有问题的方法
     */
    private fun testLeak(viewModelScope: CoroutineScope) {
        viewModelScope.launch(Dispatchers.IO) {
            var i = 0L
            while (true) {// 这里面的代码在 viewModelScope cancel之后还不能停止
                i++
                if (i % 100000000L == 0L) {
                    Log.i("xiaoyumi TestScopeCancelLeak testLeak ", "i=$i   ThreadName=${Thread.currentThread().name}  ${isActive}")
                }
            }
        }
    }

    suspend fun suspendLeak1(viewModelScope: CoroutineScope) {
        delay(1000)
        Log.i("xiaoyumi TestScopeCancelLeak suspendLeak1 ", "ThreadName=${Thread.currentThread().name}  ${viewModelScope.isActive}")
    }

    suspend fun suspendLeak2(viewModelScope: CoroutineScope) {
        suspendCoroutine<Unit> {
            it.resume(Unit)
        }
        Log.i("xiaoyumi TestScopeCancelLeak suspendLeak2 ", "  ThreadName=${Thread.currentThread().name}  ${viewModelScope.isActive}")
    }
    suspend fun suspendLeak3(viewModelScope: CoroutineScope) {
        delay(1000)
        Log.i("xiaoyumi TestScopeCancelLeak suspendLeak3 ", "  ThreadName=${Thread.currentThread().name}  ${viewModelScope.isActive}")
    }
}