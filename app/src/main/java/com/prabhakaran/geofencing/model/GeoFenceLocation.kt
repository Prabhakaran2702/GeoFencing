package com.prabhakaran.geofencing.model


data class GeoFenceLocation(
    var id: Int,
    var lat: Double,
    var lng: Double,
    var isTracked: Boolean,
    var isEntered: Boolean,
    var isExisted: Boolean
)