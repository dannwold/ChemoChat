package com.example.chemochat

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Helper class to handle audio recording and playback.
 */
object AudioHandler {
    private const val TAG = "AudioHandler"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    fun startRecording(context: Context, outputFile: File) {
        stopRecording()
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed", e)
            }
        }
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                Log.e(TAG, "stop() failed", e)
            }
            release()
        }
        mediaRecorder = null
    }

    fun startPlayback(file: File, onFinished: () -> Unit) {
        stopPlayback()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    onFinished()
                    stopPlayback()
                }
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed", e)
            }
        }
    }

    fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }
}
