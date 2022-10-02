package com.udacity.project4.locationreminders.savereminder

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.PointOfInterest

class EditedReminder {
    val title = MutableLiveData<String?>()
    val description = MutableLiveData<String?>()
    val selectedLocationStr = MutableLiveData<String?>()
    val selectedPOI = MutableLiveData<PointOfInterest?>()
    val latitude = MutableLiveData<Double?>()
    val longitude = MutableLiveData<Double?>()
    val radiusInMeters= MutableLiveData(100)

    val geofenceId = MutableLiveData<String?>()
    var id: Int = 0

    var initialLatitude: Double? = null
    var initialLongitude: Double? = null
    var initialRadiusInMeters: Int? = null
    val isLocationChanged
        get() = !(latitude.value == initialLatitude &&
                longitude.value == initialLongitude &&
                radiusInMeters.value == initialRadiusInMeters)

    var isInitialized = false

    fun clear() {
        title.value = null
        description.value = null
        selectedLocationStr.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null
        radiusInMeters.value = 100
        geofenceId.value = null
        id = 0
        initialLatitude = null
        initialLongitude = null
        initialRadiusInMeters = null
        isInitialized = false
    }
}