package com.socialhub.downloader.ui.screens.profile

import android.content.Context
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialhub.downloader.data.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class StorageDetails(
    val downloadedBytes: String,
    val cacheBytes: String,
    val freeBytes: String,
    val totalBytes: String,
    val downloadedFraction: Float,
    val cacheFraction: Float
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    private val _storageDetails = MutableStateFlow(emptyStorageDetails())
    val storageDetails: StateFlow<StorageDetails> = _storageDetails.asStateFlow()

    init {
        refreshStorageDetails()
        viewModelScope.launch {
            downloadRepository.completedDownloads.collect {
                refreshStorageDetails()
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun unlockPremium() {
        _isPremiumUser.value = true
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            context.cacheDir.deleteChildren()
            context.externalCacheDir?.deleteChildren()
            refreshStorageDetails()
        }
    }

    private fun refreshStorageDetails() {
        val completedDownloads = downloadRepository.completedDownloads.value
        val downloadedBytes = completedDownloads.sumOf { it.sizeLabel.toBytes() }
        val cacheBytes = context.cacheDir.folderSize() + (context.externalCacheDir?.folderSize() ?: 0L)
        val statFs = StatFs(context.filesDir.absolutePath)
        val totalBytes = statFs.totalBytes
        val freeBytes = statFs.availableBytes

        _storageDetails.value = StorageDetails(
            downloadedBytes = downloadedBytes.formatBytes(),
            cacheBytes = cacheBytes.formatBytes(),
            freeBytes = freeBytes.formatBytes(),
            totalBytes = totalBytes.formatBytes(),
            downloadedFraction = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f,
            cacheFraction = if (totalBytes > 0) cacheBytes.toFloat() / totalBytes else 0f
        )
    }

    private fun emptyStorageDetails() = StorageDetails(
        downloadedBytes = 0L.formatBytes(),
        cacheBytes = 0L.formatBytes(),
        freeBytes = 0L.formatBytes(),
        totalBytes = 0L.formatBytes(),
        downloadedFraction = 0f,
        cacheFraction = 0f
    )

    private fun File.folderSize(): Long =
        if (!exists()) 0L else walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    private fun File.deleteChildren() {
        listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun String.toBytes(): Long {
        val number = substringBefore(" ").toDoubleOrNull() ?: return 0L
        return when {
            contains("GB", ignoreCase = true) -> (number * 1024 * 1024 * 1024).toLong()
            contains("MB", ignoreCase = true) -> (number * 1024 * 1024).toLong()
            contains("KB", ignoreCase = true) -> (number * 1024).toLong()
            else -> number.toLong()
        }
    }

    private fun Long.formatBytes(): String {
        val kb = this / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$this B"
        }
    }
}
