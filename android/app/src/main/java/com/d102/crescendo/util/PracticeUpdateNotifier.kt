package com.d102.crescendo.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 연주(Practice/Evaluation)가 완료되었음을 앱 전역에 알리는 Notifier
 */
@Singleton
class PracticeUpdateNotifier @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>()
    val events = _events.asSharedFlow()

    /**
     * 연주가 저장되었음을 알리는 신호
     */
    suspend fun notifyPracticeSaved() {
        _events.emit(Unit)
    }
}