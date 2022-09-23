package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.LocationState
import com.udacity.project4.utils.toDTO
import kotlinx.coroutines.launch

class EditReminderViewModel(val app: Application, private val dataSource: ReminderDataSource) :
    BaseViewModel(app) {
    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val selectedPOI = MutableLiveData<PointOfInterest?>()
    val reminderLatitude = MutableLiveData<Double?>()
    val reminderLongitude = MutableLiveData<Double?>()
    val reminderRadiusInMeters= MutableLiveData(100)
    val reminderGeofenceId = MutableLiveData<String?>()
    private var reminderId: Int = 0

    private val reminderDataItem
        get() = ReminderDataItem(
            reminderTitle.value,
            reminderDescription.value,
            reminderSelectedLocationStr.value,
            reminderLatitude.value,
            reminderLongitude.value,
            reminderRadiusInMeters.value,
            reminderGeofenceId.value,
            reminderId
        )

    private var initialReminderLatitude: Double? = null
    private var initialReminderLongitude: Double? = null
    private var initialRadiusInMeters: Int? = null
    val isLocationChanged
        get() = !(reminderLatitude.value == initialReminderLatitude &&
                reminderLongitude.value == initialReminderLongitude &&
                reminderRadiusInMeters.value == initialRadiusInMeters)

    private var isReminderInitialized = false

    fun setReminderIfNotInitialized(reminderDataItem: ReminderDataItem) {
        if (!isReminderInitialized)
            reminderDataItem.run {
                reminderTitle.value = title
                reminderDescription.value = description
                reminderSelectedLocationStr.value = location
                reminderLatitude.value = latitude
                reminderLongitude.value = longitude
                reminderRadiusInMeters.value = radiusInMeters
                reminderGeofenceId.value = geofenceId
                initialReminderLatitude = latitude
                initialReminderLongitude = longitude
                initialRadiusInMeters = radiusInMeters
                isReminderInitialized = true
                reminderId = id
            }
    }

    private val _locationState = MutableLiveData(LocationState.CHECK_SETTINGS)
    val locationState: LiveData<LocationState>
        get() = _locationState

    fun setLocationState(newLocationState: LocationState) {
        _locationState.value = newLocationState
    }

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
        reminderLatitude.value = null
        reminderLongitude.value = null
        reminderRadiusInMeters.value = 100
        reminderGeofenceId.value = null
        reminderId = 0
        initialReminderLatitude = null
        initialReminderLongitude = null
        initialRadiusInMeters = null
        isReminderInitialized = false
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder() {
        if (validateEnteredData()) {
            saveOrUpdateReminder()
        }
    }

    /**
     * Save the reminder to the data source
     */
    private fun saveOrUpdateReminder() {
        showLoading.value = true
        viewModelScope.launch {
            if (reminderId == 0) {
                dataSource.saveReminder(reminderDataItem.toDTO())
            } else {
                dataSource.updateReminder(reminderDataItem.toDTO())
            }
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(): Boolean {
        if (reminderTitle.value.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderSelectedLocationStr.value.isNullOrEmpty()
            || reminderLatitude.value == null
            || reminderLongitude.value == null
        ) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }
}