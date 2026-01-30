package com.d102.crescendo.util

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

@Composable
fun rememberDownloadHandler(
    context: Context
): Pair<(url: String, title: String) -> Unit, () -> Unit> {

    var currentDownloadId by remember { mutableStateOf<Long?>(null) }
    val dm = remember { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    // 권한 런처 (API 28 이하)
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "저장소 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
        // 권한 콜백에서 즉시 다운로드 재시작 로직은 단순화를 위해 생략
    }

    // 완료 브로드캐스트 리시버
    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id != -1L && id == currentDownloadId) {
                        // 상태 체크 (성공/실패)
                        val query = DownloadManager.Query().setFilterById(id)
                        dm.query(query)?.use { c: Cursor ->
                            if (c.moveToFirst()) {
                                val statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                val status = c.getInt(statusIdx)
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    Toast.makeText(context, "다운로드 완료!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "다운로드 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        // 한 번 처리 후 id 초기화(선택)
                        currentDownloadId = null
                    }
                }
            }
        }
    }

    // 리시버 등록/해제
    DisposableEffect(Unit) {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    fun startDownload(url: String, title: String) {
        if (url.isBlank()) {
            Toast.makeText(context, "다운로드 URL이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // API 28 이하 권한 확인
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // WRITE_EXTERNAL_STORAGE 권한이 매니페스트에 있어야 합니다.
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // (권한 허용 후 다시 누르게 유도하는 단순 플로우)
        }

        val uri = Uri.parse(url)
        val guessedName = uri.lastPathSegment?.substringAfterLast("/") ?: "sheet"
        val fileName = if (guessedName.contains('.')) guessedName else "$guessedName.mxl"

        val request = DownloadManager.Request(uri)
            .setTitle("$title")
            .setDescription("악보 다운로드 중…")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // 공개 Downloads 폴더에 저장 (사용자가 바로 확인 가능)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        try {
            val id = dm.enqueue(request)
            currentDownloadId = id
            Toast.makeText(context, "악보 파일이 저장되었습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "다운로드 요청 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    return Pair(::startDownload, { /* 외부에서 추가 정리할 것 없음 */ })
}
