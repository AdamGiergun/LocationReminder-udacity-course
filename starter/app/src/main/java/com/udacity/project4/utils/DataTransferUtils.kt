package com.udacity.project4.utils

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

fun ReminderDTO.toDataItem(): ReminderDataItem =
    this.run {
        ReminderDataItem(
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