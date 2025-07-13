package com.example.pixelart.ui.coloring

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelart.PixelArtApplication
import com.example.pixelart.data.repository.ArtProjectRepository
import com.example.pixelart.domain.processing.ImageExporter
import com.example.pixelart.ui.coloring.composables.ColorPalette
import com.example.pixelart.ui.coloring.composables.CompletionButtons
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun PixelArtScreen(
    navController: NavController,
    projectId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember {
        ArtProjectRepository(
            (context.applicationContext as PixelArtApplication).database.artProjectDao()
        )
    }

    val viewModel: PixelArtViewModel =
        viewModel(factory = PixelArtViewModelFactory(repository, projectId))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val bottomUiAlpha by animateFloatAsState(
        targetValue = if (uiState.isComplete) 0f else 1f, label = "Bottom UI Alpha"
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE)
                viewModel.saveProgress()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val vibrationEffect = when (event) {
                is PixelArtEvent.ColorCompleted -> {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }

                is PixelArtEvent.ArtworkCompleted -> {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                }
            }
            vibrator.vibrate(vibrationEffect)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.grid.isNotEmpty()) {
                PixelArtCanvas(
                    uiState = uiState,
                    onPixelTapped = viewModel::onPixelTapped,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Box(modifier = Modifier.graphicsLayer { alpha = bottomUiAlpha }) {
                    if (bottomUiAlpha > 0.01f) {
                        ColorPalette(
                            palette = uiState.palette,
                            selectedColorIndex = uiState.selectedColorIndex,
                            progress = uiState.progress,
                            onColorSelected = viewModel::selectColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
        if (uiState.isComplete) {
            CompletionButtons(
                onDone = { navController.popBackStack() },
                onSave = {
                    viewModel.viewModelScope.launch {
                        val success =
                            ImageExporter.saveToDevice(context, uiState.grid, uiState.palette)
                        Toast.makeText(
                            context,
                            if (success) "Saved to gallery!" else "Save failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onShare = {
                    viewModel.viewModelScope.launch {
                        ImageExporter.getShareableUri(context, uiState.grid, uiState.palette)
                            ?.let { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Artwork"))
                            }
                    }
                }
            )
        }
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
