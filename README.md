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
    api 'com.audient:super_library:1.4.9'
}
```

## 二、使用

* 在 `AndroidManifest.xml` 中注册权限，如：

```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

* 在代码中需要权限的地方申请即可，如：

```java
PermissionHelper.requestPermissions(activity, () -> {
    // on granted
    Toast.makeText(MainActivity.this, "onGranted", Toast.LENGTH_SHORT).show();

}, () -> {
    // on denied
    Toast.makeText(MainActivity.this, "onDenied", Toast.LENGTH_SHORT).show();

}, Manifest.permission.READ_CONTACTS);
```
