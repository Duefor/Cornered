package dev.duefor.cornered.data

import android.content.Context
import dev.duefor.cornered.game.RunState

/** 存档落到 SharedPreferences，一次一局，只保留最新的。 */
class RunRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("cornered_save", Context.MODE_PRIVATE)

    fun hasSave(): Boolean = prefs.contains(KEY_RUN)

    /** 启动时校验：旧版本或损坏的存档会被清掉，避免主菜单出现点了没反应「继续游戏」。 */
    fun hasValidSave(): Boolean = load() != null

    fun save(run: RunState) {
        prefs.edit().putString(KEY_RUN, RunCodec.encode(run)).apply()
    }

    /** 存档损坏或版本不匹配时返回 null，并清掉它，避免反复失败。 */
    fun load(): RunState? {
        val raw = prefs.getString(KEY_RUN, null) ?: return null
        val run = RunCodec.decode(raw)
        if (run == null) clear()
        return run
    }

    fun clear() {
        prefs.edit().remove(KEY_RUN).apply()
    }

    private companion object {
        const val KEY_RUN = "run_json"
    }
}
