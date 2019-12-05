package com.audient.permissionhelper

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.audient.permission_helper.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_has_permissions.setOnClickListener {
            // 检查某个权限列表是否都已经允许了
            val has = hasPermissions(this, arrayOf(Manifest.permission.CAMERA))
            Toast.makeText(this, has.toString(), Toast.LENGTH_SHORT).show()
        }

        btn_run_on_permissions.setOnClickListener {
            // 申请某个权限列表
            runOnPermissions(this, arrayOf(Manifest.permission.CAMERA), object : OnPermissionCallback {
                override fun onAllGranted() {
                    // 所有权限都允许后回调。
                    Toast.makeText(this@MainActivity, "onAllGranted", Toast.LENGTH_SHORT).show()
                }

                override fun onAllDenied() {
                    // 只有传入的权限列表中有一个拒绝了，都会回调。
                    Toast.makeText(this@MainActivity, "onAllDenied", Toast.LENGTH_SHORT).show()
                }

                override fun onShowRationale() {
                    // 拒绝后再次申请，弹框提示用户为什么需要这个权限
                    Toast.makeText(this@MainActivity, "onShowRationale", Toast.LENGTH_SHORT).show()
                }

                override fun onNeverAskAgain() {
                    // 用户选择不再询问的时候回调
                    Toast.makeText(this@MainActivity, "onNeverAskAgain", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btn_run_on_permissions_or_finish.setOnClickListener {
            // 判断某些权限是否已经允许，未允许则申请。
            // 用户拒绝后会弹框提示用户必须允许，否则finish。
            runOnPermissionsOrFinish(this, arrayOf(Manifest.permission.CAMERA), {
                Toast.makeText(this, "onAllGranted", Toast.LENGTH_SHORT).show()
            })
        }

        btn_has_nfc_for_xiaomi.setOnClickListener {
            // 检查小米NFC权限是否已经打开
            val has = hasOpPermission(this, 10016)
            Toast.makeText(this, has.toString(), Toast.LENGTH_SHORT).show()
        }
    }
}