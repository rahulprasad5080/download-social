package com.socialhub.downloader.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialhub.downloader.data.DownloadRepository
import com.socialhub.downloader.ui.components.SocialPlatform
import com.socialhub.downloader.ui.screens.download.CompletedDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

enum class LibraryTab {
    VIDEOS,
    AUDIO,
    FAVORITES,
    FOLDERS
}

enum class SortOption {
    ALL,
    SIZE,
    DATE
}

data class LibraryItem(
    val id: String,
    val title: String,
    val platform: SocialPlatform,
    val sizeLabel: String,
    val dateLabel: String,
    val isFavorite: Boolean,
    val isVideo: Boolean,
    val filePath: String
)

data class FolderItem(
    val id: String,
    val name: String,
    val itemCount: Int,
    val storageSize: String
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(LibraryTab.VIDEOS)
    val currentTab: StateFlow<LibraryTab> = _currentTab.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.ALL)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val favoriteIds = MutableStateFlow<Set<String>>(emptySet())

    val items: StateFlow<List<LibraryItem>> = combine(
        downloadRepository.completedDownloads,
        favoriteIds
    ) { downloads, favorites ->
        downloads.map { it.toLibraryItem(favorites.contains(it.id)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<FolderItem>> = downloadRepository.completedDownloads
        .map { downloads ->
            downloads
                .groupBy { it.platform }
                .map { (platform, items) ->
                    FolderItem(
                        id = platform.name,
                        name = platform.displayName,
                        itemCount = items.size,
                        storageSize = formatBytes(items.sumOf { it.sizeLabel.toBytes() })
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setTab(tab: LibraryTab) {
        _currentTab.value = tab
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleFavorite(id: String) {
        favoriteIds.value = if (favoriteIds.value.contains(id)) {
            favoriteIds.value - id
        } else {
            favoriteIds.value + id
        }
    }

    fun deleteItem(id: String) {
        downloadRepository.deleteCompleted(id)
        favoriteIds.value = favoriteIds.value - id
    }

    private fun CompletedDownload.toLibraryItem(isFavorite: Boolean) = LibraryItem(
        id = id,
        title = title,
        platform = platform,
        sizeLabel = sizeLabel,
        dateLabel = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(createdAtMillis)),
        isFavorite = isFavorite,
        isVideo = !filePath.endsWith(".mp3", ignoreCase = true),
        filePath = filePath
    )

    private fun String.toBytes(): Long {
        val number = substringBefore(" ").toDoubleOrNull() ?: return 0L
        return when {
            contains("GB", ignoreCase = true) -> (number * 1024 * 1024 * 1024).toLong()
            contains("MB", ignoreCase = true) -> (number * 1024 * 1024).toLong()
            contains("KB", ignoreCase = true) -> (number * 1024).toLong()
            else -> number.toLong()
        }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) {
            String.format("%.1f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }
}
