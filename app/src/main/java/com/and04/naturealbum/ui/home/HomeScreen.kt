package com.and04.naturealbum.ui.home

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.and04.naturealbum.R
import com.and04.naturealbum.ui.LocationHandler
import com.and04.naturealbum.ui.PermissionHandler
import com.and04.naturealbum.ui.component.NavigationImageButton
import com.and04.naturealbum.ui.component.PermissionDialogState
import com.and04.naturealbum.ui.component.PermissionDialogs
import com.and04.naturealbum.ui.theme.NatureAlbumTheme
import com.and04.naturealbum.utils.isPortrait

const val MAP_BUTTON_BACKGROUND_OUTLINE_SVG = "btn_home_menu_map_background_outline.svg"

@Composable
fun HomeScreen(
    locationHandler: LocationHandler,
    takePicture: () -> Unit,
    onNavigateToAlbum: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToMyPage: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    var permissionDialogState by remember { mutableStateOf(PermissionDialogState.None) }

    val locationSettingsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                takePicture()
            }
        }
    var permissionsToRequestAgain by remember { mutableStateOf(listOf<String>()) }

    val requestCameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val deniedPermissions = permissions.filter { permission -> !permission.value }.keys
            when {
                deniedPermissions.isEmpty() -> {
                    locationHandler.checkLocationSettings(
                        takePicture = takePicture,
                        showGPSActivationDialog = { intentSenderRequest ->
                            locationSettingsLauncher.launch(intentSenderRequest)
                        },
                        airPlaneModeMessage = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.home_need_wifi_or_off_airplane_mode),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }

                else -> {
                    val hasPreviouslyDeniedPermission = deniedPermissions.any { permission ->
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                    }
                    permissionsToRequestAgain = deniedPermissions.toList()
                    permissionDialogState = if (hasPreviouslyDeniedPermission) {
                        PermissionDialogState.Explain
                    } else {
                        PermissionDialogState.GoToSettings
                    }
                }
            }
        }

    val cameraPermissionHandler = remember {
        PermissionHandler(
            context = context,
            allPermissionGranted = {
                locationHandler.checkLocationSettings(
                    takePicture = takePicture,
                    showGPSActivationDialog = { intentSenderRequest ->
                        locationSettingsLauncher.launch(intentSenderRequest)
                    },
                    airPlaneModeMessage = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.home_need_wifi_or_off_airplane_mode),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onRequestPermission = { deniedPermissions ->
                requestCameraPermissionLauncher.launch(deniedPermissions)
            },
            showPermissionExplainDialog = {
                permissionDialogState = PermissionDialogState.Explain
            }
        )
    }

    if (context.isPortrait()) {
        HomeScreenPortrait(
            context = context,
            onClickCamera = { cameraPermissionHandler.checkPermissions(PermissionHandler.Permissions.CAMERA) },
            onNavigateToAlbum = onNavigateToAlbum,
            onNavigateToMyPage = onNavigateToMyPage,
            onNavigateToMap = onNavigateToMap,
        )
    } else {
        HomeScreenLandscape(
            context = context,
            onClickCamera = { cameraPermissionHandler.checkPermissions(PermissionHandler.Permissions.CAMERA) },
            onNavigateToAlbum = onNavigateToAlbum,
            onNavigateToMyPage = onNavigateToMyPage,
            onNavigateToMap = onNavigateToMap,
        )
    }
    PermissionDialogs(
        permissionDialogState = permissionDialogState,
        onDismiss = { permissionDialogState = PermissionDialogState.None },
        onRequestPermission = {
            cameraPermissionHandler.requestPermissions(permissionsToRequestAgain)
        },
        onGoToSettings = {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(this)
            }
        }
    )
}

@Composable
fun MainBackground(modifier: Modifier) {
    Image(
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
        imageVector = ImageVector.vectorResource(id = R.drawable.drawable_home_main_background),
        contentDescription = null
    )
}

@Composable
fun NavigateContent(
    modifier: Modifier = Modifier,
    onClickCamera: () -> Unit,
    onNavigateToAlbum: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        NavigationImageButton(
            text = stringResource(R.string.home_navigate_to_album),
            modifier = Modifier
                .weight(1f),
            textColor = Color.White,
            imageVector = ImageVector.vectorResource(id = R.drawable.btn_album_background)
        ) { onNavigateToAlbum() }

        NavigationImageButton(
            text = stringResource(R.string.home_navigate_to_camera),
            modifier = Modifier
                .weight(1f),
            textColor = Color.Black,
            imageVector = ImageVector.vectorResource(id = R.drawable.btn_camera_background)
        ) { onClickCamera() }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun HomeScreenPreview() {
    NatureAlbumTheme {
        HomeScreenPortrait(
            context = LocalContext.current,
            onClickCamera = { },
            onNavigateToAlbum = { },
            onNavigateToMyPage = { },
            onNavigateToMap = { },
        )
    }
}
