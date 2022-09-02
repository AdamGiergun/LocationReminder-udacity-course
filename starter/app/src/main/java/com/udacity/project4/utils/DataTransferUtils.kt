package com.udacity.project4.utils

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
            active,
            id
        )
    }