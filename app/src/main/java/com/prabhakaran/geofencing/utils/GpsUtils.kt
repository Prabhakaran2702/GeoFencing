package com.prabhakaran.geofencing.utils

import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import java.util.*

object GpsUtils {
    // If none of the methods above worked
    val serialNumber: String?
        get() {
            var serialNumber: String?
            try {
                val c = Class.forName("android.os.SystemProperties")
                val get = c.getMethod("get", String::class.java)
                serialNumber = get.invoke(c, "gsm.sn1") as String
                if (serialNumber == "") serialNumber = get.invoke(c, "ril.serialnumber") as String
                if (serialNumber == "") serialNumber = get.invoke(c, "ro.serialno") as String
                if (serialNumber == "") serialNumber = get.invoke(c, "sys.serialnumber") as String
                if (serialNumber == "") serialNumber = Build.SERIAL
                // If none of the methods above worked
                if (serialNumber == "") serialNumber = null
            } catch (e: Exception) {
                e.printStackTrace()
                serialNumber = null
            }
            return serialNumber
        }

    fun isLocationEnabled(mContext: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            val lm =
                mContext.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.isLocationEnabled
        } else {
            // This is Deprecated in API 28
            val mode = Settings.Secure.getInt(
                mContext.applicationContext.contentResolver, Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            mode != Settings.Secure.LOCATION_MODE_OFF
        }
    }

    fun getDeviceID(mContext: Context): String {

        // updated method to generate device id
        return Settings.Secure.getString(
            mContext.contentResolver, Settings.Secure.ANDROID_ID
        )
    }

    fun isNetworkConnected(mContext: Context): Boolean {
        val cm = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    fun getCompleteAddressString(LATITUDE: Double, LONGITUDE: Double, mContext: Context?): String {
        var strAdd = ""
        val geocoder = Geocoder(mContext, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1)
            if (addresses != null) {
                val returnedAddress = addresses[0]
                val strReturnedAddress = StringBuilder()
                for (i in 0..returnedAddress.maxAddressLineIndex) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n")
                }
                strAdd = strReturnedAddress.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strAdd
    }

}

