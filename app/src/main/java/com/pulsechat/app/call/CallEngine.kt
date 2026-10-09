package com.pulsechat.app.call

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class CallState { IDLE, CONNECTING, CONNECTED, RECONNECTING, FAILED, ENDED }

/**
 * Wraps a LiveKit Room: joins the call room, publishes mic (and camera),
 * and exposes the remote/local video tracks to Compose.
 */
class CallEngine(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room

    private val _state = MutableStateFlow(CallState.IDLE)
    val state: StateFlow<CallState> = _state

    private val _micOn = MutableStateFlow(true)
    val micOn: StateFlow<Boolean> = _micOn

    private val _camOn = MutableStateFlow(false)
    val camOn: StateFlow<Boolean> = _camOn

    private val _remoteVideo = MutableStateFlow<VideoTrack?>(null)
    val remoteVideo: StateFlow<VideoTrack?> = _remoteVideo

    private val _localVideo = MutableStateFlow<VideoTrack?>(null)
    val localVideo: StateFlow<VideoTrack?> = _localVideo

    private val _remoteCount = MutableStateFlow(0)
    val remoteCount: StateFlow<Int> = _remoteCount

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun start(url: String, token: String, video: Boolean) {
        if (_state.value != CallState.IDLE) return
        val r = LiveKit.create(appContext)
        _room.value = r
        _error.value = null
        _camOn.value = video
        _micOn.value = true
        _state.value = CallState.CONNECTING

        job = scope.launch {
            launch {
                r.events.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> {
                            val t = event.track
                            if (t is VideoTrack) _remoteVideo.value = t
                            _remoteCount.value = r.remoteParticipants.size
                        }
                        is RoomEvent.TrackUnsubscribed -> {
                            val t = event.track
                            if (t is VideoTrack && _remoteVideo.value === t) _remoteVideo.value = null
                        }
                        is RoomEvent.ParticipantConnected -> _remoteCount.value = r.remoteParticipants.size
                        is RoomEvent.ParticipantDisconnected -> {
                            _remoteCount.value = r.remoteParticipants.size
                            if (r.remoteParticipants.isEmpty()) {
                                _remoteVideo.value = null
                                _state.value = CallState.ENDED
                            }
                        }
                        is RoomEvent.Reconnecting -> _state.value = CallState.RECONNECTING
                        is RoomEvent.Reconnected -> _state.value = CallState.CONNECTED
                        is RoomEvent.Disconnected -> _state.value = CallState.ENDED
                        is RoomEvent.FailedToConnect -> {
                            _error.value = event.error.message ?: "Connection failed"
                            _state.value = CallState.FAILED
                        }
                        else -> {}
                    }
                }
            }

            try {
                r.connect(url, token)
                _state.value = CallState.CONNECTED
                r.localParticipant.setMicrophoneEnabled(true)
                if (video) {
                    r.localParticipant.setCameraEnabled(true)
                    val lp = r.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track
                    if (lp is LocalVideoTrack) _localVideo.value = lp
                }
                _remoteCount.value = r.remoteParticipants.size
                val rv = r.remoteParticipants.values.firstOrNull()
                    ?.getTrackPublication(Track.Source.CAMERA)?.track
                if (rv is VideoTrack) _remoteVideo.value = rv
            } catch (e: Exception) {
                _error.value = e.message ?: "Connection failed"
                _state.value = CallState.FAILED
            }
        }
    }

    fun toggleMic() {
        val r = _room.value ?: return
        val next = !_micOn.value
        _micOn.value = next
        scope.launch { runCatching { r.localParticipant.setMicrophoneEnabled(next) } }
    }

    fun toggleCamera() {
        val r = _room.value ?: return
        val next = !_camOn.value
        _camOn.value = next
        scope.launch {
            runCatching {
                r.localParticipant.setCameraEnabled(next)
                _localVideo.value = if (next) {
                    val t = r.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track
                    if (t is LocalVideoTrack) t else null
                } else null
            }
        }
    }

    fun flipCamera() {
        val r = _room.value ?: return
        scope.launch {
            runCatching {
                val t = r.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track
                if (t is LocalVideoTrack) t.switchCamera()
            }
        }
    }

    fun stop() {
        val r = _room.value
        _room.value = null
        _state.value = CallState.IDLE
        _remoteVideo.value = null
        _localVideo.value = null
        _remoteCount.value = 0
        job?.cancel()
        job = null
        if (r != null) scope.launch { runCatching { r.disconnect() } }
    }
}
