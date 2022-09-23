package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.utils.errorMessage
import com.udacity.project4.utils.getGeofencingClient
import com.udacity.project4.utils.sendNotification
import com.udacity.project4.utils.toDataItem
import org.koin.java.KoinJavaComponent.inject

private const val TAG = "GeofenceTransitionsWrkr"
const val GEOFENCE_ID = "requestId"

class GeofenceTransitionsWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    companion object {
        fun buildWorkRequest(
            context: Context,
            geofencingEvent: GeofencingEvent
        ): OneTimeWorkRequest? {
            if (geofencingEvent.hasError()) {
                val errorMessage = errorMessage(context, geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
            }

            // Test that the reported transition was of interest.
            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                geofencingEvent.triggeringGeofences?.let {
                    val geofenceId = it.first().requestId
                    val data =
                        Data.Builder().putString(GEOFENCE_ID, geofenceId).build()
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
            return null
        }
    }

    override suspend fun doWork(): Result {
        inputData.getString(GEOFENCE_ID)?.let { geofenceId ->
            val geofencingClient = getGeofencingClient(applicationContext)
            geofencingClient.removeGeofences(listOf(geofenceId))
                .addOnSuccessListener {
                    InactivateReminderWorker.buildWorkRequest(geofenceId).let { workRequest ->
                        WorkManager
                            .getInstance(applicationContext)
                            .enqueueUniqueWork(
                                "LocationReminderRemove_$geofenceId",
                                ExistingWorkPolicy.KEEP,
                                workRequest
                            )
                    }
                    Log.d(TAG, "Geofence removed")
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Geofence not removed ${exception.localizedMessage}")
                }
            sendNotification(geofenceId)
        }
        return Result.success()
    }

    private suspend fun sendNotification(requestId: String) {
        val remindersLocalRepository: ReminderDataSource by inject(ReminderDataSource::class.java)
        //get the reminder with the request id
        val result = remindersLocalRepository.getReminder(requestId)
        if (result is com.udacity.project4.locationreminders.data.dto.Result.Success<ReminderDTO>) {
            val reminderDTO = result.data
            //send a notification to the user with the reminder details
            sendNotification(
                applicationContext, reminderDTO.toDataItem()
            )
        }
    }
}