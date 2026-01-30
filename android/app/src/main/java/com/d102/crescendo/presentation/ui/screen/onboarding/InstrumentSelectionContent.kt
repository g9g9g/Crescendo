package com.d102.crescendo.presentation.ui.screen.onboarding

import android.R.attr.translationY
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.onboarding.Instrument

@Composable
fun InstrumentSelectionStep(
    instruments: List<Instrument>,
    selectedInstrumentId: Int?,
    onInstrumentClick: (Int) -> Unit
) {
    val density = LocalDensity.current

    // 1단계: 처음에는 아래쪽(약 80.dp)에서 시작
    val startOffsetY = with(density) { 80.dp.toPx() }
    val enterOffsetY = remember { Animatable(startOffsetY) }

    // 2단계: 자리에 온 뒤에는 살짝 둥둥 떠있는 애니
    val infiniteTransition = rememberInfiniteTransition(label = "instrumentFloat")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffsetY"
    )

    var hasEntered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 아래에서 위로 슥 올라오기
        enterOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )
        hasEntered = true
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.graphicsLayer {
            // 1단계: 아래에서 위로 등장
            // 2단계: 다 올라온 뒤에는 살짝 위아래 둥둥
            translationY = enterOffsetY.value + if (hasEntered) floatOffsetY else 0f
            alpha = 1f
        }
    ) {
        items(instruments, key = { it.id }) { instrument ->

            val icon = when (instrument.id) {
                1 -> R.drawable.ic_piano
                2 -> R.drawable.ic_guitar
                else -> null
            }

            SelectableItem(
                name = instrument.korName,
                isSelected = instrument.id == selectedInstrumentId,
                iconRes = icon,
                onClick = { onInstrumentClick(instrument.id) }
            )
        }
    }
}
