package com.task.ui.component.login

import android.util.Log

/**
 * Created by xiaoxiaoyu on 2023/6/9.
 */
object TestInterface {
    fun test(){
        val testInterface1 : Interface1 = Test()
        testInterface1.test()


        val testInterface2 : Interface2 = Test()
        testInterface2.test()
    }


}

class Test : Interface1, Interface2{
    override fun test() { // 继承两个接口同样的方法，只需要实现一次
       Log.i("testxiaoyu TestInterface ", "test")
    }

    override fun hello1() {

    }

    override fun hello() {

    }
}

interface Interface1{
    fun test()
    fun hello()
}

interface Interface2{
    fun test()
    fun hello1()
}