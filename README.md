# PermissionHelper

Android 权限申请，目标是把权限选择的代码做到最简洁。

## 一、引入

```groovy
// 在 project 根目录的 build.gradle 中添加：
allprojects {
    repositories {
        maven { url "https://raw.githubusercontent.com/AudienL/repos/master" }
    }
}

// 在 module 根目录的 build.gradle 中添加：
dependencies {
    implementation 'com.audient:permission_helper:1.1.0'
}
```

## 二、使用

* 在 `AndroidManifest.xml` 中注册权限，如：

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

* 在代码中需要权限的地方申请即可，如：

```java
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
```

## 三、博客链接

[https://blog.csdn.net/qiantujava/article/details/103402239](https://blog.csdn.net/qiantujava/article/details/103402239)