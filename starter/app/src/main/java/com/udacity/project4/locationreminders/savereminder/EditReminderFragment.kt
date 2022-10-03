package com.udacity.project4.locationreminders.savereminder

import android.content.IntentSender
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.navArgs
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentEditReminderBinding
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class EditReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: EditReminderViewModel by inject()
    private val resolveApiException = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentEditReminderBinding.inflate(inflater).let { binding ->

        binding.lifecycleOwner = viewLifecycleOwner

        val args: EditReminderFragmentArgs by navArgs()
        args.reminder?.let {
            _viewModel.setReminderIfNotInitialized(it)
        }

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        _viewModel.resolvableApiException.observe(viewLifecycleOwner) { resolvableApiException ->
            if (resolvableApiException != null) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(resolvableApiException.resolution.intentSender)
                            .build()

                    resolveApiException.launch(intentSenderRequest)

                } catch (sendEx: IntentSender.SendIntentException) {
                    _viewModel.showErrorMessage.value = sendEx.localizedMessage
                } finally {
                    _viewModel.resolvableApiException.value = null
                }
            }
        }

        binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}