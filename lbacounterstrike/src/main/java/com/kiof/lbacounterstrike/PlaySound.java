package com.kiof.lbacounterstrike;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Random;

public class PlaySound extends Activity {
    private Context mContext;
    private Resources mResources;
    private TypedArray sounds;
    private String[] textButtons;

    private int soundId = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // super.onCreate(savedInstanceState);
        super.onCreate(null);

        mContext = getApplicationContext();
        mResources = mContext.getResources();
        sounds = mResources.obtainTypedArray(R.array.sounds);
        textButtons = mResources.getStringArray(R.array.textButtons);

        soundId = this.getIntent().getIntExtra("soundId", 0);
        if (soundId == 0) {
            int soundNb = new Random().nextInt(sounds.length());
            Toast.makeText(this, textButtons[soundNb], Toast.LENGTH_SHORT).show();
            playSound(sounds.getResourceId(soundNb, 0));
        } else {
            playSound(soundId);
        }
        this.finish();
    }

    int playSound(int soundId) {
        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), soundId);
        if (mp == null)
            return 0;
        mp.setOnCompletionListener(new OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
        mp.start();
        return mp.getDuration();
    }
}
