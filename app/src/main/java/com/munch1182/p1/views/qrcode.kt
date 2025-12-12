package com.munch1182.p1.views

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.munch1182.android.lib.base.launchIO
import com.munch1182.android.lib.base.shareImage
import com.munch1182.android.lib.helper.FileHelper
import com.munch1182.android.lib.helper.createIfNotExist
import com.munch1182.android.lib.helper.currAsFM
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.corner
import com.munch1182.p1.ui.weight.Loading
import java.io.FileOutputStream

@Composable
fun QrCodeView() {
    var str by remember { mutableStateOf("") }
    val isEnable by remember { derivedStateOf { !str.isEmpty() } }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    val file = FileHelper.newFile("qrcode", "qrcode.png")
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            launchIO {
                file.createIfNotExist()
                bitmap = generate(str, 200)

                FileOutputStream(file).use { bitmap?.compress(Bitmap.CompressFormat.PNG, 100, it) }
                isGenerating = false
            }
        }
    }
    Items(Modifier.fillMaxWidth()) {
        TextField(str, onValueChange = { str = it })
        ClickButton("GENERATE", enable = isEnable) { isGenerating = true }
        SpacerV()
        if (isGenerating) {
            Loading()
        } else {
            bitmap?.let {
                Image(
                    it.asImageBitmap(), null, Modifier
                        .size(200.dp)
                        .corner()
                        .clickable(true) {
                            FileHelper.uri(file)?.let { uri ->
                                currAsFM.startActivity(shareImage(uri, "qrCode"))
                            }
                        })
            }
        }
    }
}

private fun generate(str: String, wInt: Int, hInt: Int = wInt): Bitmap {
    val writer = QRCodeWriter()
    val matrix = writer.encode(str, BarcodeFormat.QR_CODE, wInt, hInt)
    val bitMap = createBitmap(wInt, hInt, Bitmap.Config.RGB_565)
    val dark = Color.BLACK
    val light = Color.WHITE

    for (x in 0 until wInt) {
        for (y in 0 until hInt) {
            bitMap[x, y] = if (matrix[x, y]) dark else light
        }
    }
    return bitMap
}