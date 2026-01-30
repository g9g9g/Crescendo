package com.d102.crescendo.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로필 데이터가 변경되었음을 앱 전역에 알리는 Notifier
 */
@Singleton
class ProfileUpdateNotifier @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>()
    val events = _events.asSharedFlow()

    /**
     * 프로필이 수정되었음을 알리는 신호
     */
    suspend fun notifyProfileUpdated() {
        _events.emit(Unit)
    }
}