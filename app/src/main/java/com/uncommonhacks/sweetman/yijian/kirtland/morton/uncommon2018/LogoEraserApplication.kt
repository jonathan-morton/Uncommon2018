package com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018

import android.app.Application

import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.utils.DebugTree

import timber.log.Timber

/**
 * Created by Jonathan Morton on 2/10/18.
 */
class LogoEraserApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        //        restClient = new RestClient(this);
        Timber.plant(DebugTree())
    }

    companion object {
        val restClient by lazy {

        }
    }


}
