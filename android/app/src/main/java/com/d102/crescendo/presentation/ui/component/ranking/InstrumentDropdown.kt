package com.d102.crescendo.presentation.ui.component.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d102.crescendo.domain.model.onboarding.Instrument
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.presentation.theme.White

/**
 * 랭킹 화면용 악기 선택 드랍다운
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentDropdown(
    instruments: List<Instrument>, // (ProfileViewModel에서 공유)
    selectedInstrumentId: Int,
    onInstrumentSelected: (Int) -> Unit, // (RankingViewModel로 이벤트 전달)
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val selectedInstrumentName = remember(instruments, selectedInstrumentId) {
        instruments.find { it.id == selectedInstrumentId }?.korName ?: "악기 선택"
    }

    Box (
        modifier = modifier.fillMaxWidth(),  // 전체 폭 사용
        contentAlignment = Alignment.CenterStart
    ) {
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = it },
            modifier = Modifier.width(200.dp)  // 원하는 폭으로 제한
        ) {
            // 선택된 항목을 보여주는 TextField (ReadOnly)
            TextField(
                value = selectedInstrumentName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Gray3,
                    unfocusedContainerColor = Gray3,
                    disabledContainerColor = Gray3,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),  // 둥근 모서리
                textStyle = Typography.bodyLarge,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(56.dp)  // 높이 제한
            )

            // 펼쳐지는 메뉴
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                modifier = Modifier.background(White)

            ) {
                instruments.forEach { instrument ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = instrument.korName,
                                style = Typography.bodyMedium
                            )
                        },
                        onClick = {
                            onInstrumentSelected(instrument.id)
                            isExpanded = false
                        },
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        )  // 패딩 줄임
                    )
                }
            }
        }
    }
}