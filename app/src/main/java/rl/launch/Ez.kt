package rl.launch

import android.util.Log
import android.widget.Toast
import java.io.File

class Ez(private val ma: MainActivity) {
    private fun runOnUiThread(action: MainActivity.() -> Unit) {
        ma.runOnUiThread { ma.action() }
    }

    fun millis(): Long {
        return System.currentTimeMillis()
    }

    fun log(vararg any: Any) {
        Log.d("666", any.joinToString(" "))
    }

    fun toast(vararg any: Any) {
        runOnUiThread {
            Toast.makeText(this, any.joinToString(" "), Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAndUpdate() {
        rawFiles.clear()
        totalFilesLength = 0
        updateView()
    }

    fun updateView() {
        runOnUiThread {
            val count = rawFiles.size
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
        runOnUiThread {
            binding.clearBtn.isEnabled = isEnable
            binding.pickBtn.isEnabled = isEnable
            binding.aboutBtn.isEnabled = isEnable
            binding.launchBtn.isEnabled = isEnable
        }
    }

    fun totalLength(root: File): Long {
        return root.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    private fun sizeWithUnit(length: Long): String {
        val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
        var size = length.toDouble()
        var i = 0
        while (size >= 1024 && i < 5) {
            size /= 1024
            i++
        }
        return "%.2f ${units[i]}".format(size)
    }
}