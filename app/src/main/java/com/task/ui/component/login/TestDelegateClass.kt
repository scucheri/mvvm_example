package com.task.ui.component.login

import android.util.Log
import com.bumptech.glide.Glide.init
import com.task.ui.component.login.TestKotlinDelegate.Delegate1
import com.task.ui.component.login.TestKotlinDelegate.Delegate2

/**
 * Created by xiaoxiaoyu on 2023/5/26.
 */
class TestDelegateClass {
    private val testInner : TestInner = TestInner(this)

    class TestInner(parent : TestDelegateClass){
        private val parent1 = parent
       init {
           Log.i("xiaoyumi TestDelegateClass", "$parent  $parent1")
       }
    }

    interface  TestDelegateInterface{
        fun getLogTag() : String
    }

    class  TestDelegateInterfaceImpl : TestDelegateInterface{
        override fun getLogTag() : String{
            return "xiaoyumi"
        }
    }

    //class的定义写在method里面的话会跟顺序有关，只能用定义在自己前面的class，如果不是在method里面，跟顺序没关系
    // 测试
    class Test : TestDelegateInterface by (TestDelegateInterfaceImpl()) {
        // 属性委托
        val d1: String by Delegate1()
        var d2: Int by Delegate2()
    }

    fun test(){
        val test = Test()
        test.getLogTag()
    }

}