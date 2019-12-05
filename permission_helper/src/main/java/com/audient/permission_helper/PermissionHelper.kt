package com.audient.permission_helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.SparseArray
import androidx.collection.SimpleArrayMap
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import java.lang.Exception

private const val TAG = "PermissionHelper"

// ------------------- 公开方法 START -------------------

// 注意：某些手机在onCreate的时候请求权限，同时又跳转到下一个页面，
// 下一个页面会盖住权限对话框，看起来就想没弹出，实际上只是盖住了。

/**
 * @param permissions e.g. `Manifest.permission.CAMERA`
 * @return true if all granted, false otherwise.
 */
fun hasPermissions(activity: Activity, permissions: Array<String>): Boolean {
    for (permission in permissions) {
        checkPermissionIfInManifest(activity, permission)

        if (isExistsInSdk(permission) && !hasSelfPermission(activity, permission)) {
            return false
        }
    }
    return true
}

/**
 * 判断某些权限是否已经允许，未允许则申请。
 * 用户拒绝后会弹框提示用户必须允许，否则finish。
 */
fun runOnPermissionsOrFinish(
        activity: Activity,
        permissions: Array<String>,
        onGranted: () -> Unit,
        message: String = "必须允许权限才能使用",
        positiveButtonText: String = "重新申请"
) {
    runOnPermissions(activity, permissions, object : OnPermissionCallback {
        override fun onAllGranted() {
            onGranted.invoke()
        }

        override fun onAllDenied() {
            AlertDialog.Builder(activity)
                    .setMessage(message)
                    .setPositiveButton(positiveButtonText) { _, _ ->
                        runOnPermissions(activity, permissions, this)
                    }
                    .setCancelable(false)
                    .show()
        }

        override fun onShowRationale() {
            runOnPermissions(activity, permissions, this, false)
        }

        override fun onNeverAskAgain() {
            AlertDialog.Builder(activity)
                    .setMessage(message)
                    .setPositiveButton("知道了") { _, _ ->
                        activity.finish()
                    }
                    .setCancelable(false)
                    .show()
        }
    })
}

/**
 * 判断某些权限是否已经允许，未允许则申请，并回调结果到[callback]
 * @param checkRationale 是否判断[shouldShowRequestPermissionRationale]方法
 */
@SuppressLint("NewApi")
fun runOnPermissions(
        activity: Activity,
        permissions: Array<String>,
        callback: OnPermissionCallback,
        checkRationale: Boolean = true
) {

    if (hasPermissions(activity, permissions)) {
        callback.onAllGranted()
        return
    }

    // 拒绝后再次申请，弹框提示用户为什么需要这个权限
    if (checkRationale && shouldShowRequestPermissionRationale(activity, permissions)) {
        callback.onShowRationale()
        return
    }

    var fragment: Fragment? = activity.fragmentManager.findFragmentByTag(TAG)
    if (fragment == null) {
        fragment = PermissionFragment()
        val fragmentManager = activity.fragmentManager
        fragmentManager.beginTransaction().add(fragment, TAG).commitAllowingStateLoss()
        fragmentManager.executePendingTransactions()
    }

    mOnPermissionCallbacks.put(mRequestCode, callback)
    fragment.requestPermissions(permissions, mRequestCode)
    mRequestCode++
}

/**
 * 检查类似小米那样独有的权限是否已经允许。
 * 比如：NFC、后台弹出界面等非官方权限。
 *
 * @param op 取值如下：
 * * op=10016 对应 NFC
 * * op=10021 对应 后台弹出界面
 * * 其它未知，根据博客的方法自己去找你需要的
 *
 * @return true为允许，false为询问或者拒绝。
 */
@SuppressLint("NewApi")
fun hasOpPermission(context: Context, op: Int): Boolean {
    return try {
        val manager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val method = manager.javaClass.getMethod("checkOpNoThrow", Int::class.java, Int::class.java, String::class.java)
        val result = method.invoke(manager, op, Process.myUid(), context.packageName)
        AppOpsManager.MODE_ALLOWED == result
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// ------------------- 公开方法 END ---------------------

// 某些新版本新增的权限，旧版本的SDK中没有。
private var MIN_SDK_PERMISSIONS = SimpleArrayMap<String, Int>(8).apply {
    put("com.android.voicemail.permission.ADD_VOICEMAIL", 14)
    put("android.permission.BODY_SENSORS", 20)
    put("android.permission.READ_CALL_LOG", 16)
    put("android.permission.READ_EXTERNAL_STORAGE", 16)
    put("android.permission.USE_SIP", 9)
    put("android.permission.WRITE_CALL_LOG", 16)
    put("android.permission.SYSTEM_ALERT_WINDOW", 23)
    put("android.permission.WRITE_SETTINGS", 23)
}

// manifest中声明的权限
private var permissionsInManifest: Array<String>? = null

// 读取manifest中声明的权限
private fun checkPermissionIfInManifest(activity: Activity, permission: String) {
    if (permissionsInManifest == null) {
        permissionsInManifest = try {
            val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
            packageInfo.requestedPermissions
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            arrayOf()
        }
    }

    // 检查manifest中是否声明了此权限，未声明直接抛出异常
    val declare = permissionsInManifest!!.contains(permission)
    require(declare) { "the permission $permission is not registered in AndroidManifest.xml" }
}

// 当前安卓SDK是否存在此权限
private fun isExistsInSdk(permission: String): Boolean {
    val minSdkVersion = MIN_SDK_PERMISSIONS.get(permission)
    if (minSdkVersion != null && Build.VERSION.SDK_INT < minSdkVersion) {
        return false
    }
    return true
}

// 检查权限
private fun hasSelfPermission(context: Context, permission: String): Boolean {
    return try {
        PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
    } catch (e: Exception) {
        return false
    }
}

// 用户拒绝后再次申请，是否需要提示用户
private fun shouldShowRequestPermissionRationale(activity: Activity?, permissions: Array<out String>): Boolean {
    try {
        permissions.forEach {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!, it)) {
                return true
            }
        }
    } catch (e: Exception) {
        // nothing
    }
    return false
}

interface OnPermissionCallback {
    /**
     * 所有权限都允许后回调。
     */
    fun onAllGranted()

    /**
     * 只有传入的权限列表中有一个拒绝了，都会回调。
     */
    fun onAllDenied()

    /**
     * 拒绝后再次申请，弹框提示用户为什么需要这个权限
     */
    fun onShowRationale()

    /**
     * 用户选择不再询问的时候回调。
     */
    fun onNeverAskAgain()
}

private var mRequestCode = 1
private val mOnPermissionCallbacks by lazy { SparseArray<OnPermissionCallback>() }// requestCode对应的callback

// must be a public static class to be  properly recreated from instance state.
class PermissionFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        mOnPermissionCallbacks.get(requestCode)?.let { callback ->
            var allGranted = true

            // 在Activity A申请权限，然后马上跳转到Activity B，则grantResults.length=0
            if (grantResults.isEmpty()) allGranted = false

            // 有一个不通过，都判断为不通过
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                }
            }

            if (allGranted) {
                callback.onAllGranted()
            } else {
                // 如果没有一个shouldShowRequestPermissionRationale，则判定为用户选择了不再询问
                if (!shouldShowRequestPermissionRationale(activity, permissions)) {
                    callback.onNeverAskAgain()
                } else {
                    callback.onAllDenied()
                }
            }

            mOnPermissionCallbacks.delete(requestCode)
        }
    }
}