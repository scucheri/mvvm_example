package com.task.ui.component.login

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

/**
 * Created by xiaoxiaoyu on 2023/4/20.
 */
object TestSuspend {

    suspend fun doSomethingUsefulOne(): Int {
        delay(1000L) // pretend we are doing something useful here
        return 13
    }

    suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L) // pretend we are doing something useful here, too
        return 29
    }


    suspend fun requestElectricCost(): Flow<ExpendModel> =
        flow {
            delay(500)
            emit(ExpendModel("电费", 10f, 500))
        }.flowOn(Dispatchers.IO)

    suspend fun requestWaterCost(): Flow<ExpendModel> =
        flow {
            delay(1000)
            emit(ExpendModel("水费", 20f, 1000))
        }.flowOn(Dispatchers.IO)

    suspend fun requestInternetCost(): Flow<ExpendModel> =
        flow {
            delay(2000)
            emit(ExpendModel("网费", 30f, 2000))
        }.flowOn(Dispatchers.IO)

    data class ExpendModel(val type: String, val cost: Float, val apiTime: Int) {
        fun info(): String {
            return "${type}: ${cost}, 接口请求耗时约$apiTime ms"
        }
    }


    fun test(){

        GlobalScope.launch {
            val time = measureTimeMillis {
                val one = doSomethingUsefulOne()
                val two = doSomethingUsefulTwo()
                Log.i("xiaoyumi testsuspend ","The answer is ${one + two}")
            }
            Log.i("xiaoyumi testsuspend ","Completed in $time ms")
        }

        MainScope().launch {
             flow {
                 repeat(10) {
                     emit(it)
                 }
             }.map {
                 Log.i("xiaoyumi testsuspend  before flowOn", " $it  thread name: ${Thread.currentThread().name}")
                 it*10
             }.flowOn(Dispatchers.IO)// 这个前面的会切换线程到IO；后面的还是在这个scope默认的线程
                 .collect {
                 Log.i("xiaoyumi testsuspend   after flowOn  ", " $it  thread name: ${Thread.currentThread().name}")
             }
            Log.i("xiaoyumi testsuspend ","Completed flow  thread name: ${Thread.currentThread().name}")
        }


        //测试下zip实现接口并行处理
        MainScope().launch {
            val electricFlow = requestElectricCost()
            val waterFlow = requestWaterCost()
            val internetFlow = requestInternetCost()

            val builder = StringBuilder()
            var totalCost = 0f
            val startTime = System.currentTimeMillis()
            //NOTE:注意这里可以多个zip操作符来合并Flow，且多个Flow之间是并行关系
            electricFlow.zip(waterFlow) { electric, water ->
                totalCost = electric.cost + water.cost
                Log.i("xiaoyumi test flow zip 00","总耗时：${System.currentTimeMillis() - startTime} ms  thread name: ${Thread.currentThread().name}")
                builder.append("${electric.info()},\n").append("${water.info()},\n")
            }.zip(internetFlow) { two, internet ->
                totalCost += internet.cost
                Log.i("xiaoyumi test flow zip 11 ","总耗时：${System.currentTimeMillis() - startTime} ms  thread name: ${Thread.currentThread().name}")
                two.append(internet.info()).append(",\n\n总花费：$totalCost")
            }.collect {
                Log.i("xiaoyumi test flow zip 22 ","总耗时：${System.currentTimeMillis() - startTime} ms  thread name: ${Thread.currentThread().name}")
            }
        }


        MainScope().launch {
            //code channelFlow
            val testFlow1 = channelFlow {
                send(20)
                withContext(Dispatchers.IO) { //可切换线程
                    send(22)
                }
            }
            testFlow1.collect {
                Log.i("xiaoyumi channelFlow "," $it thread name: ${Thread.currentThread().name}")
            }
        }

    }
}