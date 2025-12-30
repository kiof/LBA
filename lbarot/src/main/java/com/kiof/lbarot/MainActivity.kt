package com.kiof.lbarot

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    companion object {
        private const val CHECK_VOLUME: String = "checkvolume"
        private const val VOLUME_MAX: String = "volumemax"
        private const val VOLUME_RESTORE: String = "volumerestore"
        private const val BGMUSIC: String = "bgmusic"
        const val LGCLICK: String = "lgclick"
        const val TAG: String = "Soundboard"
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var sharedPreferences: SharedPreferences
    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager

    private var initVolume: Int = 0
    private var buttonSounds: TypedArray? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        buttonSounds = resources.obtainTypedArray(R.array.sounds)

        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHostFragment.navController

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val actionBarDrawerToggle =
            ActionBarDrawerToggle(this, drawerLayout, R.string.yes, R.string.no)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)

        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        supportActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        appBarConfiguration.fallbackOnNavigateUpListener;

        navView.setNavigationItemSelectedListener { menuItem ->
            // Handle menu item selected
            when (menuItem.itemId) {
                android.R.id.home -> {
                    if (drawerLayout.isDrawerOpen(navView)) {
                        drawerLayout.closeDrawer(navView, true)
                    }
                }

                R.id.settingsFragment -> navController.navigate(R.id.settingsFragment)

                R.id.share -> {
                    val sharingIntent = Intent(Intent.ACTION_SEND)
                    sharingIntent.setType("text/plain")
                    sharingIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_title))
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title))
                    sharingIntent.putExtra(
                        Intent.EXTRA_TEMPLATE,
                        Html.fromHtml(getString(R.string.share_link), Html.FROM_HTML_MODE_LEGACY)
                    )
                    sharingIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        Html.fromHtml(getString(R.string.share_link), Html.FROM_HTML_MODE_LEGACY)
                    )
                    startActivity(
                        Intent.createChooser(
                            sharingIntent,
                            getString(R.string.share_with)
                        )
                    )
                }

                R.id.about -> {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.about_title)
                    builder.setIcon(android.R.drawable.ic_menu_info_details)
                    builder.setMessage(
                        Html.fromHtml(
                            getString(R.string.about),
                            Html.FROM_HTML_MODE_LEGACY
                        )
                    )
                    builder.setCancelable(true)
                    builder.setNeutralButton(
                        R.string.Ok
                    ) { dialog, _ -> dialog.dismiss() }
                    builder.show()
                }

                R.id.other -> {
                    val otherIntent = Intent(Intent.ACTION_VIEW)
                    otherIntent.setData(getString(R.string.other_link).toUri())
                    startActivity(otherIntent)
                }

                R.id.quit -> {
                    // Create out AlterDialog
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.quit_title)
                    builder.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                    builder.setMessage(R.string.quit_message)
                    builder.setCancelable(true)
                    builder.setPositiveButton(
                        R.string.yes
                    ) { _, _ -> finish() }
                    builder.setNegativeButton(
                        R.string.no
                    ) { _, _ ->
                        Toast.makeText(this, R.string.goingon, Toast.LENGTH_SHORT).show()
                    }
                    builder.show()
                }
            }
            drawerLayout.close()
            true
        }

        // Sensor management
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Get Preferences
        PreferenceManager.setDefaultValues(applicationContext, R.xml.settings, false)
        // 2. Initialize the property
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Get Preferences and log them
        sharedPreferences.all.forEach {
            Log.d(TAG, "${it.key} -> ${it.value}")
        }

        // AdMob
        MobileAds.initialize(applicationContext)
        val adView = this.findViewById<View?>(R.id.adView) as AdView
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        // Audio management for volume control
        audioManager =
            getSystemService(AUDIO_SERVICE) as AudioManager // Assign to the class property
        initVolume =
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)   // Assign to the class property
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        // Check if the preference to check volume is enabled
        if (sharedPreferences.getBoolean(CHECK_VOLUME, false) && maxVolume > 0) {
            // If volume is less than half, ask the user to increase it
            if (initVolume < maxVolume / 2) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.volume_title)
                    .setIcon(android.R.drawable.ic_menu_preferences)
                    .setMessage(R.string.volume_question)
                    .setNegativeButton(R.string.volume_no, null)
                    .setPositiveButton(R.string.volume_yes) { _, _ ->
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            maxVolume,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                    .show()
            }
        } else {
            // If the preference is to always maximize volume, do it directly
            if (sharedPreferences.getBoolean(VOLUME_MAX, false)) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    maxVolume,
                    AudioManager.FLAG_SHOW_UI
                )
            }
        }

        // Display change log if new version
        val changeLog = ChangeLog(this)
        if (changeLog.firstRun()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.about_title)
            builder.setIcon(android.R.drawable.ic_menu_info_details)
            builder.setMessage(
                Html.fromHtml(
                    getString(R.string.about),
                    Html.FROM_HTML_MODE_LEGACY
                )
            )
            builder.setCancelable(true)
            builder.setNeutralButton(
                R.string.Ok
            ) { dialog, _ -> dialog.dismiss() }
            builder.show()
        }

        // Background music
        if (sharedPreferences.getBoolean(BGMUSIC, false)) {
            playSound(R.raw.bgmusic);
        }
    }

    override fun onResume() {
        super.onResume() // Best practice to call super first
        if (!sharedPreferences.getBoolean(
                CHECK_VOLUME,
                false
            ) && sharedPreferences.getBoolean(
                VOLUME_MAX, false
            )
        ) {
            // This will now work correctly
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_SHOW_UI
            )
        }
    }

    override fun onPause() {
        if (!sharedPreferences.getBoolean(
                CHECK_VOLUME,
                false
            ) && sharedPreferences.getBoolean(
                VOLUME_MAX, false
            ) && sharedPreferences.getBoolean(VOLUME_RESTORE, false)
        ) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                initVolume, // This will now resolve correctly
                AudioManager.FLAG_SHOW_UI
            )
        }
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // Catch hardware volume buttons to adjust MusicStream volume
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI
                )
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI
                )
                return true
            }

            else -> return super.onKeyDown(keyCode, event)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun playSound(soundId: Int): Int {
        // Release any existing player before creating a new one
        mediaPlayer?.release()
        val mp = MediaPlayer.create(this, soundId) ?: return 0
        mediaPlayer = mp
        mp.setOnCompletionListener {
            it.release()
            mediaPlayer = null // Clear the reference
        }
        mp.start()
        return mp.duration
    }
}
