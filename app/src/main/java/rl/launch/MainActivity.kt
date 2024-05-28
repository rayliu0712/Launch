package rl.launch

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rl.launch.databinding.ActivityMainBinding
import java.io.File
import java.util.TreeSet

val pendingFiles = TreeSet<File> { f1, f2 -> f2.lastModified().compareTo(f1.lastModified()) }
var totalFilesLength = 0L
var neverAskPermissionsAgain = false

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var homeDir: File
    lateinit var appExtDir: File
    lateinit var repairLF: File
    lateinit var launchLF: File
    lateinit var moveLF: File
    lateinit var clientKF: File
    lateinit var launchCF: File
    private val sec = Secretary(this)

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

        sec.onCreate()
    }

    override fun onResume() {
        super.onResume()
        sec.onResume()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_CANCELED)
            return

        when (requestCode) {
            0 -> genData(data!!.apply { action = Intent.ACTION_OPEN_DOCUMENT })
            1 -> genData(data!!.apply { action = Intent.ACTION_OPEN_DOCUMENT_TREE })
        }
    }

    fun genData(intent: Intent) {
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
                sec.updateView()
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
                totalFilesLength += totalLength(file)
        }

        sec.updateView()
    }

    fun launch() {
        sec.setBtnEnable(false)

        var available = pendingFiles.size
        val repairList = arrayListOf<Pair<File, File>>()

        for (raw in pendingFiles) {
            if (!raw.exists()) {
                available--
                Dialog.warn("File Not Exist", "request for this file will be ignored")
                continue
            }

            var cooked = raw
            if (isNotASCII(raw.absolutePath)) {
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

        sec.toast("LAUNCH!")
        lifecycleScope.launch {
            while (!launchCF.exists()) {
                clientKF.writeText("${System.currentTimeMillis()}")
                delay(250)
            }
            sec.toast("LAUNCH COMPLETED")

            for ((cooked, raw) in repairList) {
                cooked.renameTo(raw)
            }

            repairLF.delete()
            launchLF.delete()
            moveLF.delete()
            clientKF.delete()
            launchCF.delete()

            sec.setBtnEnable(true)
            sec.clearAndUpdate()
        }
    }
}