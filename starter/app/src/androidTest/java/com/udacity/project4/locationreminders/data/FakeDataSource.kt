package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source

    private val listOfReminders = mutableListOf<ReminderDTO>()

    override suspend fun getReminders(): Result<List<ReminderDTO>> =
        if (listOfReminders.isNotEmpty())
            Result.Success(listOfReminders)
        else
            Result.Error("No reminders")

    override suspend fun saveReminder(reminder: ReminderDTO) {
        listOfReminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        TODO("return the reminder with the id")
    }

    override suspend fun setReminderState(id: String, isActive: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllReminders() {
        TODO("delete all the reminders")
    }


}