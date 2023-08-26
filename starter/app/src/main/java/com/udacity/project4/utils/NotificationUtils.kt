package com.udacity.project4.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem


private const val NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".channel"

internal fun sendNotification(context: Context, reminderDataItem: ReminderDataItem) {
    val notificationManager = context
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // We need to create a NotificationChannel associated with our CHANNEL_ID before sending a notification.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(context)
    }

    getNotification(context, reminderDataItem).let {
        notificationManager.notify(getUniqueId(), it)
    }
}

internal fun getNotification(
    context: Context,
    reminderDataItem: ReminderDataItem?
): Notification {
    val intent = ReminderDescriptionActivity.newIntent(context.applicationContext, reminderDataItem)

    //create a pending intent that opens ReminderDescriptionActivity when the user clicks on the notification
    val stackBuilder = TaskStackBuilder.create(context)
        .addParentStack(ReminderDescriptionActivity::class.java)
        .addNextIntent(intent)
    val notificationPendingIntent = stackBuilder
        .getPendingIntent(
            getUniqueId(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

//    build the notification object with the data to be shown
    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(reminderDataItem?.title ?: "")
        .setContentText(reminderDataItem?.location ?: "")
        .setContentIntent(notificationPendingIntent)
        .setAutoCancel(true)
        .build()
}

internal fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())

internal fun isPostNotificationsPermissionGranted(context: Context) =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

/**
needed for getForegroundInfo() on older APIs
 */
internal fun getFakeNotification(context: Context): Notification {

    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("")
        .setTicker("")
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(false)
        .setAutoCancel(true)
        .setSilent(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        createNotificationChannel(context)?.also {
            builder.setChannelId(it.id)
        }

    return builder.build()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createNotificationChannel(
    context: Context,
    notificationImportance: Int = NotificationManager.IMPORTANCE_MIN
): NotificationChannel? {
    val notificationManager = context
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
        val name = context.getString(R.string.app_name)
        return NotificationChannel(
            NOTIFICATION_CHANNEL_ID, name, notificationImportance
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    return null
}