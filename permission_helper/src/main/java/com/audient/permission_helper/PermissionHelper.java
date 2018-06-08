package com.audient.permission_helper;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class PermissionHelper extends Fragment {
    private static final String TAG = "PermissionHelper";

    private static List<String> mRegisteredInManifestPermissions;

    // key=request_code
    private static Map<Integer, OnPermissionGrantedListener> mOnGrantedListeners;
    private static Map<Integer, OnPermissionDeniedListener> mOnDeniedListeners;
    private static int mRequestCode = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /**
     * @param permissions e.g. {@link Manifest.permission#CAMERA}
     * @return true if all granted, false otherwise.
     */
    public static boolean isGranted(Activity activity, String... permissions) {
        checkPermissions(activity, permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param grantedListener run on all permission granted
     * @param deniedListener  run if one permission denied
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static void requestPermissions(Activity activity, OnPermissionGrantedListener grantedListener, OnPermissionDeniedListener deniedListener, String... permissions) {
        checkPermissions(activity, permissions);

        if (isGranted(activity, permissions)) {
            if (deniedListener != null) deniedListener.onDenied();
            return;
        }

        Fragment fragment = activity.getFragmentManager().findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new PermissionHelper();
            FragmentManager fragmentManager = activity.getFragmentManager();
            fragmentManager.beginTransaction().add(fragment, TAG).commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }

        mOnGrantedListeners.put(mRequestCode, grantedListener);
        mOnDeniedListeners.put(mRequestCode, deniedListener);
        fragment.requestPermissions(permissions, mRequestCode++);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (mOnGrantedListeners.containsKey(requestCode)) {
            // 这里规定全部权限都通过才算通过
            boolean granted = true;
            // 在Activity A申请权限，然后马上跳转到Activity B，则grantResults.length=0
            if (grantResults.length == 0) granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    granted = false;
                }
            }
            if (granted) {
                OnPermissionGrantedListener listener = mOnGrantedListeners.get(requestCode);
                if (listener != null) listener.onGranted();
            } else {
                OnPermissionDeniedListener listener = mOnDeniedListeners.get(requestCode);
                if (listener != null) listener.onDenied();
            }
            mOnGrantedListeners.remove(mRequestCode);
            mOnDeniedListeners.remove(mRequestCode);
        }
    }

    /**
     * call before {@link #isGranted(Activity, String...)} and {@link #requestPermissions(Activity, OnPermissionGrantedListener, OnPermissionDeniedListener, String...)}
     */
    private static void checkPermissions(Activity activity, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("requires at least one input permission");
        }

        if (mRegisteredInManifestPermissions == null) {
            mRegisteredInManifestPermissions = getRegisteredInManifestPermissions(activity);
        }

        if (mOnGrantedListeners == null) mOnGrantedListeners = new Hashtable<>();
        if (mOnDeniedListeners == null) mOnDeniedListeners = new Hashtable<>();

        for (String permission : permissions) {
            if (!mRegisteredInManifestPermissions.contains(permission)) {
                throw new IllegalArgumentException("the permission \"" + permission + "\" is not registered in AndroidManifest.xml");
            }
        }
    }

    private static List<String> getRegisteredInManifestPermissions(Activity activity) {
        List<String> list = new ArrayList<>();
        try {
            PackageInfo packageInfo = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] permissions = packageInfo.requestedPermissions;
            if (permissions != null) {
                list.addAll(Arrays.asList(permissions));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }
}
