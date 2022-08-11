package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

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

        assertThat(
            remindersListViewModel.remindersList.value?.size ?: 0,
            `is`(2)
        )

        assertThat(
            remindersListViewModel.showLoading.value,
            `is`(false)
        )

        assertThat(
            remindersListViewModel.showSnackBar.value,
            `is`(nullValue())
        )

        assertThat(
            remindersListViewModel.showNoData.value,
            `is`(false)
        )
    }
}