package com.prabhakaran.geofencing

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prabhakaran.geofencing.utils.GpsUtils.isLocationEnabled
import com.prabhakaran.geofencing.utils.GpsUtils.isNetworkConnected
import com.prabhakaran.geofencing.model.GeoFenceLocation
import java.lang.Math.*
import java.lang.reflect.Type
import java.util.*


class TrackingService : Service {
    var counter = 0
    var counterGeoFencing = 0
    var disableGeoFencing = false

    var actualLatitude = 0.0
    var actualLongitude = 0.0

   private var gpsLocations: ArrayList<GeoFenceLocation> =  ArrayList()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback
    private var mContext: Context? = null

    var LOCATION_TRACKTIMER = 20
    var GEOFENCING_TRACKTIMER = 5

    constructor(applicationContext: Context?) : super() {}
    constructor() {}

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        mContext = applicationContext
        if (intent.extras != null && intent.extras != null && intent.extras!!.getInt(TRACK_COUNT) != 0) {
            counter =
                intent.extras!!.getInt(TRACK_COUNT)
        }
        Log.i("START", "onStartCommand!")
        startTimer()
        // in onCreate() initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocationLocal()
        getLocationUpdates()
        startLocationUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i("EXIT", "ondestroy!")
        stoptimertask()
        stopLocationUpdates()

        val sp = getSharedPreferences("checkbox", 0)
        val enableTracking = sp.getBoolean("TRACKING", false)

        if(enableTracking){
            val broadcastIntent = Intent(this, TrackingReceiver::class.java)
            broadcastIntent.putExtra(
                TRACK_COUNT,
                counter
            )
            sendBroadcast(broadcastIntent)
        }

    }

    private fun getLocationLocal(){
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("locations", null)
        val type: Type = object : TypeToken<ArrayList<GeoFenceLocation?>?>() {}.type
        gpsLocations = gson.fromJson<Any>(json, type) as ArrayList<GeoFenceLocation>
        if (gpsLocations == null) {
            gpsLocations = ArrayList()
        }
    }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private fun startTimer() {
        timer = Timer()
        initializeTimerTask()
        timer!!.schedule(timerTask, 1000, 1000)
    }


    private fun initializeTimerTask() {
        timerTask = object : TimerTask() {
            override fun run() {
                // to reset the counter when it reaches the count
                if (counter >= LOCATION_TRACKTIMER) {
                    counter = 0
                }

                if (counterGeoFencing >= GEOFENCING_TRACKTIMER) {
                    counterGeoFencing = 0
                }
                if (counter == 0) {
                   fetchGps()
                  }

                if (counterGeoFencing == 0) {
                if(!disableGeoFencing) checkGeoFencing()
                }

                counter++
                counterGeoFencing++
            }
        }
    }


    fun stoptimertask() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchGps()  {


        if (isNetworkConnected(mContext!!)) {
            if (isLocationEnabled(mContext!!)) {
                // fetchLocation();
            } else {
                showTurnOnNotification("Location")
            }
        } else {
            showTurnOnNotification("Mobile data")
        }

        sendLocationToServer(actualLatitude,actualLongitude)


    }

    private fun sendLocationToServer(lat: Double, lng: Double) {
        Log.d("Send location to server", "${lat} - $${ lng}")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startMyOwnForeground()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground() {
        val NOTIFICATION_CHANNEL_ID = "com.daemon.emco_android"
        val channelName = "My Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Tracking started")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.i(javaClass.name, "Service Stop")
    }

    private fun showTurnOnNotification(msg: String) {

        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = mContext!!.getString(R.string.app_name)
            val description = mContext!!.getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(mContext!!.getString(R.string.app_name), name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = mContext!!.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(
            mContext,
            1,
            Intent(Settings.ACTION_SETTINGS),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder =
            NotificationCompat.Builder(mContext!!, mContext!!.getString(R.string.app_name))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(getString(R.string.app_name) + " Warning")
                .setContentText("$msg is not enabled. Go to settings menu and turn on the $msg")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$msg is not enabled. Go to settings menu and turn on the $msg")
                )
        val notificationManager = NotificationManagerCompat.from(mContext!!)
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1001, builder.build())
    }

    private fun showNotificationForTest(msg: String) {

        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = mContext!!.getString(R.string.app_name)
            val description = mContext!!.getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(mContext!!.getString(R.string.app_name), name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = mContext!!.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(
            mContext,
            1,
            Intent(Settings.ACTION_SETTINGS),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder =
            NotificationCompat.Builder(mContext!!, mContext!!.getString(R.string.app_name))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(getString(R.string.app_name) + "location testing")
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
        val notificationManager = NotificationManagerCompat.from(mContext!!)
        // notificationId is a unique int for each notification that you must define
        val id= Random(System.currentTimeMillis()).nextInt(1000)
        notificationManager.notify(id, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun checkGeoFencing(){

        if (isNetworkConnected(mContext!!)) {
            if (isLocationEnabled(mContext!!)) {
                // fetchLocation();
            } else {
                showTurnOnNotification("Location")
            }
        } else {
            showTurnOnNotification("Mobile data")
        }

        calculateGeoFencing(actualLatitude,actualLongitude)

     //   Log.d("Geo Fencing", "Check - ${actualLatitude} && ${actualLongitude}")

    }

    private fun calculateGeoFencing(lat: Double, lon: Double) {

        gpsLocations.forEach {

            Log.d("gpsLocations ->", "$it")

            if(!it.isTracked){

                disableGeoFencing = false;

                if(!it.isEntered){

                    if(distance(lat,lon,it.lat,it.lng) < 250) {
                        showNotificationForTest("Location: ${it.id} entered")
                        it.isEntered = true
                    }

                }

                else{

                    if(!it.isExisted){
                        if(distance(lat,lon,it.lat,it.lng) > 250)
                        {
                            showNotificationForTest("Location: ${it.id} exited")
                            it.isExisted = true
                            it.isTracked = true
                        }
                    }
                }
            }

            else{
                disableGeoFencing = checkToDisableGeoFencing()
            }
        }

    }

    private fun checkToDisableGeoFencing() : Boolean {
        gpsLocations.map {
            if(!it.isTracked)  return false
        }
        Log.d("Geo Fencing","All the locations are tracked & Geo fencing disabled.")
        return true
    }


    private fun distance(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Float {
        val fromLocation = Location("")
        fromLocation.latitude = fromLat
        fromLocation.longitude = fromLon
        val toLocation = Location("")
        toLocation.latitude = toLat
        toLocation.longitude = toLon
        val distanceInMeters = fromLocation.distanceTo(toLocation)
       Log.d("Distance: ",(distanceInMeters).toString())
        return distanceInMeters
    }

    private fun getLocationUpdates()
    {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 3000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 50f // 170 m = 0.1 mile
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //set according to your app function
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0 ?: return

                if (p0.locations.isNotEmpty()) {
                    // get latest location
                    val location =
                        p0.lastLocation
                    // use your location object
                    // get latitude , longitude and other info from this
                    actualLatitude = location!!.latitude
                    actualLongitude = location!!.longitude

                }


            }
        }
    }

    //start location updates
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper() /* Looper */
        )
    }

    // stop location updates
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


    companion object {
        var TRACK_COUNT = "TRACK_COUNT"
    }
}