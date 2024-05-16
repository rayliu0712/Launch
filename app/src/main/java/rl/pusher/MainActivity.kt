package rl.pusher

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import rl.pusher.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var homeDir: File
    private lateinit var appExternalStorageDir: File
    private lateinit var pushFile: File
    private val rawFiles = mutableListOf<File>()
    private val cookedPaths = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.pushBtn.setOnClickListener { push() }

        homeDir = Environment.getExternalStorageDirectory()
        appExternalStorageDir = getExternalFilesDir(null)!!
        pushFile = File(appExternalStorageDir, "push_list.txt").apply { this.createNewFile() }
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        else {
            val read = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            val write = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
            if (read == PERMISSION_DENIED || write == PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 0
                )
            }
        }

        genData(intent)
    }

    private fun genData(intent: Intent) {
        pushFile.forEachLine { line ->
            rawFiles.add(File(line.trim()))
        }

        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: return
        val hashSet = hashSetOf<Pair<String, Long>>()
        uris.forEach { uri ->
            val cursor = contentResolver.query(uri, null, null, null, null)!!
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                if (!hashSet.add(name to size))
                    Ez.toast(this, "Duplicated")
            }
            cursor.close()
        }

        val deque = ArrayDeque<File>()
        deque.addLast(homeDir)
        while (!deque.isEmpty()) {
            val dir = deque.removeLast()
            val files = dir.listFiles() ?: continue

            files.forEach { file ->
                val pair = file.name to file.length()

                if (hashSet.contains(pair)) {
                    hashSet.remove(pair)
                    rawFiles.add(file)
                }
                else if (file.isDirectory)
                    deque.addLast(file)
            }
        }

        rawFiles.sortByDescending { it.lastModified() }
        pushFile.bufferedWriter().use { bw ->
            rawFiles.joinTo(bw, "\n") { it.absolutePath }
        }
    }

    private fun push() {
        rawFiles.forEach { file ->
            var path = file.absolutePath
            if (!Ez.isASCII(path)) {
                val nodes = path.split("/")
                
            }
            cookedPaths.add(path)
        }
    }

    /* ========== MainActivity ========== */
}