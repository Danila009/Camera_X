package com.example.camerax

import android.annotation.SuppressLint
import android.content.Context.CAMERA_SERVICE
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camerax.ui.theme.Purple500
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("NewApi")
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun CameraView(
    directory:File,
    onMediaCaptured: (Uri?) -> Unit
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashEnabled by remember { mutableStateOf(false) }
    val cameraListId = cameraManager.cameraIdList[0]
    var flashRes by remember { mutableStateOf(R.drawable.ic_outline_flashlight_off) }
    val executor = ContextCompat.getMainExecutor(context)
    var cameraSelector: CameraSelector?
    val cameraProvider = cameraProviderFuture.get()

    Box {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(executor, FaceAnalyzer())
                        }
                    imageCapture = ImageCapture.Builder()
                        .setTargetRotation(previewView.display.rotation)
                        .build()

                    val cameraSelectorView = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelectorView,
                        imageCapture,
                        preview
                    )
                }, executor)
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                previewView
            }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
                .align(Alignment.TopStart)
        ) {
            IconButton(onClick = {
                flashEnabled = !flashEnabled
                flashRes = if (flashEnabled){
                    R.drawable.ic_outline_flashlight_off
                }else{
                    R.drawable.ic_outline_flashlight_on
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraListId,flashEnabled)
                    }
                } catch (e: CameraAccessException) {
                    Log.e("flashlight", e.message.toString())
                }

            }) {
                Icon(
                    painter = painterResource(id = flashRes),
                    contentDescription = null,
                    modifier = Modifier.size(35.dp),
                    tint = MaterialTheme.colors.surface
                )
            }

            Button(
                onClick = {
                    val imgCapture = imageCapture ?: return@Button
                    val photoFile = File(
                        directory,
                        SimpleDateFormat("yyyyMMDD-HHmmss", Locale.US)
                            .format(System.currentTimeMillis()) + ".jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imgCapture.takePicture(
                        outputOptions,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onMediaCaptured(Uri.fromFile(photoFile))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .size(70.dp)
                    .background(Purple500, CircleShape)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .border(5.dp, Color.LightGray, CircleShape),
                colors = ButtonDefaults.buttonColors(backgroundColor = Purple500),
            ) {

            }

            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                    else CameraSelector.LENS_FACING_BACK

                    cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector as CameraSelector,
                        imageCapture,
                        preview
                    )
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_rotate),
                    contentDescription = "",
                    modifier = Modifier.size(35.dp),
                    tint = MaterialTheme.colors.surface
                )
            }
        }
    }
}

private class FaceAnalyzer: ImageAnalysis.Analyzer {
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val imagePic = image.image
        imagePic?.close()
    }
}