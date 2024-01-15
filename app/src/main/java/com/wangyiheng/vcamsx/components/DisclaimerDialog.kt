package com.wangyiheng.vcamsx.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.wangyiheng.vcamsx.MainActivity

@Composable
fun DisclaimerDialog() {
    var showDialog by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val shengm = "免责声明\n" +
            "关于本应用\n" +
            "   本应用（以下简称“应用”）旨在提供替换摄像头数据的功能。用户可以通过本应用改变和调整通过摄像头捕捉的数据和图像。\n" +
            "\n" +
            "使用条件\n" +
            "   数据处理: 用户理解并同意，通过本应用处理的所有数据和图像可能包括但不限于用户上传、修改、分享的内容。用户应对其提交给应用的数据和图像承担全部责任。\n" +
            "\n" +
            "合法用途: 用户同意仅将本应用用于合法目的，并承诺不会利用本应用进行任何非法或未经授权的活动。\n" +
            "\n" +
            "版权和知识产权: 用户保证拥有或合法授权使用通过本应用处理的所有数据和图像的所有相关权利，包括但不限于版权、商标权和专利权。\n" +
            "\n" +
            "隐私保护: 用户应尊重他人的隐私权，并承诺不会通过本应用收集、处理或分发他人的个人信息，除非已获得明确的授权。\n" +
            "\n" +
            "责任限制: 开发者不对用户使用本应用产生的任何直接或间接后果承担责任。用户应自行承担使用本应用可能导致的任何风险和后果。\n" +
            "\n" +
            "免责声明：开发者在此声明不对以下事项承担任何责任：\n" +
            "   1. 任何由用户不当使用本应用造成的损害或损失。\n" +
            "   2. 任何第三方对本应用的使用或依赖。\n" +
            "   3. 任何因使用或无法使用本应用而产生的间接、偶然、特殊、惩罚性或后果性损害。\n" +
            "   4. 任何未经授权访问或使用本应用所导致的数据丢失。\n" +
            "\n" +
            "本免责声明的任何修改将会在本应用或官方网站上更新，并且自公布之日起生效。用户继续使用本应用将视为接受修改后的免责声明。\n" +
            "\n" +
            "法律适用\n" +
            "本免责声明的解释和适用应遵循相关法律法规。任何因本应用引起的争议应提交至有管辖权的法院。"

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
            },
            title = {
                Text(text = "免责声明")
            },
            text = {
                // 如果免责声明文本较短，可以考虑使用 Column + Scrollable
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        shengm.split("\n").forEach { line ->
                            Text(text = line)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text("我同意")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        closeApp(context)
                    }
                ) {
                    Text("我不同意")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        )
    }
}

private fun closeApp(context: Context) {
    // 停止相关服务
    // 例如：douyin.stop() 和 cloudPlatform.disableAutoRotate()
    // 关闭所有活动
    if (context is MainActivity) {
        context.finishAffinity()
    }
}
