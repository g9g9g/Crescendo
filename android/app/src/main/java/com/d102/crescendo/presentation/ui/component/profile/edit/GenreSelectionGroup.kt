package com.d102.crescendo.presentation.ui.component.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.Dark
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.presentation.theme.White

@Composable
fun GenreSelectionGroup(
    availableTags: List<String>,
    selectedTags: Set<String>,
    onTagClicked: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp) // 줄(Row) 사이의 세로 간격
    ) {
        availableTags.chunked(3).forEach { rowItems ->
            // 3개의 아이템을 담을 Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp), // 칩(Column) 사이의 가로 간격
                modifier = Modifier.fillMaxWidth()
            ) {
                // 3개의 열(Column)을 만듬
                for (i in 0 until 3) {
                    Box(modifier = Modifier.weight(1f)) { // 👈 1. 1:1:1 비율로 공간 차지
                        if (i < rowItems.size) {
                            // 현재 인덱스(i)에 해당하는 태그가 있으면 칩 생성
                            val tag = rowItems[i]
                            val isSelected = tag in selectedTags

                            FilterChip(
                                selected = isSelected,
                                onClick = { onTagClicked(tag) },
                                label = {
                                    Text(
                                        text = tag,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        // 칩 내부 텍스트도 중앙 정렬
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Gray3,
                                    labelColor = Gray,
                                    selectedContainerColor = DarkHover,
                                    selectedLabelColor = White
                                ),
                                border = null,
                                modifier = Modifier.fillMaxWidth().height(40.dp) // 칩이 1f 공간을 꽉 채우도록
                            )
                        } else {
                            // 태그가 없으면 (예: 마지막 줄) 빈 공간으로 남겨둠
                            // (Spacer를 두지 않아야 weight(1f)가 깨지지 않음)
                        }
                    }
                }
            }
        }
    }
}