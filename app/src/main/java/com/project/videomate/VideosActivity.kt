package com.project.videomate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class VideosActivity : AppCompatActivity() {

    private lateinit var addVideoFab: FloatingActionButton
    private lateinit var videoRv: RecyclerView

    private lateinit var videoArrayList: ArrayList<ModelVideo>

    private lateinit var adapterVideo: AdapterVideo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_videos)

        addVideoFab = findViewById(R.id.addVideoFab)
        videoRv = findViewById(R.id.videosRv)

        title = "Videos"

        loadVideosFromFirebase()

        addVideoFab.setOnClickListener {
            startActivity(Intent(this,AddVideoActivity::class.java))
        }

    }

    private fun loadVideosFromFirebase() {
        videoArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Videos")
        ref.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                videoArrayList.clear()
                for (ds in snapshot.children){
                    val modelVideo = ds.getValue(ModelVideo::class.java)
                    videoArrayList.add(modelVideo!!)
                }
                adapterVideo = AdapterVideo(this@VideosActivity, videoArrayList)
                videoRv.adapter = adapterVideo
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}