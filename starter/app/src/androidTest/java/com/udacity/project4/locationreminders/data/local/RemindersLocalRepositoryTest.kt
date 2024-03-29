package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.util.toDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var repository: ReminderDataSource

    @Before
    fun initRepo() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        repository = RemindersLocalRepository(
            database.reminderDao()
        )
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminderAndGetByGeofenceId() = runTest {
        // GIVEN - reminder inserted
        val reminder = ReminderDataItem(
            "title1",
            "desc1",
            "location1",
            1.0,
            2.0,
            100,
            "test_geofence_id1"
        )
        repository.saveReminder(reminder.toDTO())

        // WHEN - Get the reminder by id from the database
        val loaded = database.reminderDao().getReminderByGeofenceId("test_geofence_id1")

        // THEN - The loaded data contains the expected values
        assertThat(loaded).isNotNull()
        assertThat(loaded?.title).isEqualTo(reminder.title)
        assertThat(loaded?.description).isEqualTo(reminder.description)
        assertThat(loaded?.location).isEqualTo(reminder.location)
        assertThat(loaded?.latitude).isEqualTo(reminder.latitude)
        assertThat(loaded?.longitude).isEqualTo(reminder.longitude)
        assertThat(loaded?.geofenceId).isEqualTo(reminder.geofenceId)
    }

    @Test
    fun saveReminderAndRemoveGeofenceId() = runTest {
        // GIVEN - reminder inserted
        val geofenceId = "test_geofence_id1"
        val reminder = ReminderDataItem(
            "title1",
            "desc1",
            "location1",
            1.0,
            2.0,
            100,
            geofenceId
        )
        repository.saveReminder(reminder.toDTO())

        // WHEN - Remove geofenceId (the reminder is deactivated)
        repository.removeGeofenceId(geofenceId)

        // THEN - The loaded data is empty
        val loaded = database.reminderDao().getReminderByGeofenceId(geofenceId)
        assertThat(loaded).isNull()
    }

    @Test
    fun saveMultipleRemindersAndDeleteAll() = runTest {
        // GIVEN - multiple reminders inserted
        val reminders = listOf(
            ReminderDataItem(
                "title2",
                "desc2",
                "location2",
                2.0,
                3.0,
                100,
                "test_geofence_id2"
            ),
            ReminderDataItem(
                "title1",
                "desc1",
                "location1",
                1.0,
                2.0,
                100,
                "test_geofence_id1"
            ),
            ReminderDataItem(
                "title3",
                "desc3",
                "location3",
                3.0,
                4.0,
                100,
                "test_geofence_id3"
            )
        )

        reminders.forEach { reminder ->
            repository.saveReminder(reminder.toDTO())
        }

        // WHEN - Get the reminders from the database
        var result =
            repository.getReminders() as com.udacity.project4.locationreminders.data.dto.Result.Success
        // THEN - check count
        var loaded = result.data
        assertThat(loaded.size).isEqualTo(reminders.size)

        // WHEN - delete all reminders
        repository.deleteAllReminders()
        result =
            repository.getReminders() as com.udacity.project4.locationreminders.data.dto.Result.Success<List<ReminderDTO>>
        //THEN - reminders count in db is zero
        loaded = result.data
        assertThat(loaded.size).isEqualTo(0)
    }
}