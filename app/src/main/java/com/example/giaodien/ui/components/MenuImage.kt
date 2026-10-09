package com.example.giaodien.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

/** Fixed caller geometry; explicit async/error state instead of an empty image slot. */
@Composable
fun MenuImage(model: String?, contentDescription: String, modifier: Modifier = Modifier,
              contentScale: ContentScale = ContentScale.Crop) {
    var attempt by remember(model) { mutableIntStateOf(0) }
    key(model, attempt) {
        SubcomposeAsyncImage(
            model = model, contentDescription = contentDescription,
            modifier = modifier, contentScale = contentScale,
            loading = {
                Box(Modifier.fillMaxSize().testTag("food-image-loading"), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            },
            error = {
                Box(Modifier.fillMaxSize().testTag("food-image-error"), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { attempt++ }) { Text("Tải lại ảnh") }
                }
            },
            success = { SubcomposeAsyncImageContent(Modifier.testTag("food-image-loaded")) }
        )
    }
}
