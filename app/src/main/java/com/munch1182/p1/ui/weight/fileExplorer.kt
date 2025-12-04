package com.munch1182.p1.ui.weight

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.munch1182.lib.base.formatFileSize
import com.munch1182.lib.base.launchIO
import com.munch1182.p1.ui.corner
import com.munch1182.p1.ui.theme.FontManySize
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingHalfLarge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FileExplorer(
    initialDirectory: File, itemDirClickable: Boolean = true, config: FileExplorerConfig = DefaultFileExplorerConfig, onDirClick: (FileItemData) -> Unit = {}, onFileClick: (FileItemData) -> Unit = {}, vm: FileExplorerVM = viewModel(), onFileSelected: (File?) -> Unit = {}
) {
    val uiState by vm.uiState.collectAsState()
    val currentDir by vm.currentDirectory.collectAsState()
    val isAtRoot by vm.isAtRoot.collectAsState()

    BackHandler(enabled = !isAtRoot) {
        // 如果不在根目录，返回上一级目录
        if (!isAtRoot) {
            uiState.files.firstOrNull()?.let { vm.onItemClick(it) }
        }
    }

    LaunchedEffect(initialDirectory) { vm.setInitialDirectory(initialDirectory) }

    // 监听过滤函数变化
    LaunchedEffect(config.filter) {
        vm.setFilter(config.filter)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 目录路径显示
        if (config.showRootPath) currentDir?.let { dir -> CurrDir(dir) }

        // 文件列表
        when {
            uiState.isLoading -> Loading()
            uiState.files.isEmpty() -> EmptyContent()
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.files,
                        key = { file ->
                            // 为每个项目创建唯一键
                            if (file.isBackItem) "BACK_${file.file.parentFile}"
                            else "${file.file.name}_${file.file.lastModified()}"
                        },
                    ) { file ->
                        val isClickable = when {
                            file.isBackItem -> true
                            file.isDirectory -> itemDirClickable
                            else -> config.allowFileSelection
                        }

                        val onClick = {
                            if (file.isDirectory || file.isBackItem) {
                                onDirClick(file)
                            } else {
                                onFileClick(file)
                            }
                        }

                        config.FileItemContent(
                            file = file, isClickable = isClickable, onClick = onClick
                        )
                    }
                }
            }
        }
    }

    // 监听文件选择事件
    if (config.allowFileSelection) {
        LaunchedEffect(vm.selectedFile) {
            vm.selectedFile.let { file ->
                onFileSelected(file.value)
                vm.clearSelection()
            }
        }
    }
}

// 自定义过滤条件类型
typealias FileFilter = (FileItemData) -> Boolean

// 一些常用的过滤函数
object FileFilters {
    /** 显示所有文件 */
    val ALL: FileFilter = { true }

    /** 只显示图片文件 */
    val IMAGES: FileFilter = { item ->
        item.isDirectory || item.extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }

    /** 只显示视频文件 */
    val VIDEOS: FileFilter = { item ->
        item.isDirectory || item.extension in listOf("mp4", "avi", "mkv", "mov", "flv", "wmv")
    }

    /** 只显示音频文件 */
    val AUDIO: FileFilter = { item ->
        item.isDirectory || item.extension in listOf("mp3", "wav", "flac", "aac", "pcm", "ogg")
    }

    /** 只显示文档文件 */
    val DOCUMENTS: FileFilter = { item ->
        item.isDirectory || item.extension in listOf("txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx")
    }

    /** 根据扩展名过滤 */
    fun byExtensions(vararg extensions: String): FileFilter = { item ->
        item.isDirectory || item.extension.lowercase() in extensions.map { it.lowercase() }
    }

    /** 根据文件名过滤（支持通配符） */
    fun byName(pattern: String): FileFilter = { item ->
        item.isDirectory || item.file.name.matches(Regex(pattern, RegexOption.IGNORE_CASE))
    }

    /** 组合多个过滤条件（AND逻辑） */
    fun and(vararg filters: FileFilter): FileFilter = { item ->
        filters.all { it(item) }
    }

    /** 组合多个过滤条件（OR逻辑） */
    fun or(vararg filters: FileFilter): FileFilter = { item ->
        filters.any { it(item) }
    }
}

// 自定义项配置接口
interface FileExplorerConfig {
    val showFileSize: Boolean
    val showFileExtension: Boolean
    val allowFileSelection: Boolean
    val showRootPath: Boolean
    val filter: FileFilter

    @Composable
    fun FileItemContent(
        file: FileItemData, isClickable: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null
    )
}

// 默认配置实现
object DefaultFileExplorerConfig : FileExplorerConfig {
    override val showFileSize: Boolean = true
    override val showFileExtension: Boolean = false
    override val allowFileSelection: Boolean = true
    override val showRootPath: Boolean get() = true
    override val filter: FileFilter = FileFilters.ALL

    @Composable
    override fun FileItemContent(
        file: FileItemData, isClickable: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?
    ) {
        DefaultFileItem(
            file = file, isClickable = isClickable, onClick = onClick, onLongClick = onLongClick
        )
    }
}

// 创建自定义配置的便捷方法
fun createFileExplorerConfig(
    showFileSize: Boolean = true, showFileExtension: Boolean = false, allowFileSelection: Boolean = true, showRootPath: Boolean = true, filter: FileFilter = FileFilters.ALL
): FileExplorerConfig = object : FileExplorerConfig {
    override val showFileSize: Boolean = showFileSize
    override val showFileExtension: Boolean = showFileExtension
    override val allowFileSelection: Boolean = allowFileSelection
    override val showRootPath: Boolean = showRootPath
    override val filter: FileFilter = filter

    @Composable
    override fun FileItemContent(
        file: FileItemData, isClickable: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?
    ) {
        DefaultFileItem(
            file = file, isClickable = isClickable, onClick = onClick, onLongClick = onLongClick
        )
    }
}

@Composable
fun DefaultFileExplorer(
    initialDirectory: File, itemDirClickable: Boolean = true, onFileSelected: (File?) -> Unit = {}, vm: FileExplorerVM = viewModel()
) {
    FileExplorer(
        initialDirectory = initialDirectory, itemDirClickable = itemDirClickable, onFileSelected = onFileSelected, onDirClick = { vm.onItemClick(it) }, config = DefaultFileExplorerConfig, vm = vm
    )
}

@Composable
fun FileExplorerWithFilter(
    initialDirectory: File, itemDirClickable: Boolean = true, filter: FileFilter = FileFilters.ALL, onFileSelected: (File?) -> Unit = {}, vm: FileExplorerVM = viewModel()
) {
    val config = remember(filter) {
        createFileExplorerConfig(filter = filter)
    }

    FileExplorer(
        initialDirectory = initialDirectory, itemDirClickable = itemDirClickable, onFileSelected = onFileSelected, config = config, vm = vm
    )
}

@Composable
private fun CurrDir(dir: File) {
    Text(
        text = "当前: ${dir.absolutePath}", modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(), fontSize = FontManySize, color = Color.Gray, maxLines = 1, overflow = TextOverflow.StartEllipsis
    )
}

@Composable
private fun DefaultFileItem(
    file: FileItemData, isClickable: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null
) {
    val backgroundColor = if (file.isDirectory) Color.LightGray.copy(alpha = 0.1f) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .fillMaxWidth()
            .corner()
            .clickable(
                enabled = isClickable, onClick = onClick, indication = LocalIndication.current, interactionSource = remember { MutableInteractionSource() })
            .apply {
                if (onLongClick != null) {
                    combinedClickable(onClick = onClick, onLongClick = onLongClick)
                }
            }
            .background(backgroundColor)
            .padding(horizontal = PagePadding, vertical = PagePaddingHalfLarge)) {
        Icon(
            painter = rememberVectorPainter(image = getFileIcon(file)), contentDescription = if (file.isDirectory) "文件夹" else "文件", modifier = Modifier.size(24.dp), tint = getFileIconColor(file)
        )

        Spacer(modifier = Modifier.width(PagePadding))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.displayName, fontSize = 16.sp, fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis
            )

            if (DefaultFileExplorerConfig.showFileSize && !file.isDirectory && !file.isBackItem) {
                Text(
                    text = file.size.formatFileSize(), fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (file.isDirectory && !file.isBackItem) {
            Icon(
                imageVector = Icons.Default.ChevronRight, contentDescription = "进入", modifier = Modifier.size(16.dp), tint = Color.Gray
            )
        }
    }
}


fun getFileIcon(file: FileItemData) = when {
    file.isBackItem -> Icons.AutoMirrored.Filled.ArrowBack
    file.isDirectory -> Icons.Default.Folder
    file.extension in listOf("mp3", "wav", "flac", "aac", "pcm", "ogg") -> Icons.Default.AudioFile
    file.extension in listOf("mp4", "avi", "mkv", "mov", "flv", "wmv") -> Icons.Default.VideoFile
    file.extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> Icons.Default.Image
    file.extension in listOf("txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx") -> Icons.Default.Description
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

fun getFileIconColor(file: FileItemData) = when {
    file.isBackItem -> Color.Blue
    file.isDirectory -> Color(0xFF2196F3)
    file.extension in listOf("mp3", "wav", "flac", "aac", "pcm", "ogg") -> Color(0xFFFF9800)
    file.extension in listOf("mp4", "avi", "mkv", "mov", "flv", "wmv") -> Color(0xFFF44336)
    file.extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> Color(0xFF4CAF50)
    file.extension in listOf("txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx") -> Color(0xFF9C27B0)
    else -> Color.Gray
}

// 数据类，封装文件信息
@Stable
data class FileItemData(
    val file: File, val isDirectory: Boolean, val isBackItem: Boolean = false, val size: Long = 0, val extension: String = "", val displayName: String = file.name
)

@Stable
class FileExplorerVM : ViewModel() {

    // UI状态封装
    @Stable
    data class UIState(
        val files: List<FileItemData> = emptyList(), val filteredFiles: List<FileItemData> = emptyList(), val isLoading: Boolean = false, val error: String? = null
    )

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _currentDirectory = MutableStateFlow<File?>(null)
    val currentDirectory = _currentDirectory.asStateFlow()

    private val _selectedFile = MutableStateFlow<File?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    private val _isAtRoot = MutableStateFlow(true)
    val isAtRoot = _isAtRoot.asStateFlow()

    private val _filter = MutableStateFlow<FileFilter> { true }

    private var rootDirectory: File? = null
    private var allFiles: List<FileItemData> = emptyList()

    init {
        viewModelScope.launchIO {
            currentDirectory.filterNotNull().distinctUntilChanged().collect { loadDirectoryContents(it) }
        }

        // 监听过滤函数变化
        viewModelScope.launchIO {
            _filter.collect { filter ->
                applyFilter(filter)
            }
        }

        refreshFiles()
    }

    fun refreshFiles() {
        viewModelScope.launchIO { currentDirectory.value?.let { loadDirectoryContents(it) } }
    }

    fun setInitialDirectory(directory: File) {
        if (rootDirectory == null) {
            rootDirectory = directory
            _currentDirectory.value = directory
        }
    }

    fun setFilter(filter: FileFilter) {
        _filter.value = filter
    }

    private fun updateIsAtRoot(currentDir: File) {
        _isAtRoot.value = (rootDirectory == null) || (currentDir == rootDirectory)
    }

    private suspend fun loadDirectoryContents(directory: File) {
        if (!directory.exists() || !directory.isDirectory) {
            _uiState.update { it.copy(error = "目录不存在或无权限访问") }
            return
        }
        updateIsAtRoot(directory)

        _uiState.update { it.copy(isLoading = true, error = null) }

        try {
            val files = withContext(Dispatchers.IO) {
                directory.listFiles()?.toList() ?: emptyList()
            }

            val fileItems = buildList {
                // 如果不是根目录，添加返回项
                if (rootDirectory != null && directory != rootDirectory) {
                    add(
                        FileItemData(
                            file = File(".."), isDirectory = true, isBackItem = true, displayName = "返回"
                        )
                    )
                }

                // 添加目录
                files.filter { it.isDirectory }.sortedBy { it.name.lowercase() }.forEach { dir ->
                    add(
                        FileItemData(
                            file = dir, isDirectory = true, size = dir.length(), extension = ""
                        )
                    )
                }

                // 添加文件
                files.filter { !it.isDirectory }.sortedBy { it.name.lowercase() }.forEach { file ->
                    val extension = file.extension.ifEmpty { "" }
                    add(
                        FileItemData(
                            file = file, isDirectory = false, size = file.length(), extension = extension.lowercase()
                        )
                    )
                }
            }

            allFiles = fileItems
            applyFilter(_filter.value)
            _uiState.update { it.copy(isLoading = false) }

        } catch (e: Exception) {
            _uiState.update { it.copy(error = "加载失败: ${e.message}", isLoading = false) }
        }
    }

    private fun applyFilter(filter: FileFilter) {
        val filteredFiles = allFiles.filter { file ->
            filter(file)
        }
        _uiState.update { it.copy(files = filteredFiles) }
    }

    fun onItemClick(fileItem: FileItemData) {
        when {
            fileItem.isBackItem -> {
                val currentDir = _currentDirectory.value
                currentDir?.parentFile?.let { parent -> _currentDirectory.value = parent }
            }

            fileItem.isDirectory -> {
                _currentDirectory.value = fileItem.file
            }

            else -> {
                // 处理文件点击
                _selectedFile.value = fileItem.file
            }
        }
    }

    fun isItemClickable(fileItem: FileItemData): Boolean {
        return fileItem.isDirectory || fileItem.isBackItem
    }

    fun clearSelection() {
        _selectedFile.value = null
    }
}