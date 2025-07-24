package com.example.pixelart.ui.create

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelart.PixelArtApplication
import com.example.pixelart.R
import com.example.pixelart.data.repository.ArtProjectRepository
import kotlin.math.roundToInt

@Composable
fun CreateArtScreen(
    navController: NavController,
    encodedImageUri: String,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val imageUri = remember { Uri.decode(encodedImageUri).toUri() }
    val repository = remember {
        ArtProjectRepository((context.applicationContext as PixelArtApplication).database.artProjectDao())
    }
    val viewModel: CreateArtViewModel = viewModel(
        factory = CreateArtViewModelFactory(application, repository, imageUri)
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            Button(
                onClick = {
                    viewModel.createProject { newId ->
                        navController.navigate("coloring/$newId") {
                            popUpTo("gallery") { inclusive = false }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !uiState.isProcessing && !uiState.isLoading
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.create_artwork))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    uiState.previewBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Artwork Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingSlider(
                label = stringResource(R.string.detailing),
                value = uiState.width.toFloat(),
                onValueChange = { viewModel.onWidthChange(it.toInt()) },
                range = 40f..120f,
                steps = 15,
                valueText = "${uiState.width} px"
            )

            Spacer(modifier = Modifier.height(16.dp))

            val colorRangeStart = uiState.recommendedColorCount * 0.8f
            val colorRangeEnd = uiState.recommendedColorCount * 1.2f
            SettingSlider(
                label = stringResource(R.string.number_of_colors),
                value = uiState.colorCount.toFloat(),
                onValueChange = { viewModel.onColorCountChange(it.roundToInt()) },
                range = colorRangeStart..colorRangeEnd,
                valueText = "${uiState.colorCount}"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.anti_aliasing),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.useFilter,
                    onCheckedChange = { viewModel.onFilterChange(it) }
                )
            }
        }
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    steps: Int = 0
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
    }
}