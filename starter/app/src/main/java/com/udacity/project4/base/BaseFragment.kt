package com.udacity.project4.base

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment() {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val _viewModel: BaseViewModel
    private var snackbar: Snackbar? = null

    override fun onStart() {
        super.onStart()

        _viewModel.showErrorMessage.observe(this) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }

        _viewModel.showToast.observe(this) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }

        _viewModel.showSnackBar.observe(this) {
            Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).also { newSnackbar ->
                snackbar = newSnackbar
                newSnackbar.show()
            }
        }

        _viewModel.showSnackBarInt.observe(this) {
            Snackbar.make(requireView(), getString(it), Snackbar.LENGTH_LONG).also { newSnackbar ->
                snackbar = newSnackbar
                newSnackbar.show()
            }
        }

        _viewModel.navigationCommand.observe(this) { command ->
            when (command) {
                is NavigationCommand.To -> findNavController().navigate(command.directions)
                is NavigationCommand.Back -> findNavController().popBackStack()
                is NavigationCommand.BackTo -> findNavController().popBackStack(
                    command.destinationId,
                    false
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        snackbar?.let {
            it.dismiss()
            snackbar = null
        }
    }
}