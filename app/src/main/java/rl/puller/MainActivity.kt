package rl.puller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import rl.puller.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var pullFile: File
    private val pullList = mutableListOf<File>()

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

        pullFile = File(getExternalFilesDir(null), "pull_list.txt")
        pullFile.createNewFile()

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
        val br = pullFile.bufferedReader()
        br.forEachLine {
            pullList.add(File(it.trim()))
        }
        br.close()

        if (newUris != null) {
            val hashSet = hashSetOf<Pair<String, Long>>()
            val deque = ArrayDeque<File>()
            deque.addLast(Environment.getExternalStorageDirectory())

            for (uri in newUris) {
                val cursor = contentResolver.query(uri, null, null, null, null)!!
                if (cursor.moveToFirst()) {
                    val name =
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                    if (!hashSet.add(Pair(name, size)))
                        Ez.toast(this, "Duplicated")
                }
                cursor.close()
            }

            while (!deque.isEmpty()) {
                val dir = deque.removeLast()
                if (!dir.exists()) continue
                val files = dir.listFiles() ?: continue

                for (file in files) {
                    val pair = Pair(file.name, file.length())
                    if (file.isDirectory)
                        deque.addLast(file)
                    else if (hashSet.contains(pair)) {
                        hashSet.remove(pair)
                        pullList.add(file)
                    }
                }
            }

            pullList.sortByDescending { it.lastModified() }

            val bw = pullFile.bufferedWriter()
            for (line in pullList) {
                bw.write(line.absolutePath)
                bw.newLine()
            }
            bw.close()
        }

        for (pl in pullList)
            Ez.log(pl)
    }
}