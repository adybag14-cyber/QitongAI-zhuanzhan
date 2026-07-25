package com.qtwl.YitongAIzhuanzhan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qtwl.YitongAIzhuanzhan.Bookmark
import com.qtwl.YitongAIzhuanzhan.BookmarkManager
import com.qtwl.YitongAIzhuanzhan.R
import com.qtwl.YitongAIzhuanzhan.ui.components.GlassCard
import com.qtwl.YitongAIzhuanzhan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkEditScreen(
    onBack: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == GlassBackgroundDark
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var bookmarks by remember { mutableStateOf(BookmarkManager.getBookmarks(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bookmarks_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_bookmark))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) GlassBackgroundDark else GlassBackground,
                    titleContentColor = if (isDark) AppleLabelDark else AppleLabel,
                    navigationIconContentColor = if (isDark) AppleBlueLight else AppleBlue,
                    actionIconContentColor = if (isDark) AppleBlueLight else AppleBlue
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDark) GlassBackgroundDark else GlassBackground)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_bookmark))
                }
                OutlinedButton(
                    onClick = {
                        BookmarkManager.resetToDefault(context)
                        bookmarks = BookmarkManager.getBookmarks(context)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) AppleBlueLight else AppleBlue
                    )
                ) {
                    Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.restore_default))
                }
            }

            Spacer(Modifier.height(16.dp))

            // 收藏列表
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp
            ) {
                Column {
                    if (bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_bookmarks),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                            )
                        }
                    } else {
                        bookmarks.forEachIndexed { index, bookmark ->
                            BookmarkEditItem(
                                bookmark = bookmark,
                                isDark = isDark,
                                onEdit = { editingBookmark = bookmark },
                                onDelete = {
                                    BookmarkManager.removeBookmark(context, bookmark.url)
                                    bookmarks = BookmarkManager.getBookmarks(context)
                                }
                            )
                            if (index < bookmarks.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = if (isDark) GlassBorderDark else GlassBorder
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.bookmarks_total, bookmarks.size),
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) AppleTertiaryLabelDark else AppleTertiaryLabel,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    // 添加/编辑对话框
    if (showAddDialog || editingBookmark != null) {
        BookmarkEditDialog(
            bookmark = editingBookmark,
            isDark = isDark,
            onSave = { name, url ->
                if (name.isNotBlank() && url.isNotBlank()) {
                    val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) url
                    else "https://$url"
                    BookmarkManager.addBookmark(context, name, finalUrl)
                    bookmarks = BookmarkManager.getBookmarks(context)
                }
                showAddDialog = false
                editingBookmark = null
            },
            onDismiss = {
                showAddDialog = false
                editingBookmark = null
            }
        )
    }
}

@Composable
private fun BookmarkEditItem(
    bookmark: Bookmark,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Web,
            contentDescription = null,
            tint = if (isDark) AppleBlueLight else AppleBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) AppleLabelDark else AppleLabel
            )
            Text(
                text = bookmark.url,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel,
                maxLines = 1
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.edit),
                tint = if (isDark) AppleBlueLight else AppleBlue,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BookmarkEditDialog(
    bookmark: Bookmark?,
    isDark: Boolean,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(bookmark?.name ?: "") }
    var url by remember { mutableStateOf(bookmark?.url ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) GlassBackgroundDark else GlassBackground,
        title = {
            Text(
                text = if (bookmark == null) stringResource(R.string.add_bookmark) else stringResource(R.string.edit_bookmark),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.bookmark_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppleBlue,
                        cursorColor = AppleBlue,
                        focusedTextColor = if (isDark) AppleLabelDark else AppleLabel,
                        unfocusedTextColor = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.website)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppleBlue,
                        cursorColor = AppleBlue,
                        focusedTextColor = if (isDark) AppleLabelDark else AppleLabel,
                        unfocusedTextColor = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, url) },
                enabled = name.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}