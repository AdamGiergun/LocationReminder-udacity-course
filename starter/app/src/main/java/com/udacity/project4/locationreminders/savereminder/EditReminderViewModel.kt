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
    private var isReminderInitialized = false
    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val selectedPOI = MutableLiveData<PointOfInterest?>()
    val reminderLatitude = MutableLiveData<Double?>()
    val reminderLongitude = MutableLiveData<Double?>()
    val reminderGeofenceId = MutableLiveData<String?>()
    var reminderId: String? = null

    private var initialReminderLatitude: Double? = null
    private var initialReminderLongitude: Double? = null

    fun setReminder(reminderDataItem: ReminderDataItem) {
        if (!isReminderInitialized)
            reminderDataItem.run {
                reminderTitle.postValue(title)
                reminderDescription.postValue(description)
                reminderSelectedLocationStr.postValue(location)
                reminderLatitude.postValue(latitude)
                reminderLongitude.postValue(longitude)
                reminderGeofenceId.postValue(geofenceId)
                initialReminderLatitude = latitude
                initialReminderLongitude = longitude
                isReminderInitialized = true
                reminderId = id
            }
    }

    val isLocationChanged
        get() = !(reminderLatitude.value == initialReminderLatitude &&
                reminderLongitude.value == initialReminderLongitude)

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
        reminderGeofenceId.value = null
        initialReminderLatitude = null
        initialReminderLongitude = null
        isReminderInitialized = false
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder() {
        val reminderData = ReminderDataItem(
            reminderTitle.value,
            reminderDescription.value,
            reminderSelectedLocationStr.value,
            reminderLatitude.value,
            reminderLongitude.value,
            reminderGeofenceId.value,
            reminderId
        )

        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    private fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            if (reminderData.id == null) {
                dataSource.saveReminder(reminderData.toDTO())
            } else {
                dataSource.updateReminder(reminderData.toDTO())
            }
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()
            || reminderData.latitude == null
            || reminderData.longitude == null
        ) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }
}