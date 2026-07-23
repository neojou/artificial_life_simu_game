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
 * - Exposes immutable [snapshot] via [StateFlow]
 * - Drives [SimulationEngine.stepHour] on a coroutine loop when playing
 * - Speed multipliers: 1×, 2×, 5×, 10×
 *
 * UI layers must only read [snapshot]; they must not mutate engine state directly.
 *
 * @param initialSeed RNG seed for the first world.
 * @param scope Coroutine scope owned by the UI (e.g. [androidx.compose.runtime.rememberCoroutineScope]).
 * @param baseDelayMs Delay between hours at 1× speed.
 */
class SimulationController(
    initialSeed: Long = DEFAULT_SEED,
    private val scope: CoroutineScope,
    private val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
) {
    private var engine: SimulationEngine = SimulationEngine.create(initialSeed)

    private val _snapshot = MutableStateFlow(engine.snapshot())
    val snapshot: StateFlow<SimSnapshot> = _snapshot.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _speed = MutableStateFlow(1)
    /** Current speed multiplier (1, 2, 5, or 10). */
    val speed: StateFlow<Int> = _speed.asStateFlow()

    private val _seed = MutableStateFlow(initialSeed)
    val seed: StateFlow<Long> = _seed.asStateFlow()

    private var loopJob: Job? = null

    /** Allowed speed multipliers (GDD control strip). */
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

    /**
     * Sets simulation speed. Only [SPEED_OPTIONS] values are accepted; others are ignored.
     */
    fun setSpeed(mult: Int) {
        if (mult !in SPEED_OPTIONS) return
        _speed.value = mult
        // Restart loop so the new delay applies immediately while playing.
        if (_isPlaying.value) {
            loopJob?.cancel()
            startLoop()
        }
    }

    /**
     * Rebuilds the world with [seed], pauses playback, and publishes a fresh snapshot.
     */
    fun reset(seed: Long = _seed.value) {
        pause()
        _seed.value = seed
        engine = SimulationEngine.create(seed)
        publish()
    }

    /** Advances exactly one hour while paused or playing (debug / stepping). */
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

    /** Cancels the play loop; call from Compose [androidx.compose.runtime.DisposableEffect]. */
    fun dispose() {
        pause()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (isActive && _isPlaying.value) {
                if (engine.isGameOver) {
                    pause()
                    break
                }
                engine.stepHour()
                publish()
                if (engine.isGameOver) {
                    pause()
                    break
                }
                delay(delayForSpeed(_speed.value))
            }
        }
    }

    private fun publish() {
        _snapshot.value = engine.snapshot()
    }

    private fun delayForSpeed(mult: Int): Long {
        val m = mult.coerceAtLeast(1)
        return (baseDelayMs / m).coerceAtLeast(MIN_DELAY_MS)
    }

    companion object {
        const val DEFAULT_SEED: Long = 0L
        const val DEFAULT_BASE_DELAY_MS: Long = 400L
        const val MIN_DELAY_MS: Long = 16L
        val SPEED_OPTIONS: List<Int> = listOf(1, 2, 5, 10)
    }
}
