package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

//Unit test the DAO
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminderAndGetById() = runTest {
        // GIVEN - Insert a reminder
        val reminder = ReminderDataItem(
            "title1",
            "desc1",
            "location1",
            1.0,
            2.0,
            "test_geofence_id1",
            "test_id1"

        )
        database.reminderDao().saveReminder(
            ReminderDTO(
                reminder.title,
                reminder.description,
                reminder.location,
                reminder.latitude,
                reminder.longitude,
                reminder.geofenceId,
                reminder.id ?: ""
            )
        )

        // WHEN - Get the reminder by id from the database
        val loaded = database.reminderDao().getReminderById("test_id1")

        // THEN - The loaded data contains the expected values
        assertThat(loaded).isNotNull()
        assertThat(loaded?.id).isEqualTo(reminder.id)
        assertThat(loaded?.title).isEqualTo(reminder.title)
        assertThat(loaded?.description).isEqualTo(reminder.description)
        assertThat(loaded?.location).isEqualTo(reminder.location)
        assertThat(loaded?.latitude).isEqualTo(reminder.latitude)
        assertThat(loaded?.longitude).isEqualTo(reminder.longitude)
        assertThat(loaded?.geofenceId).isEqualTo(reminder.geofenceId)
    }

    @Test
    fun updateReminderStateAndGetById() = runTest {
        // GIVEN - Insert a reminder
        val reminder = ReminderDataItem(
            "title1",
            "desc1",
            "location1",
            1.0,
            2.0,
            "test_geofence_id1",
            "test_id1"
        )
        database.reminderDao().saveReminder(
            ReminderDTO(
                reminder.title,
                reminder.description,
                reminder.location,
                reminder.latitude,
                reminder.longitude,
                reminder.geofenceId,
                reminder.id ?: ""
            )
        )

        // WHEN - The reminder is deactivated
        reminder.geofenceId?.let {
            database.reminderDao().removeGeofenceId(it)
        }

        // THEN - The loaded data contains the expected values
        val loaded = database.reminderDao().getReminderById("test_id1")
        assertThat(loaded).isNotNull()
        assertThat(loaded?.id).isEqualTo(reminder.id)
        assertThat(loaded?.title).isEqualTo(reminder.title)
        assertThat(loaded?.description).isEqualTo(reminder.description)
        assertThat(loaded?.location).isEqualTo(reminder.location)
        assertThat(loaded?.latitude).isEqualTo(reminder.latitude)
        assertThat(loaded?.longitude).isEqualTo(reminder.longitude)
        assertThat(loaded?.geofenceId).isEqualTo(null)
    }

    @Test
    fun saveMultipleRemindersAndDeleteAll() = runTest {
        // GIVEN - Insert reminders
        val reminders = listOf(
            ReminderDataItem(
                "title2",
                "desc2",
                "location2",
                2.0,
                3.0,
                "test_geofence_id2",
                "test_id2"
            ),
            ReminderDataItem(
                "title1",
                "desc1",
                "location1",
                1.0,
                2.0,
                "test_geofence_id1",
                "test_id1"
            ),
            ReminderDataItem(
                "title3",
                "desc3",
                "location3",
                3.0,
                4.0,
                "test_geofence_id3",
                "test_id3"
            )
        )

        reminders.forEach { reminder ->
            database.reminderDao().saveReminder(
                ReminderDTO(
                    reminder.title,
                    reminder.description,
                    reminder.location,
                    reminder.latitude,
                    reminder.longitude,
                    reminder.geofenceId,
                    reminder.id ?: ""
                )
            )
        }

        // WHEN - Get the reminders from the database
        var loaded = database.reminderDao().getReminders()
        // THEN - check count
        assertThat(loaded.size).isEqualTo(reminders.size)

        // WHEN - delete all reminders
        database.reminderDao().deleteAllReminders()
        loaded = database.reminderDao().getReminders()

        //THEN - reminders count in db is zero
        assertThat(loaded.size).isEqualTo(0)
    }
}