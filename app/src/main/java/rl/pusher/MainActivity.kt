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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rl.pusher.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var homeDir: File
    private lateinit var appExternalStorageDir: File

    private lateinit var pushListTXT: File
    private lateinit var moveListTXT: File
    private lateinit var clientKeyTXT: File
    private lateinit var pushDoneFile: File

    private val rawPaths = mutableListOf<String>()

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
        pushListTXT = File(appExternalStorageDir, "Push_List.txt").apply { this.createNewFile() }
        moveListTXT = File(appExternalStorageDir, "Move_List.txt").apply { this.createNewFile() }
        clientKeyTXT = File(appExternalStorageDir, "Client_Key.txt").apply { this.createNewFile() }
        pushDoneFile = File(appExternalStorageDir, "PUSH_DONE").apply { this.delete() }
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
        pushListTXT.forEachLine {
            rawPaths.add(it.trim())
        }

        val uris: ArrayList<Uri> = when (intent.action) {

            Intent.ACTION_SEND ->
                arrayListOf(intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)

            Intent.ACTION_SEND_MULTIPLE ->
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!

            else -> return
        }

        val hashSet = hashSetOf<Pair<String, Long>>()
        for (uri in uris) {
            val cursor = contentResolver.query(uri, null, null, null, null)!!
            if (cursor.moveToFirst()) {
                val nameColumnIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                val sizeColumnIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)

                val name = cursor.getString(nameColumnIndex)
                val size = cursor.getLong(sizeColumnIndex)

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

            for (file in files) {
                val pair = file.name to file.length()

                if (hashSet.contains(pair)) {
                    hashSet.remove(pair)
                    rawPaths.add(file.absolutePath)
                }
                else if (file.isDirectory)
                    deque.addLast(file)
            }
        }

        rawPaths.sortByDescending { File(it).lastModified() }
    }

    private fun push() {
        val cookedRawPairs = mutableListOf<Pair<String, String>>()

        for (rawPath in rawPaths) {
            val isASCII = rawPath.matches("\\A\\p{ASCII}*\\z".toRegex())
            if (!isASCII) {
                val cookedFile = File(appExternalStorageDir, "${rawPath.hashCode()}")
                File(rawPath).renameTo(cookedFile)
                cookedRawPairs.add(cookedFile.absolutePath to rawPath)
            }
            else
                cookedRawPairs.add(rawPath to rawPath)
        }

        pushListTXT.writeText(cookedRawPairs.joinToString("\n") { it.first })
        moveListTXT.writeText(cookedRawPairs.joinToString("\n") { "${it.first}\t${it.second}" })

        // notify server to pull
        clientKeyTXT.writeText("${Ez.millis()}")
        Ez.toast(this, "START PUSHING")

        lifecycleScope.launch {
            while (!pushDoneFile.exists()) {
                delay(100)
            }

            Ez.toast(this@MainActivity, "PUSH DONE")

            for ((cooked, raw) in cookedRawPairs) {
                File(cooked).renameTo(File(raw))
            }

            pushListTXT.delete()
            moveListTXT.delete()
            clientKeyTXT.delete()
            pushDoneFile.delete()
        }

        /* ========== push() ========== */
    }

    /* ========== MainActivity.kt ========== */
}
