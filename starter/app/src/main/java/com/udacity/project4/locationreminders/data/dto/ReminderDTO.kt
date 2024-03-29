package com.udacity.project4.locationreminders.data.dto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Immutable model class for a Reminder. In order to compile with Room
 *
 * @param title         title of the reminder
 * @param description   description of the reminder
 * @param location      location name of the reminder
 * @param latitude      latitude of the reminder location
 * @param longitude     longitude of the reminder location
 * @param geofenceId    id of the geofence request attached to the reminder
 * @param id            id of the reminder
 */

@Entity(tableName = "reminders")
data class ReminderDTO(
    @ColumnInfo(name = "title") var title: String?,
    @ColumnInfo(name = "description") var description: String?,
    @ColumnInfo(name = "location") var location: String?,
    @ColumnInfo(name = "latitude") var latitude: Double?,
    @ColumnInfo(name = "longitude") var longitude: Double?,
    @ColumnInfo(name = "radius_in_meters") var radiusInMeters: Int?,
    @ColumnInfo(name = "geofence_id") var geofenceId: String?,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "entry_id") val id: Int = 0
)
