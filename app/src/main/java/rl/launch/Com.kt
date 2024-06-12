package rl.launch

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rl.launch.MainActivity.Companion.me

object Com {
    private suspend fun <T> ui(block: suspend CoroutineScope.() -> T): T =
        withContext(Dispatchers.Main, block)

    fun clear() {
        launchFile.delete()
        keyA.delete()
        keyB.delete()
        pending.clear()
    }

    fun updateView() {
        val count = pending.size
        me.binding.launchBtn.isEnabled = (count != 0)
        me.binding.totalFilesSizeTv.text = Ez.sizeTv()
        me.binding.totalFilesCountTv.text =
            if (count <= 1) "$count File"
            else "$count Files"
    }

    fun setBtnEnable(isEnable: Boolean) {
        me.binding.clearBtn.isEnabled = isEnable
        me.binding.pickBtn.isEnabled = isEnable
        me.binding.aboutBtn.isEnabled = isEnable
        me.binding.launchBtn.isEnabled = isEnable && pending.isNotEmpty()
    }

    fun about() {
        val aboutLayout = me.layoutInflater.inflate(R.layout.about_layout, null)
        val icon = aboutLayout.findViewById<ImageView>(R.id.icon)
        val date = aboutLayout.findViewById<TextView>(R.id.date)
        val dialog = Ez.dialog(true)
            .setTitle("關於Launch")
            .setView(aboutLayout)
            .show()

        icon.setOnClickListener {
            it.rotation = (it.rotation + 45) % 360
            if (it.rotation == 45f) {
                dialog.setTitle("\uD83C\uDF82\uD83C\uDF82\uD83C\uDF82")
                date.text = me.getString(R.string.birthday)
            }
            else {
                dialog.setTitle("關於Launch")
                date.text = me.getString(R.string.last_modified_date)
            }
        }

        date.setOnClickListener {
            Ez.url(
                if (icon.rotation == 45f) "https://www.facebook.com/profile.php?id=61551233015895"
                else "https://github.com/rayliu0712/Launch")
        }
    }

    fun grant() {
        if (SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:rl.launch"))
                Ez.permissionDialog { startActivity(intent) }
            }
        }
        else {
            val read = ContextCompat.checkSelfPermission(me, READ_EXTERNAL_STORAGE)
            val write = ContextCompat.checkSelfPermission(me, WRITE_EXTERNAL_STORAGE)
            if (read == PackageManager.PERMISSION_DENIED || write == PackageManager.PERMISSION_DENIED) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:rl.launch"))

                if (neverAskAgain)
                    Ez.permissionDialog { startActivity(intent) }
                else
                    Ez.permissionDialog {
                        ActivityCompat.requestPermissions(
                            this, arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 0)
                    }
            }
        }

        val devStatus = Settings.Global.getInt(me.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
        val adbStatus = Settings.Global.getInt(me.contentResolver, Settings.Global.ADB_ENABLED, 0)
        val deviceInfoAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        val devAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
        if (adbStatus != 1) {
            if (devStatus != 1)
                Ez.adbDialog { startActivity(Intent(deviceInfoAction)) }
            else
                Ez.adbDialog { startActivity(Intent(devAction)) }
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

    fun genData(intent: Intent) {
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
            val cursor = me.contentResolver.query(uri, null, null, null, null)!!
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
            val documentFile = DocumentFile.fromTreeUri(me, intent.data!!)!!
            val name = documentFile.name!!
            val isDuplicated = pending.any { it.name == name && it.isDirectory }

            if (isDuplicated || !hashSet.add(name to -1))
                Ez.warnDialog("檔案重複", "重複的${name}不會加入到名單中")
        }

        for (h in hashSet) {
            val possibilities = Environment.getExternalStorageDirectory().walkTopDown().filter {
                it.name == h.first && Ez.specialLen(it) == h.second
            }.toList()

            pending.add(possibilities[0])
            for ((i, file) in possibilities.withIndex()) {
                if (i == 0) continue
                Ez.warnDialog("搜尋到相同的檔案", "${file.name}\n" +
                        "採用 ${Ez.relPath(pending.filter { it.name == file.name }[0].parentFile!!)}\n" +
                        "不採用 ${Ez.relPath(file.parentFile!!)}")
            }
        }

        updateView()
    }

    fun launchEvent() {
        launchFile.writeText("${pending.sumOf { Ez.length(it) }}\n" +
                pending.joinToString("\n") { it.absolutePath })

        CoroutineScope(Dispatchers.IO).launch {
            if (connectServer())
                startLaunch()
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

        launchFile.delete()
        ui {
            setBtnEnable(true)
            connectDialog.dismiss()
            if (!exit)
                Ez.warnDialog("連接失敗", "請檢查Server是否啟動和USB連接設定")
        }
        return false
    }

    private suspend fun startLaunch() {
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

        clear()
        ui {
            updateView()
            setBtnEnable(true)
            waitDialog.setMessage("完成 !")
            waitDialog.setCancelable(true)
        }
    }
}