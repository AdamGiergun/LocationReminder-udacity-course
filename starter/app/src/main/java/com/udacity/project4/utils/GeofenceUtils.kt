package com.udacity.project4.utils

import android.content.Context
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import java.util.*

const val ACTION_GEOFENCE_EVENT = "com.udacity.project4.action.ACTION_GEOFENCE_EVENT"

/**
 * Returns the error string for a geofencing error code.
 */
fun errorMessage(context: Context, errorCode: Int): String {
    val resources = context.resources
    return when (errorCode) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
            R.string.geofence_not_available
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
            R.string.geofence_too_many_geofences
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
            R.string.geofence_too_many_pending_intents
        )
        else -> resources.getString(R.string.geofence_unknown_error)
    }
}

fun getGeofencingRequest(latitude: Double, longitude: Double, radiusInMeters: Int): GeofencingRequest {
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