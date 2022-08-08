package com.udacity.project4.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

enum class LocationState {
    ENABLED,
    CHECK_SETTINGS,
    GPS_NOT_PRESENT,
    FINE_LOCATION_NOT_PERMITTED,
    BACKGROUND_LOCATION_NOT_PERMITTED,
    LOCATION_NOT_USABLE
}

val locationRequest
    get() = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 2000
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

fun isBackgroundLocationPermissionGranted(context: Context) =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        true
    } else {
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

fun isFineLocationPermissionGranted(context: Context) =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED