package org.opensheetmusicdisplay.osmd.kotlin

import ai.onnxruntime.*
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

/**
 * ONNX Runtime 기반 피아노 피치 검출 엔진
 * GPU/NNAPI 가속 지원
 */
class OnnxInferenceEngine(
    private val context: Context,
    private val modelName: String = "onsets_velocities.onnx",
    private val useNNAPI: Boolean = true,
) {
    private var session: OrtSession? = null
    private var environment: OrtEnvironment? = null

    private val TAG = "OnnxEngine"

    /**
     * 모델 로드 및 세션 초기화
     */
    fun initialize(): Boolean {
        try {
            Log.i(TAG, "Initializing ONNX Runtime...")

            // ONNX Runtime 환경 생성
            environment = OrtEnvironment.getEnvironment()

            // 세션 옵션 설정
            val sessionOptions = OrtSession.SessionOptions().apply {
                // 스레드 수 설정 (멀티코어 활용)
                val numCores = Runtime.getRuntime().availableProcessors()
                val optimalThreads = maxOf(1, numCores - 1)
                setIntraOpNumThreads(optimalThreads)
                setInterOpNumThreads(optimalThreads)

                // 최적화 레벨 설정
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

                // NNAPI 가속 활성화 (GPU/NPU 사용)
                if (useNNAPI) {
                    try {
                        addNnapi()
                        Log.i(TAG, "NNAPI acceleration enabled (GPU/NPU)")
                    } catch (e: Exception) {
                        Log.w(TAG, "NNAPI not available, using CPU", e)
                    }
                }

                Log.i(TAG, "Using $optimalThreads CPU threads")
            }

            // 모델 파일 로드
            val modelFile = loadModelFromAssets()
            Log.i(TAG, "Model file: ${modelFile.absolutePath}")

            // 세션 생성
            session = environment!!.createSession(modelFile.absolutePath, sessionOptions)

            // 입출력 정보 확인
            val inputInfo = session!!.inputNames.joinToString()
            val outputInfo = session!!.outputNames.joinToString()
            Log.i(TAG, "ONNX session created successfully")
            Log.i(TAG, "  Input: $inputInfo")
            Log.i(TAG, "  Output: $outputInfo")

            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ONNX Runtime", e)
            return false
        }
    }

    /**
     * 추론 실행
     * @param melSpectrogram [T, M] 형태의 멜 스펙트로그램 (T=21, M=229)
     * @return [P, T-1] 형태의 onset 확률 배열 (P=88, 피아노 건반)
     */
    fun runInference(melSpectrogram: Array<FloatArray>): FloatArray? {
        val session = this.session ?: run {
            Log.e(TAG, "Session not initialized")
            return null
        }

        try {
            val T = melSpectrogram.size
            val M = melSpectrogram[0].size

            // [T, M] → [1, M, T] 형태로 변환 (모델 입력 형식)
            val flatData = FloatArray(M * T)
            for (t in 0 until T) {
                for (m in 0 until M) {
                    flatData[m * T + t] = melSpectrogram[t][m]
                }
            }

            // OnnxTensor 생성
            val inputTensor = OnnxTensor.createTensor(
                environment!!,
                FloatBuffer.wrap(flatData),
                longArrayOf(1, M.toLong(), T.toLong())
            )

            // 추론 실행
            val t0 = System.nanoTime()
            val outputs = session.run(mapOf("logmel_input" to inputTensor))
            val t1 = System.nanoTime()

            // 결과 추출 (첫 번째 출력: onset_probs)
            val outputTensor = outputs[0].value as Array<*>
            val onsetProbs = extractFloatArray(outputTensor)

            val inferenceTime = (t1 - t0) / 1_000_000.0  // ms
            Log.d(TAG, "Inference time: ${"%.2f".format(inferenceTime)}ms")

            // 리소스 정리
            inputTensor.close()
            outputs.close()

            return onsetProbs

        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            return null
        }
    }

    /**
     * OnnxValue에서 FloatArray 추출
     */
    private fun extractFloatArray(tensor: Any): FloatArray {
        return when (tensor) {
            is FloatArray -> tensor
            is Array<*> -> {
                // 다차원 배열을 1차원으로 평탄화
                val list = mutableListOf<Float>()
                flattenArray(tensor, list)
                list.toFloatArray()
            }
            else -> throw IllegalArgumentException("Unsupported tensor type: ${tensor::class.java}")
        }
    }

    /**
     * 다차원 배열 평탄화
     */
    private fun flattenArray(arr: Any, list: MutableList<Float>) {
        when (arr) {
            is Array<*> -> {
                for (item in arr) {
                    if (item != null) flattenArray(item, list)
                }
            }
            is FloatArray -> {
                list.addAll(arr.toList())
            }
            is Float -> {
                list.add(arr)
            }
        }
    }

    /**
     * Assets에서 모델 파일 로드
     */
    private fun loadModelFromAssets(): File {
        // 메인 ONNX 파일
        val mainModelFile = File(context.filesDir, modelName)
        context.assets.open(modelName).use { input ->
            FileOutputStream(mainModelFile).use { output ->
                input.copyTo(output)
            }
        }

        // .data 파일도 같이 복사 (external data)
        val dataFileName = "$modelName.data"
        try {
            val dataFile = File(context.filesDir, dataFileName)
            context.assets.open(dataFileName).use { input ->
                FileOutputStream(dataFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "Copied external data file: $dataFileName")
        } catch (e: Exception) {
            Log.d(TAG, "No external data file (embedded model)")
        }

        return mainModelFile
    }

    /**
     * 워밍업 (첫 추론은 느리므로 미리 실행)
     */
    fun warmup(T: Int = AudioConfig.WINDOW_T_FRAMES, M: Int = AudioConfig.MEL_BINS): Boolean {
        try {
            Log.i(TAG, "Warming up with zero tensor [$T, $M]...")
            val zeroMel = Array(T) { FloatArray(M) { 0f } }
            val result = runInference(zeroMel)
            return result != null
        } catch (e: Exception) {
            Log.e(TAG, "Warmup failed", e)
            return false
        }
    }

    /**
     * 리소스 해제
     */
    fun close() {
        try {
            session?.close()
            environment?.close()
            Log.i(TAG, "ONNX Runtime closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ONNX Runtime", e)
        }
    }
}
