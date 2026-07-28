package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class GameAudioManager(context: Context) {
    private val soundPool: SoundPool
    
    private var turnSoundId: Int = 0
    private var collectSoundId: Int = 0
    private var crashSoundId: Int = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
            
        // Assuming sound files would be loaded here:
        // turnSoundId = soundPool.load(context, R.raw.turn, 1)
        // collectSoundId = soundPool.load(context, R.raw.collect, 1)
        // crashSoundId = soundPool.load(context, R.raw.crash, 1)
    }
    
    fun playTurn() {
        if (turnSoundId != 0) soundPool.play(turnSoundId, 0.5f, 0.5f, 1, 0, 1f)
    }
    
    fun playCollect() {
        if (collectSoundId != 0) soundPool.play(collectSoundId, 1f, 1f, 1, 0, 1f)
    }
    
    fun playCrash() {
        if (crashSoundId != 0) soundPool.play(crashSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
