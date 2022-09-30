package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource

object EspressoIdlingResource {

    private const val RESOURCE = "GLOBAL"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }

    inline fun <reified T> wrapEspressoIdlingResource(function: () -> T): T {
        increment() //Set app as busy
        return try {
            function()
        } finally {
            decrement() // Set app as idle
        }
    }
}