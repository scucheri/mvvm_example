package com.task.ui.component.login

import android.util.Log

/**
 * Created by xiaoxiaoyu on 2023/6/26.
 */
object TestSealedClass {
    sealed class Mathematics{
        data class Dou(val number: Double) : Mathematics()
        data class Sub(val e1: Mathematics, val e2: Mathematics) : Mathematics()
        object NotANumber : Mathematics()
        data class TestSeal(val test : Double) : Mathematics()

        fun eval(m: Mathematics): Double = when(m) {
            is Dou -> {
                m.number
            }
            is Sub -> eval(m.e1) - eval(m.e2)
            is TestSeal -> m.test
            NotANumber -> Double.NaN
        }
    }



    fun test(){
        var ec1:Mathematics = Mathematics.Dou(5.0)
        var d1 = ec1.eval(ec1)
        Log.i("TestSealedClass d1", "$d1")

        var ec2:Mathematics = Mathematics.Sub(ec1, Mathematics.Dou(3.0))
        var d2 = ec2.eval(ec2)
        Log.i("TestSealedClass d2", "$d2")


        var ec3:Mathematics = Mathematics.NotANumber
        var d3 = ec3.eval(ec3)
        Log.i("TestSealedClass d3", "$d3")


        var ec4:Mathematics = Mathematics.TestSeal(100.0)
        var d4 = ec3.eval(ec4)
        Log.i("TestSealedClass d4", "$d4")
    }
}