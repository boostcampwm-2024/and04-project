package com.and04.naturealbum.ui.maps

import android.graphics.PointF
import androidx.annotation.IntRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.and04.naturealbum.R
import com.naver.maps.map.NaverMap
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.DefaultClusterMarkerUpdater
import com.naver.maps.map.clustering.DefaultLeafMarkerUpdater
import com.naver.maps.map.clustering.DefaultMarkerManager
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.clustering.MarkerInfo
import com.naver.maps.map.overlay.Align
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage

class ClusterManager(
    val colorRange: ColorRange,
    val onMarkerClick: (MarkerInfo) -> Overlay.OnClickListener,
    val onClusterChange: (MarkerInfo) -> Unit
) {
    private val cluster: Clusterer<PhotoKey>

    init {
        cluster = Clusterer.ComplexBuilder<PhotoKey>().tagMergeStrategy { cluster ->
            cluster.children.flatMap { node -> node.tag as List<*> }
        }.clusterMarkerUpdater(object : DefaultClusterMarkerUpdater() {
            override fun updateClusterMarker(info: ClusterMarkerInfo, marker: Marker) {
                onClusterChange(info)
                marker.zIndex = info.size
                marker.captionText = info.size.toString()
                marker.iconTintColor = sizeToTint(info.size)
                marker.onClickListener = onMarkerClick(info)
            }
        }).leafMarkerUpdater(object : DefaultLeafMarkerUpdater() {
            override fun updateLeafMarker(info: LeafMarkerInfo, marker: Marker) {
                onClusterChange(info)
                marker.zIndex = LEAF_NODE_SIZE
                marker.captionText = LEAF_NODE_SIZE.toString()
                marker.iconTintColor = sizeToTint(LEAF_NODE_SIZE)
                marker.onClickListener = onMarkerClick(info)
            }
        }).markerManager(object : DefaultMarkerManager() {
            override fun createMarker(): Marker {
                return Marker().apply {
                    globalZIndex = DEFAULT_MARKER_Z_INDEX - 1
                    icon = markerIcon
                    isFlat = true
                    anchor = PointF(0.5f, 0.5f)
                    setCaptionAligns(Align.Center, Align.Center)
                    captionTextSize = 24f
                }
            }
        }).build()
    }

    fun setPhotoItems(photoItems: List<PhotoItem>) {
        cluster.clear()
        cluster.addAll(photoItems.associate { photoItem ->
            PhotoKey(
                photoItem
            ) to listOf(photoItem)
        })
    }

    fun setMap(map: NaverMap?) {
        cluster.map = map
    }

    fun clear() {
        cluster.clear()
        setMap(null)
    }

    private fun sizeToTint(size: Int): Int = sizeToTint(size, colorRange.min, colorRange.max)

    companion object {
        private const val LEAF_NODE_SIZE = 1
        private const val DEFAULT_MARKER_Z_INDEX = 200000

        private val markerIcon = OverlayImage.fromResource(R.drawable.ic_cluster)

        private fun sizeToTint(
            size: Int, min: Color, max: Color, @IntRange(from = 1) threshold: Int = 20
        ): Int = lerp(min, max, size / threshold.toFloat()).toArgb()
    }
}

enum class ColorRange(val min: Color, val max: Color) {
    RED(Color(0xFF400000), Color(0xFFFF0000)),
    YELLOW(        Color(0xFF404000), Color(0xFFFFFF00)    ),
    GREEN(Color(0xFF004000), Color(0xFF00FF00)), CYAN(
        Color(0xFF004040), Color(0xFF00FFFF)
    ),
    BLUE(Color(0xFF000040), Color(0xFF0000FF)),
}