package dev.duefor.cornered.data

import android.content.Context

/** 跨局记录：最深层数、通关次数。 */
class RunRecords(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("cornered", Context.MODE_PRIVATE)

    var bestFloor: Int
        get() = prefs.getInt(KEY_BEST_FLOOR, 0)
        private set(value) {
            prefs.edit().putInt(KEY_BEST_FLOOR, value).apply()
        }

    var victories: Int
        get() = prefs.getInt(KEY_VICTORIES, 0)
        private set(value) {
            prefs.edit().putInt(KEY_VICTORIES, value).apply()
        }

    fun record(floor: Int, victory: Boolean) {
        if (floor > bestFloor) bestFloor = floor
        if (victory) victories += 1
    }

    private companion object {
        const val KEY_BEST_FLOOR = "best_floor"
        const val KEY_VICTORIES = "victories"
    }
}
