package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.GeofenceStatusCodes
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.LocationState
import com.udacity.project4.utils.getGeofencePendingIntent
import com.udacity.project4.utils.getGeofencingClient
import com.udacity.project4.utils.getGeofencingRequest
import kotlinx.coroutines.launch

class EditReminderViewModel(private val dataSource: ReminderDataSource) :
    BaseViewModel() {

    val resolvableApiException = MutableLiveData<ResolvableApiException?>()

    private val editedReminder = EditedReminder()

    val reminderTitle = editedReminder.title
    val reminderDescription = editedReminder.description
    val reminderSelectedLocationStr = editedReminder.selectedLocationStr
    val selectedPOI = editedReminder.selectedPOI
    val reminderLatitude = editedReminder.latitude
    val reminderLongitude = editedReminder.longitude
    val reminderRadiusInMeters = editedReminder.radiusInMeters

    private fun EditedReminder.toDTO() = ReminderDTO(
        title.value,
        description.value,
        selectedLocationStr.value,
        latitude.value,
        longitude.value,
        radiusInMeters.value,
        geofenceId.value,
        id
    )

    fun setReminderIfNotInitialized(reminderDataItem: ReminderDataItem) {
        editedReminder.apply {
            if (!isInitialized) {
                title.value = reminderDataItem.title
                description.value = reminderDataItem.description
                selectedLocationStr.value = reminderDataItem.location
                latitude.value = reminderDataItem.latitude
                longitude.value = reminderDataItem.longitude
                radiusInMeters.value = reminderDataItem.radiusInMeters
                geofenceId.value = reminderDataItem.geofenceId
                initialLatitude = reminderDataItem.latitude
                initialLongitude = reminderDataItem.longitude
                initialRadiusInMeters = reminderDataItem.radiusInMeters
                isInitialized = true
                id = reminderDataItem.id
            }
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
        editedReminder.clear()
    }

    fun onSelectLocationClicked() {
//        Navigate to another fragment to get the user location
        navigationCommand.value = NavigationCommand.To(
            EditReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
        )
    }

    fun deleteReminder(context: Context) {
        viewModelScope.launch {
            editedReminder.geofenceId.value?.let { geofenceId ->
                getGeofencingClient(context.applicationContext)
                    .removeGeofences(listOf(geofenceId))
            }
            dataSource.deleteReminder(editedReminder.toDTO())
            navigationCommand.value = NavigationCommand.Back
        }
    }

    @SuppressLint("MissingPermission")
    fun saveReminder(context: Context) {
        val geofencingClient = getGeofencingClient(context)

        if (validateEnteredData()) {
            if (editedReminder.isLocationChanged) {
                editedReminder.geofenceId.value?.let { requestId ->
                    val logTag = this::class.java.name
                    editedReminder.geofenceId.value = null
                    geofencingClient.removeGeofences(listOf(requestId))
                        .addOnSuccessListener {
                            Log.d(logTag, "Old geofence removed")
                        }
                        .addOnFailureListener { exception ->
                            handleGeofenceClientException(exception)
                            Log.d(logTag, "Old geofence not removed ${exception.localizedMessage}")
                        }
                }
            }

            if (editedReminder.geofenceId.value == null) {
                val geofencingRequest = getGeofencingRequest(
                    // already checked by _viewModel.validateEnteredData())
                    editedReminder.latitude.value!!,
                    editedReminder.longitude.value!!,
                    editedReminder.radiusInMeters.value!!
                )
                geofencingClient.addGeofences(
                    geofencingRequest,
                    getGeofencePendingIntent(context)
                ).apply {
                    addOnSuccessListener {
                        showSnackBar.value = "Geofence added"
                        editedReminder.geofenceId.value =
                            geofencingRequest.geofences.first().requestId
                        validateAndSaveReminder()
                    }
                    addOnFailureListener { exception ->
                        handleGeofenceClientException(exception)
                    }
                }
            } else {
                validateAndSaveReminder()
            }
        }
    }

    private fun handleGeofenceClientException(exception: java.lang.Exception) {
        if (exception is ApiException) {
            when (exception.statusCode) {
                GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION -> {
                    if (exception is ResolvableApiException)
                        resolvableApiException.value = exception
                }

                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
                    showSnackBarInt.value =
                        R.string.geofence_not_available

                GeofenceStatusCodes.GEOFENCE_REQUEST_TOO_FREQUENT ->
                    showSnackBarInt.value =
                        R.string.geofence_request_too_frequent

                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
                    showSnackBarInt.value =
                        R.string.geofence_too_many_geofences

                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS ->
                    showSnackBarInt.value =
                        R.string.geofence_too_many_pending_intents
            }
        } else
            showSnackBarInt.value =
                R.string.error_adding_geofence
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
            editedReminder.toDTO().let { reminderDTO ->
                if (reminderDTO.id == 0) {
                    dataSource.saveReminder(reminderDTO)
                } else {
                    dataSource.updateReminder(reminderDTO)
                }
            }
            showLoading.value = false
            showToastInt.value = R.string.reminder_saved
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    private fun validateEnteredData(): Boolean {
        editedReminder.run {
            if (title.value.isNullOrEmpty()) {
                showSnackBarInt.value = R.string.err_enter_title
                return false
            }

            if (selectedLocationStr.value.isNullOrEmpty()
                || latitude.value == null
                || longitude.value == null
            ) {
                showSnackBarInt.value = R.string.err_select_location
                return false
            }
            return true
        }
    }
}