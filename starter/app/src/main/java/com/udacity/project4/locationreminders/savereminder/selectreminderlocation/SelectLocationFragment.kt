package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.ktx.awaitMap
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.*
import org.koin.android.ext.android.inject

private const val TAG = "LocationReminder"

class SelectLocationFragment : BaseFragment(), MenuProvider {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private var googleMap: GoogleMap? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val permissionsRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var granted = true
        permissions.forEach {
            granted = granted and it.value
        }

        if (granted)
            _viewModel.setLocationState(LocationState.CHECK_SETTINGS)
        else {

            val permission =
                if (Manifest.permission.ACCESS_BACKGROUND_LOCATION in permissions)
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                else
                    Manifest.permission.ACCESS_FINE_LOCATION

            _viewModel.showToast.value =
                if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    "Access background location is needed"
                else
                    "Fine location is needed"

            if (shouldShowRequestPermissionRationale(permission)) {
                _viewModel.setLocationState(LocationState.CHECK_SETTINGS)
            } else {
                appPermissionsSettingsRequest.launch(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireActivity().packageName, null)
                    }
                )
            }
        }
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectLocationBinding.inflate(inflater)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setDisplayHomeAsUpEnabled(true)

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
                            locationSettingsResponse.locationSettingsStates?.let { states ->
                                if (states.isGpsPresent) {
                                    val fineLoc = isFineLocationPermissionGranted(requireContext())
                                    val bgrLoc =
                                        isBackgroundLocationPermissionGranted(requireContext())
//                                        val gps = states.isGpsUsable

                                    if (!fineLoc)
                                        _viewModel.setLocationState(LocationState.FINE_LOCATION_NOT_PERMITTED)

                                    if (!bgrLoc)
                                        _viewModel.setLocationState(LocationState.BACKGROUND_LOCATION_NOT_PERMITTED)

                                    if (fineLoc and bgrLoc)
                                        _viewModel.setLocationState(LocationState.ENABLED)

                                } else {
                                    _viewModel.setLocationState(LocationState.GPS_NOT_PRESENT)
                                }
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
                    permissionsRequest.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    )
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
                    _viewModel.showSnackBar.value =
                        getString(R.string.gps_is_needed_for_this_to_work)
                }

                LocationState.LOCATION_NOT_USABLE -> {
                    _viewModel.navigationCommand.value = NavigationCommand.Back
                    _viewModel.showSnackBar.value =
                        getString(R.string.location_service_is_needed_for_this_to_work)
                }

                else -> {
                    _viewModel.navigationCommand.value = NavigationCommand.Back
                    _viewModel.showSnackBar.value = getString(R.string.error_happened)
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        lifecycleScope.launchWhenCreated {
            val mapFragment: SupportMapFragment? =
                childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            googleMap = mapFragment?.awaitMap()
            googleMap?.apply {
                setMyMapStyle()
                setOnMapLongClickListener{ latLng ->
                    addMarker(
                        MarkerOptions()
                            .position(latLng)
                    )
//                    TODO: show dialog asking for saving marked location
                }
            }
        }

        _viewModel.showSnackBar.value = "Select some location for the reminder by pointing it and holding it for a moment."

//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

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

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.map_options, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }
        R.id.hybrid_map -> {
            true
        }
        R.id.satellite_map -> {
            true
        }
        R.id.terrain_map -> {
            true
        }
        else -> false
    }
}