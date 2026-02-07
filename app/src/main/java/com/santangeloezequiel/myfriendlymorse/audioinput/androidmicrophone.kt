package com.santangeloezequiel.myfriendlymorse.audioinput

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat


class MicrophoneInput(
    private val context: Context,
    private val sampleRate: Int,
    private val bufferSize: Int,
    private val onBuffer: (FloatArray) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null
    @Volatile private var running = false

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val internalBufferSize = maxOf(minBuffer, bufferSize * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            internalBufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) return

        running = true
        audioRecord!!.startRecording()

        audioThread = Thread {
            val buffer = ShortArray(bufferSize)
            while (running && audioRecord != null) {
                val read = audioRecord!!.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val floatBuffer = FloatArray(read) { buffer[it] / 32768f }
                    onBuffer(floatBuffer)
                }
            }
        }.apply { start() }
    }

    fun stop() {
        running = false

        try {
            audioThread?.join()
        } catch (_: InterruptedException) {}

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}

        audioRecord = null
        audioThread = null
    }
}
