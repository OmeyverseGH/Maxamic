package com.omeyverse.maxamic

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SAMPLE_RATE = 44100 // 44.1 kHz
        private const val PERMISSION_CODE = 200
    }

    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isEchoing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_CODE)
        } else {
            setupAudio()
        }

        val volumeControl = findViewById<SeekBar>(R.id.volumeControl)
        volumeControl.max = 100 // SeekBar range is 0 to 100
        volumeControl.progress = 100 // Default to max volume
        volumeControl.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioTrack?.setVolume(progress / 100.0f) // Set volume as a fraction of max
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupAudio() {
        try {
            audioRecord = AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            startEcho()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission required to access the microphone!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startEcho() {
        isEchoing = true
        Thread {
            val buffer = ByteArray(bufferSize)
            audioRecord?.startRecording()
            audioTrack?.play()

            while (isEchoing) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    audioTrack?.write(buffer, 0, read)
                }
            }

            audioRecord?.stop()
            audioTrack?.stop()
        }.start()
    }

    private fun stopEcho() {
        isEchoing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEcho()
        audioRecord?.release()
        audioTrack?.release()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // Call the superclass

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAudio()
            } else {
                Toast.makeText(this, "Microphone permission is required!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
