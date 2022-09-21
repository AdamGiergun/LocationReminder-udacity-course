package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class EditReminderViewModelTest : KoinTest {
    //TODO: provide testing to the SaveReminderView and its live data objects

    private val editReminderViewModel: EditReminderViewModel by inject()

    private val viewModelModule = module {
        single {
            EditReminderViewModel(
                get(),
                get() as ReminderDataSource
            )
        }

        single<ReminderDataSource> { FakeDataSource() }
    }

    //     Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        loadKoinModules(viewModelModule)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `no title snackbar`() = runTest {
        editReminderViewModel.validateAndSaveReminder()

        val showSnackBarInt = editReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(showSnackBarInt).isEqualTo(R.string.err_enter_title)
    }

    @Test
    fun `no location snackbar`() = runTest {
        editReminderViewModel.reminderTitle.postValue("fake title")
        editReminderViewModel.validateAndSaveReminder()

        val showSnackBarInt = editReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(showSnackBarInt).isEqualTo(R.string.err_select_location)
    }

    @Test
    fun `reminder saved`() = runTest {
        editReminderViewModel.apply {
            reminderTitle.postValue("fake title")
            reminderSelectedLocationStr.postValue("fake location")
            reminderLatitude.postValue(0.0)
            reminderLongitude.postValue(0.0)
            validateAndSaveReminder()
        }

        val showLoading = editReminderViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading).isFalse()

        val appContext = getApplicationContext<MyApp>().applicationContext
        val showToast = editReminderViewModel.showToast.getOrAwaitValue()
        assertThat(showToast).isEqualTo(appContext.getString(R.string.reminder_saved))

        val navigationCommand = editReminderViewModel.navigationCommand.getOrAwaitValue()
        assertThat(navigationCommand).isEqualTo(NavigationCommand.Back)
    }
}