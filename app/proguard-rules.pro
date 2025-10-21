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
-dontwarn com.munch1182.lib.AppHelper
-dontwarn com.munch1182.lib.base.ContextKt
-dontwarn com.munch1182.lib.base.IntentKt
-dontwarn com.munch1182.lib.base.JudgeKt
-dontwarn com.munch1182.lib.base.LogKt
-dontwarn com.munch1182.lib.base.Logger
-dontwarn com.munch1182.lib.base.PhonestateKt
-dontwarn com.munch1182.lib.base.ReRunJob
-dontwarn com.munch1182.lib.base.ScreenKt
-dontwarn com.munch1182.lib.base.ThreadKt
-dontwarn com.munch1182.lib.bluetooth.le.BLEConnector$ConnectState$Connected
-dontwarn com.munch1182.lib.bluetooth.le.BLEConnector$ConnectState$Disconnected
-dontwarn com.munch1182.lib.bluetooth.le.BLEConnector$ConnectState
-dontwarn com.munch1182.lib.bluetooth.le.BLEConnector$ServicesDiscoveryResult
-dontwarn com.munch1182.lib.bluetooth.le.BLEConnector
-dontwarn com.munch1182.lib.bluetooth.le.BleCommand
-dontwarn com.munch1182.lib.bluetooth.le.BleCommandSender$Config
-dontwarn com.munch1182.lib.bluetooth.le.BleCommandSender
-dontwarn com.munch1182.lib.bluetooth.le.BleConnectManager
-dontwarn com.munch1182.lib.bluetooth.le.BlueScanRecordHelper$BlueRecord
-dontwarn com.munch1182.lib.bluetooth.le.BlueScanRecordHelper
-dontwarn com.munch1182.lib.bluetooth.le.CommandResult
-dontwarn com.munch1182.lib.bluetooth.le.ScanKt
-dontwarn com.munch1182.lib.helper.ActivityCurrHelper
-dontwarn com.munch1182.lib.helper.ActivityKt
-dontwarn com.munch1182.lib.helper.AllowDeniedDialog
-dontwarn com.munch1182.lib.helper.DialogKt
-dontwarn com.munch1182.lib.helper.ResultDialog
-dontwarn com.munch1182.lib.helper.result.ExtendKt
-dontwarn com.munch1182.lib.helper.result.PermissionsIntentHelper
-dontwarn com.munch1182.lib.helper.result.ResultHelper$JudgeHelper
-dontwarn com.munch1182.lib.helper.result.ResultHelper$PermissionDialogTime$BeforeRequest
-dontwarn com.munch1182.lib.helper.result.ResultHelper$PermissionDialogTime$Denied
-dontwarn com.munch1182.lib.helper.result.ResultHelper$PermissionDialogTime$NeverAsk
-dontwarn com.munch1182.lib.helper.result.ResultHelper$PermissionDialogTime
-dontwarn com.munch1182.lib.helper.result.ResultHelper$PermissionsResultHelper

-dontwarn com.munch1182.lib.base.NumberKt
-dontwarn com.munch1182.lib.base.StrKt
-dontwarn com.munch1182.lib.base.ThreadHelper
-dontwarn com.munch1182.lib.base.ThreadProvider
-dontwarn com.munch1182.lib.bluetooth.BluetoothReceiver$OnBlueStateChange
-dontwarn com.munch1182.lib.bluetooth.BluetoothReceiver
-dontwarn com.munch1182.lib.helper.ARManager
-dontwarn com.munch1182.lib.helper.DataStore$Companion
-dontwarn com.munch1182.lib.helper.DataStore$Key
-dontwarn com.munch1182.lib.helper.DataStore
-dontwarn com.munch1182.lib.helper.FileHelper
-dontwarn com.munch1182.lib.helper.FileWriteHelper
-dontwarn com.munch1182.lib.helper.RecordHelper$Companion
-dontwarn com.munch1182.lib.helper.RecordHelper
-dontwarn com.munch1182.lib.helper.result.CantactKt
-dontwarn com.munch1182.lib.helper.result.ContactPermissionsIntentHelper
-dontwarn com.munch1182.lib.helper.result.ContactResultHelper$ContactJudgeHelper
-dontwarn com.munch1182.lib.helper.result.ContactResultHelper$ContactPermissionsResultHelper
-dontwarn com.munch1182.lib.helper.result.ContactResultHelper
-dontwarn com.munch1182.lib.scan.QrScanHelper$OnQrCodeListener
-dontwarn com.munch1182.lib.scan.QrScanHelper