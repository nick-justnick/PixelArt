package com.example.pixelart.ui.gallery

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelart.PixelArtApplication
import com.example.pixelart.R
import com.example.pixelart.data.model.ArtProject
import com.example.pixelart.data.repository.ArtProjectRepository

@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember {
        ArtProjectRepository(
            (context.applicationContext as PixelArtApplication).database.artProjectDao()
        )
    }
    val viewModel: GalleryViewModel = viewModel(factory = GalleryViewModelFactory(repository))

    val projects by viewModel.projects.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var projectForOptions by remember { mutableStateOf<ArtProject?>(null) }
    var showResetConfirmation by remember { mutableStateOf<ArtProject?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<ArtProject?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                showSettingsDialog = true
            }
        }
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.create_new_project)
                )
            }
        }
    ) { paddingValues ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_projects_yet_tap_to_create_one))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects) { project ->
                    ProjectThumbnail(
                        project = project,
                        onClick = { navController.navigate("coloring/${project.id}") },
                        onLongClick = { projectForOptions = project }
                    )
                }
            }
        }
    }

    if (showSettingsDialog && selectedImageUri != null) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onConfirm = { width, colorCount ->
                showSettingsDialog = false
                viewModel.createNewProject(
                    context,
                    selectedImageUri!!,
                    width,
                    colorCount
                ) { newId ->
                    navController.navigate("coloring/$newId")
                }
            }
        )
    }

    projectForOptions?.let { project ->
        ProjectOptionMenu(
            onDismiss = { projectForOptions = null },
            onResetClick = {
                projectForOptions = null
                showResetConfirmation = project
            },
            onDeleteClick = {
                projectForOptions = null
                showDeleteConfirmation = project
            }
        )
    }

    showResetConfirmation?.let { project ->
        ConfirmationDialog(
            title = stringResource(R.string.reset_artwork),
            text = stringResource(R.string.reset_confirm),
            onConfirm = {
                viewModel.resetProject(project)
                showResetConfirmation = null
            },
            onDismiss = { showResetConfirmation = null }
        )
    }

    showDeleteConfirmation?.let { project ->
        ConfirmationDialog(
            title = stringResource(R.string.delete_artwork),
            text = stringResource(R.string.delete_confirm),
            onConfirm = {
                viewModel.deleteProject(project)
                showDeleteConfirmation = null
            },
            onDismiss = { showDeleteConfirmation = null }
        )
    }
}

@Composable
fun ProjectThumbnail(
    project: ArtProject,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        project.thumbnail?.let {
            val bitmap = remember(it) { BitmapFactory.decodeByteArray(it, 0, it.size) }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.project_thumbnail),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                filterQuality = FilterQuality.None
            )
        }
    }
}

@Composable
fun ProjectOptionMenu(
    onDismiss: () -> Unit,
    onResetClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.project_options)) },
        text = {
            Column {
                TextButton(onClick = onResetClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reset_progress))
                }
                TextButton(onClick = onDeleteClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.do_delete_artwork))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (width: Int, colorCount: Int) -> Unit,
) {
    var width by remember { mutableStateOf("64") }
    var colorCount by remember { mutableStateOf("16") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Artwork") },
        text = {
            Column {
                OutlinedTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = { Text("Grid Width") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = colorCount,
                    onValueChange = { colorCount = it },
                    label = { Text("Number of Colors") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val widthInt = width.toIntOrNull() ?: 64
                val colorsInt = colorCount.toIntOrNull() ?: 16
                onConfirm(widthInt, colorsInt)
            }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}