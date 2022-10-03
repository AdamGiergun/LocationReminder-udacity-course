package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.util.concurrent.TimeoutException

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class RemindersListViewModelTest : KoinTest {

    private val fakeDataSource: ReminderDataSource by inject()
    private val remindersListViewModel: RemindersListViewModel by inject()

    private val viewModelModule = module {
        single {
            RemindersListViewModel(
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
    fun getDataFromPopulatedDatasource() = runTest {
        // GIVEN: fake datasource with a list of some reminders
        listOf(
            ReminderDTO(
                "test1",
                "test1",
                "test1",
                0.0,
                0.0,
                100,
                "test_id",
                1
            ),
            ReminderDTO(
                "test2",
                "test2",
                "test2",
                180.0,
                180.0,
                100,
                null,
                2
            )
        ).forEach {
            fakeDataSource.saveReminder(it)
        }

        // WHEN: viewmodel loads reminders
        remindersListViewModel.loadReminders()

        // THEN: LiveData values are set properly
        val remindersList = remindersListViewModel.remindersList.getOrAwaitValue()
        assertThat(remindersList.size).isEqualTo(2)

        val showLoading = remindersListViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading).isEqualTo(false)

        try {
            remindersListViewModel.showSnackBar.getOrAwaitValue()
            Assert.fail()
        } catch (e: Exception) {
            assertThat(e is TimeoutException).isEqualTo(true)
        }

        val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()
        assertThat(showNoData).isEqualTo(false)
    }

    @Test
    fun getDataFromEmptyDatasource() = runTest {
        // GIVEN: empty fake datasource
        // WHEN: viewmodel loads reminders
        remindersListViewModel.loadReminders()

        // THEN: LiveData values are set properly
        try {
            remindersListViewModel.remindersList.getOrAwaitValue()
            Assert.fail()
        } catch (e: Exception) {
            assertThat(e is TimeoutException).isEqualTo(true)
        }

        val showLoading = remindersListViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading).isEqualTo(false)

        val showSnackBar = remindersListViewModel.showSnackBar.getOrAwaitValue()
        assertThat(showSnackBar).isEqualTo("No reminders")

        val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()
        assertThat(showNoData).isEqualTo(true)
    }
}