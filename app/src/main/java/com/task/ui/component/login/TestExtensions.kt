package com.task.ui.component.login

import android.util.Log

/**
 * Created by xiaoxiaoyu on 2023/8/10.
 */
object TestExtensions {

    fun test(){
        val str : String? = null
        str.log()
    }
}

fun String?.log(){
    Log.i("testxiaoyu String?.log", "${this}")
    if (this == null){
        Log.i("testxiaoyu String?.log", "${this} is null")

    }
}