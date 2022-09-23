package com.udacity.project4.locationreminders.reminderslist

import java.io.Serializable

/**
 * data class acts as a data mapper between the DB and the UI
 */
data class ReminderDataItem(
    var title: String?,
    var description: String?,
    var location: String?,
    var latitude: Double?,
    var longitude: Double?,
    var radiusInMeters: Int?,
    var geofenceId: String? = null,
    val id: Int = 0
) : Serializable