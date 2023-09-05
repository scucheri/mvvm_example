package com.task.ui.component.login

import android.util.Log

/**
 * Created by xiaoxiaoyu on 2023/6/6.
 */
object HashMapTest {
    fun test(){
        val map = HashMap<Demo, String>()
        map.put(Demo("xiaoyumi"), "dhjshfjdsgshg")
        map.put(Demo("xiaoyumi"), "lallalladljfjdngnng")
        map.put(Demo("xiaoyumi"), "xiaoyumi dsfbf baobao dnnnfhdshavfg mammai")

        Log.i("xiaoyumi HashMapTest ", "${map.get(Demo("xiaoyumi"))}  ${map.size}")
    }

    class Demo(val str : String){
        override fun hashCode(): Int {
            return str.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if(other is Demo){
                return other.str == str
            }
            return false
        }
    }


}