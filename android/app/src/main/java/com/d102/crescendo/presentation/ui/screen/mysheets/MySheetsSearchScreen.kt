package com.d102.crescendo.presentation.ui.screen.mysheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d102.crescendo.R
import com.d102.crescendo.domain.model.sheet.MySheet
import com.d102.crescendo.presentation.theme.Black
import com.d102.crescendo.presentation.theme.DarkHover
import com.d102.crescendo.presentation.theme.Gray
import com.d102.crescendo.presentation.theme.Gray3
import com.d102.crescendo.presentation.theme.White
import com.d102.crescendo.util.getGenreName
import com.d102.crescendo.util.getInstrumentDrawable
import com.d102.crescendo.util.matchThumbnail

@Composable
fun MySheetsSearchScreen(
    viewModel: MySheetSearchViewModel = hiltViewModel(),
    onNavigateToDetail: (Int, Boolean) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchMode by viewModel.isSearchMode.collectAsState()
    val focusManager = LocalFocusManager.current

    val imePaddingValues = WindowInsets.ime.asPaddingValues()
    val bottomImePadding = imePaddingValues.calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                onClick = {
                    viewModel.clearSuggestions()
                    focusManager.clearFocus()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 검색 입력 필드
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.clickable(
                        onClick = { /* 이벤트 전파 방지 */ },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                ) {
                    // 검색 바
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { newValue ->
                            viewModel.updateSearchQuery(newValue)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .background(
                                color = White,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = Black
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.executeSearch()
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sheet_search),
                                    contentDescription = "검색",
                                    tint = Black,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.text.isEmpty()) {
                                        Text(
                                            text = "제목, 작곡가, 장르로 검색해보세요",
                                            color = Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }

                                // X 버튼 (검색 모드일 때만 표시)
                                if (isSearchMode && searchQuery.text.isNotEmpty()) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_topbar_close),
                                        contentDescription = "검색 취소",
                                        tint = Gray,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable {
                                                viewModel.clearSearch()
                                                focusManager.clearFocus()
                                            }
                                    )
                                }
                            }
                        }
                    )

                    // 자동완성 제안 목록 (검색 모드가 아닐 때 표시)
                    if (!isSearchMode && suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = White
                            )
                        ) {
                            LazyColumn {
                                items(suggestions) { suggestion ->
                                    SearchSuggestionItem(
                                        text = suggestion,
                                        onClick = {
                                            viewModel.selectSuggestion(suggestion)
                                            focusManager.clearFocus()
                                        }
                                    )

                                    if (suggestion != suggestions.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = Gray.copy(alpha = 0.3f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 검색 결과 리스트 (검색 모드일 때만 표시)
            if (isSearchMode && suggestions.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                onClick = { /* 이벤트 전파 방지 */ },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "검색 결과가 없습니다.",
                            color = Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(
                            bottom = 16.dp + bottomImePadding
                        ),
                        modifier = Modifier
                            .clickable(
                                onClick = { /* 이벤트 전파 방지 */ },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                    ) {
                        items(searchResults) { sheet ->
                            MySheetSearchResultItem(
                                sheet = sheet,
                                onClick = {
                                    val isCompleted = sheet.progressRate >= 100
                                    onNavigateToDetail(sheet.userSheetId, isCompleted)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSuggestionItem(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sheet_search),
            contentDescription = null,
            tint = DarkHover,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Black
        )
    }
}

@Composable
fun MySheetSearchResultItem(
    sheet: MySheet,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 썸네일
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gray3)
            ) {
                // 썸네일 이미지
                Icon(
                    painter = painterResource(
                        matchThumbnail(title = sheet.title)
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified
                )

                // 악기 아이콘
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(20.dp)
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(getInstrumentDrawable(sheet.instrumentId)),
                        contentDescription = null,
                        tint = DarkHover,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = sheet.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${sheet.composer ?: "Unknown"} • ${getGenreName(sheet.genreId)}",
                    fontSize = 12.sp,
                    color = Gray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 구분선
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Gray3,
            thickness = 1.dp
        )
    }
}