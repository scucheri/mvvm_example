package com.task.ui.component.login

import android.util.Log
import kotlin.properties.Delegates
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by xiaoxiaoyu on 2023/4/18.
 */
object TestKotlinDelegate {
    interface IGamePlayer {
        // 打排位赛
        fun rank()
        // 升级
        fun upgrade()
    }

    interface IDiagramController {
        /**
         * 为了进一步加快启动速度，启动时先从磁盘中读取instance
         */
        fun preLoadDiskInstance()
    }

    fun testDelegate(){
        // 约束类


        // 被委托对象，本场景中的游戏代练
        class RealGamePlayer(private val name: String): IGamePlayer{
            override fun rank() {
                Log.i("xiaoyumi test delegate","$name 开始排位赛")
            }

            override fun upgrade() {
                Log.i("xiaoyumi test delegate","$name 升级了")
            }

        }

        // 委托对象
        class DelegateGamePlayer(private val player: IGamePlayer): IGamePlayer by player

        val realGamePlayer = RealGamePlayer("张三")
        val delegateGamePlayer = DelegateGamePlayer(realGamePlayer)
        delegateGamePlayer.rank()
        delegateGamePlayer.upgrade()



        fun createController(): IDiagramController{
            return object : IDiagramController {
                override fun preLoadDiskInstance() {

                }
            }
        }
        //这种其实就是单例模式
        val diagramController: IDiagramController by lazy {
            return@lazy object : IDiagramController {
                override fun preLoadDiskInstance() {

                }
            }
        }
        val diagramController1: IDiagramController by lazy {
           createController()
        }


        val diagramController2: IDiagramController by lazy{
            createController()
        }

    }


    fun testPropertyDelegate() {
        class Delegate {
            operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
                return "$thisRef, thank you for delegating '${property.name}  ${property.getter}' to me!"
            }

            operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
                Log.i("xiaoyumi test_property delegate set value: ","$value has been assigned to '${property.name}' in $thisRef.")
            }
        }

        class Test {
            // 属性委托
            var prop: String by Delegate()
        }

        Log.i("xiaoyumi test_property delegate init",Test().prop)
        Test().prop = "Hello, Android技术杂货铺！"

        Log.i("xiaoyumi test_property delegate init 22",Test().prop)


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

    class Delegate1_1: ReadOnlyProperty<Any, String> {
        override fun getValue(thisRef: Any, property: KProperty<*>): String {
            return "通过实现ReadOnlyProperty实现，name:${property.name}"
        }
    }
    // val 属性委托实现
    class Delegate1: ReadOnlyProperty<Any, String> {
        override fun getValue(thisRef: Any, property: KProperty<*>): String {
            return "通过实现ReadOnlyProperty实现，name:${property.name}"
        }
    }
    // var 属性委托实现
    class Delegate2: ReadWriteProperty<Any, Int> {
        var  myName :Int = 0

        override fun getValue(thisRef: Any, property: KProperty<*>): Int {
            Log.i("xiaoyumi testProperDelegate2","thisRef is Test : ${thisRef is Test}   ${(thisRef as Test).d1}")
            return  myName
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
            Log.i("xiaoyumi testProperDelegate2","委托属性为： ${property.name} 委托值为： $value")
            myName = value
        }
    }
    fun testProperDelegate2(){
        val test = Test()
        test.getLogTag()
        Log.i("xiaoyumi testProperDelegate2 ",test.d1)
        Log.i("xiaoyumi testProperDelegate2 ","${test.d2}")
//        test.d2 = 100
//        Log.i("xiaoyumi testProperDelegate2 after set  ","${test.d2}")
    }

    fun testByLazy(){
        val lazyProp: String by lazy {
            Log.i("xiaoyumi testByLazy after set  ","Hello，第一次调用才会执行我！")
            "西哥！"
        }

        // 打印lazyProp 3次，查看结果
        Log.i("xiaoyumi testByLazy after set  ",lazyProp)
        Log.i("xiaoyumi testByLazy after set  ",lazyProp)
        Log.i("xiaoyumi testByLazy after set  ",lazyProp)
    }

    fun testDelegateObservable(){
        var observableProp: String by Delegates.observable("默认值：xxx"){
                property, oldValue, newValue ->
            Log.i("xiaoyumi testDelegateObservable observableProp  ","property: $property: $oldValue -> $newValue ")
        }

        // 测试
        observableProp = "第一次修改值"
        observableProp = "第二次修改值"

        var vetoableProp: Int by Delegates.vetoable(0){
                _, oldValue, newValue ->
            // 如果新的值大于旧值，则生效
            newValue > oldValue
        }

        Log.i("xiaoyumi testDelegateObservable vetoableProp  ","vetoableProp=$vetoableProp")
        vetoableProp = 10
        Log.i("xiaoyumi testDelegateObservable vetoableProp  ","vetoableProp=$vetoableProp")
        vetoableProp = 5
        Log.i("xiaoyumi testDelegateObservable vetoableProp  ","vetoableProp=$vetoableProp")
        vetoableProp = 100
        Log.i("xiaoyumi testDelegateObservable vetoableProp  ","vetoableProp=$vetoableProp")
    }

    fun testDelegateMap() {
        class User(val map: Map<String, Any?>) {
            val name: String by map
            val age: Int by map
        }

        // 把map映射成User
        val user = User(
            mapOf(
                "name" to "西哥", "age" to 25
            )
        )
        Log.i("xiaoyumi testDelegateMap map  ","name=${user.name} age=${user.age}")
    }
}