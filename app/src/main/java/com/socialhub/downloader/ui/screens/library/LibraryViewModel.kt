package com.socialhub.downloader.ui.screens.library

import androidx.lifecycle.ViewModel
import com.socialhub.downloader.ui.components.SocialPlatform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class LibraryViewModel @Inject constructor() : ViewModel() {

    private val _currentTab = MutableStateFlow(LibraryTab.VIDEOS)
    val currentTab: StateFlow<LibraryTab> = _currentTab.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.ALL)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _items = MutableStateFlow<List<LibraryItem>>(emptyList())
    val items: StateFlow<List<LibraryItem>> = _items.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderItem>>(emptyList())
    val folders: StateFlow<List<FolderItem>> = _folders.asStateFlow()

    init {
        // Load initial mock library items
        _items.value = listOf(
            LibraryItem("l1", "Cool Tech Hacks Compilation", SocialPlatform.YOUTUBE, "45.8 MB", "1 day ago", true, true, "/storage/emulated/0/Download/SocialHub/tech.mp4"),
            LibraryItem("l2", "Norway Fjords Vlog", SocialPlatform.INSTAGRAM, "18.3 MB", "2 days ago", true, true, "/storage/emulated/0/Download/SocialHub/norway.mp4"),
            LibraryItem("l3", "Acoustic Guitar Loop Track", SocialPlatform.TIKTOK, "6.2 MB", "4 days ago", false, false, "/storage/emulated/0/Download/SocialHub/guitar.mp3"),
            LibraryItem("l4", "Lo-Fi Beats 1 Hour Study", SocialPlatform.YOUTUBE, "142 MB", "1 week ago", false, false, "/storage/emulated/0/Download/SocialHub/lofi.mp3"),
            LibraryItem("l5", "Cinematic drone shots 4K", SocialPlatform.X, "122 MB", "2 weeks ago", true, true, "/storage/emulated/0/Download/SocialHub/drone.mp4")
        )

        _folders.value = listOf(
            FolderItem("f1", "Instagram Reels", 12, "240 MB"),
            FolderItem("f2", "YouTube Videos", 8, "512 MB"),
            FolderItem("f3", "TikTok Audios", 24, "104 MB"),
            FolderItem("f4", "X Status Media", 4, "84 MB")
        )
    }

    fun setTab(tab: LibraryTab) {
        _currentTab.value = tab
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleFavorite(id: String) {
        _items.value = _items.value.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun deleteItem(id: String) {
        _items.value = _items.value.filter { it.id != id }
    }
}
