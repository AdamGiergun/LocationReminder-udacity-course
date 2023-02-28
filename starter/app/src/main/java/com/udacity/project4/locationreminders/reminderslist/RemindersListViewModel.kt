package com.udacity.project4.locationreminders.reminderslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.toDataItem
import com.udacity.project4.utils.tryCast
import kotlinx.coroutines.launch

class RemindersListViewModel(
    private val dataSource: ReminderDataSource
) : BaseViewModel() {
    // list that holds the reminder data to be displayed on the UI
    private val _remindersList = MutableLiveData<List<ReminderDataItem>>()
    val remindersList: LiveData<List<ReminderDataItem>>
        get() = _remindersList

    val remindersListAdapter
        get() = RemindersListAdapter {
            navigationCommand.value = NavigationCommand.To(
                ReminderListFragmentDirections.toEditReminder(it)
            )
        }

    private val _showNoData = MutableLiveData(true)
    override val showNoData: LiveData<Boolean>
        get() = _showNoData

    /**
     * Get all the reminders from the DataSource and add them to the remindersList to be shown on the UI,
     * or show error if any
     */
    fun loadReminders() {
        showLoading.value = true
        viewModelScope.launch {
            //interacting with the dataSource has to be through a coroutine
            val result = dataSource.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<*> -> {
                    result.data.tryCast<List<ReminderDTO>> {
                        val dataList = ArrayList<ReminderDataItem>()
                        dataList.addAll((this).map { reminder ->
                            //map the reminder data from the DB to the be ready to be displayed on the UI
                            reminder.toDataItem()
                        })
                        _remindersList.value = dataList
                    }
                }
                is Result.Error -> showSnackBar.value = result.message ?: ""
            }

            //check if no data has to be shown
            invalidateShowNoData()
        }
    }


    /**
     * Inform the user that there's not any data if the remindersList is empty
     */
    private fun invalidateShowNoData() {
        _showNoData.value = remindersList.value == null || remindersList.value!!.isEmpty()
    }

    fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        navigationCommand.value = NavigationCommand.To(
            ReminderListFragmentDirections.toEditReminder(null)
        )
    }
}