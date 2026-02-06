package com.debategpt.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder {

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val WARMUP_MS = 400
        private const val MIN_RECORDING_BYTES = 64000
        private const val GAIN = 2.5f
    }

    private var audioRecord: AudioRecord? = null
    private var outputStream: FileOutputStream? = null
    private var tempRawFile: File? = null
    private val stopRequested = AtomicBoolean(false)
    @Volatile
    private var recordingThread: Thread? = null

    @SuppressLint("MissingPermission")
    fun startRecording(context: Context? = null): Boolean {
        stopRequested.set(false)
        context?.let { ctx ->
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.apply {
                mode = AudioManager.MODE_IN_COMMUNICATION
                isSpeakerphoneOn = false
            }
        }
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) return false

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 3
        )
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) return false

        tempRawFile = File.createTempFile("debate_", ".raw")
        outputStream = FileOutputStream(tempRawFile)
        audioRecord?.startRecording()

        recordingThread = Thread {
            val data = ShortArray(bufferSize)
            val output = outputStream!!
            val warmupSamples = (SAMPLE_RATE * WARMUP_MS / 1000).toInt()
            var totalRead = 0
            try {
                while (!stopRequested.get() && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(data, 0, bufferSize) ?: -1
                    if (read > 0) {
                        totalRead += read
                        if (totalRead >= warmupSamples) {
                            val shorts = ShortArray(read) { data[it] }
                            output.write(bytesToByteArray(shorts), 0, shorts.size * 2)
                        }
                    }
                }
            } finally {
                output.close()
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                outputStream = null
            }
        }.apply { start() }
        return true
    }

    suspend fun stopRecording(): Result<File> = withContext(Dispatchers.IO) {
        stopRequested.set(true)
        recordingThread?.join(5000)
        recordingThread = null

        val rawFile = tempRawFile ?: return@withContext Result.failure(Exception("No recording"))
        tempRawFile = null

        try {
            if (!rawFile.exists() || rawFile.length() < MIN_RECORDING_BYTES) {
                return@withContext Result.failure(Exception("Record at least 2 seconds of speech before stopping"))
            }
            val wavFile = File.createTempFile("debate_", ".wav")
            rawToWav(rawFile, wavFile)
            rawFile.delete()
            Result.success(wavFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun recordToWav(durationMs: Long): Result<File> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile("debate_", ".raw")
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            var ar = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize * 2)
            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                ar.release()
                ar = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize * 2)
            }
            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                ar.release()
                ar = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize * 2)
            }
            val audioRecord = ar

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("AudioRecord init failed"))
            }

            audioRecord.startRecording()
            val data = ShortArray(bufferSize)
            val output = FileOutputStream(tempFile)
            val endTime = System.currentTimeMillis() + durationMs

            try {
                while (System.currentTimeMillis() < endTime) {
                    val read = audioRecord.read(data, 0, bufferSize)
                    if (read > 0) {
                        val shorts = ShortArray(read) { data[it] }
                        val bytes = bytesToByteArray(shorts)
                        output.write(bytes, 0, bytes.size)
                    }
                }
            } finally {
                audioRecord.stop()
                audioRecord.release()
                output.close()
            }

            val wavFile = File.createTempFile("debate_", ".wav")
            rawToWav(tempFile, wavFile)
            tempFile.delete()
            Result.success(wavFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun rawToWav(rawFile: File, wavFile: File) {
        val rawBytes = rawFile.readBytes()
        val samples = rawBytes.size / 2
        val boosted = ByteArray(rawBytes.size)
        for (i in 0 until samples) {
            val lo = rawBytes[i * 2].toInt() and 0xff
            val hi = rawBytes[i * 2 + 1].toInt() and 0xff
            var s = (lo or (hi shl 8)).toShort().toInt()
            if (s >= 0x8000) s -= 0x10000
            var amplified = (s * GAIN).toInt()
            if (amplified > 32767) amplified = 32767
            if (amplified < -32768) amplified = -32768
            val out = amplified and 0xffff
            boosted[i * 2] = (out and 0xff).toByte()
            boosted[i * 2 + 1] = (out shr 8).toByte()
        }
        val totalDataLen = boosted.size + 36
        val byteRate = SAMPLE_RATE * 2

        FileOutputStream(wavFile).use { out ->
            out.write("RIFF".toByteArray(), 0, 4)
            out.write(intToByteArray(totalDataLen), 0, 4)
            out.write("WAVE".toByteArray(), 0, 4)
            out.write("fmt ".toByteArray(), 0, 4)
            out.write(intToByteArray(16), 0, 4)
            out.write(shortToByteArray(1), 0, 2)
            out.write(shortToByteArray(1), 0, 2)
            out.write(intToByteArray(SAMPLE_RATE), 0, 4)
            out.write(intToByteArray(byteRate), 0, 4)
            out.write(shortToByteArray(2), 0, 2)
            out.write(shortToByteArray(16), 0, 2)
            out.write("data".toByteArray(), 0, 4)
            out.write(intToByteArray(boosted.size), 0, 4)
            out.write(boosted, 0, boosted.size)
        }
    }

    private fun bytesToByteArray(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            bytes[i * 2] = (shorts[i].toInt() and 0xff).toByte()
            bytes[i * 2 + 1] = (shorts[i].toInt() shr 8 and 0xff).toByte()
        }
        return bytes
    }

    private fun intToByteArray(value: Int): ByteArray =
        byteArrayOf(
            (value and 0xff).toByte(),
            (value shr 8 and 0xff).toByte(),
            (value shr 16 and 0xff).toByte(),
            (value shr 24 and 0xff).toByte()
        )

    private fun shortToByteArray(value: Int): ByteArray =
        byteArrayOf((value and 0xff).toByte(), (value shr 8 and 0xff).toByte())
}
