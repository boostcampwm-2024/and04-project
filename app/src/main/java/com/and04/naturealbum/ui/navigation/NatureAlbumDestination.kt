package com.and04.naturealbum.ui.navigation

import kotlinx.serialization.Serializable

interface Destination

@Serializable
sealed class NavigateDestination(val route: String) : Destination {
    @Serializable
    data object Home : NavigateDestination("home")

    @Serializable
    data object SavePhoto : NavigateDestination("save_photo")

    @Serializable
    data object Album : NavigateDestination("album")

    @Serializable
    data object SearchLabel : NavigateDestination("search_label")

    @Serializable
    data object Map : NavigateDestination("map")

    @Serializable
    data object MyPage : NavigateDestination("my_page")

    @Serializable
    data object AlbumFolder : NavigateDestination("album_folder")

    @Serializable
    data object PhotoInfo : NavigateDestination("photo_info")

    @Serializable
    data object FriendSearch : NavigateDestination("friend_search")
}
