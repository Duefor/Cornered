package dev.duefor.cornered.data

import android.content.Context

/** 本地最高分，只用 SharedPreferences，不值得为一行数字引入 DataStore。 */
class ScoreStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("cornered", Context.MODE_PRIVATE)

    var best: Int
        get() = prefs.getInt(KEY_BEST, 0)
        set(value) = prefs.edit().putInt(KEY_BEST, value).apply()

    private companion object {
        const val KEY_BEST = "best_seconds"
    }
}
