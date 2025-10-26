package info.plateaukao.einkbro.util

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.view.KeyEvent

class MediaSessionHelper(
    context: Context,
    private val onPlayPause: () -> Unit
) {
    private val mediaSession = MediaSession(context, "EinkBroTTS").apply {
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                onPlayPause()
            }
            override fun onPause() {
                onPlayPause()
            }
            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                val event = mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_HEADSETHOOK -> {
                            onPlayPause()
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(mediaButtonEvent)
            }
        })
        setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_STOP
                )
                .build()
        )
        isActive = true
    }

    fun release() {
        mediaSession.release()
    }
}

