# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn com.munch1182.android.lib.AppHelper
-dontwarn com.munch1182.android.base.ContextKt
-dontwarn com.munch1182.android.base.IntentKt
-dontwarn com.munch1182.android.base.JudgeKt
-dontwarn com.munch1182.android.base.LogKt
-dontwarn com.munch1182.android.lib.base.Logger
-dontwarn com.munch1182.android.base.PhonestateKt
-dontwarn com.munch1182.android.lib.base.ReRunJob
-dontwarn com.munch1182.android.base.ScreenKt
-dontwarn com.munch1182.android.base.ThreadKt
-dontwarn com.munch1182.android.bluetooth.le.BLEConnector$ConnectState$Connected
-dontwarn com.munch1182.android.bluetooth.le.BLEConnector$ConnectState$Disconnected
-dontwarn com.munch1182.android.bluetooth.le.BLEConnector$ConnectState
-dontwarn com.munch1182.android.bluetooth.le.BLEConnector$ServicesDiscoveryResult
-dontwarn com.munch1182.android.bluetooth.le.BLEConnector
-dontwarn com.munch1182.android.bluetooth.le.BleCommand
-dontwarn com.munch1182.android.bluetooth.le.BleCommandSender$Config
-dontwarn com.munch1182.android.bluetooth.le.BleCommandSender
-dontwarn com.munch1182.android.bluetooth.le.BleConnectManager
-dontwarn com.munch1182.android.bluetooth.le.BlueScanRecordHelper$BlueRecord
-dontwarn com.munch1182.android.bluetooth.le.BlueScanRecordHelper
-dontwarn com.munch1182.android.bluetooth.le.CommandResult
-dontwarn com.munch1182.android.bluetooth.le.ScanKt
-dontwarn com.munch1182.android.lib.helper.ActivityCurrHelper
-dontwarn com.munch1182.android.helper.ActivityKt
-dontwarn com.munch1182.android.lib.helper.AllowDeniedDialog
-dontwarn com.munch1182.android.helper.DialogKt
-dontwarn com.munch1182.android.lib.helper.ResultDialog
-dontwarn com.munch1182.android.helper.result.ExtendKt
-dontwarn com.munch1182.android.helper.result.PermissionsIntentHelper
-dontwarn com.munch1182.android.helper.result.ResultHelper$JudgeHelper
-dontwarn com.munch1182.android.helper.result.ResultHelper$PermissionDialogTime$BeforeRequest
-dontwarn com.munch1182.android.helper.result.ResultHelper$PermissionDialogTime$Denied
-dontwarn com.munch1182.android.helper.result.ResultHelper$PermissionDialogTime$NeverAsk
-dontwarn com.munch1182.android.helper.result.ResultHelper$PermissionDialogTime
-dontwarn com.munch1182.android.helper.result.ResultHelper$PermissionsResultHelper

-dontwarn com.munch1182.android.base.NumberKt
-dontwarn com.munch1182.android.base.StrKt
-dontwarn com.munch1182.android.lib.base.ThreadHelper
-dontwarn com.munch1182.android.lib.base.ThreadProvider
-dontwarn com.munch1182.android.bluetooth.BluetoothReceiver$OnBlueStateChange
-dontwarn com.munch1182.android.bluetooth.BluetoothReceiver
-dontwarn com.munch1182.android.lib.helper.ARManager
-dontwarn com.munch1182.android.lib.helper.DataStore$Companion
-dontwarn com.munch1182.android.lib.helper.DataStore$Key
-dontwarn com.munch1182.android.lib.helper.DataStore
-dontwarn com.munch1182.android.lib.helper.FileHelper
-dontwarn com.munch1182.android.lib.helper.FileWriteHelper
-dontwarn com.munch1182.android.lib.helper.RecordHelper$Companion
-dontwarn com.munch1182.android.lib.helper.RecordHelper
-dontwarn com.munch1182.android.helper.result.ChainKt
-dontwarn com.munch1182.android.helper.result.ContactPermissionsIntentHelper
-dontwarn com.munch1182.android.lib.helper.result.ContactResultHelper$ContactJudgeHelper
-dontwarn com.munch1182.android.lib.helper.result.ContactResultHelper$ContactPermissionsResultHelper
-dontwarn com.munch1182.android.lib.helper.result.ContactResultHelper
-dontwarn com.munch1182.android.scan.QrScanHelper$OnQrCodeListener
-dontwarn com.munch1182.android.scan.QrScanHelper

-dontwarn com.munch1182.android.base.DateKt
-dontwarn com.munch1182.android.lib.base.OnUpdateListener
-dontwarn com.munch1182.android.lib.helper.AudioFocus$GainTransient
-dontwarn com.munch1182.android.lib.helper.AudioFocus
-dontwarn com.munch1182.android.lib.helper.AudioFocusHelper
-dontwarn com.munch1182.android.lib.helper.AudioHelper
-dontwarn com.munch1182.android.helper.AudioKt
-dontwarn com.munch1182.android.lib.helper.AudioStreamHelper
-dontwarn com.munch1182.android.helper.FileKt
-dontwarn com.munch1182.android.lib.helper.NetStateHelper
-dontwarn com.munch1182.android.lib.helper.NoticeHelper
-dontwarn com.munch1182.android.helper.result.ChainPermissionJumpIntentHelper
-dontwarn com.munch1182.android.helper.result.PermissionJumpIntentIfNeverAskHelper
-dontwarn com.munch1182.android.helper.result.ResultChainHelper$ChainContractResultHelper
-dontwarn com.munch1182.android.helper.result.ResultChainHelper$ChainJudgeResultHelper
-dontwarn com.munch1182.android.helper.result.ResultChainHelper$ChainPermissionsResultHelper
-dontwarn com.munch1182.android.helper.result.ResultChainHelper
-dontwarn com.munch1182.android.helper.result.ResultHelper$JudgeResultHelper