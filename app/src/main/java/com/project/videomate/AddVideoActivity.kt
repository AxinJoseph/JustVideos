package com.project.videomate

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.security.Permissions
import java.util.jar.Manifest

class AddVideoActivity : AppCompatActivity() {

    private lateinit var titleEt: EditText
    private lateinit var videoView: VideoView
    private lateinit var uploadBtn: Button
    private lateinit var pickVideoFab: FloatingActionButton

    private lateinit var actionBar: ActionBar

    private val VIDEO_PICK_GALLERY_CODE = 100
    private val VIDEO_PICK_CAMERA_CODE = 101

    private val CAMERA_REQUEST_CODE = 102

    private lateinit var cameraPermissions:Array<String>

    private var videoUri: Uri? = null

    private var title:String = ""

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_video)

        titleEt = findViewById(R.id.titleEt)
        videoView = findViewById(R.id.videoView)
        uploadBtn = findViewById(R.id.uploadBtn)
        pickVideoFab = findViewById(R.id.pickVideoFab)

        actionBar = supportActionBar!!

        actionBar.title = "Add New Video"

        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        cameraPermissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setMessage("Uploading Your Video")
        progressDialog.setCanceledOnTouchOutside(false)

        uploadBtn.setOnClickListener {
            title = titleEt.text.toString().trim()
            if (TextUtils.isEmpty(title)){
                Toast.makeText(this,"Title required",Toast.LENGTH_SHORT).show()
            }else if (videoUri == null){
                Toast.makeText(this,"Select a Video",Toast.LENGTH_SHORT).show()
            }else{
                uploadVideoToFirebase()
            }
            titleEt.setText("")
        }

        pickVideoFab.setOnClickListener {
            videoPickDialog()
        }

    }

    private fun uploadVideoToFirebase() {
        progressDialog.show()
        val timestamp = ""+System.currentTimeMillis()
        val filePathAndName = "Videos/video_$timestamp"
        val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageReference.putFile(videoUri!!)
            .addOnSuccessListener { taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val downloadUri = uriTask.result
                if (uriTask.isSuccessful){


                    val hashMap = HashMap<String, Any>()
                    hashMap["id"] = "$timestamp"
                    hashMap["title"] = "$title"
                    hashMap["timestamp"] = "$timestamp"
                    hashMap["videoUri"] = "$downloadUri"

                    val dbReference = FirebaseDatabase.getInstance().getReference("Videos")
                    dbReference.child(timestamp)
                        .setValue(hashMap)
                        .addOnSuccessListener { taskSnapshot ->
                            progressDialog.dismiss()
                            Toast.makeText(this,"Video Uploaded",Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e->
                            progressDialog.dismiss()
                            Toast.makeText(this,"${e.message}",Toast.LENGTH_SHORT).show()
                        }

                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setVideoToViewVideo() {
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)

        videoView.setMediaController(mediaController)

        videoView.setVideoURI(videoUri)
        videoView.requestFocus()
        videoView.setOnPreparedListener {
            videoView.pause()
        }
    }

    private fun videoPickDialog() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Video From")
            .setItems(options){ dialogInterface, i->
                if (i==0){
                    if (!checkCameraPermissions()){
                        requestCameraPermissions()
                    }else{
                        videoPickCamera()
                    }
                }else{
                    videoPickGallery()
                }
            }
            .show()
    }

    private fun requestCameraPermissions() {
        ActivityCompat.requestPermissions(
            this,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
    }

    private fun checkCameraPermissions():Boolean {
        val result1 = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val result2 = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        return result1 && result2
    }

    private fun videoPickGallery() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(
            Intent.createChooser(intent, "Choose Video"),
            VIDEO_PICK_GALLERY_CODE
        )
    }

    private fun videoPickCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent,VIDEO_PICK_CAMERA_CODE)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            CAMERA_REQUEST_CODE ->
                if (grantResults.size > 0){
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && storageAccepted){
                        videoPickCamera()
                    }else{
                        Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == RESULT_OK){
            if (requestCode == VIDEO_PICK_CAMERA_CODE){
                videoUri = data!!.data
                setVideoToViewVideo()
            }else if (requestCode == VIDEO_PICK_GALLERY_CODE){
                videoUri = data!!.data
                setVideoToViewVideo()
            }
        }else{
            Toast.makeText(this,"Cancelled", Toast.LENGTH_SHORT).show()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }


}