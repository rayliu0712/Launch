package rl.launch

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class Secretary {
    lateinit var me: MainActivity
    fun setThis(ma: MainActivity) {
        this.me = ma
    }

    private fun ui(action: MainActivity.() -> Unit) {
        me.runOnUiThread { me.action() }
    }

    fun toast(vararg any: Any) {
        ui {
            Toast.makeText(this, any.joinToString(" "), Toast.LENGTH_SHORT).show()
        }
    }

    fun onCreate() {
        me.apply {
            homeDir = Environment.getExternalStorageDirectory()
            repairFile = File(filesDir, "repair.txt")
            launchFile = File(filesDir, "launch.txt").apply { delete() }
            moveFile = File(filesDir, "move.txt").apply { delete() }
            keyA = File(filesDir, "key_a").apply { delete() }
            keyB = File(filesDir, "key_b").apply { delete() }

            binding.clearBtn.setOnClickListener {
                Ez.ynDialog("Are You Sure ?") { clearAndUpdate() }
            }
            binding.pickBtn.setOnClickListener {

                val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                fileIntent.type = "*/*"
                fileIntent.addCategory(Intent.CATEGORY_OPENABLE)
                fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                val dialog = AlertDialog.Builder(this)
                    .setTitle("File or Folder ?")
                    .setPositiveButton("FILE")
                    { _, _ -> startActivityForResult(fileIntent, 0) }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val folderIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

                    dialog.setNegativeButton("FOLDER")
                    { _, _ -> startActivityForResult(folderIntent, 1) }.show()
                }
                else
                    dialog.show()
            }
            binding.aboutBtn.setOnClickListener {
                Ez.warnDialog("About", "what")
            }
            binding.launchBtn.setOnClickListener {
                launch()
            }
        }
    }

    fun onResume() {
        me.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                if (!Environment.isExternalStorageManager()) {

                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName"))

                    Ez.permissionDialog { me.startActivity(intent) }
                }
            }
            else {
                val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
                    Ez.adbDialog("啟用開發人員選項") { startActivity(Intent(deviceInfoAction)) }
                else
                    Ez.adbDialog("啟用USB偵錯") { startActivity(Intent(devAction)) }
            }

            if (repairFile.exists()) {
                repairFile.forEachLine {
                    val (cookedPath, rawPath) = it.split("\t")
                    File(cookedPath).renameTo(File(rawPath))
                }
                repairFile.delete()

                Ez.warnDialog("Automatic Repair Performed", "due to unexpectedly termination")
            }
            genData(intent)
        }
    }

    fun clearAndUpdate() {
        pending.clear()
        totalLength = 0
        updateView()
    }

    fun updateView() {
        ui {
            val count = pending.size
            if (count == 0) {
                binding.launchBtn.isEnabled = false
                binding.totalFilesSizeTv.text = getString(R.string.zero_byte)
                binding.totalFilesCountTv.text = getString(R.string.no_file)
            }
            else {
                binding.launchBtn.isEnabled = true
                binding.totalFilesSizeTv.text = Ez.sizeWithUnit(totalLength)
                binding.totalFilesCountTv.text =
                    if (count == 1) "1 File"
                    else "$count Files"
            }
        }
    }

    fun setBtnEnable(isEnable: Boolean) {
        ui {
            binding.clearBtn.isEnabled = isEnable
            binding.pickBtn.isEnabled = isEnable
            binding.aboutBtn.isEnabled = isEnable
            binding.launchBtn.isEnabled = isEnable
        }
    }
}