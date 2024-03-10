package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.ktx.awaitMap
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.EditReminderViewModel
import com.udacity.project4.utils.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val TAG = "LocationReminder"

class SelectLocationFragment : BaseFragment() {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: EditReminderViewModel by inject()

    private var googleMap: GoogleMap? = null

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private val permissionsRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
//        permissions.forEach {
//            granted = granted and it.value
//        }

        if (granted)
            _viewModel.setLocationState(LocationState.ENABLED)
        else {
            val permission =
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION in permissions ->
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION

                    Manifest.permission.ACCESS_FINE_LOCATION in permissions ->
                        Manifest.permission.ACCESS_FINE_LOCATION

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            Manifest.permission.POST_NOTIFICATIONS in permissions ->
                        Manifest.permission.POST_NOTIFICATIONS

                    else ->
                        null
                }

            when {
                permission != null && shouldShowRequestPermissionRationale(permission) -> {
                    showRationale(permission)
                    _viewModel.setLocationState(LocationState.CHECK_SETTINGS)
                }

                permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                    appPermissionsSettingsRequest.launch(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", requireActivity().packageName, null)
                        }
                    )

                }

                permission == Manifest.permission.POST_NOTIFICATIONS -> {
                    _viewModel.setLocationState(LocationState.ENABLED)
                }
            }
//            if (permission != null && shouldShowRequestPermissionRationale(permission)) {
//                showRationale(permission)
//                _viewModel.setLocationState(LocationState.CHECK_SETTINGS)
//            } else {
//                _viewModel.setLocationState(LocationState.ENABLED)
//                if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//                    appPermissionsSettingsRequest.launch(
//                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                            data = Uri.fromParts("package", requireActivity().packageName, null)
//                        }
//                    )
//            }
        }
    }

    private fun showRationale(permission: String?) {
        _viewModel.showToast.value =
            if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                getString(R.string.background_location_rationale)
            else
                getString(R.string.fine_location_rationale)
    }

    private val appPermissionsSettingsRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        _viewModel.setLocationState(LocationState.CHECK_SETTINGS)
    }

    private val enableLocationRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        _viewModel.setLocationState(LocationState.CHECK_SETTINGS)
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSelectLocationBinding.inflate(inflater)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setDisplayHomeAsUpEnabled(true)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.map_options, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                R.id.normal_map -> {
                    googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                    true
                }

                R.id.hybrid_map -> {
                    googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
                    true
                }

                R.id.satellite_map -> {
                    googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    true
                }

                R.id.terrain_map -> {
                    googleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    true
                }

                else -> false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        _viewModel.setLocationState(LocationState.CHECK_SETTINGS)

        _viewModel.locationState.observe(viewLifecycleOwner) {
            when (it) {
                LocationState.CHECK_SETTINGS -> {
                    val builder = LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)

                    val client: SettingsClient =
                        LocationServices.getSettingsClient(requireContext())
                    client.checkLocationSettings(builder.build()).apply {
                        addOnSuccessListener { locationSettingsResponse ->
                            try {
                                locationSettingsResponse.locationSettingsStates?.let { states ->
                                    if (states.isGpsPresent) {
                                        val context = requireContext()
                                        val fineLoc =
                                            isFineLocationPermissionGranted(context)
                                        val bgrLoc =
                                            isBackgroundLocationPermissionGranted(context)
//                                        val gps = states.isGpsUsable
                                        val notifications =
                                            isPostNotificationsPermissionGranted(context)

                                        if (!fineLoc)
                                            _viewModel.setLocationState(LocationState.FINE_LOCATION_NOT_PERMITTED)

                                        if (!bgrLoc)
                                            _viewModel.setLocationState(LocationState.BACKGROUND_LOCATION_NOT_PERMITTED)

                                        if (!notifications)
                                            _viewModel.setLocationState(LocationState.NOTIFICATIONS_NOT_PERMITTED)

                                        if (fineLoc and bgrLoc and notifications)
                                            _viewModel.setLocationState(LocationState.ENABLED)

                                    } else {
                                        _viewModel.setLocationState(LocationState.GPS_NOT_PRESENT)
                                    }
                                }
                            } catch (e: IllegalStateException) {
                                Log.d(TAG, e.localizedMessage ?: "Unknown error")
                            }
                        }

                        addOnFailureListener { exception ->
                            if (exception is ResolvableApiException) {
                                try {
                                    val intentSenderRequest = IntentSenderRequest
                                        .Builder(exception.resolution.intentSender)
                                        .build()

                                    enableLocationRequestLauncher.launch(intentSenderRequest)

                                } catch (sendEx: IntentSender.SendIntentException) {
                                    _viewModel.setLocationState(LocationState.LOCATION_NOT_USABLE)
                                }
                            }
                        }
                    }
                }

                LocationState.FINE_LOCATION_NOT_PERMITTED -> {
                    permissionsRequest.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                LocationState.BACKGROUND_LOCATION_NOT_PERMITTED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissionsRequest.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        )
                    } else {
                        _viewModel.setLocationState(LocationState.CHECK_SETTINGS)
                    }
                }

                LocationState.NOTIFICATIONS_NOT_PERMITTED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsRequest.launch(
                            arrayOf(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        )
                    } else {
                        _viewModel.setLocationState(LocationState.CHECK_SETTINGS)
                    }
                }

                LocationState.ENABLED -> {
                    fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val currentLocation = task.result
                                googleMap?.apply {
                                    moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(
                                                currentLocation.latitude,
                                                currentLocation.longitude
                                            ),
                                            15f
                                        )
                                    )
                                }
                            }
                        }
                }

                LocationState.GPS_NOT_PRESENT -> {
                    _viewModel.navigationCommand.value = NavigationCommand.Back
                    _viewModel.showSnackBarInt.value = R.string.gps_is_needed_for_this_to_work
                }

                LocationState.LOCATION_NOT_USABLE -> {
                    _viewModel.navigationCommand.value = NavigationCommand.Back
                    _viewModel.showSnackBarInt.value =
                        R.string.location_service_is_needed_for_this_to_work
                }

                else -> {
                    _viewModel.navigationCommand.value = NavigationCommand.Back
                    _viewModel.showSnackBarInt.value = R.string.error_happened
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)
            {
                val mapFragment: SupportMapFragment? =
                    childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
                googleMap = mapFragment?.awaitMap()
                googleMap?.apply {
                    setMyMapStyle()

                    setOnMapLongClickListener { latLng ->
                        val marker = addMarker(
                            MarkerOptions()
                                .position(latLng)
                        )
                        SaveLocationDialog(_viewModel, null, latLng, marker)
                            .show(childFragmentManager, TAG)
                    }

                    setOnPoiClickListener { pointOfInterest ->
                        val poiMarker = addMarker(
                            MarkerOptions()
                                .position(pointOfInterest.latLng)
                                .title(pointOfInterest.name)
                        )

                        poiMarker?.showInfoWindow()

                        SaveLocationDialog(_viewModel, pointOfInterest, null, poiMarker)
                            .show(childFragmentManager, TAG)
                    }
                }
            }
        }

        _viewModel.showSnackBarInt.value = R.string.info_about_selecting_location

        return binding.root
    }

    private fun GoogleMap.setMyMapStyle() {
        try {
            val success = setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.d(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.d(TAG, "Can't find style. Error: ", e)
        }
    }

    internal class SaveLocationDialog(
        private val viewModel: EditReminderViewModel,
        private val selectedPOI: PointOfInterest? = null,
        private val latLng: LatLng? = null,
        private val marker: Marker? = null
    ) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog =
            AlertDialog.Builder(requireActivity()).run {
                setMessage(
                    if (selectedPOI == null)
                        getString(R.string.save_this_location, "")
                    else
                        getString(R.string.save_this_location, "\n(${selectedPOI.name})")
                )

                setPositiveButton(android.R.string.ok) { _, _ ->
                    if (selectedPOI == null) {
                        latLng?.let {
                            viewModel.reminderLatitude.value = latLng.latitude
                            viewModel.reminderLongitude.value = latLng.longitude
                            viewModel.reminderSelectedLocationStr.value =
                                getString(R.string.undisclosed_location)
                        }
                    } else {
                        viewModel.selectedPOI.value = selectedPOI
                        viewModel.reminderLatitude.value = selectedPOI.latLng.latitude
                        viewModel.reminderLongitude.value = selectedPOI.latLng.longitude
                        viewModel.reminderSelectedLocationStr.value = selectedPOI.name
                    }
                    viewModel.navigationCommand.value = NavigationCommand.Back
                }

                setNegativeButton(android.R.string.cancel) { _, _ ->
                    marker?.remove()
                }

                create()
            }
    }
}