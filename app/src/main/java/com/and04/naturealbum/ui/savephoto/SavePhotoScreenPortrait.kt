package com.and04.naturealbum.ui.savephoto

import android.content.Intent
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Create
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.and04.naturealbum.R
import com.and04.naturealbum.data.room.Label
import com.and04.naturealbum.service.FirebaseInsertService

@Composable
fun SavePhotoScreenPortrait(
    innerPadding: PaddingValues,
    model: Any,
    label: Label?,
    location: Location?,
    rememberDescription: MutableState<String>,
    isRepresented: MutableState<Boolean>,
    photoSaveState: State<UiState>,
    onLabelSelect: () -> Unit,
    onBack: () -> Unit,
    viewModel: SavePhotoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(innerPadding)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.save_photo_screen_image_description),
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(10.dp))
        )

        LabelSelection(
            label = label,
            onClick = onLabelSelect,
        )

        Description(description = rememberDescription.value,
            modifier = Modifier.weight(1f),
            onValueChange = { newDescription -> rememberDescription.value = newDescription }
        )

        ToggleButton(
            selected = isRepresented.value,
            onClick = { isRepresented.value = !isRepresented.value },
            modifier = Modifier
                .padding(vertical = 8.dp)
                .padding(start = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTextButton(
                modifier = Modifier.weight(1f),
                imageVector = Icons.Default.Close,
                stringRes = R.string.save_photo_screen_cancel,
                onClick = { onBack() })
            IconTextButton(
                enabled = (label != null) && (photoSaveState.value != UiState.Loading),
                modifier = Modifier.weight(1f),
                imageVector = Icons.Outlined.Create,
                stringRes = R.string.save_photo_screen_save,
                onClick = {
                    viewModel.savePhoto(
                        uri = model.toString(),
                        label = label!!,
                        description = rememberDescription.value,
                        location = location!!, // TODO : Null 처리 필요
                        isRepresented = isRepresented.value
                    )

                    val intent = Intent(context, FirebaseInsertService::class.java)
                    intent.putExtra("uri", model.toString())
                    intent.putExtra("label", label)
                    intent.putExtra("location", location)
                    intent.putExtra("description", rememberDescription.value)
                    context.startService(intent)
                })
        }

    }
}
