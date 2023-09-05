package com.task.ui.component.login

import android.util.Log

/**
 * Created by xiaoxiaoyu on 2023/4/19.
 */
object TestLambdaFun {
    // 入参是一个函数
    inline fun inlined(getString: () -> String?) = Log.i("xiaoyumi test inlined ",getString().toString())

    fun notInlined(getString: () -> String?) = Log.i("xiaoyumi test notInlined ",getString().toString())

    inline fun inlineMultiFun(
        getString: () -> String?,
        logStr: (String?) -> Unit
    ) = logStr(getString())

    fun multiFun(getString: () -> String?, logStr: (String?) -> Unit) = logStr(getString())


    // lambda 带有return的时候，不建议用inline，否则会导致后面的代码无法执行
    inline fun sum(a: Int, b: Int, lambda: (result: Int) -> Int, lambda1: (result: Int) -> Unit) : Int {
        val r = a + b
        lambda.invoke(r)
        lambda1.invoke(a)
        return r
    }


    inline fun sum_1(a: Int, b: Int, lambda: (result: Int) -> Int): Int {
        val r = a + b
        Log.i("xiaoyumi sum_1 lambda invoke result  lambda.invoke(r)","${lambda.invoke(r)}")
        Log.i("xiaoyumi sum_1 lambda invoke result lambda(r)  ","${lambda(r)}")// 调用lambda表达式的两种写法
        return r
    }

    // lambda 带有return的时候，不建议用inline，否则会导致后面的代码无法执行
    inline fun sum_2(a: Int, b: Int, crossinline lambda: (result: Int) -> String): Int {
        val r = a + b
        lambda(r)
        lambda.invoke(r)
        return r
    }

    fun test(): Int {

        var testVar = "Test"

        notInlined { testVar } //这个会每次跑都会new Function接口对象，然后执行

        inlined { testVar } //  这个会把 函数内容复制到这里执行


        multiFun( {testVar},  {
            Log.i("xiaoyumi test multiFun 11 ",it.toString())
        })

        val testLambda : (String?) -> Unit=  {
            Log.i("xiaoyumi test multiFun inlined ",it.toString())
        }
        inlineMultiFun( {testVar}, testLambda)

         sum_1(1, 2) {
            Log.i("xiaoyumi sum_1","Result is: $it")// 这个不会引起整个函数return
             120009 // 在lambda表达式中，最后一行默认就是返回值
        }

        var sum2 = sum_2(1, 2) {
            Log.i("xiaoyumi sum_1","Result is: $it")
//            return 2 // 这个会报错误， 因为sum_2的lamba函数定义成crossinline类型
            return@sum_2 "dfnsngnng" //
        }

        sum(1, 2, {
            println("Result is: $it")
            return 1 // 这个会导致 main 函数直接 return，后面的不会执行了
        },{
            println("Result is: $it")
        })

        println("xiaoyumi Result is: end ")
        return 2
    }
}