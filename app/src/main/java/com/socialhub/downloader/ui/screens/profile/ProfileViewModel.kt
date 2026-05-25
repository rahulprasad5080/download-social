package com.socialhub.downloader.ui.screens.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    private val _storageDetails = MutableStateFlow(
        StorageDetails(
            downloadedBytes = "4.8 GB",
            cacheBytes = "1.2 GB",
            freeBytes = "58.0 GB",
            totalBytes = "64 GB",
            downloadedFraction = 0.075f,
            cacheFraction = 0.018f
        )
    )
    val storageDetails: StateFlow<StorageDetails> = _storageDetails.asStateFlow()

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
        // Reset cache bytes to 0
        _storageDetails.value = _storageDetails.value.copy(
            cacheBytes = "0 KB",
            cacheFraction = 0f
        )
    }
}
