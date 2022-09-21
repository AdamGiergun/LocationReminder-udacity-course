package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

/**
 * Main entry point for accessing reminders data.
 */
interface ReminderDataSource {
    suspend fun getReminders(): Result<List<ReminderDTO>>
    suspend fun saveReminder(reminder: ReminderDTO)
    suspend fun getReminder(geofenceId: String): Result<ReminderDTO>
    suspend fun resetGeofenceId(geofenceId: String)
    suspend fun updateReminder(reminder: ReminderDTO)
    suspend fun deleteReminder(reminder: ReminderDTO)
    suspend fun deleteAllReminders()
}