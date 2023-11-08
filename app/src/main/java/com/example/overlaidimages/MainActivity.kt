package com.example.overlaidimages

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.overlaidimages.ui.theme.OverlaidImagesTheme
import kotlin.math.sin

enum class TransitionMode {
    FADE,
    SWIPE,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var transitionMode by rememberSaveable {
                mutableStateOf(TransitionMode.FADE)
            }

            OverlaidImagesTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    transitionMode = if (transitionMode == TransitionMode.FADE)
                                        TransitionMode.SWIPE
                                    else
                                        TransitionMode.FADE
                                }
                            )
                        }
                    ,
                    color = MaterialTheme.colorScheme.background
                ) {
                    OverlaidImages(transitionMode)
                }
            }
        }
    }
}

@Composable
fun OverlaidImages(mode: TransitionMode = TransitionMode.FADE) {
    val ctx = LocalContext.current
    val cr = ctx.contentResolver

    var bottomURI by rememberSaveable { mutableStateOf<Uri?>(null) }
    var topURI by rememberSaveable { mutableStateOf<Uri?>(null) }
    val bottomBm = remember { mutableStateOf<Bitmap?>(null) }
    val topBm = remember { mutableStateOf<Bitmap?>(null) }

    var th by rememberSaveable { mutableStateOf(0f) }
    var freq by rememberSaveable { mutableStateOf(1f) }
    var scale by rememberSaveable { mutableStateOf(1f) }

    val launcher = rememberLauncherForActivityResult(
        //contract = ActivityResultContracts.GetMultipleContents()
        contract = ActivityResultContracts.PickMultipleVisualMedia(2)
    ) { uriList ->
        val uriCount = uriList.count()
        var moveTopToBottom = false
        var uriForBottom: Uri? = null
        var uriForTop: Uri? = null

        if (uriCount == 1) {
            moveTopToBottom = topURI != null
            if (bottomURI == null)
                uriForBottom = uriList.first()
            else
                uriForTop = uriList.first()
        } else if (uriCount == 2) {
            uriForBottom = uriList.first()
            uriForTop = uriList.last()
        } else if (uriCount > 2) {
            Toast.makeText(ctx, "Pick 1 or 2 images, not $uriCount", Toast.LENGTH_SHORT).show()
        }

        if (uriForBottom != null) {
            bottomURI = uriForBottom
            loadBitmap(cr, bottomURI!!, bottomBm)
        } else if (moveTopToBottom) {
            bottomURI = topURI
            bottomBm.value = topBm.value
        }

        if (uriForTop != null) {
            topURI = uriForTop
            loadBitmap(cr, topURI!!, topBm)
        }
    }

    if (bottomURI != null && bottomBm.value == null) {
        loadBitmap(cr, bottomURI!!, bottomBm)
    }
    if (topURI != null && topBm.value == null) {
        loadBitmap(cr, topURI!!, topBm)
    }

    Column {
        Button(onClick = {
            launcher.launch(
                //"image/*"
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }) {
            Text("Pick one or two images")
        }

        Box (Modifier.weight(10f)) {
            bottomBm.value?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
            topBm.value?.let { bm ->
                val factor = sin(th) / 2f + 0.5f

                val transitionModifier =
                    when (mode) {
                        TransitionMode.FADE ->
                            Modifier.alpha(factor)

                        TransitionMode.SWIPE ->
                            Modifier.clip(GenericShape { size, _ ->
                                addRect(
                                    rect = Rect(
                                        topLeft = Offset.Zero,
                                        bottomRight = Offset(
                                            size.width * factor,
                                            size.height
                                        )
                                    )
                                )
                            })
                    }

                Image(
                    bitmap = bm.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .then(transitionModifier)
                )
            }
        }

        Row {
            Icon(Icons.Default.Speed, "Frequency")
            Slider(
                value = freq,
                onValueChange = { freq = it },
                valueRange = 0f..4f,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        Row {
            Icon(Icons.Default.ZoomIn, "Zoom")
            Slider(
                value = scale,
                onValueChange = { scale = it },
                valueRange = 0.8f..1.4f,
                modifier = Modifier
                    .fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Red,
                    activeTrackColor = Color.Red,
                    inactiveTrackColor = Color.LightGray
                )
            )
        }
    }
    th += .071f * freq
}

private fun loadBitmap(cr: ContentResolver, uri: Uri, bitmap: MutableState<Bitmap?>) {
    ImageDecoder.createSource(cr, uri).let { bitmap.value = ImageDecoder.decodeBitmap(it) }
}
