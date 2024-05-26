package rl.launch

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
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rl.launch.databinding.ActivityMainBinding
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

        binding.aboutBtn.setOnClickListener {
            Ez.toast(this@MainActivity, "coming soon")
        }
        binding.launchBtn.setOnClickListener {
            if (rawPaths.size > 0)
                push()
        }


        homeDir = Environment.getExternalStorageDirectory()
        appExternalStorageDir = getExternalFilesDir(null)!!
        pushListTXT = File(appExternalStorageDir, "Push_List.txt").apply { this.delete() }
        moveListTXT = File(appExternalStorageDir, "Move_List.txt").apply { this.delete() }
        clientKeyTXT = File(appExternalStorageDir, "Client_Key.txt").apply { this.delete() }
        pushDoneFile = File(appExternalStorageDir, "PUSH_DONE").apply { this.delete() }
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName"))

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

        val devStatus = Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
        val adbStatus = Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0)
        if (adbStatus != 1) {
            val action =
                if (devStatus == 1) Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                else Settings.ACTION_DEVICE_INFO_SETTINGS

            startActivity(Intent(action))
        }

        genData(intent)
    }

    private fun genData(intent: Intent) {
        val uris: ArrayList<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                arrayListOf(intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)

            Intent.ACTION_SEND_MULTIPLE ->
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!

            else -> return
        }
        // RETURN IF INTENT IS NOT SHARE

        val hashList = mutableListOf<Pair<String, Long>>()
        for (uri in uris) {
            val cursor = contentResolver.query(uri, null, null, null, null)!!
            if (cursor.moveToFirst()) {
                val nameColumnIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                val sizeColumnIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)

                val name = cursor.getString(nameColumnIndex)
                val size = cursor.getLong(sizeColumnIndex)
                val pair = name to size

                if (hashList.contains(pair))
                    Ez.toast(this, "Duplicated")
                else
                    hashList.add(pair)
            }
            cursor.close()
        }

        val deque = ArrayDeque<File>().apply { this.addLast(homeDir) }
        while (!deque.isEmpty()) {
            val dir = deque.removeLast()
            val files = dir.listFiles()
                ?: continue

            for (file in files) {
                val index = hashList.indexOf(file.name to file.length())

                if (index != -1) {
                    hashList.removeAt(index)
                    rawPaths.add(file.absolutePath)
                }
                else if (file.isDirectory)
                    deque.addLast(file)
            }
        }

        rawPaths.sortByDescending { File(it).lastModified() }
    }

    private fun push() {
        binding.launchBtn.isEnabled = false
        binding.aboutBtn.isEnabled = false

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

        Ez.toast(this, "START PUSHING")
        lifecycleScope.launch {
            while (!pushDoneFile.exists()) {
                clientKeyTXT.writeText("${Ez.millis()}")
                delay(250)
            }
            Ez.toast(this@MainActivity, "PUSH DONE")

            for ((cooked, raw) in cookedRawPairs) {
                File(cooked).renameTo(File(raw))
            }

            moveListTXT.delete()
            clientKeyTXT.delete()
            pushDoneFile.delete()

            runOnUiThread {
                binding.launchBtn.isEnabled = true
                binding.aboutBtn.isEnabled = true
            }
        }

        /* ========== push() ========== */
    }

    /* ========== MainActivity.kt ========== */
}
