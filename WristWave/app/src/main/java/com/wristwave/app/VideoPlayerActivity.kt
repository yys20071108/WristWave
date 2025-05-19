package com.wristwave.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.wristwave.app.databinding.ActivityVideoPlayerBinding
import com.wristwave.app.model.MediaItem as AppMediaItem
import com.wristwave.app.utils.PreferenceManager
import java.util.*
import kotlin.collections.ArrayList

class VideoPlayerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            try {
                if (player != null && player?.isPlaying == true) {
                    val currentPosition = player?.currentPosition ?: 0
                    val duration = player?.duration ?: 1
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
    
    private var currentPlaylist = ArrayList<AppMediaItem>()
    private var currentIndex = 0
    private var isPlaying = false
    private var isShuffle = false
    private var repeatMode = RepeatMode.OFF
    private var volume = 50
    private var isMuted = false
    private var isFullscreen = false
    
    // 用于存储之前的音量，静音时使用
    private var previousVolume = 50
    
    // 重复模式枚举
    enum class RepeatMode {
        OFF, ALL, ONE
    }
    
    // 文件选择结果处理
    private val openVideoFile = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val newPlaylist = ArrayList<AppMediaItem>()
            
            for ((index, uri) in uris.withIndex()) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                
                val fileName = getFileNameFromUri(uri) ?: "未知视频 ${index + 1}"
                val mediaItem = AppMediaItem(
                    id = System.currentTimeMillis() + index,
                    type = "video",
                    name = fileName,
                    uri = uri
                )
                newPlaylist.add(mediaItem)
            }
            
            currentPlaylist.addAll(newPlaylist)
            refreshPlaylistUI()
            
            // 如果当前没有播放任何视频，则开始播放新添加的第一个
            if (player == null || !isPlaying) {
                currentIndex = currentPlaylist.size - newPlaylist.size
                playVideo(currentPlaylist[currentIndex])
            }
            
            Toast.makeText(this, "已添加 ${newPlaylist.size} 个文件", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
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
                pauseVideo()
            } else {
                resumeVideo()
            }
        }
        
        // 下一个按钮
        binding.btnNext.setOnClickListener {
            if (currentPlaylist.isEmpty()) return@setOnClickListener
            playNext()
        }
        
        // 上一个按钮
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
        
        // 全屏按钮
        binding.btnFullscreen.setOnClickListener {
            toggleFullscreen()
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
                if (fromUser && player != null) {
                    val duration = player?.duration ?: 0
                    val newPosition = duration * progress / 100
                    player?.seekTo(newPosition)
                    binding.tvCurrentTime.text = formatTime(newPosition)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // 选择文件按钮
        binding.btnSelectFile.setOnClickListener {
            openVideoFile.launch(arrayOf("video/*"))
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
    
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        
        if (isFullscreen) {
            // 隐藏所有控件，只显示视频播放器
            binding.controlContainer.visibility = View.GONE
            binding.playerView.layoutParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
            
            // 隐藏系统UI
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        } else {
            // 恢复正常模式
            binding.controlContainer.visibility = View.VISIBLE
            binding.playerView.layoutParams.height = resources.getDimensionPixelSize(R.dimen.video_player_height)
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen)
            
            // 显示系统UI
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
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
                iconView.setImageResource(R.drawable.ic_video) // 设置为视频图标
                
                // 高亮当前正在播放的项
                if (index == currentIndex) {
                    titleText.setTextColor(getColor(R.color.accent))
                }
                
                itemView.setOnClickListener {
                    currentIndex = index
                    playVideo(item)
                }
                
                binding.playlistContainer.addView(itemView)
            }
        }
    }
    
    private fun initializePlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        binding.playerView.player = player
        
        player?.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        when (repeatMode) {
                            RepeatMode.ONE -> {
                                // 单个循环，重新播放当前视频
                                player?.seekTo(0)
                                player?.play()
                            }
                            RepeatMode.ALL -> {
                                // 全部循环，播放下一个
                                playNext()
                            }
                            RepeatMode.OFF -> {
                                // 不循环，如果是最后一个则停止
                                if (currentIndex < currentPlaylist.size - 1) {
                                    playNext()
                                } else {
                                    isPlaying = false
                                    updatePlayPauseButton()
                                }
                            }
                        }
                    }
                    Player.STATE_READY -> {
                        isPlaying = player?.isPlaying ?: false
                        updatePlayPauseButton()
                        binding.tvVideoTitle.text = currentPlaylist[currentIndex].name
                        binding.tvTotalTime.text = formatTime(player?.duration ?: 0)
                    }
                }
            }
        })
    }
    
    private fun playVideo(item: AppMediaItem) {
        try {
            // 如果播放器未初始化，创建播放器
            if (player == null) {
                initializePlayer()
            }
            
            // 准备新的视频
            val mediaItem = MediaItem.fromUri(item.uri)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
            
            isPlaying = true
            updatePlayPauseButton()
            
            // 设置音量
            setVolume(volume)
            
            // 刷新播放列表 UI
            refreshPlaylistUI()
            
        } catch (e: Exception) {
            Toast.makeText(this, "无法播放视频: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun playNext() {
        if (currentPlaylist.isEmpty()) return
        
        if (isShuffle) {
            // 随机模式：随机选择一个（不包括当前正在播放的）
            if (currentPlaylist.size > 1) {
                val nextIndex = Random().nextInt(currentPlaylist.size)
                currentIndex = if (nextIndex == currentIndex) (nextIndex + 1) % currentPlaylist.size else nextIndex
            }
        } else {
            // 顺序模式：播放下一个
            if (currentIndex < currentPlaylist.size - 1) {
                currentIndex++
            } else if (repeatMode == RepeatMode.ALL) {
                // 如果是列表循环，回到第一个
                currentIndex = 0
            } else {
                return // 不是列表循环，且已经是最后一个，不播放
            }
        }
        
        playVideo(currentPlaylist[currentIndex])
    }
    
    private fun playPrevious() {
        if (currentPlaylist.isEmpty()) return
        
        if (isShuffle) {
            // 随机模式：随机选择一个
            if (currentPlaylist.size > 1) {
                val prevIndex = Random().nextInt(currentPlaylist.size)
                currentIndex = if (prevIndex == currentIndex) (prevIndex + 1) % currentPlaylist.size else prevIndex
            }
        } else {
            // 顺序模式：播放上一个
            if (currentIndex > 0) {
                currentIndex--
            } else if (repeatMode == RepeatMode.ALL) {
                // 如果是列表循环，跳转到最后一个
                currentIndex = currentPlaylist.size - 1
            } else {
                return // 不是列表循环，且已经是第一个，不播放
            }
        }
        
        playVideo(currentPlaylist[currentIndex])
    }
    
    private fun pauseVideo() {
        player?.pause()
        isPlaying = false
        updatePlayPauseButton()
    }
    
    private fun resumeVideo() {
        if (player == null && currentPlaylist.isNotEmpty()) {
            playVideo(currentPlaylist[currentIndex])
        } else {
            player?.play()
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
            RepeatMode.ONE -> "单个循环已开启"
        }
        
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun setVolume(newVolume: Int) {
        volume = newVolume
        binding.volumeSeekBar.progress = volume
        binding.tvVolume.text = "$volume%"
        
        player?.volume = volume / 100f
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
    
    private fun formatTime(timeMs: Long): String {
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
    
    private fun releasePlayer() {
        player?.release()
        player = null
    }
    
    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            pauseVideo()
        }
        saveSettings()
    }
    
    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        releasePlayer()
    }
}