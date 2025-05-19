package com.wristwave.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.wristwave.app.databinding.ActivityImageViewerBinding
import com.wristwave.app.model.MediaItem
import com.wristwave.app.utils.PreferenceManager
import java.util.*
import kotlin.collections.ArrayList

class ImageViewerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityImageViewerBinding
    private var currentPlaylist = ArrayList<MediaItem>()
    private var currentIndex = 0
    private var isFullscreen = false
    private var isShuffle = false
    
    // 文件选择结果处理
    private val openImageFile = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val newPlaylist = ArrayList<MediaItem>()
            
            for ((index, uri) in uris.withIndex()) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                
                val fileName = getFileNameFromUri(uri) ?: "未知图片 ${index + 1}"
                val mediaItem = MediaItem(
                    id = System.currentTimeMillis() + index,
                    type = "image",
                    name = fileName,
                    uri = uri
                )
                newPlaylist.add(mediaItem)
            }
            
            currentPlaylist.addAll(newPlaylist)
            refreshPlaylistUI()
            
            // 如果当前没有显示任何图片，则显示新添加的第一张
            if (currentPlaylist.isNotEmpty() && binding.tvImageTitle.text == "未选择图片") {
                currentIndex = currentPlaylist.size - newPlaylist.size
                showImage(currentPlaylist[currentIndex])
            }
            
            Toast.makeText(this, "已添加 ${newPlaylist.size} 个文件", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 加载设置
        loadSettings()
        
        // 初始化UI
        initUI()
        
        // 设置监听器
        setupListeners()
    }
    
    private fun initUI() {
        // 初始化空播放列表
        refreshPlaylistUI()
        
        // 设置随机模式图标
        updateShuffleUI()
    }
    
    private fun setupListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        
        // 下一张按钮
        binding.btnNext.setOnClickListener {
            if (currentPlaylist.isEmpty()) return@setOnClickListener
            showNext()
        }
        
        // 上一张按钮
        binding.btnPrevious.setOnClickListener {
            if (currentPlaylist.isEmpty()) return@setOnClickListener
            showPrevious()
        }
        
        // 全屏按钮
        binding.btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }
        
        // 随机模式按钮
        binding.btnShuffle.setOnClickListener {
            toggleShuffle()
        }
        
        // 选择文件按钮
        binding.btnSelectFile.setOnClickListener {
            openImageFile.launch(arrayOf("image/*"))
        }
        
        // 播放列表按钮
        binding.btnPlaylist.setOnClickListener {
            togglePlaylistVisibility()
        }
        
        // 图片点击事件
        binding.ivImage.setOnClickListener {
            toggleControlsVisibility()
        }
    }
    
    private fun toggleControlsVisibility() {
        if (!isFullscreen) return
        
        if (binding.controlContainer.visibility == View.VISIBLE) {
            binding.controlContainer.visibility = View.GONE
            binding.btnBack.visibility = View.GONE
            binding.tvTitle.visibility = View.GONE
        } else {
            binding.controlContainer.visibility = View.VISIBLE
            binding.btnBack.visibility = View.VISIBLE
            binding.tvTitle.visibility = View.VISIBLE
        }
    }
    
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        
        if (isFullscreen) {
            // 隐藏系统UI
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            // 显示系统UI
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen)
        }
    }
    
    private fun togglePlaylistVisibility() {
        if (binding.playlistContainer.visibility == View.VISIBLE) {
            binding.playlistContainer.visibility = View.GONE
        } else {
            binding.playlistContainer.visibility = View.VISIBLE
        }
    }
    
    private fun toggleShuffle() {
        isShuffle = !isShuffle
        updateShuffleUI()
        
        Toast.makeText(
            this,
            if (isShuffle) "随机查看已开启" else "随机查看已关闭",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun updateShuffleUI() {
        binding.btnShuffle.setImageResource(
            if (isShuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle
        )
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
                iconView.setImageResource(R.drawable.ic_image)
                
                // 高亮当前正在显示的项
                if (index == currentIndex) {
                    titleText.setTextColor(getColor(R.color.accent))
                }
                
                itemView.setOnClickListener {
                    currentIndex = index
                    showImage(item)
                }
                
                binding.playlistContainer.addView(itemView)
            }
        }
    }
    
    private fun showImage(item: MediaItem) {
        try {
            Glide.with(this)
                .load(item.uri)
                .error(R.drawable.ic_image)
                .into(binding.ivImage)
            
            binding.tvImageTitle.text = item.name
            
            // 刷新播放列表 UI
            refreshPlaylistUI()
            
        } catch (e: Exception) {
            Toast.makeText(this, "无法显示图片: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun showNext() {
        if (currentPlaylist.isEmpty()) return
        
        if (isShuffle) {
            // 随机模式：随机选择一张（不包括当前正在显示的）
            if (currentPlaylist.size > 1) {
                val nextIndex = Random().nextInt(currentPlaylist.size)
                currentIndex = if (nextIndex == currentIndex) (nextIndex + 1) % currentPlaylist.size else nextIndex
            }
        } else {
            // 顺序模式：显示下一张
            if (currentIndex < currentPlaylist.size - 1) {
                currentIndex++
            } else {
                // 如果是最后一张，回到第一张
                currentIndex = 0
            }
        }
        
        showImage(currentPlaylist[currentIndex])
    }
    
    private fun showPrevious() {
        if (currentPlaylist.isEmpty()) return
        
        if (isShuffle) {
            // 随机模式：随机选择一张
            if (currentPlaylist.size > 1) {
                val prevIndex = Random().nextInt(currentPlaylist.size)
                currentIndex = if (prevIndex == currentIndex) (prevIndex + 1) % currentPlaylist.size else prevIndex
            }
        } else {
            // 顺序模式：显示上一张
            if (currentIndex > 0) {
                currentIndex--
            } else {
                // 如果是第一张，跳转到最后一张
                currentIndex = currentPlaylist.size - 1
            }
        }
        
        showImage(currentPlaylist[currentIndex])
    }
    
    private fun loadSettings() {
        val prefs = PreferenceManager(this)
        isShuffle = prefs.getShuffle()
    }
    
    private fun saveSettings() {
        val prefs = PreferenceManager(this)
        prefs.setShuffle(isShuffle)
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        }
    }
    
    override fun onPause() {
        super.onPause()
        saveSettings()
    }
}