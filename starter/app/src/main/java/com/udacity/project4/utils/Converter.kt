package com.udacity.project4.utils

import androidx.databinding.InverseMethod

object Converter {
    @InverseMethod("intToString")
    @JvmStatic
    fun stringToInt(
        value: String
    ): Int? =
        if (value == "")
            null
        else
            value.toInt()

    @JvmStatic
    fun intToString(
        value: Int
    ): String =
        value.toString()
}