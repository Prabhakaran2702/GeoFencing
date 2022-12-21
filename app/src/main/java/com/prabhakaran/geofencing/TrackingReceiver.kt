package com.prabhakaran.geofencing


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.prabhakaran.geofencing.TrackingService.Companion.TRACK_COUNT


class TrackingReceiver : BroadcastReceiver() {
    var counter = 0
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TrackingReceiver::class.java.simpleName, "Service Stopped! !!!!")
        if (intent.extras != null && intent.extras != null && intent.extras!!.getInt(TRACK_COUNT) != 0) {
            counter = intent.extras!!.getInt(TRACK_COUNT)
        }
        val `in` = Intent(context, TrackingService::class.java)
        `in`.putExtra(TRACK_COUNT, counter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(`in`)
        } else {
            context.startService(`in`)
        }
    }
}
