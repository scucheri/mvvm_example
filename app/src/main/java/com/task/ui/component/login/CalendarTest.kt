package com.task.ui.component.login

import android.util.Log
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Created by xiaoxiaoyu on 2023/5/11.
 */
object CalendarTest {

    fun test(){
        val mGregorianCalendar = GregorianCalendar()
        Log.i("xiaoyumi CalendarTest","${mGregorianCalendar[Calendar.MONTH]}   ${mGregorianCalendar[Calendar.DATE]}  ${mGregorianCalendar[Calendar.DAY_OF_WEEK]}")
    }
}