package com.udacity.project4

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class RoboTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(classLoader, TestRoboApp::class.java.name, context)
    }
}