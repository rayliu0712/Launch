package rl.demo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import rl.demo.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var filename: String? = null
    private var filepath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)

        val pullListFile = File(getExternalFilesDir(null), "pull_list.txt")
        pullListFile.createNewFile()

        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        filename = it.getString(displayNameIndex)
                        Log.d("666", filename!!)
                    }
                }
            }
        }

        binding.btn.setOnClickListener {
            val deque = ArrayDeque<File>()
            deque.addLast(Environment.getExternalStorageDirectory())

            while (!deque.isEmpty()) {
                val dir = deque.removeLast()
                if (!dir.exists()) continue

                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isDirectory)
                        deque.addLast(file)
                    else if (file.name == filename) {
                        filepath = file.absolutePath
                        Log.d("666", filepath!!)
                        deque.clear()
                        break
                    }
                }
            }
        }
    }
}