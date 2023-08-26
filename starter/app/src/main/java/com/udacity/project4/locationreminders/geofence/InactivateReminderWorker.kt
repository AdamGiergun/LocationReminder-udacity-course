package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.utils.getFakeNotification
import com.udacity.project4.utils.getUniqueId
import org.koin.java.KoinJavaComponent.inject

private const val TAG = "InactivateReminderW"

class InactivateReminderWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    companion object {
        fun buildWorkRequest(geofenceId: String): OneTimeWorkRequest {
            val data =
                Data.Builder().putString(GEOFENCE_ID, geofenceId).build()
            return OneTimeWorkRequestBuilder<InactivateReminderWorker>().run {
                setInputData(data)
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                build()
            }
        }
    }

    override suspend fun doWork(): Result {
        removeGeofenceFromDB()
        return Result.success()
    }

    private suspend fun removeGeofenceFromDB() {
        inputData.getString(GEOFENCE_ID)?.let { geofenceId ->
            val remindersLocalRepository: ReminderDataSource by inject(
                ReminderDataSource::class.java
            )
            remindersLocalRepository.removeGeofenceId(geofenceId)
            Log.d(TAG, "Reminder set inactive")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(getUniqueId(), getFakeNotification(applicationContext))
    }
}