package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.util.Log
import androidx.work.*
import com.udacity.project4.locationreminders.data.ReminderDataSource
import org.koin.java.KoinJavaComponent.inject

private const val TAG = "InactivateReminderW"

class InactivateReminderWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    companion object {
        fun buildWorkRequest(requestId: String): OneTimeWorkRequest {
            val data =
                Data.Builder().putString(REQUEST_ID, requestId).build()
            return OneTimeWorkRequestBuilder<InactivateReminderWorker>().run {
                setInputData(data)
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                build()
            }
        }
    }

    override suspend fun doWork(): Result {
        inputData.getString(REQUEST_ID)?.let { requestId ->
            val remindersLocalRepository: ReminderDataSource by inject(
                ReminderDataSource::class.java
            )
            remindersLocalRepository.setReminderState(requestId, false)
            Log.d(TAG, "Reminder set inactive")
        }
        return Result.success()
    }
}