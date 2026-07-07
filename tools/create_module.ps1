<#
.SYNOPSIS
    一键创建 feature-{name} 和 test-app-{name} 模块

.DESCRIPTION
    用法:
      .\tools\create_module.ps1 <name>              -> 同时创建 feature-{name} 和 test-app-{name}
      .\tools\create_module.ps1 feature <name>      -> 仅创建 feature-{name}
      .\tools\create_module.ps1 test <name>         -> 仅创建 test-app-{name}

    示例:
      .\tools\create_module.ps1 camera              -> feature-camera + test-app-camera
      .\tools\create_module.ps1 feature filemgr     -> feature-filemgr
      .\tools\create_module.ps1 test wifi           -> test-app-wifi

    说明:
      feature-{name} 模块包含 provider.kt (NavGraph 入口)、build.gradle.kts 等。
      test-app-{name} 模块仅需一个 build.gradle.kts (~18行)，
      App.kt 和 AndroidManifest.xml 由 test-app-plugin 在构建时自动生成。
#>

param(
    [Parameter(Position = 0)]
    [string]$Mode = "both",
    [Parameter(Position = 1)]
    [string]$Name = ""
)

if ($Mode -notin @("feature", "test", "both")) {
    $Name = $Mode
    $Mode = "both"
}

if (-not $Name) {
    Write-Error "Usage: .\tools\create_module.ps1 [feature|test] <name>"
    exit 1
}

$Name = $Name.ToLower() -replace '[^a-z0-9-]', '-'
$Name = $Name.Trim('-')

if (-not $Name) {
    Write-Error "module name cannot be empty"
    exit 1
}

function ToCamelCase($s) {
    -join (($s -split '-') | ForEach-Object {
        if ($_.Length -gt 0) { $_.Substring(0, 1).ToUpper() + $_.Substring(1) } else { "" }
    })
}

$Camel = ToCamelCase $Name

$RootDir = $PSScriptRoot | Split-Path -Parent

function WriteFile($Path, $Format, $FmtArgs) {
    $dir = Split-Path $Path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $content = $Format -f $FmtArgs
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($Path, $content, $utf8)
}

# ============================================================
#  Feature module templates
# ============================================================

function New-FeatureModule {
    $module = "feature-$Name"
    $ns = "com.munch1182.feature.$Name"
    $dir = Join-Path $RootDir $module
    $pkg = $ns.Replace('.', '/')
    Write-Host ">>> Creating $module" -ForegroundColor Cyan

    WriteFile "$dir\build.gradle.kts" @'
plugins {{
    id("com.munch1182.android.commonbuild_lib")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.google.hilt)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}}

android {{
    namespace = "{0}"
    buildFeatures {{
        compose = true
    }}
}}

ksp {{
    arg("compose-destinations.mode", "destinations")
    arg("compose-destinations.moduleName", "{1}")
}}

dependencies {{
    implementation(projects.libCommon)
    implementation(projects.libAndroid)
    implementation(projects.coreUi)
    implementation(projects.coreBase)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.compose.destinations)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.ksp)
    ksp(libs.kotlin.metadata.jvm)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.junit)
}}
'@ $ns, $Name

    WriteFile "$dir\consumer-rules.pro" "{0}" ""
    WriteFile "$dir\src\main\AndroidManifest.xml" '<?xml version="1.0" encoding="utf-8"?>{0}<manifest />{0}' "`n"

    WriteFile "$dir\src\main\java\$pkg\provider.kt" @'
package {0}

import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.parameters.CodeGenVisibility
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@NavGraph<ExternalModuleGraph>
annotation class Feature{1}Graph

@Destination<Feature{1}Graph>(start = true, visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun {1}Screen(navigator: DestinationsNavigator) {{
    // TODO: implement entry composable
}}

@Module
@InstallIn(ViewModelComponent::class)
object {1}Module {{
    // TODO: add DI bindings
}}
'@ $ns, $Camel

    WriteFile "$dir\src\test\java\$pkg\ExampleUnitTest.kt" @'
package {0}

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {{
    @Test
    fun addition_isCorrect() {{
        assertEquals(4, 2 + 2)
    }}
}}
'@ $ns

    AddToSettings $module
    AddToAppModule $Camel $Name
    Write-Host "  OK  $module created" -ForegroundColor Green
}

# ============================================================
#  Test-app module (minimal — App.kt + manifest 由插件生成)
# ============================================================

function New-TestAppModule {
    $module = "test-app-$Name"
    $ns = "com.munch1182.test.$Name"
    $dir = Join-Path $RootDir $module
    Write-Host ">>> Creating $module" -ForegroundColor Cyan

    WriteFile "$dir\build.gradle.kts" @'
plugins {{
    id("com.munch1182.android.testapp")
    id("com.munch1182.android.renameApk")
}}

android {{
    namespace = "{0}"
    buildFeatures {{ compose = true }}
}}

testApp {{
    feature = "{1}"
    label = "Test {2}"
}}

renameApk {{
    toDir = file("../apk")
}}
'@ $ns, $Name, $Camel

    AddToSettings $module
    Write-Host "  OK  $module created" -ForegroundColor Green
}

# ============================================================
#  Update settings.gradle.kts
# ============================================================

function AddToSettings($ModuleName) {
    $sf = Join-Path $RootDir "settings.gradle.kts"
    if (-not (Test-Path $sf)) { return }

    $raw = Get-Content $sf -Raw -Encoding UTF8
    $inc = 'include(":' + $ModuleName + '")'
    if ($raw.Contains($inc)) {
        Write-Host "  (already in settings.gradle.kts)" -ForegroundColor Gray
        return
    }

    $lines = Get-Content $sf -Encoding UTF8
    $last = -1
    for ($i = $lines.Count - 1; $i -ge 0; $i--) {
        if ($lines[$i] -match '^include\(') { $last = $i; break }
    }
    if ($last -ge 0) {
        $new = @()
        for ($i = 0; $i -le $last; $i++) { $new += $lines[$i] }
        $new += $inc
        for ($i = $last + 1; $i -lt $lines.Count; $i++) { $new += $lines[$i] }
        $utf8 = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllLines($sf, $new, $utf8)
        Write-Host "  OK  added $inc to settings.gradle.kts" -ForegroundColor Green
    }
}

# ============================================================
#  Update app module (build.gradle.kts + MainActivity.kt)
# ============================================================

function AddToAppModule($Camel, $Name) {
    # 1. app/build.gradle.kts -- 添加 implementation(projects.featureXxx)
    $buildFile = Join-Path $RootDir "app\build.gradle.kts"
    if (Test-Path $buildFile) {
        $depLine = "    implementation(projects.feature$Camel)"
        $raw = Get-Content $buildFile -Raw -Encoding UTF8
        if (-not $raw.Contains($depLine)) {
            $lines = Get-Content $buildFile -Encoding UTF8
            $lastFeatureIdx = -1
            for ($i = $lines.Count - 1; $i -ge 0; $i--) {
                if ($lines[$i] -match 'implementation\(projects\.feature') { $lastFeatureIdx = $i; break }
            }
            if ($lastFeatureIdx -ge 0) {
                $new = @()
                for ($i = 0; $i -le $lastFeatureIdx; $i++) { $new += $lines[$i] }
                $new += $depLine
                for ($i = $lastFeatureIdx + 1; $i -lt $lines.Count; $i++) { $new += $lines[$i] }
                $utf8 = New-Object System.Text.UTF8Encoding $false
                [System.IO.File]::WriteAllLines($buildFile, $new, $utf8)
                Write-Host "  OK  added feature$Camel to app/build.gradle.kts" -ForegroundColor Green
            }
        } else {
            Write-Host "  (already in app/build.gradle.kts)" -ForegroundColor Gray
        }
    }

    # 2. app/.../MainActivity.kt -- 添加 import + @ExternalNavGraph
    $mainActivityFile = Join-Path $RootDir "app\src\main\java\com\munch1182\p1\MainActivity.kt"
    if (Test-Path $mainActivityFile) {
        $importLine = "import com.ramcosta.composedestinations.generated.$Name.navgraphs.Feature${Camel}NavGraph"
        $navGraphLine = "    @ExternalNavGraph<Feature${Camel}NavGraph>"

        $raw = Get-Content $mainActivityFile -Raw -Encoding UTF8
        if (-not $raw.Contains($importLine)) {
            $lines = Get-Content $mainActivityFile -Encoding UTF8

            $lastImportIdx = -1
            for ($i = $lines.Count - 1; $i -ge 0; $i--) {
                if ($lines[$i] -match 'import com\.ramcosta\.composedestinations\.generated\.\w+\.navgraphs\.Feature\w+NavGraph') {
                    $lastImportIdx = $i; break
                }
            }

            $includesIdx = -1
            for ($i = 0; $i -lt $lines.Count; $i++) {
                if ($lines[$i] -match 'companion object Includes') { $includesIdx = $i; break }
            }

            if ($lastImportIdx -ge 0 -and $includesIdx -ge 0) {
                $new = @()
                for ($i = 0; $i -le $lastImportIdx; $i++) { $new += $lines[$i] }
                $new += $importLine
                for ($i = $lastImportIdx + 1; $i -lt $includesIdx; $i++) { $new += $lines[$i] }
                $new += $navGraphLine
                for ($i = $includesIdx; $i -lt $lines.Count; $i++) { $new += $lines[$i] }
                $utf8 = New-Object System.Text.UTF8Encoding $false
                [System.IO.File]::WriteAllLines($mainActivityFile, $new, $utf8)
                Write-Host "  OK  added Feature${Camel}NavGraph to MainActivity.kt" -ForegroundColor Green
            }
        } else {
            Write-Host "  (already in MainActivity.kt)" -ForegroundColor Gray
        }
    }
}

# ============================================================
#  Execute
# ============================================================

if ($Mode -eq "feature" -or $Mode -eq "both") {
    New-FeatureModule
}
if ($Mode -eq "test" -or $Mode -eq "both") {
    New-TestAppModule
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Cyan
