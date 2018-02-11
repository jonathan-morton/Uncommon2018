package com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.utils

import timber.log.Timber

/**
 * Created by Jonathan Morton on 10/21/17.
 */
class DebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String {
        return super.createStackElementTag(element) + ":" + element.methodName + ":" + element.lineNumber
    }


}