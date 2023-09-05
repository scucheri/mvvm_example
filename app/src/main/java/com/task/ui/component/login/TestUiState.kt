package com.task.ui.component.login

import android.util.Log
import java.util.HashMap
import java.util.Map

object TestUiState {
    fun test(){
       val uiState1 = listOf<Int>(23, 12,13)
       val uiState2 = listOf<Int>(23, 12,13)
       Log.i("testxiaoyuTestUiState1  ", "${uiState1.isUIStateChanged(uiState2)}")

       val uiState11 = listOf<Int>(23, 12,13)
       val uiState22 = listOf<Int>(21, 12,13)
       Log.i("testxiaoyuTestUiState11 ", "${uiState11.isUIStateChanged(uiState22)}")


       val uiState1112 = HashMap<String, String>()
        val uiState111212_new = HashMap<String, String>()
        uiState1112.put("xiaoyuyi","skfjjnvngf")
        uiState1112.put("xiaoyuyi12","skfjjnvngf12")

        uiState111212_new.put("xiaoyuyi","skfjjnvngf")
        uiState111212_new.put("xiaoyuyi12","skfjjnvngf12dnm")
        Log.i("testxiaoyuTestUiState11 map ", "${uiState1112.isUIStateChanged(uiState111212_new)}")


        val uiState1112_11 = HashMap<String, String>()
        val uiState111212_new_11 = HashMap<String, String>()
        uiState1112_11.put("xiaoyuyi","skfjjnvngf")
        uiState1112_11.put("xiaoyuyi12","skfjjnvngf12")
        uiState111212_new_11.put("xiaoyuyi","skfjjnvngf")
        uiState111212_new_11.put("xiaoyuyi12","skfjjnvngf12")

        Log.i("testxiaoyuTestUiState11 map 22 ", "${uiState1112_11.isUIStateChanged(uiState111212_new_11)}")


    }
}
//HashMap和List本身就支持了equals方法，不需要自己去实现
inline fun <reified UIStateData> UIStateData.isUIStateChanged(oldState : UIStateData): Boolean {
//    Log.i("testxiaoyu isUIStateChanged", "${UIStateData::class.java}")
//        if(this is Collection<*>) {
//            Log.i("testxiaoyu isUIStateChanged ", "Collection ${UIStateData::class.java}")
//
//            val newSateCollection = this as Collection<Any>
//            val oldSateCollection = oldState as Collection<Any>
//            if(newSateCollection.size != oldSateCollection.size){
//                return true
//            }
//            for (i in newSateCollection.indices){
//                if(newSateCollection.elementAt(i) != oldSateCollection.elementAt(i)){
//                    return true
//                }
//            }
//            return false
//        }
//        if(this is Map<*, *>){
//            Log.i("testxiaoyu isUIStateChanged ", "Map ${UIStateData::class.java}")
//
//            val newSateMap = this as Map<Any, Any>
//            val oldSateMap = oldState as Map<Any, Any>
//            if(newSateMap.size() != oldSateMap.size()){
//                return true
//            }
//            newSateMap.keySet().forEach {
//                if(newSateMap.get(it) != oldState.get(it)){
//                    return true
//                }
//            }
//            return false
//        }
    return this != oldState
}
