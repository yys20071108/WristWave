package com.wristwave.app

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.wristwave.app.databinding.ActivityMusicPlayerBinding
import com.wristwave.app.model.MediaItem
import com.wristwave.app.utils.PreferenceManager
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MusicPlayerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMusicPlayerBinding
    private var mediaPlayer: MediaPlayer? = null
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
    private var isPlaying = false
    private var isShuffle = false
    private var repeatMode = RepeatMode.OFF
    private var volume = 50
    private var isMuted = false
    
    // 用于存储之前的音量，静音时使用
    private var previousVolume = 50
    
    // 重复模式枚举
    enum class RepeatMode {
        OFF, ALL, ONE
    }
    
    // 文件选择结果处理
    private val openMusicFile = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val newPlaylist = ArrayList<MediaItem>()
            
            for ((index, uri) in uris.withIndex()) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                
                val fileName = getFileNameFromUri(uri) ?: "未知音乐 ${index + 1}"
                val mediaItem = MediaItem(
                    id = System.currentTimeMillis() + index,
                    type = "music",
                    name = fileName,
                    uri = uri
                )
                newPlaylist.add(mediaItem)
            }
            
            currentPlaylist.addAll(newPlaylist)
            refreshPlaylistUI()
            
            // 如果当前没有播放任何音乐，则开始播放新添加的第一首
            if (mediaPlayer == null || !isPlaying) {
                currentIndex = currentPlaylist.size - newPlaylist.size
                playMusic(currentPlaylist[currentIndex])
            }
            
            Toast.makeText(this, "已添加 ${newPlaylist.size} 个文件", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 加载设置
        loadSettings()
        
        // 初始化UI
        initUI()
        
        // 设置监听器
        setupListeners()
        
        // 开始更新进度条
        handler.postDelayed(updateSeekBar, 1000)
    }
    
    private fun initUI() {
        // 设置音量进度条
        binding.volumeSeekBar.progress = volume
        binding.tvVolume.text = "$volume%"
        
        // 设置重复模式图标
        updateRepeatModeUI()
        
        // 设置随机播放图标
        updateShuffleUI()
        
        // 初始化空播放列表
        refreshPlaylistUI()
    }
    
    private fun setupListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        
        // 播放/暂停按钮
        binding.btnPlayPause.setOnClickListener {
            if (currentPlaylist.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_no_media), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (isPlaying) {
                pauseMusic()
            } else {
                resumeMusic()
            }
        }
        
        // 下一首按钮
        binding.btnNext.setOnClickListener {
            if (currentPlaylist.isEmpty()) return@setOnClickListener
            playNext()
        }
        
        // 上一首按钮
        binding.btnPrevious.setOnClickListener {
            if (currentPlaylist.isEmpty()) return@setOnClickListener
            playPrevious()
        }
        
        // 静音按钮
        binding.btnMute.setOnClickListener {
            toggleMute()
        }
        
        // 随机播放按钮
        binding.btnShuffle.setOnClickListener {
            toggleShuffle()
        }
        
        // 重复模式按钮
        binding.btnRepeat.setOnClickListener {
            toggleRepeatMode()
        }
        
        // 音量调节
        binding.volumeSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    volume = progress
                    binding.tvVolume.text = "$volume%"
                    setVolume(volume)
                    
                    // 如果用户调整了音量，取消静音状态
                    if (isMuted) {
                        isMuted = false
                        updateMuteButton()
                    }
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
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
        
        // 选择文件按钮
        binding.btnSelectFile.setOnClickListener {
            openMusicFile.launch(arrayOf("audio/*"))
        }
        
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
                titleText.text = item.name
                
                // 高亮当前正在播放的项
                if (index == currentIndex) {
                    titleText.setTextColor(getColor(R.color.accent))
                }
                
                itemView.setOnClickListener {
                    currentIndex = index
                    playMusic(item)
                }
                
                binding.playlistContainer.addView(itemView)
            }
        }
    }
    
    private fun playMusic(item: MediaItem) {
        try {
            // 释放之前的MediaPlayer
            releaseMediaPlayer()
            
            // 创建新的MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(applicationContext, item.uri)
                setOnPreparedListener {
                    it.start()
                    isPlaying = true
                    updatePlayPauseButton()
                    
                    // 更新UI
                    binding.tvSongTitle.text = item.name
                    binding.tvTotalTime.text = formatTime(it.duration)
                    binding.seekBar.progress = 0
                }
                setOnCompletionListener {
                    when (repeatMode) {
                        RepeatMode.ONE -> {
                            // 单曲循环
                            it.seekTo(0)
                            it.start()
                        }
                        RepeatMode.ALL -> {
                            // 全部循环，播放下一首
                            playNext()
                        }
                        RepeatMode.OFF -> {
                            // 不循环，如果是最后一首则停止
                            if (currentIndex < currentPlaylist.size - 1) {
                                playNext()
                            } else {
                                isPlaying = false
                                updatePlayPauseButton()
                            }
                        }
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(
                        this@MusicPlayerActivity,
                        "播放出错: $what, $extra",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                setVolume(volume / 100f, volume / 100f)
                prepareAsync()
            }
            
            // 刷新播放列表 UI
            refreshPlaylistUI()
            
        } catch (e: IOException) {
            Toast.makeText(this, "无法播放文件: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun playNext() {
        if (currentPlaylist.isEmpty()) return
        
        if (isShuffle) {
            // 随机模式：随机选择一首（不包括当前正在播放的）
            if (currentPlaylist.size > 1) {
                val nextIndex = Random().nextInt(currentPlaylist.size)
                currentIndex = if (nextIndex == currentIndex) (nextIndex + 1) % currentPlaylist.size else nextIndex
            }
        } else {
            // 顺序模式：播放下一首
            if (currentIndex < currentPlaylist.size - 1) {
                currentIndex++
            } else if (repeatMode == RepeatMode.ALL) {
                // 如果是列表循环，回到第一首
                currentIndex = 0
            } else {
                return // 不是列表循环，且已经是最后一首，不播放
            }
        }
        
        playMusic(currentPlaylist[currentIndex])
    }
    
    private fun playPrevious() {
        if (currentPlaylist.isEmpty()) return
        
        if (isShuffle) {
            // 随机模式：随机选择一首
            if (currentPlaylist.size > 1) {
                val prevIndex = Random().nextInt(currentPlaylist.size)
                currentIndex = if (prevIndex == currentIndex) (prevIndex + 1) % currentPlaylist.size else prevIndex
            }
        } else {
            // 顺序模式：播放上一首
            if (currentIndex > 0) {
                currentIndex--
            } else if (repeatMode == RepeatMode.ALL) {
                // 如果是列表循环，跳转到最后一首
                currentIndex = currentPlaylist.size - 1
            } else {
                return // 不是列表循环，且已经是第一首，不播放
            }
        }
        
        playMusic(currentPlaylist[currentIndex])
    }
    
    private fun pauseMusic() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayPauseButton()
    }
    
    private fun resumeMusic() {
        if (mediaPlayer == null && currentPlaylist.isNotEmpty()) {
            playMusic(currentPlaylist[currentIndex])
        } else {
            mediaPlayer?.start()
            isPlaying = true
            updatePlayPauseButton()
        }
    }
    
    private fun toggleMute() {
        isMuted = !isMuted
        
        if (isMuted) {
            previousVolume = volume
            setVolume(0)
        } else {
            setVolume(previousVolume)
        }
        
        updateMuteButton()
    }
    
    private fun toggleShuffle() {
        isShuffle = !isShuffle
        updateShuffleUI()
        
        Toast.makeText(
            this,
            if (isShuffle) "随机播放已开启" else "随机播放已关闭",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun toggleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        
        updateRepeatModeUI()
        
        val message = when (repeatMode) {
            RepeatMode.OFF -> "重复播放已关闭"
            RepeatMode.ALL -> "列表循环已开启"
            RepeatMode.ONE -> "单曲循环已开启"
        }
        
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun setVolume(newVolume: Int) {
        volume = newVolume
        binding.volumeSeekBar.progress = volume
        binding.tvVolume.text = "$volume%"
        
        mediaPlayer?.setVolume(volume / 100f, volume / 100f)
    }
    
    private fun updatePlayPauseButton() {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }
    
    private fun updateMuteButton() {
        binding.btnMute.setImageResource(
            if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
        )
    }
    
    private fun updateShuffleUI() {
        binding.btnShuffle.setImageResource(
            if (isShuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle
        )
    }
    
    private fun updateRepeatModeUI() {
        when (repeatMode) {
            RepeatMode.OFF -> binding.btnRepeat.setImageResource(R.drawable.ic_repeat)
            RepeatMode.ALL -> binding.btnRepeat.setImageResource(R.drawable.ic_repeat_on)
            RepeatMode.ONE -> binding.btnRepeat.setImageResource(R.drawable.ic_repeat_one)
        }
    }
    
    private fun loadSettings() {
        val prefs = PreferenceManager(this)
        volume = prefs.getVolume()
        isShuffle = prefs.getShuffle()
        repeatMode = when (prefs.getRepeatMode()) {
            "off" -> RepeatMode.OFF
            "all" -> RepeatMode.ALL
            "one" -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
    }
    
    private fun saveSettings() {
        val prefs = PreferenceManager(this)
        prefs.setVolume(volume)
        prefs.setShuffle(isShuffle)
        prefs.setRepeatMode(
            when (repeatMode) {
                RepeatMode.OFF -> "off"
                RepeatMode.ALL -> "all"
                RepeatMode.ONE -> "one"
            }
        )
    }
    
    private fun formatTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        }
    }
    
    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }
    
    override fun onPause() {
        super.onPause()
        saveSettings()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        releaseMediaPlayer()
    }
}