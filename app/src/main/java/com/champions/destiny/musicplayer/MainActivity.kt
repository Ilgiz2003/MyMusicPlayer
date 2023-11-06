package com.champions.destiny.musicplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.Menu
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.champions.destiny.musicplayer.databinding.ActivityMainBinding
import com.champions.destiny.musicplayer.databinding.PlayerViewBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import java.time.Duration


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var songAdapter: SongAdapter
    private var allSongs: ArrayList<Song> = ArrayList()
    private lateinit var player: ExoPlayer
    private var isBound = false
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            println("ДА")
        } else {
            println("НЕТ")
        }
    }
    private var playerService: PlayerService? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(binding.playerView.playerView.visibility == View.VISIBLE){
                    exitPlayerView()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = resources.getString(R.string.app_name)
        if(checkPermission()){
            doBindService()
        } else {
            requestPermission()
        }
    }
    private fun checkPermission():Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }



    private fun doBindService(){
        val playerServiceIntent = Intent(this, PlayerService::class.java)
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private val playerServiceConnection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: PlayerService.ServiceBinder = service as PlayerService.ServiceBinder
            playerService = binder.getPlayerService()
            player = playerService!!.player
            isBound = true
            playerControls()
            fetchSongs()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
        }
    }


    private fun playerControls() {
        binding.playerView.songNameView.isSelected = true
        binding.homeSongNameView.isSelected = true

        binding.playerView.playerCloseBtn.setOnClickListener {
            exitPlayerView()
        }

        binding.homeControlWrapper.setOnClickListener {
            showPlayerView()
        }

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (mediaItem != null) {
                    binding.playerView.songNameView.text = mediaItem.mediaMetadata.title
                    binding.homeSongNameView.text = mediaItem.mediaMetadata.title
                }
                binding.playerView.progressView.text = getReadableTime(player.currentPosition)
                binding.playerView.seekbar.progress = player.currentPosition.toInt()
                binding.playerView.seekbar.max = player.duration.toInt()
                binding.playerView.durationView.text = getReadableTime(player.duration)
                binding.playerView.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_pause_outline,
                    0,
                    0,
                    0
                )
                binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_pause,
                    0,
                    0,
                    0
                )
                showCurrentArtImage()
                updatePlayerPositionProgress()
                binding.playerView.artImageView.animation = loadRotation()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == ExoPlayer.STATE_READY) {
                    binding.playerView.songNameView.text =
                        player.currentMediaItem?.mediaMetadata!!.title
                    binding.homeSongNameView.text = player.currentMediaItem?.mediaMetadata!!.title
                    binding.playerView.progressView.text = getReadableTime(player.currentPosition)
                    binding.playerView.durationView.text = getReadableTime(player.duration)
                    binding.playerView.seekbar.max = player.duration.toInt()
                    binding.playerView.seekbar.progress = player.currentPosition.toInt()
                    binding.playerView.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_pause_outline,
                        0,
                        0,
                        0
                    )
                    binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_pause,
                        0,
                        0,
                        0
                    )
                    showCurrentArtImage()
                    updatePlayerPositionProgress()
                } else {
                    binding.playerView.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_play_outline,
                        0,
                        0,
                        0
                    )
                    binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_play,
                        0,
                        0,
                        0
                    )
                }
            }
        })
        binding.playerView.skipNextBtn.setOnClickListener {
            skipToNextSong()
        }
        binding.homeSkipNextBtn.setOnClickListener {
            skipToNextSong()
        }
        binding.playerView.skipPreviousBtn.setOnClickListener {
            skipToPreviousSong()
        }
        binding.homeSkipPreviousBtn.setOnClickListener {
            skipToPreviousSong()
        }
        binding.homePlayPauseBtn.setOnClickListener {
            playOrPausePlayer()
        }
        binding.playerView.playPauseBtn.setOnClickListener {
            playOrPausePlayer()
        }
        binding.playerView.seekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            var progressValue = 0

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                progressValue = seekBar!!.progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (player.playbackState == ExoPlayer.STATE_READY) {
                    seekBar!!.progress = progressValue
                    binding.playerView.progressView.text = getReadableTime(progressValue.toLong())
                    player.seekTo(progressValue.toLong())
                }
            }

        })
    }

    private fun playOrPausePlayer() {
        if (player.isPlaying) {
            player.pause()
            binding.playerView.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_play_outline,
                0,
                0,
                0
            )
            binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_play,
                0,
                0,
                0
            )
            binding.playerView.artImageView.clearAnimation()
        } else {
            player.play()
            binding.playerView.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_pause_outline,
                0,
                0,
                0
            )
            binding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_pause,
                0,
                0,
                0
            )
            binding.playerView.artImageView.startAnimation(loadRotation())
        }
    }

    private fun skipToNextSong() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }

    private fun skipToPreviousSong() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    private fun loadRotation(): Animation {
        val rotateAnimation = RotateAnimation(
            0f,
            360f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotateAnimation.interpolator = LinearInterpolator()
        rotateAnimation.duration = 10000
        rotateAnimation.repeatCount = Animation.INFINITE
        return rotateAnimation
    }

    private fun updatePlayerPositionProgress() {
        Handler().postDelayed({
            if (player.isPlaying) {
                binding.playerView.progressView.text = getReadableTime(player.currentPosition)
                binding.playerView.seekbar.progress = player.currentPosition.toInt()
            }
            updatePlayerPositionProgress()
        }, 1000)
    }

    private fun showCurrentArtImage() {
        binding.playerView.artImageView.setImageResource(
            player.currentMediaItem?.mediaMetadata?.artist.toString().toInt()
        )
    }

    private fun getReadableTime(currentPosition: Long): String {
        val milliseconds = Duration.ofMillis(currentPosition)
        val minutes = milliseconds.toMinutes()
        val seconds = milliseconds.minusMinutes(minutes).seconds
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun showPlayerView() {
        binding.playerView.playerView.visibility = View.VISIBLE
    }

    private fun exitPlayerView() {
        binding.playerView.playerView.visibility = View.GONE
    }

    private fun fetchSongs() {
        var songs: ArrayList<Song> = ArrayList()
        songs.add(Song("1995", R.drawable.cmh_antiwrld, 160000, R.raw.cmh_1995))
        songs.add(Song("Антифриз", R.drawable.cmh_antiwrld, 166000, R.raw.cmh_antifriz))
        songs.add(Song("Aутсайдер", R.drawable.cmh_antiwrld, 129000, R.raw.cmh_autsajjdjer))
        songs.add(Song("BRATZ", R.drawable.cmh_antiwrld, 124000, R.raw.cmh_bratz))
        songs.add(
            Song(
                "Дом ленинградского техно",
                R.drawable.cmh,
                150000,
                R.raw.cmh_dom_leningradskogo_tekhno
            )
        )
        songs.add(Song("Milf Money", R.drawable.cmh, 157000, R.raw.cmh_milf_money))
        songs.add(Song("Мирамистин", R.drawable.cmh_antiwrld, 120000, R.raw.cmh_miramistin))
        songs.add(Song("Montana", R.drawable.cmh, 134000, R.raw.cmh_montana))
        songs.add(Song("Slipknot", R.drawable.cmh, 143000, R.raw.cmh_slipknot))
        songs.add(Song("Героиновый шик", R.drawable.gspd, 220000, R.raw.gspd_geroinovyjj_shik))
        songs.add(
            Song(
                "BelieverBelieverBelieverBelieverBelieverBelieverBelieverBelieverBelieverBeliever",
                R.drawable.im_dr,
                203000,
                R.raw.imagine_dragons_believer
            )
        )
        showSongs(songs)
    }

    private fun showSongs(songs: List<Song>) {
        allSongs.clear()
        allSongs.addAll(songs)

        val title = resources.getString(R.string.app_name) + " - " + songs.size
        supportActionBar?.title = title

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        songAdapter = SongAdapter(songs, this, player)
        binding.recyclerView.adapter = songAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.seatch_btn, menu)
        val menuItem = menu?.findItem(R.id.searchBtn)
        val searchView: SearchView = menuItem?.actionView as SearchView
        searchSong(searchView)
        return super.onCreateOptionsMenu(menu)
    }

    private fun searchSong(searchView: SearchView?) {
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filterSongs(newText.lowercase())
                }
                return true
            }

        });
    }

    private fun filterSongs(query: String) {
        val filteredList: ArrayList<Song> = ArrayList()
        if (allSongs.size > 0) {
            for (song: Song in allSongs) {
                if (song.title.lowercase().contains(query)) {
                    filteredList.add(song)
                }
            }
            songAdapter.filterSongs(filteredList)
        }
    }

    override fun onRestart() {
        super.onRestart()
        doBindService()
    }

    private fun doUnbindService(){
        if(isBound){
            unbindService(playerServiceConnection)
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        doUnbindService()
        playerService?.stopSelf()
    }

}