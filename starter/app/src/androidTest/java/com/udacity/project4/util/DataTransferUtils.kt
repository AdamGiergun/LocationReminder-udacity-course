package com.udacity.project4.util

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

fun ReminderDataItem.toDTO(): ReminderDTO =
    this.run {
        ReminderDTO(
            title,
            description,
            location,
            latitude,
            longitude,
            radiusInMeters,
            geofenceId,
            id
        )
    }