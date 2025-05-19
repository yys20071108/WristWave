package com.wristwave.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 管理应用设置的工具类
 */
class PreferenceManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    // 音量设置
    fun getVolume(): Int = prefs.getInt(KEY_VOLUME, 50)
    fun setVolume(volume: Int) = prefs.edit().putInt(KEY_VOLUME, volume).apply()
    
    // 随机播放设置
    fun getShuffle(): Boolean = prefs.getBoolean(KEY_SHUFFLE, false)
    fun setShuffle(shuffle: Boolean) = prefs.edit().putBoolean(KEY_SHUFFLE, shuffle).apply()
    
    // 重复模式设置（off, all, one）
    fun getRepeatMode(): String = prefs.getString(KEY_REPEAT_MODE, "off") ?: "off"
    fun setRepeatMode(mode: String) = prefs.edit().putString(KEY_REPEAT_MODE, mode).apply()
    
    // 自动播放设置
    fun getAutoPlay(): Boolean = prefs.getBoolean(KEY_AUTO_PLAY, true)
    fun setAutoPlay(autoPlay: Boolean) = prefs.edit().putBoolean(KEY_AUTO_PLAY, autoPlay).apply()
    
    // 深色模式设置
    fun getDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
    fun setDarkMode(darkMode: Boolean) = prefs.edit().putBoolean(KEY_DARK_MODE, darkMode).apply()
    
    // 音乐质量设置（low, medium, high）
    fun getMusicQuality(): String = prefs.getString(KEY_MUSIC_QUALITY, "medium") ?: "medium"
    fun setMusicQuality(quality: String) = prefs.edit().putString(KEY_MUSIC_QUALITY, quality).apply()
    
    // 视频质量设置（360p, 720p, 1080p）
    fun getVideoQuality(): String = prefs.getString(KEY_VIDEO_QUALITY, "720p") ?: "720p"
    fun setVideoQuality(quality: String) = prefs.edit().putString(KEY_VIDEO_QUALITY, quality).apply()
    
    // 图片格式设置（jpg, png, webp）
    fun getImageFormat(): String = prefs.getString(KEY_IMAGE_FORMAT, "jpg") ?: "jpg"
    fun setImageFormat(format: String) = prefs.edit().putString(KEY_IMAGE_FORMAT, format).apply()
    
    // 录音格式设置（mp3, wav）
    fun getRecordingFormat(): String = prefs.getString(KEY_RECORDING_FORMAT, "mp3") ?: "mp3"
    fun setRecordingFormat(format: String) = prefs.edit().putString(KEY_RECORDING_FORMAT, format).apply()
    
    // 全屏设置
    fun getEnableFullscreen(): Boolean = prefs.getBoolean(KEY_ENABLE_FULLSCREEN, true)
    fun setEnableFullscreen(enable: Boolean) = prefs.edit().putBoolean(KEY_ENABLE_FULLSCREEN, enable).apply()
    
    // 主题设置（default, dark, light, colorful）
    fun getTheme(): String = prefs.getString(KEY_THEME, "default") ?: "default"
    fun setTheme(theme: String) = prefs.edit().putString(KEY_THEME, theme).apply()
    
    // 默认文件夹路径
    fun getDefaultMusicFolder(): String = prefs.getString(KEY_DEFAULT_MUSIC_FOLDER, "") ?: ""
    fun setDefaultMusicFolder(path: String) = prefs.edit().putString(KEY_DEFAULT_MUSIC_FOLDER, path).apply()
    
    fun getDefaultVideoFolder(): String = prefs.getString(KEY_DEFAULT_VIDEO_FOLDER, "") ?: ""
    fun setDefaultVideoFolder(path: String) = prefs.edit().putString(KEY_DEFAULT_VIDEO_FOLDER, path).apply()
    
    fun getDefaultImageFolder(): String = prefs.getString(KEY_DEFAULT_IMAGE_FOLDER, "") ?: ""
    fun setDefaultImageFolder(path: String) = prefs.edit().putString(KEY_DEFAULT_IMAGE_FOLDER, path).apply()
    
    fun getDefaultRecordingFolder(): String = prefs.getString(KEY_DEFAULT_RECORDING_FOLDER, "") ?: ""
    fun setDefaultRecordingFolder(path: String) = prefs.edit().putString(KEY_DEFAULT_RECORDING_FOLDER, path).apply()
    
    companion object {
        private const val PREFS_NAME = "wristwave_preferences"
        
        // 键值
        private const val KEY_VOLUME = "volume"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_AUTO_PLAY = "auto_play"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_MUSIC_QUALITY = "music_quality"
        private const val KEY_VIDEO_QUALITY = "video_quality"
        private const val KEY_IMAGE_FORMAT = "image_format"
        private const val KEY_RECORDING_FORMAT = "recording_format"
        private const val KEY_ENABLE_FULLSCREEN = "enable_fullscreen"
        private const val KEY_THEME = "theme"
        private const val KEY_DEFAULT_MUSIC_FOLDER = "default_music_folder"
        private const val KEY_DEFAULT_VIDEO_FOLDER = "default_video_folder"
        private const val KEY_DEFAULT_IMAGE_FOLDER = "default_image_folder"
        private const val KEY_DEFAULT_RECORDING_FOLDER = "default_recording_folder"
    }
}