package com.example.ggchat

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    private lateinit var avatarImage: ImageView
    private lateinit var editUsername: EditText
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>
    private var avatarBitmap: Bitmap? = null

    private val handler = Handler()
    private var saveRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        avatarImage = view.findViewById(R.id.avatarImage)
        editUsername = view.findViewById(R.id.editUsername)
        val buttonNext = view.findViewById<ImageButton>(R.id.nextToJoinAndCreate)

        // Load the avatar when opening the screen.
        avatarBitmap = UserData.getAvatar(requireContext())
        avatarBitmap?.let { avatarImage.setImageBitmap(it) }

        // Load the username when opening the screen.
        val savedName = UserData.getUserName(requireContext())
        editUsername.setText(savedName)

        // Watch name input changes (auto-save).
        editUsername.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newName = s.toString().trim()
                saveRunnable?.let { handler.removeCallbacks(it) }
                saveRunnable = Runnable {
                    UserData.saveUserName(requireContext(), newName)
                }
                handler.postDelayed(saveRunnable!!, 1000)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Pick an image from the gallery.
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let {
                    avatarBitmap = it
                    avatarImage.setImageBitmap(it)
                    UserData.saveAvatar(requireContext(), it)
                }
            }
        }

        // Take a new photo.
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                avatarBitmap = it
                avatarImage.setImageBitmap(it)
                UserData.saveAvatar(requireContext(), it)
            }
        }

        // Tap the avatar image to choose an image source.
        avatarImage.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Đổi ảnh đại diện")
                .setItems(arrayOf("Chọn từ thư viện", "Chụp ảnh mới")) { _, which ->
                    when (which) {
                        0 -> pickImageLauncher.launch("image/*")
                        1 -> takePictureLauncher.launch(null)
                    }
                }.show()
        }

        // Call back to the main activity to switch fragments.
        buttonNext.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(JoinAndCreateFragment())
        }

        return view
    }
}
