package com.d102.crescendo.presentation.ui.component.mysheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.d102.crescendo.R
import com.d102.crescendo.presentation.theme.*

// 장르 데이터 클래스
data class ArrangementGenre(
    val name: String,
    val code: String,
    val description: String
)

// 장르 목록
val arrangementGenres = listOf(
    ArrangementGenre(
        name = "재즈",
        code = "jazz",
        description = "Swing 리듬, 블루노트, 즉흥성"
    ),
    ArrangementGenre(
        name = "클래식",
        code = "classical",
        description = "안정적 리듬, 전통 화성, 레가토"
    ),
    ArrangementGenre(
        name = "팝",
        code = "pop",
        description = "4/4 비트, 캐치한 멜로디, 간단한 화성"
    ),
    ArrangementGenre(
        name = "보사노바",
        code = "bossa_nova",
        description = "라틴 리듬, 재즈 화성, 부드러운 멜로디"
    ),
    ArrangementGenre(
        name = "왈츠",
        code = "waltz",
        description = "3/4 박자, 우아한 멜로디"
    ),
    ArrangementGenre(
        name = "스윙",
        code = "swing",
        description = "강한 스윙 리듬, 빅밴드 사운드"
    ),
    ArrangementGenre(
        name = "바로크",
        code = "baroque",
        description = "대위법, 장식음, 푸가 패턴"
    )
)

@Composable
fun ArrangementGenreDialog(
    onDismiss: () -> Unit,
    onGenreSelected: (String, String) -> Unit // (genreCode, genreName) 전달
) {
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedGenreName by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 타이틀 영역
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            painter = painterResource(R.drawable.ic_ai),
                            contentDescription = "AI",
                            tint = DarkHover,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 악보 편곡",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkHover
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "닫기",
                            tint = Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 설명 텍스트
                Text(
                    text = "원하는 장르를 선택하면 AI가 악보를 자동으로 편곡해드립니다.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 장르 리스트
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(arrangementGenres) { genre ->
                        GenreListItem(
                            genre = genre,
                            isSelected = selectedGenre == genre.code,
                            onClick = {
                                selectedGenre = genre.code
                                selectedGenreName = genre.name
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 편곡하기 버튼
                Button(
                    onClick = {
                        selectedGenre?.let { code ->
                            selectedGenreName?.let { name ->
                                onGenreSelected(code, name)
                            }
                        }
                    },
                    enabled = selectedGenre != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkHover,
                        disabledContainerColor = Gray3
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        text = "편곡하기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedGenre != null) White else Gray
                    )
                }
            }
        }
    }
}

@Composable
fun GenreListItem(
    genre: ArrangementGenre,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Normal.copy(alpha = 0.1f) else Gray3)
            .border(
                width = 2.dp,
                color = if (isSelected) Normal else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = genre.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) DarkHover else Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = genre.description,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray,
                lineHeight = 18.sp
            )
        }

        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "선택됨",
                tint = Normal,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}