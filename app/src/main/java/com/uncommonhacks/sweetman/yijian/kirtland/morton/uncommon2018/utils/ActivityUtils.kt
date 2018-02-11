package com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.utils

import android.app.Activity
import android.content.Intent

fun Activity.startActivity(activityClass: Class<*>) {
    startActivity(Intent(this, activityClass))
}