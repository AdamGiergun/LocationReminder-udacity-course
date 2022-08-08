package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.errorMessage
import com.udacity.project4.utils.getGeofencingClient
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

private const val TAG = "GeofenceTransitionsWorker"
private const val REQUEST_ID = "requestId"
const val UNIQUE_WORK_NAME = "LocationRemindersGeofenceTransition"

class GeofenceTransitionsWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    companion object {
        fun buildWorkRequest(context: Context, intent: Intent): OneTimeWorkRequest? {
            if (intent.action == ACTION_GEOFENCE_EVENT) {
                val geofencingEvent = GeofencingEvent.fromIntent(intent)

                if (geofencingEvent == null) {
                    val errorMessage = errorMessage(context, 0)
                    Log.e(TAG, errorMessage)
                } else {
                    if (geofencingEvent.hasError()) {
                        val errorMessage = errorMessage(context, geofencingEvent.errorCode)
                        Log.e(TAG, errorMessage)
                    }

                    // Test that the reported transition was of interest.
                    if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        geofencingEvent.triggeringGeofences?.let {
                            val data =
                                Data.Builder().putString(REQUEST_ID, it.first().requestId).build()
                            return OneTimeWorkRequestBuilder<GeofenceTransitionsWorker>().run {
                                setInputData(data)
                                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                build()
                            }
                        }
                    } else {
                        Log.e(
                            TAG,
                            context.getString(
                                R.string.geofence_transition_invalid_type,
                                geofencingEvent.geofenceTransition
                            )
                        )
                    }
                }
            }
            return null
        }
    }

    override suspend fun doWork(): Result {
        inputData.getString(REQUEST_ID)?.let { requestId ->
            sendNotification(requestId)
            val geofencingClient = getGeofencingClient(applicationContext)
            geofencingClient.removeGeofences(listOf(requestId))
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence removed")
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Geofence not removed ${exception.localizedMessage}")
                }
        }
        return Result.success()
    }

    private suspend fun sendNotification(requestId: String) {

        //Get the local repository instance
        val remindersLocalRepository: ReminderDataSource by inject(ReminderDataSource::class.java)

        //get the reminder with the request id
        withContext(Dispatchers.IO) {
            val result = remindersLocalRepository.getReminder(requestId)
            if (result is com.udacity.project4.locationreminders.data.dto.Result.Success<ReminderDTO>) {
                val reminderDTO = result.data
                //send a notification to the user with the reminder details
                sendNotification(
                    applicationContext, ReminderDataItem(
                        reminderDTO.title,
                        reminderDTO.description,
                        reminderDTO.location,
                        reminderDTO.latitude,
                        reminderDTO.longitude,
                        reminderDTO.id
                    )
                )
            }
        }
    }
}
