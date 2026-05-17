package com.munch1182.core.android.result

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.munch1182.core.android.withPkgUri

fun FragmentActivity.requestPermissionWithHelper(permission: Array<String>) =
    PermissionHelper(this, permission)

// ----------------------------------------------------------------------
// 1. 权限请求
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestPermissions(permission: Array<String>) =
    request(ActivityResultContracts.RequestMultiplePermissions(), permission)

// ----------------------------------------------------------------------
// 2. 单个权限请求
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestPermission(permission: String) =
    request(ActivityResultContracts.RequestPermission(), permission)

// ----------------------------------------------------------------------
// 3.  跳转获取结果（Intent）
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestResult(intent: Intent) =
    request(ActivityResultContracts.StartActivityForResult(), intent)

// ----------------------------------------------------------------------
// 4.  拍照（需要提供目标 Uri）
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestTakePicture(uri: Uri) =
    request(ActivityResultContracts.TakePicture(), uri)

// ----------------------------------------------------------------------
// 5. 从相册选择多张图片（返回 List<Uri>）
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestPickMultipleImages() =
    request(ActivityResultContracts.GetMultipleContents(), "image/*")

// ----------------------------------------------------------------------
// 6. 选择任意类型文件（返回 Uri?）
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestGetContent(mimeType: String = "*/*") =
    request(ActivityResultContracts.GetContent(), mimeType)

// ----------------------------------------------------------------------
// 7. 打开文档（返回 Uri?）
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestOpenDocument(mimeType: String = "*/*") =
    request(ActivityResultContracts.OpenDocument(), arrayOf(mimeType))

// ----------------------------------------------------------------------
// 8. 录制视频（需要提供目标 Uri）
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestCaptureVideo(uri: Uri) =
    request(ActivityResultContracts.CaptureVideo(), uri)

// ----------------------------------------------------------------------
// 9. 选择联系人（返回 Uri?）
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestPickContact() =
    request(ActivityResultContracts.PickContact(), null)

// ----------------------------------------------------------------------
// 10. 申请安装权限（Android 8+）: 需要注册权限
// ----------------------------------------------------------------------
suspend fun FragmentActivity.requestRequestInstallPackages() =
    requestResult(
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).withPkgUri()
    )
