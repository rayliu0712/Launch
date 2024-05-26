package rl.launch

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
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
    private var filesLength = 0L
    private var neverAskPermissionsAgain = false

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

        binding.clearBtn.setOnClickListener {
            val dialog = Ez.Dialog(this@MainActivity,
                title = "Are u sure ?",
                positiveBtn = "Yes",
                negativeBtn = "No")

            Ez.buildDialog(dialog, { _, _ ->
                rawPaths.clear()
                binding.launchBtn.isEnabled = false
            })
        }
        binding.pickBtn.setOnClickListener {

            val dialog = Ez.Dialog(this@MainActivity,
                cancelable = true,
                title = "File or Folder ?",
                positiveBtn = "File")

            val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                dialog.negativeBtn = "Folder"
                val folderIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                Ez.buildDialog(dialog,
                    { _, _ -> startActivityForResult(fileIntent, 0) },
                    { _, _ -> startActivityForResult(folderIntent, 1) })
            }
            else
                Ez.buildDialog(dialog,
                    { _, _ -> startActivityForResult(fileIntent, 0) })
        }
        binding.aboutBtn.setOnClickListener {
            val dialog = Ez.Dialog(this@MainActivity,
                cancelable = true,
                title = "About",
                msg = "what")

            Ez.buildDialog(dialog)
        }
        binding.launchBtn.setOnClickListener {
            if (rawPaths.size > 0)
                push()
        }

        homeDir = Environment.getExternalStorageDirectory()
        appExternalStorageDir = getExternalFilesDir(null)!!
        moveListTXT = File(appExternalStorageDir, "Move_List.txt")
        pushListTXT = File(appExternalStorageDir, "Push_List.txt").apply { this.delete() }
        clientKeyTXT = File(appExternalStorageDir, "Client_Key.txt").apply { this.delete() }
        pushDoneFile = File(appExternalStorageDir, "PUSH_DONE").apply { this.delete() }
    }

    override fun onResume() {
        super.onResume()

        val permissionDialog = Ez.Dialog(this, title = "Permission Required", positiveBtn = "GRANT")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {

                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName"))

                Ez.buildDialog(permissionDialog,
                    { _, _ -> startActivity(intent) })
            }
        }
        else {
            val read = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            val write = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
            if (read == PERMISSION_DENIED || write == PERMISSION_DENIED) {

                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName"))

                if (neverAskPermissionsAgain)
                    Ez.buildDialog(permissionDialog,
                        { _, _ -> startActivity(intent) })
                else
                    Ez.buildDialog(permissionDialog, { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 0)
                    })
            }
        }

        val devStatus = Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
        val adbStatus = Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0)
        if (adbStatus != 1) {
            val action =
                if (devStatus == 1) Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                else Settings.ACTION_DEVICE_INFO_SETTINGS

            val adbDialog = Ez.Dialog(this, title = "ADB is disabled", positiveBtn = "ENABLE")
            Ez.buildDialog(adbDialog,
                { _, _ -> startActivity(Intent(action)) })
        }

        if (rawPaths.size > 0)
            binding.clearBtn.isEnabled = true
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

        for (file in homeDir.walkTopDown()) {
            val index = hashList.indexOf(file.name to file.length())
            if (index == -1) continue

            hashList.removeAt(index)
            rawPaths.add(file.absolutePath)
            filesLength += Ez.totalLength(file)
        }

        rawPaths.sortByDescending { File(it).lastModified() }

        binding.launchBtn.isEnabled = true
        binding.filesCountTv.text =
            if (rawPaths.size == 1) "1 File"
            else "${rawPaths.size} Files"
        binding.filesSizeTv.text = Ez.size(filesLength)

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

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != 0)
            return

        if (grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED)
            return

        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[1])

        neverAskPermissionsAgain = !shouldShowRationale
    }

}
