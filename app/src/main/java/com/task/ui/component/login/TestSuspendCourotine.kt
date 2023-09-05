package com.task.ui.component.login

import android.util.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by xiaoxiaoyu on 2023/5/9.
 */
object TestSuspendCourotine {
    private var contMain1 : Continuation<Unit>? = null

    fun test(){

        MainScope().launch {
            main()
        }

        //这样写的话就不用加suspend了，跟上面的写法是一致的
        MainScope().launch {
            Log.i("xiaoyumi TestSuspendCourotine  ","before ")

            suspendCoroutine<Unit> {continuation -> //这个会生成一个suspend point；
//                continuation.resume(Unit)
            }

            Log.i("xiaoyumi TestSuspendCourotine  ","after")
        }

        MainScope().launch {
//           while (true){
               main1()
//           }
        }

        //这样写的话就不用加suspend了，跟上面的写法是一致的
        MainScope().launch {

            val suspendCoroutineCallbackResult = suspendCoroutine<String> {continuation -> //这个会生成一个suspend point；
                                continuation.resume("xiaoyumi suspendCoroutineCallbackResult hahhahah")
            }

            Log.i("xiaoyumi suspendCoroutineCallbackResult result is :  ","$suspendCoroutineCallbackResult")
        }

    }


    suspend fun main(){
            Log.i("xiaoyumi TestSuspendCourotine main ","before ")

            suspendCoroutine<Unit> {continuation ->
                continuation.resume(Unit)
            }

            Log.i("xiaoyumi TestSuspendCourotine  main","after")

        delay(5000)
        contMain1?.resume(Unit)
    }


     suspend fun main1(){//这样写不会导致线程阻塞
        while (true) {
            Log.i("xiaoyumi TestSuspendCourotine main1 in while ", "before ")

            suspendCoroutine<Unit> { continuation ->
                contMain1 = continuation
            //            continuation.resume(Unit)
            }

            Log.i("xiaoyumi TestSuspendCourotine  main1 in while", "after")
        }
       afterWhileTrue()//这个执行不到
    }

    private fun afterWhileTrue() {
        Log.i("xiaoyumi TestSuspendCourotine  afterWhileTrue", "afterWhileTrue")

    }


}