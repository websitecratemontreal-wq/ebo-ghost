package com.rankly.eboghost.domain

import android.content.Context
import android.content.SharedPreferences

class CalibrationStore private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "calibration"
        @Volatile private var instance: CalibrationStore? = null
        fun getInstance(context: Context): CalibrationStore =
            instance ?: synchronized(this) {
                instance ?: CalibrationStore(context.applicationContext).also { instance = it }
            }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun savePoint(key: String, x: Int, y: Int) {
        prefs.edit()
            .putInt("${key}_x", x)
            .putInt("${key}_y", y)
            .apply()
    }

    fun getPoint(key: String): Pair<Int, Int>? {
        val x = prefs.getInt("${key}_x", -1)
        val y = prefs.getInt("${key}_y", -1)
        return if (x == -1 || y == -1) null else Pair(x, y)
    }

    fun hasPoint(key: String): Boolean = getPoint(key) != null

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
