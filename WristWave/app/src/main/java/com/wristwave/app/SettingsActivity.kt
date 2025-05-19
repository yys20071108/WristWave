package com.wristwave.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.wristwave.app.databinding.ActivitySettingsBinding
import com.wristwave.app.utils.PreferenceManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferenceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferenceManager(this)
        
        // 加载设置
        loadSettings()
        
        // 设置监听器
        setupListeners()
    }
    
    private fun loadSettings() {
        // 深色模式
        binding.switchDarkMode.isChecked = prefs.getDarkMode()
        
        // 自动播放
        binding.switchAutoPlay.isChecked = prefs.getAutoPlay()
        
        // 全屏模式
        binding.switchEnableFullscreen.isChecked = prefs.getEnableFullscreen()
        
        // 重复模式
        val repeatMode = prefs.getRepeatMode()
        binding.radioRepeatOff.isChecked = repeatMode == "off"
        binding.radioRepeatAll.isChecked = repeatMode == "all"
        binding.radioRepeatOne.isChecked = repeatMode == "one"
        
        // 音乐质量
        val musicQuality = prefs.getMusicQuality()
        binding.radioMusicQualityLow.isChecked = musicQuality == "low"
        binding.radioMusicQualityMedium.isChecked = musicQuality == "medium"
        binding.radioMusicQualityHigh.isChecked = musicQuality == "high"
        
        // 视频质量
        val videoQuality = prefs.getVideoQuality()
        binding.radioVideoQuality360p.isChecked = videoQuality == "360p"
        binding.radioVideoQuality720p.isChecked = videoQuality == "720p"
        binding.radioVideoQuality1080p.isChecked = videoQuality == "1080p"
        
        // 图片格式
        val imageFormat = prefs.getImageFormat()
        binding.radioImageFormatJpg.isChecked = imageFormat == "jpg"
        binding.radioImageFormatPng.isChecked = imageFormat == "png"
        binding.radioImageFormatWebp.isChecked = imageFormat == "webp"
        
        // 录音格式
        val recordingFormat = prefs.getRecordingFormat()
        binding.radioRecordingFormatMp3.isChecked = recordingFormat == "mp3"
        binding.radioRecordingFormatWav.isChecked = recordingFormat == "wav"
        
        // 默认文件夹
        binding.etDefaultMusicFolder.setText(prefs.getDefaultMusicFolder())
        binding.etDefaultVideoFolder.setText(prefs.getDefaultVideoFolder())
        binding.etDefaultImageFolder.setText(prefs.getDefaultImageFolder())
        binding.etDefaultRecordingFolder.setText(prefs.getDefaultRecordingFolder())
        
        // 主题
        val theme = prefs.getTheme()
        binding.radioThemeDefault.isChecked = theme == "default"
        binding.radioThemeDark.isChecked = theme == "dark"
        binding.radioThemeLight.isChecked = theme == "light"
        binding.radioThemeColorful.isChecked = theme == "colorful"
    }
    
    private fun setupListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        
        // 深色模式
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.setDarkMode(isChecked)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        
        // 自动播放
        binding.switchAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            prefs.setAutoPlay(isChecked)
        }
        
        // 全屏模式
        binding.switchEnableFullscreen.setOnCheckedChangeListener { _, isChecked ->
            prefs.setEnableFullscreen(isChecked)
        }
        
        // 重复模式
        binding.rgRepeatMode.setOnCheckedChangeListener { _, checkedId ->
            val repeatMode = when (checkedId) {
                R.id.radioRepeatOff -> "off"
                R.id.radioRepeatAll -> "all"
                R.id.radioRepeatOne -> "one"
                else -> "off"
            }
            prefs.setRepeatMode(repeatMode)
        }
        
        // 音乐质量
        binding.rgMusicQuality.setOnCheckedChangeListener { _, checkedId ->
            val musicQuality = when (checkedId) {
                R.id.radioMusicQualityLow -> "low"
                R.id.radioMusicQualityMedium -> "medium"
                R.id.radioMusicQualityHigh -> "high"
                else -> "medium"
            }
            prefs.setMusicQuality(musicQuality)
        }
        
        // 视频质量
        binding.rgVideoQuality.setOnCheckedChangeListener { _, checkedId ->
            val videoQuality = when (checkedId) {
                R.id.radioVideoQuality360p -> "360p"
                R.id.radioVideoQuality720p -> "720p"
                R.id.radioVideoQuality1080p -> "1080p"
                else -> "720p"
            }
            prefs.setVideoQuality(videoQuality)
        }
        
        // 图片格式
        binding.rgImageFormat.setOnCheckedChangeListener { _, checkedId ->
            val imageFormat = when (checkedId) {
                R.id.radioImageFormatJpg -> "jpg"
                R.id.radioImageFormatPng -> "png"
                R.id.radioImageFormatWebp -> "webp"
                else -> "jpg"
            }
            prefs.setImageFormat(imageFormat)
        }
        
        // 录音格式
        binding.rgRecordingFormat.setOnCheckedChangeListener { _, checkedId ->
            val recordingFormat = when (checkedId) {
                R.id.radioRecordingFormatMp3 -> "mp3"
                R.id.radioRecordingFormatWav -> "wav"
                else -> "mp3"
            }
            prefs.setRecordingFormat(recordingFormat)
        }
        
        // 主题
        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radioThemeDefault -> "default"
                R.id.radioThemeDark -> "dark"
                R.id.radioThemeLight -> "light"
                R.id.radioThemeColorful -> "colorful"
                else -> "default"
            }
            prefs.setTheme(theme)
            Toast.makeText(this, "主题设置将在下次启动应用时生效", Toast.LENGTH_SHORT).show()
        }
        
        // 默认文件夹选择
        binding.btnSelectDefaultMusicFolder.setOnClickListener {
            openFolderPicker(REQUEST_MUSIC_FOLDER)
        }
        
        binding.btnSelectDefaultVideoFolder.setOnClickListener {
            openFolderPicker(REQUEST_VIDEO_FOLDER)
        }
        
        binding.btnSelectDefaultImageFolder.setOnClickListener {
            openFolderPicker(REQUEST_IMAGE_FOLDER)
        }
        
        binding.btnSelectDefaultRecordingFolder.setOnClickListener {
            openFolderPicker(REQUEST_RECORDING_FOLDER)
        }
    }
    
    private fun openFolderPicker(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, requestCode)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            
            // 获取持久化权限
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            
            when (requestCode) {
                REQUEST_MUSIC_FOLDER -> {
                    prefs.setDefaultMusicFolder(uri.toString())
                    binding.etDefaultMusicFolder.setText(uri.toString())
                }
                REQUEST_VIDEO_FOLDER -> {
                    prefs.setDefaultVideoFolder(uri.toString())
                    binding.etDefaultVideoFolder.setText(uri.toString())
                }
                REQUEST_IMAGE_FOLDER -> {
                    prefs.setDefaultImageFolder(uri.toString())
                    binding.etDefaultImageFolder.setText(uri.toString())
                }
                REQUEST_RECORDING_FOLDER -> {
                    prefs.setDefaultRecordingFolder(uri.toString())
                    binding.etDefaultRecordingFolder.setText(uri.toString())
                }
            }
        }
    }
    
    companion object {
        private const val REQUEST_MUSIC_FOLDER = 1001
        private const val REQUEST_VIDEO_FOLDER = 1002
        private const val REQUEST_IMAGE_FOLDER = 1003
        private const val REQUEST_RECORDING_FOLDER = 1004
    }
}