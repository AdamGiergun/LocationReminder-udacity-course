package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.*
import kotlinx.coroutines.launch

class EditReminderViewModel(val app: Application, private val dataSource: ReminderDataSource) :
    BaseViewModel(app) {

    val resolvableApiException = MutableLiveData<ResolvableApiException?>()

    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val selectedPOI = MutableLiveData<PointOfInterest?>()
    val reminderLatitude = MutableLiveData<Double?>()
    val reminderLongitude = MutableLiveData<Double?>()
    val reminderRadiusInMeters= MutableLiveData(100)

    private val reminderGeofenceId = MutableLiveData<String?>()
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

    private val geofencingClient = getGeofencingClient(app.applicationContext)
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(app.applicationContext, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }

        PendingIntent.getBroadcast(
            app.applicationContext,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    private var initialReminderLatitude: Double? = null
    private var initialReminderLongitude: Double? = null
    private var initialRadiusInMeters: Int? = null
    private val isLocationChanged
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

    fun deleteReminder(view: View) {
        viewModelScope.launch {
            reminderGeofenceId.value?.let { geofenceId ->
                getGeofencingClient(view.context)
                    .removeGeofences(listOf(geofenceId))
            }
            dataSource.deleteReminder(reminderDataItem.toDTO())
            navigationCommand.value = NavigationCommand.Back
        }
    }

    @SuppressLint("MissingPermission")
    fun saveReminder(@Suppress("UNUSED_PARAMETER") view: View) {
        if (validateEnteredData()) {
            if (isLocationChanged) {
                reminderGeofenceId.value?.let { requestId ->
                    val logTag = this::class.java.name
                    reminderGeofenceId.value = null
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

            if (reminderGeofenceId.value == null) {
                val geofencingRequest = getGeofencingRequest(
                    // already checked by _viewModel.validateEnteredData())
                    reminderLatitude.value!!,
                    reminderLongitude.value!!,
                    reminderRadiusInMeters.value!!
                )
                geofencingClient.addGeofences(
                    geofencingRequest,
                    geofencePendingIntent
                ).apply {
                    addOnSuccessListener {
                        showSnackBar.value = "Geofence added"
                        reminderGeofenceId.value =
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