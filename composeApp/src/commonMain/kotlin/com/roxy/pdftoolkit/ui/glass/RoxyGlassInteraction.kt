package com.roxy.pdftoolkit.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State manager for interactive physics effects on Roxy Liquid Glass.
 * Tracks pointer coordinates and spring-animates press deformation.
 */
class RoxyGlassInteraction(
    private val scope: CoroutineScope
) {
    private val springSpec = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 380f,
        visibilityThreshold = 0.001f
    )
    private val animatable = Animatable(0f)

    /** 0.0f at rest, scales to 1.0f on press/drag */
    val pressProgress: Float get() = animatable.value

    /** Touch coordinate relative to the glass container */
    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    suspend fun processPointer(inputScope: PointerInputScope) {
        inputScope.detectDragGestures(
            onDragStart = { offset ->
                touchPosition = offset
                scope.launch { animatable.animateTo(1f, springSpec) }
            },
            onDragEnd = {
                scope.launch { animatable.animateTo(0f, springSpec) }
            },
            onDragCancel = {
                scope.launch { animatable.animateTo(0f, springSpec) }
            },
            onDrag = { change, _ ->
                touchPosition = change.position
            }
        )
    }
}

@Composable
fun rememberRoxyGlassInteraction(): RoxyGlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { RoxyGlassInteraction(scope) }
}
