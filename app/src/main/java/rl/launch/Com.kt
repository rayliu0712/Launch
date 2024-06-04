package rl.launch

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.AlertDialog
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rl.launch.MainActivity.Companion.me
import java.io.File

object Com {
    private suspend fun <T> ui(block: suspend CoroutineScope.() -> T): T =
        withContext(Dispatchers.Main, block)

    fun updateView(clear: Boolean = false) {
        if (clear) {
            repairFile.delete()
            launchFile.delete()
            moveFile.delete()
            keyA.delete()
            keyB.delete()
            pending.clear()
        }

        val count = pending.size
        me.binding.launchBtn.isEnabled = (count != 0)
        me.binding.totalFilesSizeTv.text = Ez.totalSize()
        me.binding.totalFilesCountTv.text =
            if (count <= 1) "$count File"
            else "$count Files"
    }

    fun setBtnEnable(isEnable: Boolean) {
        me.binding.apply {
            clearBtn.isEnabled = isEnable
            pickBtn.isEnabled = isEnable
            aboutBtn.isEnabled = isEnable
            launchBtn.isEnabled = isEnable && pending.isNotEmpty()
        }
    }

    fun grant() {
        me.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                if (!Environment.isExternalStorageManager()) {

                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName"))

                    Ez.permissionDialog { startActivity(intent) }
                }
            }
            else {
                val read = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
                val write = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                if (read == PackageManager.PERMISSION_DENIED || write == PackageManager.PERMISSION_DENIED) {

                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName"))

                    if (neverAskAgain)
                        Ez.permissionDialog { startActivity(intent) }
                    else
                        Ez.permissionDialog {
                            ActivityCompat.requestPermissions(
                                this, arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 0)
                        }
                }
            }

            val devStatus = Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
            val adbStatus = Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0)
            val deviceInfoAction = Settings.ACTION_DEVICE_INFO_SETTINGS
            val devAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            if (adbStatus != 1) {
                if (devStatus != 1)
                    Ez.adbDialog { startActivity(Intent(deviceInfoAction)) }
                else
                    Ez.adbDialog { startActivity(Intent(devAction)) }
            }
        }
    }

    fun pick() {
        val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        fileIntent.type = "*/*"
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE)
        fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        val dialog = Ez.dialog(true)
            .setTitle("File or Folder(Android 5.0+) ?")
            .setPositiveButton("FILE")
            { _, _ -> me.startActivityForResult(fileIntent, 0) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val folderIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

            dialog.setNegativeButton("FOLDER")
            { _, _ -> me.startActivityForResult(folderIntent, 1) }.show()
        }
        else
            dialog.show()
    }

    fun repair(isAccident: Boolean) {
        if (!repairFile.exists())
            return

        repairFile.forEachLine {
            val (cookedPath, rawPath) = it.split("\t")
            File(cookedPath).renameTo(File(rawPath))
        }
        repairFile.delete()

        if (isAccident)
            Ez.warnDialog("意外終止", "已執行自動恢復")
    }

    fun genData(intent: Intent) {
        me.apply {
            val uris: List<Uri> = when (intent.action) {

                Intent.ACTION_SEND ->
                    listOf(intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)

                Intent.ACTION_SEND_MULTIPLE ->
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!

                Intent.ACTION_OPEN_DOCUMENT -> {
                    val clipData = intent.clipData
                    if (clipData == null)
                        listOf(intent.data!!)
                    else
                        (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
                }

                Intent.ACTION_MAIN -> {
                    updateView()
                    return
                }

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
                    val isDuplicated = pending.any { it.name == name && it.length() == size }

                    if (isDuplicated || !hashSet.add(pair)) {
                        Ez.warnDialog("檔案重複", "重複的${name}不會加入到名單中")
                        continue
                    }
                }
                cursor.close()
            }

            // Intent.ACTION_OPEN_DOCUMENT_TREE
            if (uris.isEmpty()) {
                val documentFile = DocumentFile.fromTreeUri(this, intent.data!!)
                val name = documentFile!!.name!!
                val isDuplicated = pending.any { it.name == name && it.isDirectory }

                if (isDuplicated || !hashSet.add(name to -1))
                    Ez.warnDialog("檔案重複", "重複的${name}不會加入到名單中")
            }

            val possible = homeDir.walkTopDown()
                .filter { hashSet.contains(it.name to Ez.specialLen(it)) }

            for (file in possible) {
                if (!pending.add(file))
                    Ez.warnDialog("搜尋到相同的檔案", "${file.name}\n" +
                            "採用 ${Ez.relPath(pending.filter { it.name == file.name }[0].parentFile!!)}\n" +
                            "不採用 ${Ez.relPath(file.parentFile!!)}")
            }

            updateView()
        }
    }

    fun launchEvent() {
        val repairList = arrayListOf<Pair<File, File>>()
        prepareLaunch(repairList)

        CoroutineScope(Dispatchers.IO).launch {
            if (connectServer())
                startLaunch(repairList)
        }
    }

    private fun prepareLaunch(repairList: ArrayList<Pair<File, File>>) {
        for (raw in pending) {
            var cooked = raw
            if (Ez.isNotASCII(raw.absolutePath)) {
                cooked = File(raw.parent, "${raw.hashCode()}")

                repairList.add(cooked to raw)
                repairFile.appendText("${cooked.absolutePath}\t${raw.absolutePath}\n")
                moveFile.appendText("${cooked.name}\t${raw.name}\n")

                raw.renameTo(cooked)
            }

            launchFile.appendText("${cooked.absolutePath}\n")
        }
    }

    private suspend fun connectServer(): Boolean {
        var exit = false
        lateinit var connectDialog: AlertDialog
        ui {
            connectDialog = Ez.dialog()
                .setTitle("與Server連接中 (30s)")
                .setPositiveButton("EXIT") { _, _ ->
                    exit = true
                }
                .show()
        }

        for (i in 580 downTo 0) {
            if (keyA.exists()) {
                ui { connectDialog.dismiss() }
                return true
            }
            if (exit) break

            delay(50)
            if (i % 20 == 0)
                ui { connectDialog.setTitle("與Server連接中 (${i / 20}s)") }
        }

        ui {
            repair(false)
            setBtnEnable(true)
            connectDialog.dismiss()
            if (!exit)
                Ez.warnDialog("連接失敗", "請檢查Server是否啟動和USB連接設定")
        }
        return false
    }

    private suspend fun startLaunch(repairList: ArrayList<Pair<File, File>>) {
        lateinit var waitDialog: AlertDialog
        ui {
            waitDialog = Ez.dialog()
                .setTitle("Launching (0s)")
                .setMessage("結束前請不要關閉應用程式")
                .show()
        }

        var i = 0
        while (!keyB.exists()) {
            delay(1000)
            ui { waitDialog.setTitle("Launching (${++i}s)") }
        }

        for ((cooked, raw) in repairList)
            cooked.renameTo(raw)

        ui {
            updateView(true)
            setBtnEnable(true)

            waitDialog.setMessage("完成 !")
            waitDialog.setCancelable(true)
        }
    }
}