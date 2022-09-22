package com.udacity.project4.utils

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

fun ReminderDataItem.toDTO(): ReminderDTO =
    this.run {
        if (id == null)
            ReminderDTO(
                title,
                description,
                location,
                latitude,
                longitude,
                geofenceId
            )
        else
            ReminderDTO(
                title,
                description,
                location,
                latitude,
                longitude,
                geofenceId,
                id
            )
    }

fun ReminderDTO.toDataItem(): ReminderDataItem =
    this.run {
        ReminderDataItem(
            title,
            description,
            location,
            latitude,
            longitude,
            geofenceId,
            id
        )
    }