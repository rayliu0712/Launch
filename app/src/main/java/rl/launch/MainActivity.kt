package rl.launch

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private val ass = Assistant(this)

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

        Ez.ma = this
        ass.onCreate()
    }

    override fun onResume() {
        super.onResume()
        ass.onResume()
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
            0 -> ass.genData(data!!.apply { action = Intent.ACTION_OPEN_DOCUMENT })
            1 -> ass.genData(data!!.apply { action = Intent.ACTION_OPEN_DOCUMENT_TREE })
        }
    }
}