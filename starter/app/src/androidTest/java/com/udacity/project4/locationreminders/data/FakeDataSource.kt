package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    private val listOfReminders = mutableListOf<ReminderDTO>()

    override suspend fun getReminders(): Result<List<ReminderDTO>> =
        if (listOfReminders.isNotEmpty())
            Result.Success(listOfReminders)
        else
            Result.Error("No reminders")

    override suspend fun saveReminder(reminder: ReminderDTO) {
        listOfReminders.add(reminder)
    }

    override suspend fun getReminder(geofenceId: String): Result<ReminderDTO> {
        return try {
            val reminder = listOfReminders.first {
                it.geofenceId == geofenceId
            }
            Result.Success(reminder)
        } catch (e: NoSuchElementException) {
            Result.Error(e.localizedMessage)
        }
    }

    override suspend fun removeGeofenceId(geofenceId: String) {
        // not needed at this moment for fake
    }

    override suspend fun updateReminder(reminder: ReminderDTO) {
        // not needed at this moment for fake
    }

    override suspend fun deleteReminder(reminder: ReminderDTO) {
        // not needed at this moment for fake
    }

    override suspend fun deleteAllReminders() {
        // not needed at this moment for fake
    }
}