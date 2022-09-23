package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.utils.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.errorMessage
import java.util.*

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 */

private const val TAG = "GeofenceBroadcastRcvr"

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            GeofencingEvent.fromIntent(intent).let { geofencingEvent ->
                if (geofencingEvent == null) {
                    val errorMessage = errorMessage(context, 0)
                    Log.e(TAG, errorMessage)
                } else {
                    GeofenceTransitionsWorker.buildWorkRequest(context, geofencingEvent)
                        ?.let { workRequest ->
                            WorkManager
                                .getInstance(context)
                                .enqueueUniqueWork(
                                    "LocationReminderReceive_${UUID.randomUUID()}",
                                    ExistingWorkPolicy.KEEP,
                                    workRequest
                                )
                        }
                }
            }
        }
    }
}