package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.GeofenceStatusCodes.*
import com.google.android.gms.location.GeofencingClient
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentEditReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.utils.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.getGeofencingClient
import com.udacity.project4.utils.getGeofencingRequest
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class EditReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: EditReminderViewModel by inject()
    private lateinit var binding: FragmentEditReminderBinding

    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }

        PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    private val enableLocationRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditReminderBinding.inflate(inflater)
        val args: EditReminderFragmentArgs by navArgs()
        args.reminder?.let {
            _viewModel.setReminderIfNotInitialized(it)
        }

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(
                    EditReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
                )
        }

        binding.saveReminder.setOnClickListener { saveReminder() }
    }

    @SuppressLint("MissingPermission")
    private fun saveReminder() {
        if (_viewModel.validateEnteredData()) {
            if (_viewModel.isLocationChanged) {
                _viewModel.reminderGeofenceId.value?.let { requestId ->
                    val logTag = this::class.java.name
                    _viewModel.reminderGeofenceId.value = null
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

            if (_viewModel.reminderGeofenceId.value == null) {
                val geofencingRequest = getGeofencingRequest(
                    // already checked by _viewModel.validateEnteredData())
                    _viewModel.reminderLatitude.value!!,
                    _viewModel.reminderLongitude.value!!,
                    _viewModel.reminderRadiusInMeters.value!!
                )
                geofencingClient.addGeofences(
                    geofencingRequest,
                    geofencePendingIntent
                ).apply {
                    addOnSuccessListener {
                        _viewModel.showSnackBar.value = "Geofence added"
                        _viewModel.reminderGeofenceId.value =
                            geofencingRequest.geofences.first().requestId
                        _viewModel.validateAndSaveReminder()
                    }
                    addOnFailureListener { exception ->
                        handleGeofenceClientException(exception)
                    }
                }
            } else {
                _viewModel.validateAndSaveReminder()
            }
        }
    }

    private fun handleGeofenceClientException(exception: java.lang.Exception) {
        if (exception is ApiException) {
            when (exception.statusCode) {
                GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION -> {
                    if (exception is ResolvableApiException)
                        try {
                            val intentSenderRequest = IntentSenderRequest
                                .Builder(exception.resolution.intentSender)
                                .build()

                            enableLocationRequestLauncher.launch(intentSenderRequest)

                        } catch (sendEx: IntentSender.SendIntentException) {
                            _viewModel.showErrorMessage.value =
                                sendEx.localizedMessage
                        }
                }

                GEOFENCE_NOT_AVAILABLE ->
                    _viewModel.showErrorMessage.value =
                        getString(R.string.geofence_not_available)

                GEOFENCE_REQUEST_TOO_FREQUENT ->
                    _viewModel.showErrorMessage.value =
                        getString(R.string.geofence_request_too_frequent)

                GEOFENCE_TOO_MANY_GEOFENCES ->
                    _viewModel.showErrorMessage.value =
                        getString(R.string.geofence_too_many_geofences)

                GEOFENCE_TOO_MANY_PENDING_INTENTS ->
                    _viewModel.showErrorMessage.value =
                        getString(R.string.geofence_too_many_pending_intents)
            }
        } else
            _viewModel.showErrorMessage.value =
                getString(R.string.error_adding_geofence)
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}