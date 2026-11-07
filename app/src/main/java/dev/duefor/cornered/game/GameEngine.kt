package dev.duefor.cornered.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

enum class Phase { Ready, Running, GameOver }

class Enemy(
    var x: Float,
    var y: Float,
    var radius: Float,
    /** dp/秒，渲染前乘 pxPerDp 得到像素速度 */
    var speed: Float,
    var angle: Float,
    val spin: Float,
)

/**
 * 纯逻辑、无 Android 依赖的游戏状态机：场地会随时间收缩，敌人从四周涌入。
 * 所有尺寸以 dp 书写，通过 [resize] 传入的 pxPerDp 换算成像素，保证不同屏幕手感一致。
 */
class GameEngine {

    private val random = Random(System.nanoTime())

    val enemies = ArrayList<Enemy>(MAX_ENEMIES)
    val trail = ArrayList<Offset>(TRAIL_SIZE)

    var phase = Phase.Ready
        private set
    var width = 0f
        private set
    var height = 0f
        private set
    var elapsed = 0f
        private set
    var score = 0
        private set
    var inset = 0f
        private set
    var playerX = 0f
        private set
    var playerY = 0f
        private set
    var playerRadius = 0f
        private set
    var dashCooldown = 0f
        private set
    var isDashing = false
        private set

    private var pxPerDp = 3f
    private var maxInset = 0f
    private var inputX = 0f
    private var inputY = 0f
    private var inputStrength = 0f
    private var dashTimer = 0f
    private var dashDirX = 0f
    private var dashDirY = 0f
    private var spawnAccumulator = 0f

    /** 0 = 冲刺已就绪，1 = 刚进入冷却 */
    val dashProgress: Float
        get() = (1f - dashCooldown / DASH_COOLDOWN).coerceIn(0f, 1f)

    fun resize(widthPx: Float, heightPx: Float, px: Float) {
        if (widthPx <= 0f || heightPx <= 0f) return
        val first = width <= 0f || height <= 0f
        width = widthPx
        height = heightPx
        pxPerDp = px
        playerRadius = PLAYER_RADIUS_DP * px
        maxInset = min(widthPx, heightPx) * MAX_INSET_RATIO
        if (first) {
            playerX = widthPx / 2f
            playerY = heightPx / 2f
        }
        clampPlayer()
    }

    fun start() {
        if (width <= 0f) return
        phase = Phase.Running
        elapsed = 0f
        score = 0
        inset = 0f
        enemies.clear()
        trail.clear()
        spawnAccumulator = 0f
        dashTimer = 0f
        dashCooldown = 0f
        inputStrength = 0f
        isDashing = false
        playerX = width / 2f
        playerY = height / 2f
    }

    fun update(dt: Float) {
        if (phase != Phase.Running || width <= 0f || dt <= 0f) return
        elapsed += dt
        score = elapsed.toInt()
        inset = min(maxInset, inset + SHRINK_DP_PER_SEC * pxPerDp * dt)
        updateDash(dt)
        updatePlayer(dt)
        updateSpawning(dt)
        updateEnemies(dt)
    }

    // ---------------------------------------------------------------- 输入

    fun setInput(dx: Float, dy: Float, slop: Float) {
        val len = sqrt(dx * dx + dy * dy)
        if (len < slop) {
            inputStrength = 0f
            return
        }
        inputX = dx / len
        inputY = dy / len
        inputStrength = ((len - slop) / (slop * 4f)).coerceIn(0.45f, 1f)
    }

    fun clearInput() {
        inputStrength = 0f
    }

    fun dash(dx: Float, dy: Float): Boolean {
        if (phase != Phase.Running || dashCooldown > 0f || dashTimer > 0f) return false
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1f) return false
        dashDirX = dx / len
        dashDirY = dy / len
        dashTimer = DASH_TIME
        dashCooldown = DASH_COOLDOWN
        isDashing = true
        trail.clear()
        return true
    }

    // ---------------------------------------------------------------- 内部

    private fun updateDash(dt: Float) {
        if (dashCooldown > 0f) dashCooldown = max(0f, dashCooldown - dt)
        if (dashTimer > 0f) {
            dashTimer -= dt
            if (dashTimer <= 0f) {
                isDashing = false
                trail.clear()
            }
        }
    }

    private fun updatePlayer(dt: Float) {
        if (dashTimer > 0f) {
            playerX += dashDirX * DASH_SPEED_DP * pxPerDp * dt
            playerY += dashDirY * DASH_SPEED_DP * pxPerDp * dt
            trail.add(Offset(playerX, playerY))
            while (trail.size > TRAIL_SIZE) trail.removeAt(0)
        } else if (inputStrength > 0.01f) {
            playerX += inputX * PLAYER_SPEED_DP * pxPerDp * inputStrength * dt
            playerY += inputY * PLAYER_SPEED_DP * pxPerDp * inputStrength * dt
        }
        clampPlayer()
    }

    private fun clampPlayer() {
        if (width <= 0f || height <= 0f) return
        val left = inset + playerRadius
        val right = width - inset - playerRadius
        val top = inset + playerRadius
        val bottom = height - inset - playerRadius
        playerX = if (right > left) playerX.coerceIn(left, right) else width / 2f
        playerY = if (bottom > top) playerY.coerceIn(top, bottom) else height / 2f
    }

    private fun updateSpawning(dt: Float) {
        val interval = max(MIN_SPAWN_INTERVAL, BASE_SPAWN_INTERVAL - elapsed * SPAWN_RAMP)
        spawnAccumulator += dt
        while (spawnAccumulator >= interval) {
            spawnAccumulator -= interval
            if (enemies.size < MAX_ENEMIES) spawnEnemy()
        }
    }

    private fun spawnEnemy() {
        val radius = ENEMY_RADIUS_DP * pxPerDp * (0.85f + random.nextFloat() * 0.45f)
        val margin = radius * 3f
        val x: Float
        val y: Float
        when (random.nextInt(4)) {
            0 -> { x = random.nextFloat() * width; y = -margin }
            1 -> { x = width + margin; y = random.nextFloat() * height }
            2 -> { x = random.nextFloat() * width; y = height + margin }
            else -> { x = -margin; y = random.nextFloat() * height }
        }
        val speed = (BASE_ENEMY_SPEED_DP + elapsed * SPEED_RAMP) * (0.85f + random.nextFloat() * 0.3f)
        val spin = (if (random.nextBoolean()) 1f else -1f) * (25f + random.nextFloat() * 95f)
        enemies += Enemy(
            x = x,
            y = y,
            radius = radius,
            speed = min(speed, MAX_ENEMY_SPEED_DP),
            angle = random.nextFloat() * 360f,
            spin = spin,
        )
    }

    private fun updateEnemies(dt: Float) {
        val separation = ENEMY_RADIUS_DP * pxPerDp * 1.7f
        val separation2 = separation * separation
        for (i in enemies.indices) {
            val e = enemies[i]
            var dx = playerX - e.x
            var dy = playerY - e.y
            val distance = sqrt(dx * dx + dy * dy)
            if (distance > 0.001f) {
                dx /= distance
                dy /= distance
            } else {
                dx = 0f
                dy = 0f
            }
            var vx = dx * e.speed * pxPerDp
            var vy = dy * e.speed * pxPerDp

            for (j in enemies.indices) {
                if (i == j) continue
                val other = enemies[j]
                val ox = e.x - other.x
                val oy = e.y - other.y
                val d2 = ox * ox + oy * oy
                if (d2 < separation2 && d2 > 0.0001f) {
                    val d = sqrt(d2)
                    val push = (separation - d) / separation * 130f * pxPerDp
                    vx += ox / d * push
                    vy += oy / d * push
                }
            }

            e.x += vx * dt
            e.y += vy * dt
            e.angle += e.spin * dt

            if (distance < playerRadius + e.radius) {
                isDashing = false
                dashTimer = 0f
                trail.clear()
                phase = Phase.GameOver
                return
            }
        }
    }

    private companion object {
        const val PLAYER_RADIUS_DP = 15f
        const val PLAYER_SPEED_DP = 265f
        const val DASH_SPEED_DP = 1000f
        const val DASH_TIME = 0.16f
        const val DASH_COOLDOWN = 2.4f
        const val SHRINK_DP_PER_SEC = 3.4f
        const val MAX_INSET_RATIO = 0.26f
        const val ENEMY_RADIUS_DP = 13f
        const val BASE_ENEMY_SPEED_DP = 52f
        const val SPEED_RAMP = 2.0f
        const val MAX_ENEMY_SPEED_DP = 235f
        const val BASE_SPAWN_INTERVAL = 1.15f
        const val SPAWN_RAMP = 0.022f
        const val MIN_SPAWN_INTERVAL = 0.26f
        const val MAX_ENEMIES = 80
        const val TRAIL_SIZE = 16
    }
}
