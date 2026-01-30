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
import com.d102.crescendo.domain.model.onboarding.Genre
import com.d102.crescendo.presentation.ui.screen.onboarding.SelectableItem

@Composable
fun GenreSelectionStep(
    genres: List<Genre>,
    selectedGenreIds: Set<Int>,
    onGenreClick: (Int) -> Unit
) {
    val density = LocalDensity.current

    val startOffsetY = with(density) { 40.dp.toPx() }
    val enterOffsetY = remember { Animatable(startOffsetY) }

    // 자리에 온 뒤에는 살짝 둥둥 떠있는 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "genreFloat")
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
        // 아래에서 위로 올라오기 (실제 보이는 슬라이드)
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
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.graphicsLayer {
            // 아래에서 위로 올라오는 애니메이션
            // 다 올라온 뒤에는 floatOffsetY로 살짝 위아래 둥둥
            translationY = enterOffsetY.value + if (hasEntered) floatOffsetY else 0f
            alpha = 1f   // 이제는 처음부터 보이도록 고정
        }
    ) {
        items(genres, key = { it.id }) { genre ->
            SelectableItem(
                name = genre.korName,
                isSelected = genre.id in selectedGenreIds,
                onClick = { onGenreClick(genre.id) }
            )
        }
    }
}
