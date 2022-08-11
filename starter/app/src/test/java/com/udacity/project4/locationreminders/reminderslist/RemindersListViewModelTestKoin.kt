package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.util.concurrent.TimeoutException

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTestKoin : KoinTest {

    private val viewModelModule = module {
        single {
            RemindersListViewModel(
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
        unloadKoinModules(viewModelModule)
    }

    @Test
    fun test() = runTest {
        val remindersListViewModel: RemindersListViewModel by inject()

        remindersListViewModel.loadReminders()

        val remindersList = remindersListViewModel.remindersList.getOrAwaitValue()
        assertThat(remindersList.size, `is`(2))

        val showLoading = remindersListViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading, `is`(false))

        try {
            remindersListViewModel.showSnackBar.getOrAwaitValue()
            Assert.fail()
        } catch (e: Exception) {
            assertThat(e is TimeoutException, `is`(true))
        }

        val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()
        assertThat(showNoData, `is`(false))
    }
}