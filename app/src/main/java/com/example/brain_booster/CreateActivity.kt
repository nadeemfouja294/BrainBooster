package com.example.brain_booster

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brain_booster.models.BoardSize
import com.example.brain_booster.utilis.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


class CreateActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CreateActivity"
        private const val PICK_PHOTO_CODE = 621
        private const val READ_EXTERNAL_PHOTOS_CODE = 220
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private lateinit var boardSize: BoardSize
    private var chosenImageUris = mutableListOf<Uri>()
    private var downloadImagesUrls = mutableListOf<String>()
    private var imagePath = ""
    private lateinit var pbUploading: ProgressBar
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var adapter: ImagePickerAdapter
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var firebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_craete_actvity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)
        firebaseStorage = FirebaseStorage.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        boardSize = intent.getSerializableExtra(CUSTOM_BOARD_SIZE) as BoardSize
        supportActionBar?.title = "Chose pics (0/${boardSize.getNumPairs()})"

        adapter = ImagePickerAdapter(this, chosenImageUris, boardSize,
            object : ImagePickerAdapter.ImageClickListener {
                override fun onPlaceHolderClicked() {
                    if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION))
                        launchIntentForPhotos()
                    else requestPermission(
                        this@CreateActivity, READ_PHOTOS_PERMISSION,
                        READ_EXTERNAL_PHOTOS_CODE
                    )
                }

            })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())


        btnSaveEnabling()
        saveDataToFirebase()
    }

    private fun saveDataToFirebase() {

        btnSave.setOnClickListener {
            btnSave.isEnabled = false
            val customGameName = etGameName.text.toString()
            uniqueGameName(customGameName)

        }
    }

    private fun uniqueGameName(customGameName: String) {
        firebaseFirestore.collection("games").document(customGameName)
            .get().addOnSuccessListener { document ->
                if (document != null && document.data != null) {
                    btnSave.isEnabled = true
                    showPositiveAlertDialogue(
                        "Unique Name Exception",
                        "A game already exist with the name $customGameName, " +
                                "please choose another name"
                    )
                } else {
                    pbUploading.visibility = View.VISIBLE
                    uploadImagesToFirebaseStorage(customGameName)
                }
            }.addOnFailureListener { exception ->
                Log.i(TAG, "Encountered Error while saving game name", exception)
                btnSave.isEnabled = true
            }
    }

    private fun uploadImagesToFirebaseStorage(customGameName: String) {

        var didEncounterError = false
        for (i in 0 until chosenImageUris.size) {
            imagePath = "images/$customGameName/${System.currentTimeMillis()}-$i.jpg"
            val storageReference = firebaseStorage.reference.child(imagePath)
            storageReference.putFile(chosenImageUris[i])
                .continueWithTask {
                    storageReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        pbUploading.visibility = View.GONE
                        showToast("Failed to upload image in FireStorage")
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if (didEncounterError) {
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    downloadImagesUrls.add(downloadUrl)
                    pbUploading.progress = downloadImagesUrls.size * 100 / chosenImageUris.size
                    if (downloadImagesUrls.size == chosenImageUris.size) {
                        Log.i(TAG, "Images successfully uploaded in FireStorage")
                        uploadUrlsToFirestore(customGameName)

                    }
                }
        }
    }

    private fun uploadUrlsToFirestore(customGameName: String) {
        firebaseFirestore.collection("games").document(customGameName)
            .set(mapOf("images" to downloadImagesUrls))
            .addOnCompleteListener { urlsUploadingTask ->
                if (!urlsUploadingTask.isSuccessful) {
                    pbUploading.visibility = View.GONE
                    Log.i(TAG, "Exception with uploading Urls", urlsUploadingTask.exception)
                    showToast("Failed to uploading Urls")
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created game $customGameName")
                pbUploading.visibility = View.GONE
                AlertDialog.Builder(this)
                    .setTitle("Upload complete!, lets play your game $customGameName")
                    .setPositiveButton("Ok") { _, _ ->
                        val intent = Intent()
                        intent.putExtra(EXTRA_GAME_NAME, customGameName)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }.show()
            }
    }

    private fun btnSaveEnabling() {
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (PICK_PHOTO_CODE != requestCode || resultCode != Activity.RESULT_OK || data == null)
            return
        val selectedUri = data.data
        val clipData = data.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (chosenImageUris.size < boardSize.getNumPairs())
                    chosenImageUris.add(clipItem.uri)
            }
        } else if (selectedUri != null)
            chosenImageUris.add(selectedUri)
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics ${chosenImageUris.size} / ${boardSize.getNumPairs()}"
        btnSave.isEnabled = shouldEnableSaveButton()

    }

    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size != boardSize.getNumPairs())
            return false
        if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH)
            return false
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Chose pics"), PICK_PHOTO_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            launchIntentForPhotos()
        else
            showToast("In order to create a custom game , you need to provide access to your photos")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}