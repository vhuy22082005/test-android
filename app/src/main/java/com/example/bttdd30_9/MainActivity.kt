package com.example.bttdd30_9

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bttdd30_9.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    // SharedPreferences keys
    private val PREFS_NAME = "student_prefs"
    private val KEY_SCORE10 = "score10_value"
    private val KEY_CONVERTED = "is_converted"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Hiển thị thông tin sinh viên mặc định
        displayStudentProfile(
            name = "Nguyễn Doãn Vinh Huy",
            studentId = "23115141122108",
            email = "vinhhuy@example.com"
        )

        // Khôi phục trạng thái nếu đã quy đổi trước đó
        restoreConvertedState()

        // 2. Xử lý nút quy đổi điểm
        binding.btnConvert.setOnClickListener {
            // Nếu đã quy đổi thì không làm gì
            if (!isAlreadyConverted()) {
                // Lấy GPA hiện tại từ tvGpa (chuỗi dạng "3.2 / 4.0")
                val gpaText = binding.tvGpa.text.toString()
                val gpaValue = parseGpa(gpaText)
                if (gpaValue == null) {
                    Toast.makeText(this, "GPA không hợp lệ", Toast.LENGTH_SHORT).show()
                } else {
                    performConversion(gpaValue)
                }
            }
        }

        // 3. Xử lý nút cập nhật sinh viên
        binding.btnUpdate.setOnClickListener {
            Toast.makeText(this, "Đã cập nhật thông tin sinh viên", Toast.LENGTH_SHORT).show()
        }

        // 4. Xử lý nút chọn ảnh đại diện
        binding.btnAvatar.setOnClickListener {
            Toast.makeText(this, "Chọn ảnh đại diện", Toast.LENGTH_SHORT).show()
        }
    }

    // Hiển thị thông tin sinh viên bằng with scope function
    private fun displayStudentProfile(name: String, studentId: String, email: String) {
        with(binding) {
            tvName.text = name
            tvStudentId.text = "Mã sinh viên: $studentId"
            tvEmail.text = email
            tvAvatarStatus.text = "Chưa chọn ảnh đại diện"
        }
    }

    // Cập nhật điểm & ghi log sử dụng also scope function (giữ nguyên nhưng không trực tiếp quy đổi hệ 10 ở đây)
    private fun updateGpaDisplay(gpa: Double) {
        gpa.also { score ->
            Log.d("STUDENT_AUDIT", "Cập nhật GPA: $score")

            val formattedGpa = String.format(Locale.getDefault(), "%.2f", score)
            binding.tvGpa.text = "$formattedGpa / 4.0"

            // Khi chỉ cập nhật GPA, giữ trạng thái hệ 10 là "Chưa tính" nếu chưa quy đổi
            if (!isAlreadyConverted()) {
                binding.tvScore10.text = "Chưa tính"
                binding.tvResult.text = ""
                binding.btnConvert.isEnabled = true
                binding.btnConvert.text = "Quy đổi điểm"
            }

            Toast.makeText(this, "Đã cập nhật điểm thành công", Toast.LENGTH_SHORT).show()
        }
    }

    // Thực hiện quy đổi (có progress giả lập), lưu trạng thái và cập nhật UI
    private fun performConversion(gpa: Double) {
        // Vô hiệu hóa nút và hiện progress
        binding.btnConvert.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.tvResult.text = ""

        // Giả lập xử lý (1.2s)
        handler.postDelayed({
            // Quy đổi: hệ 10 = GPA4 / 4 * 10
            val score10 = (gpa / 4.0) * 10.0
            val score10Rounded = String.format(Locale.getDefault(), "%.1f / 10", score10)
            val score10ValueOnly = String.format(Locale.getDefault(), "%.1f", score10)

            // Lưu vào SharedPreferences
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SCORE10, score10Rounded)
                .putBoolean(KEY_CONVERTED, true)
                .apply()

            // Cập nhật UI
            applyConvertedState(score10Rounded)

            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Đã quy đổi điểm thành công", Toast.LENGTH_SHORT).show()
        }, 1200)
    }

    // Áp dụng trạng thái đã quy đổi lên UI
    private fun applyConvertedState(score10Rounded: String) {
        binding.tvScore10.text = score10Rounded
        val numeric = score10Rounded.split("/").firstOrNull()?.trim() ?: score10Rounded
        binding.tvResult.text = "✓ Đã cập nhật điểm: $numeric"
        binding.btnConvert.text = "Đã quy đổi"
        binding.btnConvert.isEnabled = false
    }

    // Kiểm tra trạng thái đã quy đổi
    private fun isAlreadyConverted(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CONVERTED, false)
    }

    // Khôi phục trạng thái khi mở app
    private fun restoreConvertedState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val converted = prefs.getBoolean(KEY_CONVERTED, false)
        if (converted) {
            val savedScore = prefs.getString(KEY_SCORE10, null)
            if (!savedScore.isNullOrEmpty()) {
                applyConvertedState(savedScore)
            } else {
                // Nếu cờ converted true nhưng không có giá trị, reset cờ và UI
                prefs.edit().putBoolean(KEY_CONVERTED, false).apply()
                resetUiInitial()
            }
        } else {
            resetUiInitial()
        }
    }

    private fun resetUiInitial() {
        binding.tvScore10.text = "Chưa tính"
        binding.tvResult.text = ""
        binding.btnConvert.isEnabled = true
        binding.btnConvert.text = "Quy đổi điểm"
        binding.progressBar.visibility = View.GONE
    }

    // Hàm đọc số GPA từ chuỗi dạng "2.5 / 4.0" hoặc "3.8 / 4.0"
    private fun parseGpa(text: String): Double? {
        return try {
            val parts = text.split("/")
            if (parts.isEmpty()) return null
            val left = parts[0].trim()
            left.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
