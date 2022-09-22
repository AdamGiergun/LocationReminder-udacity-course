package com.udacity.project4.locationreminders.data.local

import androidx.room.*
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

/**
 * Data Access Object for the reminders table.
 */
@Dao
interface RemindersDao {
    /**
     * @return all reminders.
     */
    @Query("SELECT * FROM reminders")
    fun getReminders(): List<ReminderDTO>

    /**
     * @param reminderId the id of the reminder
     * @return the reminder object with the reminderId
     */
    @Query("SELECT * FROM reminders where entry_id = :reminderId")
    fun getReminderById(reminderId: String): ReminderDTO?

    /**
     * @param geofenceId the id of the reminder's geofence
     * @return the reminder object with the geofenceId
     */
    @Query("SELECT * FROM reminders where geofence_id = :geofenceId")
    fun getReminderByGeofenceId(geofenceId: String): ReminderDTO?

    /**
     * Insert a reminder in the database. If the reminder already exists, replace it.
     *
     * @param reminder the reminder to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveReminder(reminder: ReminderDTO)

    /**
     * Delete all reminders.
     */
    @Query("DELETE FROM reminders")
    fun deleteAllReminders()

    /**
     * Delete a reminder from the database.
     *
     * @param reminder the reminder to be deleted.
     */
    @Delete
    fun deleteReminder(reminder: ReminderDTO)

    /**
     * Remove geofenceId from db.
     *
     * @param geofenceId of the reminder to be removed.
     */
    @Query("UPDATE reminders SET geofence_id=null WHERE geofence_id=:geofenceId")
    fun removeGeofenceId(geofenceId: String)

    /**
     * Update a reminder in the database.
     *
     * @param reminder the reminder to be updated.
     */
    @Update
    fun updateReminder(reminder: ReminderDTO)
}