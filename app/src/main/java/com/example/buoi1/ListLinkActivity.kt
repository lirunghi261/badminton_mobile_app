package com.example.buoi1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import com.example.buoi1.databinding.ListviewScreenBinding

class ListLinkActivity : AppCompatActivity() {
    private lateinit var binding: ListviewScreenBinding
    private val TAG = "ListLinkActivity"

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "đang ở onStart nè")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "đang ở onResume nè")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "đang ở onPause nè")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "đang ở onStop nè")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "đang ở onDestroy nè")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "đang ở onRestart nè")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "đang ở onCreate nè")
        enableEdgeToEdge()
        binding = ListviewScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.listview) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val links = listOf(
            Link("Google", "https://google.com"),
            Link("Facebook", "https://facebook.com"),
            Link("Youtube", "https://youtube.com"),
            Link("StackOverflow", "https://stackoverflow.com"),
            Link("Github", "https://github.com")
        )

        val adapter = LinkAdapter(this, links)
        binding.lvLinks.adapter = adapter
        binding.tvTotalLinks.text = links.size.toString()

        setSupportActionBar(binding.toolbar)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_link -> {
                // Handle add link action
                android.widget.Toast.makeText(this, "Add Link Clicked", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
