package com.example.pocketbill.ui.home

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

/*
 * Home's motion: sections rise in one after another the first time Home opens, the outstanding
 * amount counts up, and the main button gives under the finger. All of it is skipped when the
 * phone's animations are turned off (Settings → Accessibility → Remove animations).
 */

/** Material 3 "emphasized decelerate": quick start, long gentle landing. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

private const val ENTRANCE_MS = 480
private const val ENTRANCE_STAGGER_MS = 60L
private const val COUNT_UP_MS = 900

/** The entrance plays once per app run; coming back from another tab shows Home at once. */
private object HomeIntro {
    var played = false
}

/** True when the user turned animations off for the whole phone. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** Whether this showing of Home should play the entrance. Read once, then marked as played. */
@Composable
fun rememberPlayIntro(): Boolean {
    val reduce = rememberReduceMotion()
    val play = remember { !HomeIntro.played && !reduce }
    LaunchedEffect(Unit) { HomeIntro.played = true }
    return play
}

/** Fades the content in while it rises 24 dp, starting [index] steps after the first section. */
@Composable
fun Modifier.entrance(index: Int, play: Boolean): Modifier {
    val progress = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(index * ENTRANCE_STAGGER_MS)
            progress.animateTo(1f, tween(ENTRANCE_MS, easing = EmphasizedDecelerate))
        }
    }
    val rise = with(LocalDensity.current) { 24.dp.toPx() }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise
    }
}

/**
 * A money amount (minor units) that counts up from zero when [fromZero], and later eases from the
 * old figure to the new one (after Mark as paid, for example). Exact once settled: the
 * in-between values are computed in whole minor units, never shown as rounded floats at the end.
 */
@Composable
fun rememberCountUp(target: Long, fromZero: Boolean): Long {
    val reduce = rememberReduceMotion()
    var from by remember { mutableLongStateOf(if (fromZero) 0L else target) }
    var to by remember { mutableLongStateOf(target) }
    val progress = remember { Animatable(if (fromZero && !reduce) 0f else 1f) }

    fun current(): Long = if (progress.value >= 1f) to else from + ((to - from) * progress.value.toDouble()).roundToLong()

    LaunchedEffect(target) {
        if (target != to) {
            from = current()
            to = target
            if (reduce) return@LaunchedEffect
            progress.snapTo(0f)
        }
        progress.animateTo(1f, tween(COUNT_UP_MS, easing = EmphasizedDecelerate))
    }
    return current()
}

/** Shrinks slightly while pressed and springs back: the button feels physical. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
