package com.kiof.lbaklaxon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Soundboard extends Activity implements SensorEventListener {
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private AudioManager mAudioManager;
	private Context mContext;
	private Resources mResources;
	private SharedPreferences mSharedPreferences;

	private static final String CHECK_VOLUME = "checkvolume";
	private static final String VOLUME_MAX = "volumemax";
	private static final String VOLUME_RESTORE = "volumerestore";
	private static final String SHAKE = "shake";
	private static final String BGMUSIC = "bgmusic";
	private static final String LGCLICK = "lgclick";

//	private AdapterContextMenuInfo lastMenuInfo = null;
//	private static int currentSoundId;
	private static String currentSoundName;
	private boolean rotationStatus = false;

	private String[] textSectionArray;
	private String[] buttonArray;
	private String[] textButtonArray;
    public static final int TYPE_RINGTONE  = 1;
    public static final int TYPE_NOTIFICATION  = 2;

	private static int RETURN_SETTING = 1;
	private int initVolume, maxVolume;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// super.onCreate(savedInstanceState);
		super.onCreate(null);

		// Sensor management
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		// Audio management for volume control
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		mContext = getApplicationContext();
		mResources = mContext.getResources();
		textSectionArray = mResources.getStringArray(R.array.textSectionArray);
		buttonArray = mResources.getStringArray(R.array.buttonArray);
		textButtonArray = mResources.getStringArray(R.array.textButtonArray);
		
		// Get Preferences
		PreferenceManager.setDefaultValues(mContext, R.xml.setting, false);
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

//		Editor edit = mSharedPreferences.edit();
//		edit.putBoolean(CHECK_VOLUME, false);
//		edit.commit();

//		String s = mSharedPreferences.getString("text1", "essai2");
//		Toast.makeText(this, "Shared Text : " + s, Toast.LENGTH_LONG).show();

//		boolean a = mSharedPreferences.getBoolean(CHECK_VOLUME, true);
//		Toast.makeText(this, "Shared True : " + a, Toast.LENGTH_SHORT).show();
//		boolean b = mSharedPreferences.getBoolean(CHECK_VOLUME, false);
//		Toast.makeText(this, "Shared False : " + b, Toast.LENGTH_SHORT).show();

		setContentView(R.layout.main);

		View globalTab = findViewById(R.id.globaltab);

		// Initialize text and animations of sections
		for (int sectionNb = 0; sectionNb < textSectionArray.length; sectionNb++) {
			// Find the text view component in the layout
			View section = (View) globalTab.findViewWithTag("section"
					+ String.valueOf(sectionNb));
			TextView textSection = (TextView) section
					.findViewById(R.id.textView);
			// Set the text
			textSection.setText(textSectionArray[sectionNb]);
			// Animation of section
			section.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slidefromright));
		}

		// Initialize text and animations of buttons
		for (int soundNb = 0; soundNb < buttonArray.length; soundNb++) {
			// Find the button in the layout
			View buttonView = globalTab.findViewWithTag(buttonArray[soundNb]);
			Button button = (Button) buttonView.findViewById(R.id.button);
			// Set the text of the button
			button.setText(textButtonArray[soundNb]);
			// Set random animation on buttons
			Animation animButton = AnimationUtils.loadAnimation(this, R.anim.zoomin);
			animButton.setStartOffset(new Random().nextInt(500));
			button.startAnimation(animButton);
			// Register for long click action
			registerForContextMenu(button);
		};

		// Audio management for initVolume control
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		// Propose to set initVolume to max if it is not loud enough
		initVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		if (mSharedPreferences.getBoolean(CHECK_VOLUME, false)) {
			if ((2 * initVolume / maxVolume) < 1) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.volume_title);
				builder.setIcon(android.R.drawable.ic_menu_preferences);
				builder.setMessage(R.string.volume_question);
				builder.setNegativeButton(R.string.volume_no, null);
				builder.setPositiveButton(R.string.volume_yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
												mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
												AudioManager.FLAG_SHOW_UI);
							}
						});
				builder.create();
				builder.show();
			}
//		} else {
//			if (mSharedPreferences.getBoolean(VOLUME_MAX, false)) {
//				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
//						mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
//						AudioManager.FLAG_SHOW_UI);
//			}
		}

		// Display change log if new version
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun()) new HtmlAlertDialog(this, R.raw.about, getString(R.string.about_title),
				android.R.drawable.ic_menu_info_details).show();
		
		// Background music
		if (mSharedPreferences.getBoolean(BGMUSIC, false)) {
//			playSound(R.raw.bgmusic);
			startActivity(new Intent(Soundboard.this, PlaySound.class).putExtra("soundId", R.raw.bgmusic));
			}
	}

	@Override
	protected void onResume() {
		if (!mSharedPreferences.getBoolean(CHECK_VOLUME, false) && mSharedPreferences.getBoolean(VOLUME_MAX, false)) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI);
		}
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (!mSharedPreferences.getBoolean(CHECK_VOLUME, false) && mSharedPreferences.getBoolean(VOLUME_MAX, false) && mSharedPreferences.getBoolean(VOLUME_RESTORE, false)) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, initVolume, AudioManager.FLAG_SHOW_UI);
		}
		mSensorManager.unregisterListener(this);
		super.onPause();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RETURN_SETTING) {
			Toast.makeText(mContext, R.string.setting_saved, Toast.LENGTH_SHORT).show();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	// Catch hardware volume buttons to adjust MusicStream volume
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.setting:
			startActivityForResult(new Intent(Soundboard.this, Setting.class), RETURN_SETTING);
			return true;
		case R.id.about:
			new HtmlAlertDialog(this, R.raw.about, getString(R.string.about_title), android.R.drawable.ic_menu_info_details).show();
			return true;
		case R.id.quit:
			// Create out AlterDialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.quit_title);
			builder.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
			builder.setMessage(R.string.quit_message);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			builder.setNegativeButton(R.string.no,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Toast.makeText(mContext, R.string.goingon, Toast.LENGTH_SHORT) .show();
						}
					});
			builder.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		if (mSharedPreferences.getBoolean(LGCLICK, false)) {
			super.onCreateContextMenu(menu, view, menuInfo);
	
			// Keep track of the current MenuInfo
//			lastMenuInfo = (AdapterContextMenuInfo) menuInfo;
			currentSoundName = (((View) view.getParent()).getTag().toString());
			
			menu.setHeaderTitle(R.string.actions);
	        menu.setHeaderIcon(android.R.drawable.ic_menu_set_as);
			menu.add(Menu.NONE, 0, Menu.NONE, R.string.play);
			menu.add(Menu.NONE, 1, Menu.NONE, R.string.ringtone);
			menu.add(Menu.NONE, 2, Menu.NONE, R.string.notification);
		}
	}

	@Override
	public void onContextMenuClosed(Menu menu) {
		// We don't need it anymore
//		lastMenuInfo = null;
		currentSoundName = null;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		String soundName = currentSoundName; 
		int soundId;
		try {
			soundId = getResources().getIdentifier(soundName, "raw", getPackageName());
		} catch (Exception e) {
			return false;
		}
        
		switch (item.getItemId()) {
		case 0:
//			playSound(soundId);
//			String[] buttonArray = mResources.getStringArray(R.array.buttonArray);
//			String[] textButtonArray = mResources.getStringArray(R.array.textButtonArray);
//			int soundNb = 0;
//			for (int i = 0; i < buttonArray.length; i++) {
//				if (buttonArray[i].equals(soundName)) soundNb = i;
//			}
//			Toast.makeText(mContext, textButtonArray[soundNb], Toast.LENGTH_SHORT).show();
			startActivity(new Intent(Soundboard.this, PlaySound.class).putExtra("soundId", soundId));
			break;
		case 1:
			if (setAs(soundId, TYPE_RINGTONE)) {
				Toast.makeText(mContext, R.string.ringtoneok, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(mContext, R.string.ringtoneko, Toast.LENGTH_LONG).show();
			}
			break;
		case 2:
			if (setAs(soundId, TYPE_NOTIFICATION)) {
				Toast.makeText(mContext, R.string.notificationok, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(mContext, R.string.notificationko, Toast.LENGTH_LONG).show();
			}
			break;
		default:
			return false;
		}
		return true;
	}

	public void clickButton(View view) {
		String soundName = ((View) view.getParent()).getTag().toString();
		// Toast.makeText(getApplicationContext(), "Sound name : " + soundName, Toast.LENGTH_SHORT).show();
		// Play sound and start animation on the button
		Animation animClick = AnimationUtils.loadAnimation(this, R.anim.bounce);
		animClick.setDuration(playSound(soundName));
//		startActivity(new Intent(Soundboard.this, PlaySound.class).putExtra("soundName", soundName));
		view.startAnimation(animClick);
	}
 
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

		if (!mSharedPreferences.getBoolean(SHAKE, false)) return;
		
		// float azimuth = event.values[0];
		float pitch = event.values[1];
		// float roll = event.values[2];

		if (rotationStatus) {
			if (pitch > 0) {
				// ToDo: Same code as PlaySound.java -> To merge
//				int soundNb = new Random().nextInt(buttonArray.length);
//				Toast.makeText(mContext, textButtonArray[soundNb], Toast.LENGTH_SHORT).show();
//				playSound(buttonArray[soundNb]);
				startActivity(new Intent(Soundboard.this, PlaySound.class));
				rotationStatus = false;
			}
		} else {
			if (pitch < -3) {
				// Toast.makeText(getApplicationContext(), "Detect Rotation", Toast.LENGTH_SHORT).show();
				rotationStatus = true;
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}


	public void collapseView(View view) {
		String sectionNb = view.getTag().toString()
				.substring("section".length());
		// Toast.makeText(this, "Section nb : " + sectionNb, Toast.LENGTH_SHORT).show();
		View globalTab = findViewById(R.id.globaltab);
		View buttonPanel = (View) globalTab.findViewWithTag("buttonPanel"
				+ String.valueOf(sectionNb));
		LinearLayout section = (LinearLayout) globalTab
				.findViewWithTag("section" + String.valueOf(sectionNb));
		if (buttonPanel != null) {
			if (buttonPanel.getVisibility() == View.GONE) {
				section.startAnimation(AnimationUtils.loadAnimation(this,
						R.anim.slidefromright));
				section.setBackgroundColor(android.R.color.transparent);
				section.setGravity(Gravity.CENTER);
				buttonPanel.startAnimation(AnimationUtils.loadAnimation(this,
						R.anim.expand));
				buttonPanel.setVisibility(View.VISIBLE);
			} else {
				buttonPanel.startAnimation(AnimationUtils.loadAnimation(this,
						R.anim.collapse));
				buttonPanel.setVisibility(View.GONE);
				section.startAnimation(AnimationUtils.loadAnimation(this,
						R.anim.slidetoright));
				section.setBackgroundColor(android.R.color.background_light);
				section.setGravity(Gravity.RIGHT);
			}
		}
		return;
	}


	public boolean setAs(int soundId, int type) {
		byte[] buffer = null;
		InputStream input = mResources.openRawResource(soundId);
		int size = 0;
				
		try {
			size = input.available();
			buffer = new byte[size];
			input.read(buffer);
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		// Check that external storage is available and writable
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) return false;

		String  pathName = Environment.getExternalStorageDirectory().toString();
//		File path = Environment.getExternalStorageDirectory()+"/media/audio/ringtones";
		pathName = "/sdcard/media/audio/ringtones/";

		File path = new File(pathName);
		File file = new File(path, getString(R.string.soundFile));

		if ( ! path.exists() ) path.mkdirs();
		
		FileOutputStream save = null;
		try {
			save = new FileOutputStream(file);
			save.write(buffer);
			save.flush();
			save.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (save != null) {
				try {
					save.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

//		mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, fileName.toURI()));
		mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
				Uri.parse("file://" + file.getAbsolutePath())));

		ContentValues values = new ContentValues();
		values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
		values.put(MediaStore.MediaColumns.TITLE, getString(R.string.soundTitle));
		values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
		values.put(MediaStore.Audio.Media.ARTIST, getString(R.string.soundTitle));
		values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
		values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
		values.put(MediaStore.Audio.Media.IS_ALARM, true);
		values.put(MediaStore.Audio.Media.IS_MUSIC, false);
		
		// Insert it into the database
		Uri Uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());
		mContext.getContentResolver().delete(Uri, MediaStore.MediaColumns.DATA + "=\"" + file.getAbsolutePath() + "\"", null);
		Uri contentUri = mContext.getContentResolver().insert(Uri, values);
		if (type == TYPE_NOTIFICATION) {
			RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_NOTIFICATION, contentUri);
		} else {
			RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, contentUri);
		}
		return true;
	}


	int playSound(String soundName) {
		int soundId;
		try {
			soundId = mResources.getIdentifier(soundName, "raw", getPackageName());
		} catch (Exception e) {
			return 0;
		}
		return playSound(soundId);
	}
	
	int playSound(int soundId) {
		MediaPlayer mp = MediaPlayer.create(mContext, soundId);
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

