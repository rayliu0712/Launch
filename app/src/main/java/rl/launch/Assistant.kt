package rl.launch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class Assistant(private val ma: MainActivity = Ez.ma) {

    fun onCreate() {
        ma.apply {
            homeDir = Environment.getExternalStorageDirectory()
            appExtDir = getExternalFilesDir(null)!!
            repairLF = File(appExtDir, "Repair_List.txt")
            launchLF = File(appExtDir, "Launch_List.txt").apply { delete() }
            moveLF = File(appExtDir, "Move_List.txt").apply { delete() }
            clientKF = File(appExtDir, "Client_Key.txt").apply { delete() }
            launchCF = File(appExtDir, "LAUNCH_COMPLETED").apply { delete() }

            binding.clearBtn.setOnClickListener {
                Dialog.yesNo("Clear", "are you sure ?") { _, _ -> clearAndUpdate() }
            }
            binding.pickBtn.setOnClickListener {

                val dialog = Dialog.abBuilder("Pick", "File or Folder ?", "FILE")

                val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                fileIntent.type = "*/*"
                fileIntent.addCategory(Intent.CATEGORY_OPENABLE)
                fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val folderIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    dialog.negBtn = "FOLDER"
                    dialog.show(
                        { _, _ -> startActivityForResult(fileIntent, 0) },
                        { _, _ -> startActivityForResult(folderIntent, 1) })
                }
                else
                    dialog.show({ _, _ -> startActivityForResult(fileIntent, 0) })
            }
            binding.aboutBtn.setOnClickListener {
                Dialog.warn("About", "what")
            }
            binding.launchBtn.setOnClickListener {
                launch()
            }
        }
    }

    fun onResume() {
        ma.apply {
            val permissionDialog =
                Dialog.posBuilder("Permission Required", "click to grant", "GRANT")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                if (!Environment.isExternalStorageManager()) {

                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName"))

                    permissionDialog.show({ _, _ -> startActivity(intent) })
                }
            }
            else {
                val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (read == PackageManager.PERMISSION_DENIED || write == PackageManager.PERMISSION_DENIED) {

                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName"))

                    if (neverAskPermissionsAgain)
                        permissionDialog.show({ _, _ -> startActivity(intent) })
                    else
                        permissionDialog.show({ _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                        })
                }
            }

            val devStatus = Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
            val adbStatus = Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0)
            if (adbStatus != 1) {
                val action =
                    if (devStatus == 1) Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                    else Settings.ACTION_DEVICE_INFO_SETTINGS

                val adbDialog = Dialog.posBuilder("ADB is disabled", "click to enable", "ENABLE")
                adbDialog.show({ _, _ -> startActivity(Intent(action)) })
            }

            if (repairLF.exists()) {
                repairLF.forEachLine {
                    val (cookedPath, rawPath) = it.split("\t")
                    File(cookedPath).renameTo(File(rawPath))
                }
                repairLF.delete()

                Dialog.warn("Automatic Repair Performed", "due to unexpectedly termination")
            }
            genData(intent)
        }
    }

    private fun clearAndUpdate() {
        pendingFiles.clear()
        totalFilesLength = 0
        updateView()
    }

    private fun updateView() {
        Ez.ui {
            val count = pendingFiles.size
            if (count == 0) {
                binding.launchBtn.isEnabled = false
                binding.totalFilesSizeTv.text = getString(R.string.zero_byte)
                binding.totalFilesCountTv.text = getString(R.string.no_file)
            }
            else {
                binding.launchBtn.isEnabled = true
                binding.totalFilesSizeTv.text = Ez.sizeWithUnit(totalFilesLength)
                binding.totalFilesCountTv.text =
                    if (count == 1) "1 File"
                    else "$count Files"
            }
        }
    }

    private fun setBtnEnable(isEnable: Boolean) {
        Ez.ui {
            binding.clearBtn.isEnabled = isEnable
            binding.pickBtn.isEnabled = isEnable
            binding.aboutBtn.isEnabled = isEnable
            binding.launchBtn.isEnabled = isEnable
        }
    }

    fun genData(intent: Intent) {
        ma.apply {

            val uris: List<Uri> = when (intent.action) {

                Intent.ACTION_SEND ->
                    listOf(intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)

                Intent.ACTION_SEND_MULTIPLE ->
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!

                Intent.ACTION_OPEN_DOCUMENT -> {
                    // single file -> intent.data
                    // multiple files -> intent.clipData.getItem().uri

                    val clipData = intent.clipData

                    if (clipData == null)
                        listOf(intent.data!!)
                    else
                        (0 until clipData.itemCount)
                            .map { clipData.getItemAt(it).uri }
                }

                // RETURN
                Intent.ACTION_MAIN -> {
                    updateView()
                    return
                }

                // Intent.ACTION_OPEN_DOCUMENT_TREE
                else -> listOf()
            }

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
                        Dialog.warn("Duplicated File Detected", "this request will be ignored")
                }
                cursor.close()
            }

            // Intent.ACTION_OPEN_DOCUMENT_TREE
            if (uris.isEmpty()) {
                val documentFile = DocumentFile.fromTreeUri(this, intent.data!!)
                hashSet.add(documentFile!!.name!! to 0)
            }

            for (file in homeDir.walkTopDown()) {
                val pair = file.name to
                        if (file.isDirectory) 0
                        else file.length()

                if (!hashSet.contains(pair))
                    continue

                hashSet.remove(pair)
                if (!pendingFiles.add(file))
                    Dialog.warn("Duplicated File Detected", "this request will be ignored")
                else
                    totalFilesLength += Ez.totalLength(file)
            }

            updateView()
        }
    }

    private fun launch() {
        ma.apply {

            setBtnEnable(false)

            var available = pendingFiles.size
            val repairList = arrayListOf<Pair<File, File>>()

            for (raw in pendingFiles) {
                if (!raw.exists()) {
                    available--
                    Dialog.warn("File Not Exist", "request for this file will be ignored")
                    continue
                }

                var cooked = raw
                if (Ez.isNotASCII(raw.absolutePath)) {
                    cooked = File(appExtDir, "${raw.hashCode()}")

                    repairList.add(cooked to raw)
                    repairLF.appendText("${cooked.absolutePath}\t${raw.absolutePath}")
                    moveLF.appendText("${cooked.name}\t${raw.name}")

                    raw.renameTo(cooked)
                }

                launchLF.appendText(cooked.name)
            }

            if (available == 0)
                return

            Ez.toast("LAUNCH!")
            lifecycleScope.launch {
                while (!launchCF.exists()) {
                    clientKF.writeText("${System.currentTimeMillis()}")
                    delay(250)
                }
                Ez.toast("LAUNCH COMPLETED")

                for ((cooked, raw) in repairList) {
                    cooked.renameTo(raw)
                }

                repairLF.delete()
                launchLF.delete()
                moveLF.delete()
                clientKF.delete()
                launchCF.delete()

                setBtnEnable(true)
                clearAndUpdate()
            }
        }
    }
}