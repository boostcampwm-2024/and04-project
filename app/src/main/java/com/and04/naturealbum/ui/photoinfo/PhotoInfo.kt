package com.and04.naturealbum.ui.photoinfo

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.and04.naturealbum.R
import com.and04.naturealbum.data.room.Label
import com.and04.naturealbum.data.room.PhotoDetail
import com.and04.naturealbum.ui.component.AlbumLabel
import com.and04.naturealbum.ui.model.AlbumData
import com.and04.naturealbum.ui.model.UiState
import com.and04.naturealbum.utils.GetTopbar
import com.and04.naturealbum.utils.toColor
import com.and04.naturealbum.utils.toDate

@Composable
fun PhotoInfo(
    selectedPhotoDetail: Int = 0,
    onNavigateToMyPage: () -> Unit,
    photoInfoViewModel: PhotoInfoViewModel = hiltViewModel(),
) {
    val isDataLoaded = rememberSaveable { mutableStateOf(false) }
    val uiState = photoInfoViewModel.uiState.collectAsStateWithLifecycle()
    val address = photoInfoViewModel.address.collectAsStateWithLifecycle()

    LaunchedEffect(selectedPhotoDetail) {
        if (!isDataLoaded.value) {
            photoInfoViewModel.loadPhotoDetail(selectedPhotoDetail)
            isDataLoaded.value = true
        }
    }

    PhotoInfo(
        onNavigateToMyPage = onNavigateToMyPage,
        uiState = uiState,
        address = address,
        setAlbumThumbnail = photoInfoViewModel::setAlbumThumbnail,
    )
}

@Composable
fun PhotoInfo(
    onNavigateToMyPage: () -> Unit,
    uiState: State<UiState<AlbumData>>,
    address: State<String>,
    setAlbumThumbnail: (Int) -> Unit,
) {
    Scaffold(
        topBar = { LocalContext.current.GetTopbar { onNavigateToMyPage() } }
    ) { innerPadding ->
        Content(
            innerPadding = innerPadding,
            uiState = uiState,
            address = address,
            setAlbumThumbnail = setAlbumThumbnail,
        )
    }
}

@Composable
private fun Content(
    innerPadding: PaddingValues,
    uiState: State<UiState<AlbumData>>,
    address: State<String>,
    setAlbumThumbnail: (Int) -> Unit,
) {
    when (val success = uiState.value) {
        is UiState.Idle, UiState.Loading -> {
            //TODO Loading
        }

        is UiState.Success -> {
            val photoDetail = success.data.photoDetails
            val label = success.data.label

            PhotoDetailInfo(
                innerPadding = innerPadding,
                photoDetail = photoDetail,
                label = label,
                address = address,
                setAlbumThumbnail = setAlbumThumbnail,
            )
        }

        is UiState.Error<*> -> { /* TODO ERROR */
        }
    }
}

@Composable
private fun PhotoDetailInfo(
    innerPadding: PaddingValues,
    photoDetail: PhotoDetail,
    label: Label,
    address: State<String>,
    setAlbumThumbnail: (Int) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        PhotoInfoLandscape(
            innerPadding = innerPadding,
            photoDetail = photoDetail,
            label = label,
            address = address,
            setAlbumThumbnail = setAlbumThumbnail,
        )
    } else {
        PhotoInfoPortrait(
            innerPadding = innerPadding,
            photoDetail = photoDetail,
            label = label,
            address = address,
            setAlbumThumbnail = setAlbumThumbnail,
        )
    }
}

@Composable
fun RowInfo(
    imgVector: ImageVector,
    contentDescription: String,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = imgVector,
            contentDescription = contentDescription,
        )
        Text(text)
    }
}

@Composable
fun SetThumbnailContent(
    photoDetail: PhotoDetail,
    setAlbumThumbnail: (Int) -> Unit,
) {
    Button(onClick = { setAlbumThumbnail(photoDetail.id) }) {
        Text(text = stringResource(R.string.photo_info_set_thumbnail_btn_txt))
    }
}


@Preview
@Composable
private fun PhotoInfoPreview() {
    val uiState = remember {
        mutableStateOf(
            UiState.Success(
                AlbumData(
                    Label.emptyLabel(),
                    PhotoDetail.emptyPhotoDetail()
                )
            )
        )
    }
    val address = remember { mutableStateOf("") }

    PhotoInfo(
        onNavigateToMyPage = {},
        uiState = uiState,
        address = address,
        setAlbumThumbnail = { },
    )
}
