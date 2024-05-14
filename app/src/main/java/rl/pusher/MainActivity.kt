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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import rl.pusher.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var homeDir: File
    private lateinit var appExternalStorageDir: File

    private lateinit var binding: ActivityMainBinding
    private lateinit var pushListFile: File
    private lateinit var cloneDir: File
    private val pushList = mutableListOf<File>()

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

        homeDir = Environment.getExternalStorageDirectory()
        appExternalStorageDir = getExternalFilesDir(null)!!

        pushListFile = File(appExternalStorageDir, "push_list.txt")
        pushListFile.createNewFile()

        cloneDir = File(appExternalStorageDir, "clone")
        cloneDir.mkdir()

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
            val read = checkSelfPermission(READ_EXTERNAL_STORAGE)
            val write = checkSelfPermission(WRITE_EXTERNAL_STORAGE)
            if (read == PERMISSION_DENIED || write == PERMISSION_DENIED) {
                requestPermissions(arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 0)
            }
        }

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
                genData(arrayListOf(uri))
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!
                genData(ArrayList(uris))
            }

            else -> {
                genData(null)
            }
        }
    }

    private fun genData(newUris: ArrayList<Uri>?) {
        pushListFile.bufferedReader().use {
            it.forEachLine { line ->
                pushList.add(File(line.trim()))
            }
        }
        if (newUris == null) return

        val hashSet = hashSetOf<Pair<String, Long>>()
        val deque = ArrayDeque<File>()
        deque.addLast(homeDir)

        newUris.forEach { uri ->
            val cursor = contentResolver.query(uri, null, null, null, null)!!
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                if (!hashSet.add(name to size))
                    Ez.toast(this, "Duplicated")
            }
            cursor.close()
        }

        while (!deque.isEmpty()) {
            val dir = deque.removeLast()
            if (!dir.exists()) continue
            val files = dir.listFiles() ?: continue

            files.forEach { file ->
                val pair = file.name to file.length()

                if (file.isDirectory)
                    deque.addLast(file)
                else if (hashSet.contains(pair)) {
                    val relativePath = file.absolutePath.removePrefix(homeDir.absolutePath)
                    if (Ez.isASCII(relativePath)) {
                        hashSet.remove(pair)
                        pushList.add(file)
                    }
                    else {
                        val nodes = relativePath.split("/")
                        for (i in nodes.indices) {
                            if (i == nodes.size - 1) {
                            }
                            else if (i == nodes.size - 2) {
                            }
                            else {

                            }
                        }
                    }
                }

            }
        }

        pushList.sortByDescending { it.lastModified() }

        pushListFile.bufferedWriter().use { bw ->
            pushList.forEach { file ->
                bw.write(file.absolutePath)
                bw.newLine()
            }
        }
    }
}