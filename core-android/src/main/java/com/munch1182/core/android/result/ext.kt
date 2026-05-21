package com.munch1182.core.android.result

import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity

/**
 * 权限请求简化
 */
fun FragmentActivity.requestPermissionWithHelper(permission: Array<String>) =
    PermissionHelper(this, permission)

/**
 * 权限请求简化
 */
suspend fun FragmentActivity.requestPermissions(permission: Array<String>) =
    request(ActivityResultContracts.RequestMultiplePermissions(), permission)

/**
 * 权限请求简化
 */
suspend fun FragmentActivity.requestPermission(permission: String) =
    request(ActivityResultContracts.RequestPermission(), permission)

/**
 * 使用intent请求结果简化
 */
suspend fun FragmentActivity.requestResult(intent: Intent) =
    request(ActivityResultContracts.StartActivityForResult(), intent)

/**
 * 拍照简化, 需要提供[Uri]
 */
suspend fun FragmentActivity.requestTakePicture(uri: Uri) =
    request(ActivityResultContracts.TakePicture(), uri)

/**
 * 从相册中选择多张相片
 *
 * 此api不需要权限, 用户手动选择即授权
 */
suspend fun FragmentActivity.requestPickMultipleImages() =
    request(ActivityResultContracts.GetMultipleContents(), "image/*")

/**
 * 选择单个文件简化
 *
 * 此api不需要权限, 用户手动选择即授权
 */
suspend fun FragmentActivity.requestGetContent(mimeType: String = "*/*") =
    request(ActivityResultContracts.GetContent(), mimeType)

/**
 * 打开文档简化
 */
suspend fun FragmentActivity.requestOpenDocument(mimeType: String = "*/*") =
    request(ActivityResultContracts.OpenDocument(), arrayOf(mimeType))

/**
 * 录制视频简化
 *
 * 需要提供视频文件保存的uri
 */
suspend fun FragmentActivity.requestCaptureVideo(uri: Uri) =
    request(ActivityResultContracts.CaptureVideo(), uri)

/**
 * 选择联系人
 */
suspend fun FragmentActivity.requestPickContact() =
    request(ActivityResultContracts.PickContact(), null)