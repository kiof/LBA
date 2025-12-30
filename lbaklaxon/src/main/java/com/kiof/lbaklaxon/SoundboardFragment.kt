package com.kiof.lbaklaxon

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.media.RingtoneManager.TYPE_RINGTONE
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.kiof.lbaklaxon.databinding.FragmentHomeBinding

class SoundboardFragment : Fragment() {
    private lateinit var fragmentLayout: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: FragmentHomeBinding
    private lateinit var items: List<Item>
    private var mediaPlayer: MediaPlayer? = null

    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

         // Get Preferences
        PreferenceManager.setDefaultValues(requireContext(), R.xml.settings, false)
        // 2. Initialize the property
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val sectionTexts = resources.getStringArray(R.array.textSections)
        val sectionImages = resources.obtainTypedArray(R.array.pictureSections)
        val buttonTexts = resources.getStringArray(R.array.textButtons)
        val buttonImages = resources.obtainTypedArray(R.array.pictures)
        val buttonSounds = resources.obtainTypedArray(R.array.sounds)

        items = sectionTexts.indices.map { index ->
            Item(
                sectionText = sectionTexts[index],
                sectionImage = sectionImages.getResourceId(index, 0),
                buttonText = buttonTexts[index],
                buttonImage = buttonImages.getResourceId(index, 0),
                buttonSound = buttonSounds.getResourceId(index, 0)
            )
        }
        Log.d(MainActivity.TAG, items.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        fragmentLayout = view.findViewById(R.id.home_frag_layout)
        binding = FragmentHomeBinding.bind(view)
        val adapter = SoundboardAdapter(
            items,
            onClick = { view, position ->
                // Play sound and start animation on the button
                val animClick = AnimationUtils.loadAnimation(context, R.anim.bounce)
                val soundDuration = playSound(items[position].buttonSound)
                if (soundDuration > 0) {
                    animClick.duration = soundDuration.toLong()
                } else {
                    animClick.duration = 500 // fallback duration
                }
                view.startAnimation(animClick)
            },
            onLongClick = { view, position ->
                Log.d(MainActivity.TAG, "onLongClick")
                if (sharedPreferences.getBoolean(MainActivity.LGCLICK, false))
                    showMenu(view, position)
            })
        binding.rv.adapter = adapter
        binding.rv.layoutManager = LinearLayoutManager(context)
        return binding.root
    }

    /**
     * A launcher for the result of the write settings permission request.
     * We don't need a specific result, just to know when the user returns from the settings screen.
     */
    private val writeSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // The user has returned from the settings screen.
        // You can re-check the permission here if needed or update the UI.
        // For now, we will simply proceed with the action if permission is granted.
        // Example: Log a message or re-trigger the action that required the permission.
        Log.d(MainActivity.TAG, "Permission granted")
    }

    /**
     * Checks if the app has the WRITE_SETTINGS permission.
     * If not, it launches the system screen to request it.
     * @return True if the permission is already granted, false otherwise.
     */
    private fun hasWriteSettingsPermission(context: Context): Boolean {
        // Settings.System.canWrite() is the method to check for this specific permission.
        if (Settings.System.canWrite(context)) {
            return true
        }

        // If the permission is not granted, create an intent to open the specific settings screen.
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            // Pass the app's package name to navigate directly to our app's settings page.
            data = "package:${context.packageName}".toUri()
        }
        // Launch the intent to show the settings screen to the user.
        writeSettingsLauncher.launch(intent)
        return false
    }

    /**
     * Displays a popup menu for a given item in the RecyclerView.
     * The menu allows the user to play the sound, or set it as a ringtone or notification sound.
     *
     * @param view The view that was long-clicked, used as an anchor for the popup menu.
     * @param position The adapter position of the item that was clicked, used to identify the sound resource.
     */
    fun showMenu(view: View, position: Int) {
        // The context for PopupMenu can often just be `v.context`
        PopupMenu(view.context, view).apply {
            // Inflate the menu resource
            inflate(R.menu.context_menu)

            // Set the menu item click listener using a trailing lambda
            setOnMenuItemClickListener { item ->
                // Store the sound item in a variable for clarity and reuse
                val soundId = items.getOrNull(position)?.buttonSound
                    ?: return@setOnMenuItemClickListener false
                // Handle the menu item click here
                when (item.itemId) {
                    R.id.action_play -> {
                        Log.d(MainActivity.TAG, "Play")
                        playSound(soundId)
                    }

                    R.id.action_ringtone -> {
                        Log.d(MainActivity.TAG, "Ringtone")
                        // First, check for the permission.
                        // The hasWriteSettingsPermission() function will handle showing the user the request screen.
                        if (hasWriteSettingsPermission(requireContext())) {
                            // If permission is already granted, launch the coroutine immediately.
                            lifecycleScope.launch {
                                if (setAs(soundId, SoundType.RINGTONE)) {
                                    Toast.makeText(
                                        requireContext(),
                                        R.string.ringtoneok,
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        R.string.ringtoneko,
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                }
                            }
                        }
                        // If permission is not granted, the user will be taken to the settings screen.
                        // They will have to come back and press the button again.

                    }

                    R.id.action_notification -> {
                        Log.d(MainActivity.TAG, "Notification")
                        // Also check here.
                        if (hasWriteSettingsPermission(requireContext())) {
                            lifecycleScope.launch {
                                if (setAs(soundId, SoundType.NOTIFICATION)) {
                                    Toast.makeText(
                                        requireContext(),
                                        R.string.notificationok,
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        R.string.notificationok,
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                }
                            }
                        }
                    }

                    else -> return@setOnMenuItemClickListener false
                }
                // Return true to indicate that the menu click has been handled
                true
            }
            // Show the popup menu
            show()
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    enum class SoundType(val ringtoneManagerType: Int) {
        RINGTONE(TYPE_RINGTONE),
        NOTIFICATION(TYPE_NOTIFICATION),
        ALARM(RingtoneManager.TYPE_ALARM)
    }

    /**
     * Sets a raw sound resource as a system sound (Ringtone, Notification, etc.).
     *
     * This function performs file I/O and should be called from a coroutine.
     * It returns the Uri of the new ringtone on success, or null on failure.
     *
     * @param context The application context.
     * @param soundId The raw resource ID of the sound file (e.g., R.raw.my_sound).
     * @param soundType The type of sound to set (e.g., SoundType.NOTIFICATION).
     * @return The Uri of the newly created sound file in MediaStore, or null if it failed.
     */
    suspend fun setAs(soundId: Int, soundType: SoundType): Boolean =
        withContext(Dispatchers.IO) {
            val soundTitle = requireContext().getString(R.string.soundTitle)
            val soundFileName =
                requireContext().getString(R.string.soundFile) // e.g., "my_custom_sound.mp3"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, soundFileName)
                put(MediaStore.MediaColumns.TITLE, soundTitle)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
                put(
                    MediaStore.Audio.Media.ARTIST,
                    requireContext().getString(R.string.soundTitle)
                ) // Use a different string if needed

                // Set flags based on the type
                put(MediaStore.Audio.Media.IS_RINGTONE, soundType == SoundType.RINGTONE)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, soundType == SoundType.NOTIFICATION)
                put(MediaStore.Audio.Media.IS_ALARM, soundType == SoundType.ALARM)
                put(MediaStore.Audio.Media.IS_MUSIC, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            // Get the correct collection URI for audio
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val contentResolver = requireContext().contentResolver
            val newItemUri = contentResolver.insert(collection, contentValues)

            newItemUri?.let { uri ->
                try {
                    // Write the raw resource data to the new MediaStore entry
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        requireContext().resources.openRawResource(soundId).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    // Now that the file is written, clear the IS_PENDING flag
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        contentResolver.update(uri, contentValues, null, null)
                    }

                    // Set the sound as the default for the specified type
                    RingtoneManager.setActualDefaultRingtoneUri(
                        context,
                        soundType.ringtoneManagerType,
                        uri
                    )
                    // Success
                    return@let true
                } catch (e: IOException) {
                    e.printStackTrace()
                    // If something fails, clean up the incomplete MediaStore entry
                    contentResolver.delete(uri, null, null)
                    if (soundType == SoundType.RINGTONE) {
                        Toast.makeText(requireContext(), R.string.ringtoneko, Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Toast.makeText(requireContext(), R.string.notificationko, Toast.LENGTH_LONG)
                            .show()
                    }
                    // Failure
                    return@let false
                }
            }
            // Failure (couldn't create MediaStore entry)
            return@withContext false
        }


    /**
     * Plays a sound from a raw resource ID.
     * It handles releasing previous MediaPlayer instances to prevent overlapping sounds.
     * The MediaPlayer is released automatically upon completion.
     *
     * @param soundId The raw resource ID of the sound to play.
     * @return The duration of the sound in milliseconds, or 0 if playback fails.
     */
    private fun playSound(soundId: Int): Int {
        // Release any existing player before creating a new one
        mediaPlayer?.release()

        val mp = MediaPlayer.create(context, soundId) ?: return 0
        mediaPlayer = mp

        mp.setOnCompletionListener {
            it.release()
            mediaPlayer = null // Clear the reference
        }

        mp.start()
        return mp.duration
    }
}

