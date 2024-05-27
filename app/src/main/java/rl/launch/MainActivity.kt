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
import androidx.annotation.RequiresApi
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
import java.util.TreeSet

val rawFiles = TreeSet<File> { f1, f2 -> f2.lastModified().compareTo(f1.lastModified()) }
var totalFilesLength = 0L

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val ez = Ez(this)

    private lateinit var homeDir: File
    private lateinit var appExternalStorageDir: File
    private lateinit var repairListTXT: File
    private lateinit var launchListTXT: File
    private lateinit var moveListTXT: File
    private lateinit var clientKeyTXT: File
    private lateinit var launchCompletedFile: File
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
            val dialog = Dialog(
                this,
                title = "Are u sure ?",
                positiveBtn = "Yes",
                negativeBtn = "No")

            dialog.build({ _, _ -> ez.clearAndUpdate() })
        }
        binding.pickBtn.setOnClickListener {

            val dialog = Dialog(this,
                title = "File or Folder ?",
                cancelable = true,
                positiveBtn = "File")

            val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                dialog.negativeBtn = "Folder"
                val folderIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                dialog.build(
                    { _, _ -> startActivityForResult(fileIntent, 0) },
                    { _, _ -> startActivityForResult(folderIntent, 1) })
            }
            else
                dialog.build({ _, _ -> startActivityForResult(fileIntent, 0) })
        }
        binding.aboutBtn.setOnClickListener {
            Dialog.warning(this, "About", "what")
        }
        binding.launchBtn.setOnClickListener {
            launch()
        }

        homeDir = Environment.getExternalStorageDirectory()
        appExternalStorageDir = getExternalFilesDir(null)!!
        repairListTXT = File(appExternalStorageDir, "Repair_List.txt")
        launchListTXT = File(appExternalStorageDir, "Launch_List.txt").apply { delete() }
        moveListTXT = File(appExternalStorageDir, "Move_List.txt").apply { delete() }
        clientKeyTXT = File(appExternalStorageDir, "Client_Key.txt").apply { delete() }
        launchCompletedFile = File(appExternalStorageDir, "LAUNCH_COMPLETED").apply { delete() }
    }

    override fun onResume() {
        super.onResume()

        val permissionDialog = Dialog(this, title = "Permission Required", positiveBtn = "GRANT")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {

                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName"))

                permissionDialog.build({ _, _ -> startActivity(intent) })
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
                    permissionDialog.build({ _, _ -> startActivity(intent) })
                else
                    permissionDialog.build({ _, _ ->
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

            val adbDialog = Dialog(this, title = "ADB is disabled", positiveBtn = "ENABLE")
            adbDialog.build({ _, _ -> startActivity(Intent(action)) })
        }

        if (repairListTXT.exists()) {
            repairListTXT.forEachLine {
                val (cookedPath, rawPath) = it.split("\t")
                File(cookedPath).renameTo(File(rawPath))
            }
            repairListTXT.delete()

            Dialog.warning(this, "Automatic Repair Performed", "due to unexpectedly termination")
        }
        genData(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED)
            return

        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[1])

        neverAskPermissionsAgain = !shouldShowRationale
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_CANCELED)
            return

        when (requestCode) {
            0 -> genData(data!!.apply { action = Intent.ACTION_OPEN_DOCUMENT })
            1 -> genData(data!!.apply { action = Intent.ACTION_OPEN_DOCUMENT_TREE })
        }
    }

    private fun genData(intent: Intent) {
        val uris: ArrayList<Uri> = when (intent.action) {

            Intent.ACTION_SEND ->
                arrayListOf(intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)

            Intent.ACTION_SEND_MULTIPLE ->
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!

            Intent.ACTION_OPEN_DOCUMENT ->
                arrayListOf(intent.data!!)

            Intent.ACTION_OPEN_DOCUMENT_TREE -> {
                val clipData = intent.clipData!!
                arrayListOf<Uri>().apply {
                    for (i in 0 until clipData.itemCount)
                        add(clipData.getItemAt(i).uri)
                }
            }

            else -> {
                ez.updateView()
                return
            }
        }
        // RETURN IF INTENT IS NOT SHARE

        val hashSet = hashSetOf<Pair<String, Long>>()
        for (uri in uris) {
            val cursor = contentResolver.query(uri, null, null, null, null)!!
            if (cursor.moveToFirst()) {
                val nameColumnIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                val sizeColumnIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)

                val name = cursor.getString(nameColumnIndex)
                val size = cursor.getLong(sizeColumnIndex)
                val pair = name to size

                if (!hashSet.add(pair))
                    Dialog.warning(this, "Duplicated File Detected", "this request will be ignored")
            }
            cursor.close()
        }

        for (file in homeDir.walkTopDown()) {
            val pair = file.name to file.length()

            if (!hashSet.contains(pair))
                continue

            hashSet.remove(pair)
            if (!rawFiles.add(file))
                Dialog.warning(this, "Duplicated File Detected", "this request will be ignored")
            else
                totalFilesLength += ez.totalLength(file)
        }

        ez.updateView()
    }

    private fun launch() {
        ez.setBtnEnable(false)

        val cookedRawPairs = mutableListOf<Pair<File, File>>()

        val unavailable = arrayListOf<File>()
        for (rawFile in rawFiles) {
            if (!rawFile.exists()) {
                unavailable.add(rawFile)
                Dialog.warning(this, "File Not Exist", "request for this file will be ignored")
                continue
            }

            val isASCII = rawFile.absolutePath.matches("\\A\\p{ASCII}*\\z".toRegex())
            if (!isASCII) {
                val cookedFile = File(appExternalStorageDir, "${rawFile.hashCode()}")
                rawFile.renameTo(cookedFile)
                cookedRawPairs.add(cookedFile to rawFile)
            }
            else
                cookedRawPairs.add(rawFile to rawFile)
        }

        unavailable.forEach { rawFiles.remove(it) }

        if (rawFiles.size == 0)
            return

        repairListTXT.writeText(cookedRawPairs.joinToString("\n") { "${it.first.absolutePath}\t${it.second.absolutePath}" })
        launchListTXT.writeText(cookedRawPairs.joinToString("\n") { it.first.absolutePath })
        moveListTXT.writeText(cookedRawPairs.joinToString("\n") { "${it.first.name}\t${it.second.name}" })

        ez.toast("LAUNCH!")
        lifecycleScope.launch {
            while (!launchCompletedFile.exists()) {
                clientKeyTXT.writeText("${ez.millis()}")
                delay(250)
            }
            ez.toast("LAUNCH COMPLETED")

            for ((cookedFile, rawFile) in cookedRawPairs) {
                cookedFile.renameTo(rawFile)
            }

            repairListTXT.delete()
            launchListTXT.delete()
            moveListTXT.delete()
            clientKeyTXT.delete()
            launchCompletedFile.delete()

            ez.setBtnEnable(true)
            ez.clearAndUpdate()
        }
    }
}