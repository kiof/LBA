package com.kiof.lbacraft;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Adapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Soundboard extends ListActivity implements SensorEventListener {
    private static final String CHECK_VOLUME = "checkvolume";
    private static final String VOLUME_MAX = "volumemax";
    private static final String VOLUME_RESTORE = "volumerestore";
    private static final String SHAKE = "shake";
    private static final String BGMUSIC = "bgmusic";
    private static final String LGCLICK = "lgclick";
    private static final String TAG = "Soundboard";
    private static final int TYPE_RINGTONE = 1;
    private static final int TYPE_NOTIFICATION = 2;
    private static int RETURN_SETTING = 1;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private AudioManager mAudioManager;
    private Context mContext;
    private Resources mResources;
    private SharedPreferences mSharedPreferences;
    private boolean rotationStatus = false;
    private TypedArray sounds;
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
        sounds = mResources.obtainTypedArray(R.array.sounds);
        String[] buttons = mResources.getStringArray(R.array.buttons);

        // Get Preferences
        PreferenceManager.setDefaultValues(mContext, R.xml.setting, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        setContentView(R.layout.main);

        // AdMob
        MobileAds.initialize(getApplicationContext(), getString(R.string.ad_unit_id));
        AdView adView = this.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
//                .addTestDevice("53356E870D99B80A68F8E2DBBFCD28FB")
                .build();
        adView.loadAd(adRequest);

        setListAdapter(new SoundboardAdapter(this, buttons));

        ListView list = getListView();
        registerForContextMenu(list);

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
                        }
                );
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
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Log.d(TAG, "onListItemClick");
        super.onListItemClick(listView, view, position, id);
        Button button = view.findViewById(R.id.button);
        // Play sound and start animation on the button
        if (button != null) {
            Animation animClick = AnimationUtils.loadAnimation(this, R.anim.bounce);
            animClick.setDuration(playSound(sounds.getResourceId(position, 0)));
            button.startAnimation(animClick);
        } else {
            playSound(sounds.getResourceId(position, 0));
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
            case R.id.share:
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_title));
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title));
                sharingIntent.putExtra(Intent.EXTRA_TEMPLATE, Html.fromHtml(getString(R.string.share_link)));
                sharingIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(getString(R.string.share_link)));
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_with)));
                return true;
            case R.id.about:
                new HtmlAlertDialog(this, R.raw.about, getString(R.string.about_title), android.R.drawable.ic_menu_info_details).show();
                return true;
            case R.id.other:
                Intent otherIntent = new Intent(Intent.ACTION_VIEW);
                otherIntent.setData(Uri.parse(getString(R.string.other_link)));
                startActivity(otherIntent);
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
                        }
                );
                builder.setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(mContext, R.string.goingon, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
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

            // Get the info on which item was selected
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            // Get the Adapter behind your ListView (this assumes you're using
            // a ListActivity; if you're not, you'll have to store the Adapter yourself
            // in some way that can be accessed here.)
            Adapter adapter = getListAdapter();
            // Retrieve the item that was clicked on
//			Object listItem = adapter.getItem(info.position);

            menu.setHeaderTitle(R.string.actions);
            menu.setHeaderIcon(android.R.drawable.ic_menu_set_as);
            menu.add(Menu.NONE, 0, Menu.NONE, R.string.play);
            menu.add(Menu.NONE, 1, Menu.NONE, R.string.ringtone);
            menu.add(Menu.NONE, 2, Menu.NONE, R.string.notification);
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
//		currentSoundId = -1;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Here's how you can get the correct item in onContextItemSelected()
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
//	    Object listItem = getListAdapter().getItem(info.position);
        int soundId = sounds.getResourceId(info.position, 0);
        switch (item.getItemId()) {
            case 0:
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        if (!mSharedPreferences.getBoolean(SHAKE, false)) return;

        // float azimuth = event.values[0];
        float pitch = event.values[1];
        // float roll = event.values[2];

        if (rotationStatus) {
            if (pitch > 0) {
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

        String pathName = Environment.getExternalStorageDirectory().toString();
//		File path = Environment.getExternalStorageDirectory()+"/media/audio/ringtones";
        pathName = "/sdcard/media/audio/ringtones/";

        File path = new File(pathName);
        File file = new File(path, getString(R.string.soundFile));

        if (!path.exists()) path.mkdirs();

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

    int playSound(int soundId) {
        MediaPlayer mp = MediaPlayer.create(mContext, soundId);
        if (mp == null) return 0;
        mp.setOnCompletionListener(new OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
        mp.start();
        return mp.getDuration();
    }
}