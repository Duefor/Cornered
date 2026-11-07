package dev.duefor.cornered.update

/** 语义化版本的极简比较，够用且不引入依赖。 */
object Version {

    fun segments(raw: String): List<Int> =
        raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split('.', '-', '+', '_')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    /** 与 build.gradle.kts 里的 encodeVersionCode 保持一致：主*10000 + 次*100 + 修订 */
    fun encodeCode(name: String): Int {
        val segments = segments(name)
        val major = segments.getOrElse(0) { 0 }.coerceIn(0, 9_999)
        val minor = segments.getOrElse(1) { 0 }.coerceIn(0, 99)
        val patch = segments.getOrElse(2) { 0 }.coerceIn(0, 99)
        return major * 10_000 + minor * 100 + patch
    }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = segments(candidate)
        val b = segments(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
