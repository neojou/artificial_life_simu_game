package com.neojou.alsimugame.ui

import com.neojou.alsimugame.sim.SimulationEngine
import com.neojou.alsimugame.sim.model.SimSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Bridges [SimulationEngine] to Compose UI (Architecture §7).
 *
 * - Exposes immutable [snapshot] via [StateFlow] (new instance every step)
 * - [play] runs a coroutine loop: stepHour → publish → delay(speed)
 * - Speed multipliers: 1×, 2×, 5×, 10×
 *
 * UI layers must only read [snapshot]; they must not mutate engine state directly.
 */
class SimulationController(
    initialSeed: Long = DEFAULT_SEED,
    private val scope: CoroutineScope,
    private val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
    initialSpeed: Int = DEFAULT_SPEED,
) {
    private var engine: SimulationEngine = SimulationEngine.create(initialSeed)

    /**
     * Monotonic counter so every [publish] yields a distinct [UiFrame]
     * even if two snapshots were somehow equal (defensive for Compose).
     */
    private var frameId: Long = 0L

    private val _frame = MutableStateFlow(UiFrame(frameId, engine.snapshot()))
    val frame: StateFlow<UiFrame> = _frame.asStateFlow()

    /** Convenience: latest world snapshot (same as [frame].value.snapshot). */
    val snapshot: StateFlow<SimSnapshot>
        get() = _snapshotMirror

    private val _snapshotMirror = MutableStateFlow(engine.snapshot())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _speed = MutableStateFlow(
        if (initialSpeed in SPEED_OPTIONS) initialSpeed else DEFAULT_SPEED,
    )
    val speed: StateFlow<Int> = _speed.asStateFlow()

    private val _seed = MutableStateFlow(initialSeed)
    val seed: StateFlow<Long> = _seed.asStateFlow()

    private var loopJob: Job? = null

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

    /**
     * Rebuilds the world with [seed], pauses playback, and publishes a fresh snapshot.
     * Same seed always yields the same initial world (seeded RNG).
     */
    fun reset(seed: Long = _seed.value) {
        pause()
        _seed.value = seed
        engine = SimulationEngine.create(seed)
        publish()
    }

    /** Delay between sim-hours at the current speed (for tests / diagnostics). */
    fun delayMsForCurrentSpeed(): Long = delayForSpeed(_speed.value)

    fun stepOnce() {
        if (engine.isGameOver) {
            pause()
            publish()
            return
        }
        engine.stepHour()
        publish()
        if (engine.isGameOver) {
            pause()
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
                engine.stepHour()
                publish()
                if (engine.isGameOver) {
                    _isPlaying.value = false
                    break
                }
                delay(delayForSpeed(_speed.value))
            }
            loopJob = null
        }
    }

    private fun publish() {
        frameId += 1
        val snap = engine.snapshot()
        _snapshotMirror.value = snap
        _frame.value = UiFrame(frameId, snap)
    }

    private fun delayForSpeed(mult: Int): Long {
        val m = mult.coerceAtLeast(1)
        return (baseDelayMs / m).coerceAtLeast(MIN_DELAY_MS)
    }

    companion object {
        const val DEFAULT_SEED: Long = 0L
        /** Wall-clock ms per sim-hour at 1× — fast enough to see motion in ~30s. */
        const val DEFAULT_BASE_DELAY_MS: Long = 280L
        const val DEFAULT_SPEED: Int = 5
        const val MIN_DELAY_MS: Long = 16L
        val SPEED_OPTIONS: List<Int> = listOf(1, 2, 5, 10)
    }
}

/**
 * UI frame wrapper: [id] guarantees StateFlow emissions and Compose keys
 * update every simulation step.
 */
data class UiFrame(
    val id: Long,
    val snapshot: SimSnapshot,
)
