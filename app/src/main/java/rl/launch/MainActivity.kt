package rl.launch

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import rl.launch.databinding.ActivityMainBinding
import java.io.File
import java.util.TreeSet

val pending = TreeSet<File> { f1, f2 -> f2.lastModified().compareTo(f1.lastModified()) }
var neverAskAgain = false
lateinit var launchFile: File
lateinit var keyA: File
lateinit var keyB: File

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val com = Com(this)

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

        launchFile = File(filesDir, "launch.txt").apply { delete() }
        keyA = File(filesDir, "key_a").apply { delete() }
        keyB = File(filesDir, "key_b").apply { delete() }

        binding.aboutBtn.setOnClickListener {
            com.about()
        }
        binding.clearBtn.setOnClickListener {
            Ez.dialog(this)
                .setTitle("你確定嗎 ?")
                .setPositiveButton("YES") { _, _ ->
                    com.clear()
                    com.updateView()
                }
                .setNegativeButton("NO", null)
                .show()
        }
        binding.pickBtn.setOnClickListener {
            com.pick()
        }
        binding.launchBtn.setOnClickListener {
            com.launchEvent()
        }

        com.genData(intent)
    }

    override fun onResume() {
        super.onResume()
        com.grant()
        if (intent.action in arrayOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE))
            intent.action = Intent.ACTION_MAIN
        com.genData(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED)
            return

        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[1])

        neverAskAgain = !shouldShowRationale
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_CANCELED)
            return

        when (requestCode) {
            0 -> com.genData(data!!.apply { action = Intent.ACTION_OPEN_DOCUMENT })
            1 -> com.genData(data!!.apply { action = Intent.ACTION_OPEN_DOCUMENT_TREE })
        }
    }
}