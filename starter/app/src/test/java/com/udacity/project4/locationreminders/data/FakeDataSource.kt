package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> =
            Result.Success(
                listOf(
                    ReminderDTO(
                        "test1",
                        "test1",
                        "test1",
                        0.0,
                        0.0,
                        true,
                        "test1"
                    ),
                    ReminderDTO(
                        "test2",
                        "test2",
                        "test2",
                        180.0,
                        180.0,
                        false,
                        "test2"
                    )
                )
            )

    override suspend fun saveReminder(reminder: ReminderDTO) {
        TODO("save the reminder")
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