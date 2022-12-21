package com.prabhakaran.geofencing

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.prabhakaran.geofencing.databinding.ActivityMainBinding
import com.prabhakaran.geofencing.model.GeoFenceLocation


class MainActivity : AppCompatActivity() {


    var gpsLocations = arrayOf(
        GeoFenceLocation(id = 1, lat = 25.0763517, lng = 55.1419583, isTracked = false, isEntered = false, isExisted = false),
        GeoFenceLocation(id = 2, lat = 25.14119, lng = 55.1852467, isTracked = false, isEntered = false, isExisted = false),
        GeoFenceLocation(id = 3, lat = 25.1971967, lng = 55.274375, isTracked = false, isEntered = false, isExisted = false)
    )

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        if (ContextCompat.checkSelfPermission(this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION) !==
            PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }

        binding.button1.setOnClickListener {

            saveLocationsLocal()
            startTracking()

        }

        binding.button2.setOnClickListener {
          stopTracking()
        }

    }

    private fun stopTracking() {
        var mServiceIntent = Intent(this, TrackingService::class.java)
        if (isMyServiceRunning(TrackingService::class.java)) {
            val sp = getSharedPreferences("checkbox", MODE_PRIVATE)
            val et = sp.edit()
            et.putBoolean("TRACKING", false)
            et.commit()
            stopService(mServiceIntent)
        }
    }

    private fun startTracking() {
        val employeeTrackingService = TrackingService(this)
        var mServiceIntent = Intent(this, employeeTrackingService.javaClass)

        if (!isMyServiceRunning(employeeTrackingService.javaClass)) {
            val sp = getSharedPreferences("checkbox", MODE_PRIVATE)
            val et = sp.edit()
            et.putBoolean("TRACKING", true)
            et.commit()
            startService(mServiceIntent)
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (manager != null) {
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    Log.i("isMyServiceRunning?", true.toString() + "")
                    return true
                }
            }
        }
        Log.i("isMyServiceRunning?", false.toString() + "")
        return false
    }

    private fun saveLocationsLocal(){
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json: String = gson.toJson(gpsLocations)
        editor.putString("locations", json)
        editor.apply()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    if ((ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) ===
                                PackageManager.PERMISSION_GRANTED)
                    ) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "You app may not work if the Permission Denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }



}