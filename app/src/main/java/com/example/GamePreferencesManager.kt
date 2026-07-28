package com.example

import android.content.Context
import android.content.SharedPreferences

class GamePreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)

    var snakeColorPalette: Int
        get() = prefs.getInt("snake_color_palette", 0)
        set(value) = prefs.edit().putInt("snake_color_palette", value).apply()

    var batterySaverMode: Boolean
        get() = prefs.getBoolean("battery_saver_mode", false)
        set(value) = prefs.edit().putBoolean("battery_saver_mode", value).apply()

    var accelerometerSteering: Boolean
        get() = prefs.getBoolean("accelerometer_steering", false)
        set(value) = prefs.edit().putBoolean("accelerometer_steering", value).apply()
}
