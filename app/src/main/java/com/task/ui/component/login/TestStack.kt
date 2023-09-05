package com.task.ui.component.login

import android.content.Context
import android.content.Intent
import java.util.Queue
import java.util.Stack

/**
 * Created by xiaoxiaoyu on 2023/7/19.
 */
object TestStack {
    fun test() {
        val stack = Stack<Int>()
        stack.push(1)
        stack.push(3)
        stack.pop()

        val queue = ArrayDeque<Int>()
        queue.add(1)
        queue.add(2)
        queue.add(3)
        queue.first()
        queue.removeFirst()

    }
}