package com.task.ui.component.login

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by xiaoxiaoyu on 2023/4/20.
 */
object TestAsync {

    private suspend fun getContent1():String{
        delay(1000)
        Log.i("xiaoyumiTestAsync getContent1","${Thread.currentThread().name}")
        return "Kotlin"
    }

    private suspend fun getContent2():String{
        delay(1000)
        Log.i("xiaoyumiTestAsync getContent2","${Thread.currentThread().name}")
        return "协程"
    }


    fun test(){
        // 这样是串行执行
        MainScope().launch {
            val startTime = System.currentTimeMillis()
            val content_1 = getContent1()
            val content_2 = getContent2()
            Log.i("xiaoyumiTestAsync test sync","$content_1 $content_2,程序耗时：${System.currentTimeMillis() - startTime}")
        }

        // 这样是并行执行
        MainScope().launch {
            val startTime = System.currentTimeMillis()
            val content_2 = this.async { getContent2() }
            val content_1 = this.async { getContent1() }

            Log.i("xiaoyumiTestAsync test async","${content_1.await()} ${content_2.await()},程序耗时：${System.currentTimeMillis() - startTime}")
        }

        // 这样是并行执行，这个和上面那个是一样的
        MainScope().launch {
            val startTime = System.currentTimeMillis()
            val content_2 = this.async { getContent2() }
            val content_1 = this.async { getContent1() }

            Log.i("xiaoyumiTestAsync test async 222","${content_1.await()} ,程序耗时：${System.currentTimeMillis() - startTime}")
            Log.i("xiaoyumiTestAsync test async 222","${content_2.await()},程序耗时：${System.currentTimeMillis() - startTime}")

        }

        suspend fun testChannel(){
            val channel = Channel<Int>(Channel.RENDEZVOUS)

            val jobProducer = GlobalScope.launch {
                for(i in 1..2){
                    Log.i("xiaoyumiTestAsync test channel","sending $i thread name : ${Thread.currentThread().name}")
                    channel.send(i) //如果没有receive,那么send就会挂起
                    Log.i("xiaoyumiTestAsync test channel","sent $i thread name : ${Thread.currentThread().name}")
                }
//                channel.close()
            }

            val jobConsumer = GlobalScope.launch {
                while (!channel.isClosedForReceive){
                    Log.i("xiaoyumiTestAsync test channel","receiving thread name : ${Thread.currentThread().name}")
                    val value = channel.receive() //如果没有channel在send，receive也会挂起
                    Log.i("xiaoyumiTestAsync test channel","received $value thread name : ${Thread.currentThread().name}")
                }
            }
            jobProducer.join()
            jobConsumer.join()
        }

        MainScope().launch {
            testChannel()
        }


    }

}