package com.udacity.project4.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import java.util.*

const val ACTION_GEOFENCE_EVENT = "com.udacity.project4.action.ACTION_GEOFENCE_EVENT"

/**
 * Returns the error string for a geofencing error code.
 */
fun Context.getGeofenceErrorMessage(errorCode: Int) = getString(
    when (errorCode) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> R.string.geofence_not_available
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> R.string.geofence_too_many_geofences
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> R.string.geofence_too_many_pending_intents
        else -> R.string.geofence_unknown_error
    }
)

fun getGeofencingRequest(
    latitude: Double,
    longitude: Double,
    radiusInMeters: Int
): GeofencingRequest {
    val geofence = Geofence.Builder()
        .setRequestId(UUID.randomUUID().toString())
        .setCircularRegion(
            latitude,
            longitude,
            radiusInMeters.toFloat()
        )
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        .build()

    return GeofencingRequest.Builder()
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(geofence)
        .build()
}

fun getGeofencingClient(context: Context) =
    LocationServices.getGeofencingClient(context)

fun getGeofencePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
        action = ACTION_GEOFENCE_EVENT
    }

    return PendingIntent.getBroadcast(
        context.applicationContext,
        0,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    )
}