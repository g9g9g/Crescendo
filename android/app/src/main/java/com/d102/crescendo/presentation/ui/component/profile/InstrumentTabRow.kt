package com.d102.crescendo.presentation.ui.component.profile

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.GrayLine
import com.d102.crescendo.presentation.theme.Normal
import com.d102.crescendo.presentation.theme.Typography
import com.d102.crescendo.presentation.theme.White

@Composable
fun InstrumentTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = White,
        contentColor = Normal,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                height = 3.dp,
                color = Normal
            )
        },
        divider = {
            HorizontalDivider(color = GrayLine, thickness = 3.dp)
        }
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = (selectedTabIndex == index)

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = Typography.headlineMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Normal else Gray
                    )
                },
                interactionSource = remember { MutableInteractionSource() },
                selectedContentColor = Color.Transparent,
                unselectedContentColor = Color.Transparent,
            )
        }
    }
}