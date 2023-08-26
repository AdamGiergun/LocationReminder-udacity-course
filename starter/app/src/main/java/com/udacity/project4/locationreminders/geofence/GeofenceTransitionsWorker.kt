package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.getFakeNotification
import com.udacity.project4.utils.getGeofenceErrorMessage
import com.udacity.project4.utils.getGeofencingClient
import com.udacity.project4.utils.getUniqueId
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
                val errorMessage = context.getGeofenceErrorMessage(geofencingEvent.errorCode)
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
        return getReminder(getGeofenceId()).let {
            if (it == null) {
                Result.failure()
            } else {
                sendNotification(applicationContext, it)
                Result.success()
            }
        }
    }

    private fun getGeofenceId(): String {
        return inputData.getString(GEOFENCE_ID)?.let { geofenceId ->
            getGeofencingClient(applicationContext)
                .removeGeofences(listOf(geofenceId)).apply {
                    addOnSuccessListener {
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

                    addOnFailureListener { exception ->
                        Log.d(TAG, "Geofence not removed ${exception.localizedMessage}")
                    }
                }
            geofenceId
        } ?: ""
    }

    private suspend fun getReminder(requestId: String): ReminderDataItem? {
        val remindersLocalRepository: ReminderDataSource by inject(ReminderDataSource::class.java)
        //get the reminder with the request id
        val result = remindersLocalRepository.getReminder(requestId)
        return if (result is com.udacity.project4.locationreminders.data.dto.Result.Success<ReminderDTO>) {
            result.data.toDataItem()
        } else {
            null
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            getUniqueId(),
            getFakeNotification(applicationContext)
        )
    }
}