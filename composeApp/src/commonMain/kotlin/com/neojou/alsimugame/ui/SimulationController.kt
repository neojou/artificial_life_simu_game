package com.neojou.alsimugame.ui

import com.neojou.alsimugame.sim.SimulationEngine
import com.neojou.alsimugame.sim.model.AgentSnapshot
import com.neojou.alsimugame.sim.model.SimSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.math.max

/**
 * Bridges [SimulationEngine] to Compose UI.
 *
 * Logic: discrete [SimulationEngine.stepHour].
 * Display: [AgentVisual] interpolates grid positions over the wall-clock
 * duration of each hour (Vis-B smooth walk).
 */
class SimulationController(
    initialSeed: Long = DEFAULT_SEED,
    private val scope: CoroutineScope,
    private val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
    initialSpeed: Int = DEFAULT_SPEED,
) {
    private var engine: SimulationEngine = SimulationEngine.create(initialSeed)

    private var frameId: Long = 0L

    private val _frame = MutableStateFlow(
        UiFrame(frameId, engine.snapshot(), restVisuals(engine.snapshot().agents)),
    )
    val frame: StateFlow<UiFrame> = _frame.asStateFlow()

    private val _snapshotMirror = MutableStateFlow(engine.snapshot())
    val snapshot: StateFlow<SimSnapshot>
        get() = _snapshotMirror

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _speed = MutableStateFlow(
        if (initialSpeed in SPEED_OPTIONS) initialSpeed else DEFAULT_SPEED,
    )
    val speed: StateFlow<Int> = _speed.asStateFlow()

    private val _seed = MutableStateFlow(initialSeed)
    val seed: StateFlow<Long> = _seed.asStateFlow()

    private var loopJob: Job? = null

    /** In-progress move poses (mutated during animation). */
    private var visuals: List<AgentVisual> = restVisuals(engine.snapshot().agents)

    val speedOptions: List<Int> get() = SPEED_OPTIONS

    fun play() {
        if (_isPlaying.value) return
        if (engine.isGameOver) return
        _isPlaying.value = true
        startLoop()
    }

    fun pause() {
        _isPlaying.value = false
        loopJob?.cancel()
        loopJob = null
    }

    fun setSpeed(mult: Int) {
        if (mult !in SPEED_OPTIONS) return
        if (_speed.value == mult) return
        _speed.value = mult
        if (_isPlaying.value) {
            loopJob?.cancel()
            startLoop()
        }
    }

    fun reset(seed: Long = _seed.value) {
        pause()
        _seed.value = seed
        engine = SimulationEngine.create(seed)
        visuals = restVisuals(engine.snapshot().agents)
        publish(visuals)
    }

    fun delayMsForCurrentSpeed(): Long = delayForSpeed(_speed.value)

    fun stepOnce() {
        if (engine.isGameOver) {
            pause()
            publish(visuals)
            return
        }
        scope.launch {
            runAnimatedStep(durationMs = STEP_ONCE_ANIM_MS, continueWhilePaused = true)
            if (engine.isGameOver) {
                _isPlaying.value = false
            }
        }
    }

    fun dispose() {
        pause()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (isActive && _isPlaying.value) {
                if (engine.isGameOver) {
                    _isPlaying.value = false
                    break
                }
                val duration = delayForSpeed(_speed.value)
                runAnimatedStep(durationMs = duration, continueWhilePaused = false)
                if (engine.isGameOver) {
                    _isPlaying.value = false
                    break
                }
            }
            loopJob = null
        }
    }

    /**
     * One logical hour: step engine, then lerp visuals over [durationMs].
     *
     * @param continueWhilePaused when true (單步), finish animation even if not playing.
     */
    private suspend fun runAnimatedStep(durationMs: Long, continueWhilePaused: Boolean) {
        val before = engine.snapshot().agents.associate { it.id to (it.x to it.y) }
        engine.stepHour()
        val afterSnap = engine.snapshot()
        val slots = slotMap(afterSnap.agents)
        visuals = afterSnap.agents.map { agent ->
            val (ox, oy) = before[agent.id] ?: (agent.x to agent.y)
            AgentVisual(
                id = agent.id,
                gender = agent.gender,
                mode = agent.mode,
                carriedFood = agent.carriedFood,
                fromX = ox.toFloat(),
                fromY = oy.toFloat(),
                toX = agent.x.toFloat(),
                toY = agent.y.toFloat(),
                progress = 0f,
                slot = slots[agent.id] ?: 0,
            )
        }
        publish(visuals)

        val animMs = max(durationMs, 0L)
        if (animMs <= 0L) {
            visuals = visuals.map { it.copy(progress = 1f) }
            publish(visuals)
            return
        }

        val frameMs = 16L
        var elapsed = 0L
        while (elapsed < animMs && coroutineContext.isActive) {
            if (!continueWhilePaused && !_isPlaying.value) break
            delay(frameMs)
            elapsed += frameMs
            val p = (elapsed.toFloat() / animMs.toFloat()).coerceIn(0f, 1f)
            visuals = visuals.map { it.copy(progress = p) }
            publish(visuals)
        }
        // Snap to destination so logical and visual stay aligned.
        visuals = visuals.map { it.copy(progress = 1f) }
        publish(visuals)
    }

    private fun publish(agentVisuals: List<AgentVisual>) {
        frameId += 1
        val snap = engine.snapshot()
        _snapshotMirror.value = snap
        _frame.value = UiFrame(frameId, snap, agentVisuals)
    }

    private fun delayForSpeed(mult: Int): Long {
        val m = mult.coerceAtLeast(1)
        return (baseDelayMs / m).coerceAtLeast(MIN_DELAY_MS)
    }

    companion object {
        const val DEFAULT_SEED: Long = 0L
        /** Wall-clock ms per sim-hour at 1× (~1.5–2s walk between cells). */
        const val DEFAULT_BASE_DELAY_MS: Long = 1800L
        /** Default speed: 2× balances observation and progress. */
        const val DEFAULT_SPEED: Int = 2
        const val MIN_DELAY_MS: Long = 80L
        /** Short interpolation when user presses 單步. */
        const val STEP_ONCE_ANIM_MS: Long = 450L
        val SPEED_OPTIONS: List<Int> = listOf(1, 2, 5, 10)

        private fun restVisuals(agents: List<AgentSnapshot>): List<AgentVisual> {
            val slots = slotMap(agents)
            return agents.map { AgentVisual.atRest(it, slots[it.id] ?: 0) }
        }

        private fun slotMap(agents: List<AgentSnapshot>): Map<String, Int> {
            val counts = mutableMapOf<Pair<Int, Int>, Int>()
            val result = mutableMapOf<String, Int>()
            for (a in agents) {
                val key = a.x to a.y
                val slot = counts[key] ?: 0
                result[a.id] = slot
                counts[key] = slot + 1
            }
            return result
        }
    }
}

/**
 * UI frame: logical [snapshot] + display [agentVisuals] for smooth movement.
 */
data class UiFrame(
    val id: Long,
    val snapshot: SimSnapshot,
    val agentVisuals: List<AgentVisual> = emptyList(),
)
