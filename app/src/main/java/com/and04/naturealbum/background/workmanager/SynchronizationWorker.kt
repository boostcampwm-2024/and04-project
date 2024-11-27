package com.and04.naturealbum.background.workmanager

import android.content.Context
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.and04.naturealbum.data.datastore.DataStoreManager
import com.and04.naturealbum.data.dto.FirebaseLabel
import com.and04.naturealbum.data.dto.FirebasePhotoInfo
import com.and04.naturealbum.data.dto.UnSynchronizedAlbumsDto
import com.and04.naturealbum.data.dto.UnSynchronizedPhotoDetailsDto
import com.and04.naturealbum.data.repository.DataRepository
import com.and04.naturealbum.data.repository.FireBaseRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@HiltWorker
class SynchronizationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val roomRepository: DataRepository,
    private val fireBaseRepository: FireBaseRepository,
    private val syncDataStore: DataStoreManager
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        private var IS_RUNNING = false
        private const val WORKER_NAME = "WORKER_SYNCHRONIZATION"
        private const val HOUR = 0
        private const val MINUTE = 0

        fun runSync(context: Context) {
            val duration = getDurationTime()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) //Wifi 연결 시 실행
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SynchronizationWorker>()
                .setInitialDelay(duration.seconds, TimeUnit.SECONDS) // 지금부터 정각까지 지연 후 실행
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORKER_NAME,
                    ExistingWorkPolicy.REPLACE, //기존 작업을 새 작업으로 전환
                    workRequest
                )
        }

        fun runImmediately(context: Context) {
            if (!IS_RUNNING) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED) //Wifi 연결 시 실행
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<SynchronizationWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        WORKER_NAME,
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
            }
        }


        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORKER_NAME)
        }

        private fun getDurationTime(): Duration {
            val triggerHour = HOUR
            val triggerMinute = MINUTE

            val newSyncTime = LocalTime.of(triggerHour, triggerMinute)
            val now: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            val nowTime: LocalTime = now.toLocalTime()

            val plusDay = if (nowTime == newSyncTime || nowTime.isAfter(newSyncTime)) 1 else 0

            val nextTriggerTime = now.plusDays(plusDay.toLong())
                .withHour(newSyncTime.hour)
                .withMinute(newSyncTime.minute)

            return Duration.between(LocalDateTime.now(), nextTriggerTime) //다음 정각까지 남은 시간
        }
    }

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val currentUser = Firebase.auth.currentUser ?: return@coroutineScope Result.failure()
            val uid = currentUser.uid
            IS_RUNNING = true

            val label = async {
                val labels = fireBaseRepository.getLabels(uid)
                val unSynchronizedAlbums = roomRepository.getUnSynchronizedAlbums(labels)

                unSynchronizedAlbums.forEach { album ->
                    launch {
                        insertLabel(uid, album)
                    }
                }
            }

            val photoDetail = async {
                val fileNames = fireBaseRepository.getPhotos(uid)
                val unSynchronizedPhotoDetails =
                    roomRepository.getUnSynchronizedPhotoDetails(fileNames)

                unSynchronizedPhotoDetails.forEach { photo ->
                    launch {
                        insertPhotoDetail(uid, photo)
                    }
                }
            }

            label.await()
            photoDetail.await()

            syncDataStore.setSyncTime(
                LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            )
            Result.success()
        } catch (e: Exception) {
            //TODO FireStore와 LocalDB 비교 후 같이면 Result.success() 다르면 retry()
            Result.retry()
        } finally {
            IS_RUNNING = false
            runSync(applicationContext)
        }
    }

    private suspend fun insertLabel(uid: String, album: UnSynchronizedAlbumsDto) {
        val storageUri = fireBaseRepository
            .saveImageFile(
                uid = uid,
                label = album.labelName,
                fileName = album.fileName,
                uri = album.photoDetailUri.toUri(),
            )

        fireBaseRepository
            .insertLabel(
                uid = uid,
                labelName = album.labelName,
                labelData = FirebaseLabel(
                    backgroundColor = album.labelBackgroundColor,
                    thumbnailUri = storageUri.toString()
                )
            )
    }

    private suspend fun insertPhotoDetail(uid: String, photo: UnSynchronizedPhotoDetailsDto) {
        val storageUri = fireBaseRepository
            .saveImageFile(
                uid = uid,
                label = photo.labelName,
                fileName = photo.fileName,
                uri = photo.photoDetailUri.toUri(),
            )

        fireBaseRepository
            .insertPhotoInfo(
                uid = uid,
                fileName = photo.fileName,
                photoData = FirebasePhotoInfo(
                    uri = storageUri.toString(),
                    label = photo.labelName,
                    latitude = photo.latitude,
                    longitude = photo.longitude,
                    description = photo.description,
                    datetime = photo.datetime
                )
            )
    }
}
