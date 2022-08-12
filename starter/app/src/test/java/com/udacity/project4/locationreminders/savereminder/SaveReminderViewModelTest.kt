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
class SaveReminderViewModelTest : KoinTest {
    //TODO: provide testing to the SaveReminderView and its live data objects

    private val saveReminderViewModel: SaveReminderViewModel by inject()

    private val viewModelModule = module {
        single {
            SaveReminderViewModel(
                get(),
                get() as ReminderDataSource
            )
        }

        // warning about useless cast is wrong, it's needed
        @Suppress("USELESS_CAST")
        single { FakeDataSource() as ReminderDataSource }
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
        //unloadKoinModules(viewModelModule)
        stopKoin()
    }

    @Test
    fun `no title snackbar`() = runTest {
        saveReminderViewModel.validateAndSaveReminder()

        val showSnackBarInt = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(showSnackBarInt).isEqualTo(R.string.err_enter_title)
    }

    @Test
    fun `no location snackbar`() = runTest {
        saveReminderViewModel.reminderTitle.postValue("fake title")
        saveReminderViewModel.validateAndSaveReminder()

        val showSnackBarInt = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(showSnackBarInt).isEqualTo(R.string.err_select_location)
    }

    @Test
    fun `reminder saved`() = runTest {
        saveReminderViewModel.reminderTitle.postValue("fake title")
        saveReminderViewModel.reminderSelectedLocationStr.postValue("fake location")
        saveReminderViewModel.latitude.postValue(0.0)
        saveReminderViewModel.longitude.postValue(0.0)
        saveReminderViewModel.validateAndSaveReminder()

        val showLoading = saveReminderViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading).isFalse()

        val appContext = getApplicationContext<MyApp>().applicationContext
        val showToast = saveReminderViewModel.showToast.getOrAwaitValue()
        assertThat(showToast).isEqualTo(appContext.getString(R.string.reminder_saved))

        val navigationCommand = saveReminderViewModel.navigationCommand.getOrAwaitValue()
        assertThat(navigationCommand).isEqualTo(NavigationCommand.Back)
    }
}