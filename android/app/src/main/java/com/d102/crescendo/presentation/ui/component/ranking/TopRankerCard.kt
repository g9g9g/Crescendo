
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.rank.Ranker
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.util.toTierNameKor

@Composable
fun TopRankerCard(
    ranker: Ranker,
    backgroundColor: Color = Gray3,
    modifier: Modifier = Modifier
) {
    val size = if (ranker.rank == 1) 80.dp else 70.dp
    val cardColor = when (ranker.rank) {
        1 -> Color(0xFFA7BEEE)
        2 -> Color(0xFFB3B6BB)
        3 -> Color(0xFFFFB9C9)
        else -> Color.Gray
    }
    val crownIcon = when (ranker.rank) {
        1 -> R.drawable.ic_crown_gold_64
        2 -> R.drawable.ic_crown_silver_64
        3 -> R.drawable.ic_crown_bronze_64
        else -> null
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // 왕관 (카드 위에)
        if (crownIcon != null) {
            Image(
                painter = painterResource(id = crownIcon),
                contentDescription = "왕관",
                modifier = Modifier
                    .size(32.dp)
                    .offset(y = (-8).dp) // 카드 위로 살짝 빠져나오게
            )
        }

        // 카드
        Card(
            modifier = Modifier.padding(top = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 프로필 이미지 영역
                AsyncImage(
                    model = ranker.profileUrl,
                    contentDescription = ranker.nickname,
                    placeholder = painterResource(id = R.drawable.ic_profile_holder),
                    error = painterResource(id = R.drawable.ic_profile_holder),
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 텍스트 정보 영역 (하단 전체, 더 진한 배경)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White.copy(alpha = 0.7f)) // 또는 Color(0xFFEEEEEE)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = ranker.nickname,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${ranker.tierCode.toTierNameKor()} ${ranker.tierLevel}",
                        style = Typography.bodySmall,
                        color = Gray
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${ranker.exp}P",
                        style = Typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}