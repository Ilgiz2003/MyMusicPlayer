package com.champions.destiny.musicplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.champions.destiny.musicplayer.databinding.SongRowItemBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import java.time.Duration


class SongAdapter(var songs: List<Song>, private val context: Context, private val player: ExoPlayer) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class SongViewHolder(val binding: SongRowItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val artImageHolder: ImageView = binding.artImage
        val titleHolder: TextView = binding.songTitle
        val durationHolder: TextView = binding.durationText

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = SongRowItemBinding.inflate(inflater, parent, false)
        return SongViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val song: Song = songs[position]
        val viewHolder: SongViewHolder = holder as SongViewHolder

        viewHolder.titleHolder.text = song.title
        viewHolder.durationHolder.text = getDuration(song.duration)

        viewHolder.artImageHolder.setImageResource(song.artImage)

        viewHolder.itemView.setOnClickListener {
            context.startService(Intent(context.applicationContext, PlayerService::class.java))
            if(!player.isPlaying){
                 player.setMediaItems(getMediaItems(), position, 0)
            } else {
                player.pause()
                player.seekTo(position, 0)
            }

            player.prepare()
            player.play()
        }
    }

    private fun getMediaItems(): ArrayList<MediaItem> {
        val mediaItems:ArrayList<MediaItem> = ArrayList()
        for(song: Song in songs){
            val rawDataSource = RawResourceDataSource(context)
            rawDataSource.open(DataSpec(RawResourceDataSource.buildRawResourceUri(song.mediaId)))
            val mediaItem = MediaItem.Builder()
                .setUri(rawDataSource.uri)
                .setMediaMetadata(getMetadata(song))
                .build()

            mediaItems.add(mediaItem)
        }
        return mediaItems
    }

    private fun getMetadata(song: Song): MediaMetadata {
        return MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artImage.toString())
            .build()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterSongs(filteredList: List<Song>) {
        songs = filteredList
        notifyDataSetChanged()
    }

    private fun getDuration(duration: Int): String {
        val milliseconds = Duration.ofMillis(duration.toLong())
        val minutes = milliseconds.toMinutes()
        val seconds = milliseconds.minusMinutes(minutes).seconds
        return String.format("%02d:%02d", minutes, seconds)
    }

}