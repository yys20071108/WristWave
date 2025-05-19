package com.wristwave.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wristwave.app.databinding.ActivityRecordingBinding
import com.wristwave.app.model.MediaItem
import com.wristwave.app.utils.PreferenceManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RecordingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRecordingBinding
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPlaying = false
    private var currentRecordingFile: File? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            try {
                if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    val duration = mediaPlayer?.duration ?: 1
                    val progress = (currentPosition.toFloat() / duration.toFloat() * 100).toInt()
                    binding.seekBar.progress = progress
                    
                    // 更新时间显示
                    binding.tvCurrentTime.text = formatTime(currentPosition)
                    binding.tvTotalTime.text = formatTime(duration)
                }
                handler.postDelayed(this, 1000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private var currentPlaylist = ArrayList<MediaItem>()
    private var currentIndex = 0
    private var recordingStartTime: Long = 0
    private var recordingTimer: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化UI
        initUI()
        
        // 设置监听器
        setupListeners()
        
        // 开始更新进度条
        handler.postDelayed(updateSeekBar, 1000)
        
        // 检查录音权限
        checkRecordPermission()
    }
    
    private fun initUI() {
        // 初始化空播放列表
        refreshPlaylistUI()
        
        // 设置录音格式
        val prefs = PreferenceManager(this)
        val recordingFormat = prefs.getRecordingFormat()
        binding.tvRecordingFormat.text = getString(R.string.recording_format) + ": ${recordingFormat.toUpperCase(Locale.ROOT)}"
    }
    
    private fun setupListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        
        // 录音按钮
        binding.btnRecord.setOnClickListener {
            if (isPlaying) {
                stopPlayback()
            }
            
            if (isRecording) {
                stopRecording()
            } else {
                if (checkRecordPermission()) {
                    startRecording()
                }
            }
        }
        
        // 播放/暂停按钮
        binding.btnPlayPause.setOnClickListener {
            if (isRecording) {
                Toast.makeText(this, "请先停止录音", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (currentPlaylist.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_no_media), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (isPlaying) {
                pausePlayback()
            } else {
                resumePlayback()
            }
        }
        
        // 下一个按钮
        binding.btnNext.setOnClickListener {
            if (isRecording) {
                Toast.makeText(this, "请先停止录音", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (currentPlaylist.isEmpty()) return@setOnClickListener
            playNext()
        }
        
        // 上一个按钮
        binding.btnPrevious.setOnClickListener {
            if (isRecording) {
                Toast.makeText(this, "请先停止录音", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (currentPlaylist.isEmpty()) return@setOnClickListener
            playPrevious()
        }
        
        // 进度条
        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    val duration = mediaPlayer?.duration ?: 0
                    val newPosition = duration * progress / 100
                    mediaPlayer?.seekTo(newPosition)
                    binding.tvCurrentTime.text = formatTime(newPosition)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // 播放列表按钮
        binding.btnPlaylist.setOnClickListener {
            togglePlaylistVisibility()
        }
    }
    
    private fun togglePlaylistVisibility() {
        if (binding.playlistContainer.visibility == View.VISIBLE) {
            binding.playlistContainer.visibility = View.GONE
        } else {
            binding.playlistContainer.visibility = View.VISIBLE
        }
    }
    
    private fun refreshPlaylistUI() {
        binding.playlistContainer.removeAllViews()
        
        if (currentPlaylist.isEmpty()) {
            val emptyView = layoutInflater.inflate(R.layout.item_empty_playlist, binding.playlistContainer, false)
            binding.playlistContainer.addView(emptyView)
        } else {
            for ((index, item) in currentPlaylist.withIndex()) {
                val itemView = layoutInflater.inflate(R.layout.item_playlist, binding.playlistContainer, false)
                val titleText = itemView.findViewById<android.widget.TextView>(R.id.tvTitle)
                val iconView = itemView.findViewById<android.widget.ImageView>(R.id.ivIcon)
                
                titleText.text = item.name
                iconView.setImageResource(R.drawable.ic_mic)
                
                // 高亮当前正在播放的项
                if (index == currentIndex && isPlaying) {
                    titleText.setTextColor(getColor(R.color.accent))
                }
                
                itemView.setOnClickListener {
                    if (isRecording) {
                        Toast.makeText(this, "请先停止录音", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    currentIndex = index
                    playRecording(item)
                }
                
                binding.playlistContainer.addView(itemView)
            }
        }
    }
    
    private fun checkRecordPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_PERMISSION_CODE
            )
            return false
        }
        return true
    }
    
    private fun startRecording() {
        try {
            // 创建录音目录
            val recordingDir = File(getExternalFilesDir(null), "Recordings")
            if (!recordingDir.exists()) {
                recordingDir.mkdirs()
            }
            
            // 准备文件
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val prefs = PreferenceManager(this)
            val format = prefs.getRecordingFormat()
            currentRecordingFile = File(recordingDir, "REC_$timestamp.$format")
            
            // 初始化MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(
                    if (format == "mp3") MediaRecorder.OutputFormat.MPEG_4
                    else MediaRecorder.OutputFormat.THREE_GPP
                )
                setAudioEncoder(
                    if (format == "mp3") MediaRecorder.AudioEncoder.AAC
                    else MediaRecorder.AudioEncoder.AMR_NB
                )
                setOutputFile(currentRecordingFile?.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            
            // 更新UI
            binding.btnRecord.setImageResource(R.drawable.ic_stop)
            binding.tvStatusRecording.visibility = View.VISIBLE
            binding.tvRecordingTime.visibility = View.VISIBLE
            binding.btnPlayPause.isEnabled = false
            binding.btnNext.isEnabled = false
            binding.btnPrevious.isEnabled = false
            
            // 开始计时
            startRecordingTimer()
            
            Toast.makeText(this, "开始录音", Toast.LENGTH_SHORT).show()
            
        } catch (e: IOException) {
            Toast.makeText(this, "录音失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            isRecording = false
        }
    }
    
    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            // 停止计时
            recordingTimer?.let { handler.removeCallbacks(it) }
            recordingTimer = null
            
            // 添加到播放列表
            currentRecordingFile?.let { file ->
                val uri = Uri.fromFile(file)
                val newRecording = MediaItem(
                    id = System.currentTimeMillis(),
                    type = "recording",
                    name = file.name,
                    uri = uri
                )
                currentPlaylist.add(newRecording)
                currentIndex = currentPlaylist.size - 1
                
                Toast.makeText(this, "录音已保存", Toast.LENGTH_SHORT).show()
            }
            
            // 更新UI
            isRecording = false
            binding.btnRecord.setImageResource(R.drawable.ic_mic)
            binding.tvStatusRecording.visibility = View.GONE
            binding.tvRecordingTime.visibility = View.GONE
            binding.btnPlayPause.isEnabled = true
            binding.btnNext.isEnabled = true
            binding.btnPrevious.isEnabled = true
            
            // 刷新播放列表
            refreshPlaylistUI()
            
        } catch (e: Exception) {
            Toast.makeText(this, "停止录音失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun startRecordingTimer() {
        recordingTimer = object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - recordingStartTime
                binding.tvRecordingTime.text = formatTime(elapsedTime.toInt())
                handler.postDelayed(this, 1000)
            }
        }
        recordingTimer?.let { handler.post(it) }
    }
    
    private fun playRecording(item: MediaItem) {
        try {
            // 释放之前的MediaPlayer
            releaseMediaPlayer()
            
            // 创建新的MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, item.uri)
                setOnPreparedListener {
                    it.start()
                    isPlaying = true
                    updatePlayPauseButton()
                    
                    // 更新UI
                    binding.tvRecordingTitle.text = item.name
                    binding.tvTotalTime.text = formatTime(it.duration)
                    binding.seekBar.progress = 0
                }
                setOnCompletionListener {
                    isPlaying = false
                    updatePlayPauseButton()
                }
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(
                        this@RecordingActivity,
                        "播放出错: $what, $extra",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                prepareAsync()
            }
            
            // 刷新播放列表 UI
            refreshPlaylistUI()
            
        } catch (e: IOException) {
            Toast.makeText(this, "无法播放录音: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun playNext() {
        if (currentPlaylist.isEmpty()) return
        
        // 播放下一个
        if (currentIndex < currentPlaylist.size - 1) {
            currentIndex++
        } else {
            // 如果是最后一个，回到第一个
            currentIndex = 0
        }
        
        playRecording(currentPlaylist[currentIndex])
    }
    
    private fun playPrevious() {
        if (currentPlaylist.isEmpty()) return
        
        // 播放上一个
        if (currentIndex > 0) {
            currentIndex--
        } else {
            // 如果是第一个，跳到最后一个
            currentIndex = currentPlaylist.size - 1
        }
        
        playRecording(currentPlaylist[currentIndex])
    }
    
    private fun pausePlayback() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayPauseButton()
    }
    
    private fun resumePlayback() {
        if (mediaPlayer == null && currentPlaylist.isNotEmpty()) {
            playRecording(currentPlaylist[currentIndex])
        } else {
            mediaPlayer?.start()
            isPlaying = true
            updatePlayPauseButton()
        }
    }
    
    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        isPlaying = false
        updatePlayPauseButton()
    }
    
    private fun updatePlayPauseButton() {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }
    
    private fun formatTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == RECORD_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "录音权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "录音权限被拒绝，录音功能无法使用", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        if (isRecording) {
            stopRecording()
        }
        
        releaseMediaPlayer()
        handler.removeCallbacks(updateSeekBar)
        recordingTimer?.let { handler.removeCallbacks(it) }
    }
    
    companion object {
        private const val RECORD_PERMISSION_CODE = 200
    }
}