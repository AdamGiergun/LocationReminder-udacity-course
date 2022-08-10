package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
//    @get:Rule
//    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var app: Application

    @Before
    fun setup() {
        // Initialise the repository with no tasks.
        fakeDataSource = FakeDataSource()
        app = ApplicationProvider.getApplicationContext()
        remindersListViewModel = RemindersListViewModel(
            app,
            fakeDataSource
        )
    }

    @Test
    fun test() = runTest {
        remindersListViewModel.loadReminders()
//        advanceUntilIdle()

        assertThat(
            remindersListViewModel.remindersList.value?.size ?: 0,
            `is`(2)
        )

        assertThat(
            remindersListViewModel.showLoading.value,
        `is`(false))

        assertThat(
            remindersListViewModel.showSnackBar.value,
            `is`(nullValue()))

        assertThat(
            remindersListViewModel.showNoData.value,
            `is`(false)
        )
    }
}