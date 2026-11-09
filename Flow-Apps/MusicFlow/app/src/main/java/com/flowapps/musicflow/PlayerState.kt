package com.flowapps.musicflow

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

class PlayerState(ctx: Context) {
    var controller by mutableStateOf<MediaController?>(null)
    var playing by mutableStateOf(false)
    var mediaId by mutableStateOf<String?>(null)
    var pos by mutableLongStateOf(0L)
    var dur by mutableLongStateOf(0L)
    var queue by mutableStateOf<List<Song>>(emptyList())
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)

    private val future = MediaController.Builder(
        ctx, SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
    ).buildAsync()

    init {
        future.addListener({
            try {
                val c = future.get()
                controller = c
                playing = c.isPlaying
                mediaId = c.currentMediaItem?.mediaId
                shuffle = c.shuffleModeEnabled
                repeat = c.repeatMode
                c.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { mediaId = mediaItem?.mediaId }
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { shuffle = shuffleModeEnabled }
                    override fun onRepeatModeChanged(repeatMode: Int) { repeat = repeatMode }
                })
            } catch (e: Exception) {
                controller = null
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun Song.toItem() = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).build()
        ).build()

    fun play(list: List<Song>, index: Int) {
        val c = controller ?: return
        queue = list
        c.setMediaItems(list.map { it.toItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun release() { controller?.release() }
}
