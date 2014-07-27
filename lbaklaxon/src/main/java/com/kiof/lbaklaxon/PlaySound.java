package com.kiof.lbaklaxon;

import java.util.Random;
import android.app.Activity;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.widget.Toast;

public class PlaySound extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// super.onCreate(savedInstanceState);
		super.onCreate(null);
		final Resources resources = getResources();
		String[] buttonArray = resources.getStringArray(R.array.buttonArray);
		String[] textButtonArray = resources.getStringArray(R.array.textButtonArray);

		String soundName = this.getIntent().getStringExtra("soundName");
		if (soundName == null) {
			int soundId = this.getIntent().getIntExtra("soundId", 0);
			if (soundId == 0) {
				int soundNb = new Random().nextInt(buttonArray.length);
				Toast.makeText(this, textButtonArray[soundNb], Toast.LENGTH_SHORT).show();
				playSound(buttonArray[soundNb]);
			} else {
				playSound(soundId);
			}
		} else {
			playSound(soundName);
		}

//		int soundNb = new Random().nextInt(buttonArray.length);
//		Toast.makeText(this, textButtonArray[soundNb], Toast.LENGTH_SHORT).show();
//		playSound(buttonArray[soundNb]);
		
		this.finish();
	}

	int playSound(String soundName) {
		int soundId;
		try {
			soundId = getResources().getIdentifier(soundName, "raw", getPackageName());
		} catch (Exception e) {
			return 0;
		}
		return playSound(soundId);
	}
	
	int playSound(int soundId) {
		MediaPlayer mp = MediaPlayer.create(getApplicationContext(), soundId);
		if (mp == null) return 0;
		mp.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				mp.release();
			};
		});
		mp.start();
		return mp.getDuration();
	}
}
