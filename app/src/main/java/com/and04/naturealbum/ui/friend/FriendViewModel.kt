package com.and04.naturealbum.ui.friend

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.and04.naturealbum.data.dto.FirebaseFriend
import com.and04.naturealbum.data.dto.FirebaseFriendRequest
import com.and04.naturealbum.data.dto.FirestoreUserWithStatus
import com.and04.naturealbum.data.dto.FriendStatus
import com.and04.naturealbum.data.repository.FireBaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class FriendViewModel @Inject constructor(
    private val fireBaseRepository: FireBaseRepository,
) : ViewModel() {

    private val _receivedFriendRequests = MutableStateFlow<List<FirebaseFriendRequest>>(emptyList())
    val receivedFriendRequests: StateFlow<List<FirebaseFriendRequest>> = _receivedFriendRequests

    private val _friends = MutableStateFlow<List<FirebaseFriend>>(emptyList())
    val friends: StateFlow<List<FirebaseFriend>> = _friends

    private val _searchQuery = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<Map<String, FirestoreUserWithStatus>>(emptyMap())
    val searchResults: StateFlow<Map<String, FirestoreUserWithStatus>> = _searchResults

    private val _friendRequestStatus = MutableStateFlow<Boolean?>(null)
    val friendRequestStatus: StateFlow<Boolean?> = _friendRequestStatus

    private val debouncePeriod = 100L
    private var uid: String? = null

    private var currentSearchJob: Job? = null

    fun initialize(userUid: String) {
        if (uid == userUid) return // 이미 로그인된 상태라면 중복 초기화 방지
        uid = userUid
        listenToFriends()
        listenToReceivedFriendRequests()
        startSearchQueryListener()
    }

    private fun startSearchQueryListener() {
        viewModelScope.launch {
            _searchQuery
                .debounce(debouncePeriod)
                .distinctUntilChanged()
                .collect { query ->
                    uid?.let {
                        fetchFilteredUsersAsFlow(query)
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun fetchFilteredUsersAsFlow(query: String) {
        uid?.let { currentUid ->
            currentSearchJob?.cancel()
            currentSearchJob = viewModelScope.launch {
                fireBaseRepository.searchUsersAsFlow(currentUid, query).collectLatest { results ->
                    _searchResults.value = results
                }
            }
        }
    }

    private fun listenToFriends() {
        uid?.let { currentUid ->
            viewModelScope.launch {
                fireBaseRepository.getFriendsAsFlow(currentUid).collect { friends ->
                    _friends.value = friends
                }
            }
        }
    }

    private fun listenToReceivedFriendRequests() {
        uid?.let { currentUid ->
            viewModelScope.launch {
                fireBaseRepository.getReceivedFriendRequestsAsFlow(currentUid)
                    .collect { receivedFriendRequests ->
                        _receivedFriendRequests.value = receivedFriendRequests
                    }
            }
        }
    }

    fun setFriendRequestStatusNull() {
        _friendRequestStatus.value = null
    }

    fun sendFriendRequest(targetUid: String) {
        uid?.let { currentUid ->
            viewModelScope.launch {
                val success = fireBaseRepository.sendFriendRequest(currentUid, targetUid)
                if (success) {
                    // 친구 요청이 성공적으로 전송되었을 경우 UI 상태를 업데이트
                    _searchResults.value = _searchResults.value.toMutableMap().apply {
                        // 검색 결과에서 해당 targetUid의 STATUS를 SENT로 변경
                        this[targetUid] =
                            this[targetUid]?.copy(status = FriendStatus.SENT) ?: return@launch
                    }
                }
                _friendRequestStatus.value = success
            }
        }
    }

    fun acceptFriendRequest(targetUid: String) {
        uid?.let { currentUid ->
            viewModelScope.launch {
                fireBaseRepository.acceptFriendRequest(currentUid, targetUid)
            }
        }
    }

    fun rejectFriendRequest(targetUid: String) {
        uid?.let { currentUid ->
            viewModelScope.launch {
                fireBaseRepository.rejectFriendRequest(currentUid, targetUid)
            }
        }
    }
}
