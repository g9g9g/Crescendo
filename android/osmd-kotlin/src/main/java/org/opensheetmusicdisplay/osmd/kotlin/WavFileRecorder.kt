package org.opensheetmusicdisplay.osmd.kotlin

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val TAG = "Crescendo_WavFileRecorder"

/**
 * MicPitchDetector에 WAV 파일 저장 기능을 추가한 클래스.
 *
 * @param context Context (파일 저장을 위해 필요)
 */
class WavFileRecorder(
    private val context: Context,
    private val sampleRate: Int = 44100,
    private val bufferSizeInFrames: Int = 2048,
) {
    @Volatile private var running = false
    private var thread: Thread? = null
    private var recorder: AudioRecord? = null

    // WAV 파일 저장 관련
    private var outputStream: FileOutputStream? = null
    private var recordingFile: File? = null
    private var totalBytesWritten: Long = 0

    /**
     * 녹음 시작
     * @param isPracticeMode 'false'일 경우 (평가 모드) 파일 저장을 활성화.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(isPracticeMode: Boolean) {
        if (running) return

        // 1. [수정] 우리가 'loop'에서 처리할 고정 버퍼 크기
        val processingBufSize = bufferSizeInFrames * 2 // (예: 4096 바이트)

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // 2. AudioRecord는 OS 최소 요구사항과 우리 버퍼 중 '더 큰 값'을 사용
        val recorderBufSizeInBytes = maxOf(minBuf, processingBufSize)

        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recorderBufSizeInBytes
        )

        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord 초기화 실패")
            stop()
            return
        }

        // --- 평가 모드일 때만 파일 스트림 준비 ---
        if (!isPracticeMode) {
            try {
                // 앱 내부 캐시 디렉토리에 파일 생성
                val outputFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav")
                outputStream = FileOutputStream(outputFile)
                recordingFile = outputFile
                totalBytesWritten = 0

                // WAV 헤더를 위한 공간(44바이트)을 비워두고 시작
                outputStream?.write(ByteArray(44))
                Log.d(TAG, "평가 모드: WAV 파일 저장 시작. 경로: ${outputFile.absolutePath}")

            } catch (e: IOException) {
                Log.e(TAG, "파일 스트림 생성 실패", e)
                outputStream = null
                recordingFile = null
            }
        } else {
            Log.d(TAG, "연습 모드: 파일 저장을 활성화하지 않음.")
        }
        // --- ---

        running = true
        recorder?.startRecording()
        // [!!] loop에 바이트 단위 버퍼 크기 전달
        thread = Thread { loop(processingBufSize) }.also { it.start() }
        Log.d(TAG, "녹음 스레드 시작 (AudioRecord=$recorderBufSizeInBytes, Processing=$processingBufSize)")}

    /**
     * 녹음 중지
     * @return 저장된 파일의 Uri (평가 모드였고 저장이 성공한 경우), 그 외에는 null.
     */
    fun stop(): Uri? {
        running = false
        try {
            thread?.join(300) // 루프가 정상 종료되기를 기다림
        } catch (e: InterruptedException) {
            Log.e(TAG, "스레드 join 중단", e)
        }
        thread = null

        try {
            if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder?.stop()
            }
            recorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord 중지/해제 실패", e)
        }
        recorder = null

        // --- 평가 모드였다면 파일 후처리 ---
        var resultUri: Uri? = null
        if (outputStream != null && recordingFile != null) {
            try {
                outputStream?.close() // 스트림을 먼저 닫고
                // 녹음이 완료된 후, 파일 시작 부분에 실제 WAV 헤더를 덮어쓴다.
                writeWavHeader(recordingFile!!, totalBytesWritten)
                resultUri = Uri.fromFile(recordingFile) // Uri.fromFile() 사용
                Log.d(TAG, "WAV 파일 저장 완료. Uri: $resultUri, 크기: $totalBytesWritten 바이트")
            } catch (e: IOException) {
                Log.e(TAG, "WAV 헤더 쓰기 또는 파일 닫기 실패", e)
                resultUri = null
            }
        }
        // --- ---

        outputStream = null
        recordingFile = null
        totalBytesWritten = 0

        return resultUri // 저장된 파일의 Uri 반환
    }

    private fun loop(bufSizeInBytes: Int) {
        val rec = recorder ?: return

        // 모든 버퍼가 'bufSizeInBytes' (4096) 기준으로 생성됨
        val byteBuf = ByteArray(bufSizeInBytes)      // 4096

        while (running) {
            // 2. 'byteBuf' (4096) 크기만큼 오디오 데이터 읽기
            val bytesRead = rec.read(byteBuf, 0, byteBuf.size)
            if (bytesRead <= 0) continue

            if (outputStream != null) {
                try {
                    // AudioRecord에서 읽은 byteBuf를 그대로 파일에 쓴다
                    outputStream?.write(byteBuf, 0, bytesRead)
                    totalBytesWritten += bytesRead
                } catch (e: IOException) {
                    Log.e(TAG, "파일 쓰기 중 오류 발생", e)
                    // 오류 발생 시 녹음 중단
                    running = false
                }
            }
        }
    }

    // WAV 파일 헤더를 작성하는 헬퍼 함수
    @Throws(IOException::class)
    private fun writeWavHeader(file: File, totalAudioLen: Long) {
        val totalDataLen = totalAudioLen + 36 // 헤더 크기(44) - 8 (RIFF, WAVE)
        val channels = 1 // MONO
        val bitDepth: Short = 16 // PCM_16BIT
        val byteRate = (sampleRate * channels * (bitDepth / 8)).toLong()

        val header = ByteArray(44)

        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte(); header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte(); header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte() // 'fmt ' chunk
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // Sub-chunk 1 size (16 for PCM)
        header[20] = 1; header[21] = 0 // Audio format (1 for PCM)
        header[22] = channels.toByte(); header[23] = 0 // Number of channels
        header[24] = (sampleRate and 0xff).toByte(); header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte(); header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte(); header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte(); header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * (bitDepth / 8)).toByte(); header[33] = 0 // Block align
        header[34] = bitDepth.toByte(); header[35] = 0 // Bits per sample
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte(); header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte(); header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        // RandomAccessFile을 "rw" (읽기/쓰기) 모드로 개방
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0) // 파일의 맨 처음(0)으로 이동
            raf.write(header) // 44바이트 헤더를 덮어씁니다.
        }
    }
}