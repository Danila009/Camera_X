package com.example.camerax

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@RequiresApi(Build.VERSION_CODES.M)
@ExperimentalPermissionsApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uri = remember { mutableStateOf("") }
            val permission = rememberPermissionState(permission = Manifest.permission.CAMERA)

            LaunchedEffect(key1 = Unit, block = {
                permission.launchPermissionRequest()
            })

            PermissionRequired(
                permissionState = permission,
                permissionNotGrantedContent = {
                    Text(text = "permissionNotGrantedContent")
                }, permissionNotAvailableContent = {
                    Text(text = "permissionNotAvailableContent")
                }, content = {
                    Column {
                        Text(text = uri.value)
                        CameraView(
                            directory =  getDirectory(),
                            onMediaCaptured = {
                                uri.value = it.toString()
                            }
                        )
                    }
                }
            )
        }
    }

    private fun getDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
}
