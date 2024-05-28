package rl.launch

import android.Manifest
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

class Secretary(private val ma: MainActivity) {
    private fun ui(action: MainActivity.() -> Unit) {
        ma.runOnUiThread { ma.action() }
    }

    fun toast(vararg any: Any) {
        ui {
            Toast.makeText(this, any.joinToString(" "), Toast.LENGTH_SHORT).show()
        }
    }

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

    fun clearAndUpdate() {
        pendingFiles.clear()
        totalFilesLength = 0
        updateView()
    }

    fun updateView() {
        ui {
            val count = pendingFiles.size
            if (count == 0) {
                binding.launchBtn.isEnabled = false
                binding.totalFilesSizeTv.text = getString(R.string.zero_byte)
                binding.totalFilesCountTv.text = getString(R.string.no_file)
            }
            else {
                binding.launchBtn.isEnabled = true
                binding.totalFilesSizeTv.text = sizeWithUnit(totalFilesLength)
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