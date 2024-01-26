package com.wangyiheng.vcamsx.data.services
//
//import android.content.Context
//import android.content.pm.PackageManager
//import android.os.Build
//import androidx.annotation.RequiresApi
//import cn.dianbobo.dbb.shared.utils.InfoManager
//import okhttp3.Interceptor
//import okhttp3.Request
//import okhttp3.Response
//import okio.Buffer
//import java.nio.charset.StandardCharsets
//import java.util.*
//import javax.crypto.Mac
//import javax.crypto.spec.SecretKeySpec
//
//class ApiInterceptor(private val context: Context,private val infoManager: InfoManager) : Interceptor {
//    private val tokenExcludedUrls = mapOf(
//        "/v1/user/sms" to true,
//        "/v1/user/login" to true
//    )
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun intercept(chain: Interceptor.Chain): Response {
//        val originalRequest = chain.request()
//
//        val token = infoManager.getToken()
//
//        if (token != null && !tokenExcludedUrls.containsKey(originalRequest.url().toString())) {
//            val newRequest = getRequest(chain, originalRequest, token)
//            return refreshToken(chain.proceed(newRequest), chain,originalRequest)
//        }
//        return chain.proceed(getRequest(chain, originalRequest))
//    }
//
//    fun getAppVersion(): String {
//        return try {
//            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
//            packageInfo.versionName
//        } catch (e: PackageManager.NameNotFoundException) {
//            e.printStackTrace()
//            "3.0.7"
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    @Synchronized
//    private fun refreshToken(response: Response, chain: Interceptor.Chain, originalRequest: Request): Response {
//        val newToken = response.header("Authorization")
//        if (newToken != null) {
//            infoManager.removeToken()
//            infoManager.saveToken(newToken)
//            response.close()
//            val newRequest = getRequest(chain, originalRequest, newToken)
//            return chain.proceed(newRequest)
//        }
//        return response
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getRequest(chain: Interceptor.Chain, originalRequest: Request, token: String? = null): Request {
//        val signatureMap = getSignatureMap(originalRequest)
//        val requestBuilder = chain.request().newBuilder()
//
//        // 只有在token非空和非null时才添加
//        if (!token.isNullOrEmpty()) {
//            requestBuilder.header("Authorization", token)
//        }
//        val appVersion = getAppVersion()
//
//        requestBuilder
//            .header("x-ca-key", signatureMap["x-ca-key"]!!)
//            .header("x-ca-timestamp", signatureMap["x-ca-timestamp"]!!)
//            .header("x-ca-nonce", signatureMap["x-ca-nonce"]!!)
//            .header("app-version", appVersion)
//            .header("x-ca-signature", signatureMap["x-ca-signature"]!!)
//        return requestBuilder.build()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getSignatureMap(originalRequest: Request): Map<String, String> {
//        val secretMap = mapOf(
//            "vC*%oZx^cDjS&3jv" to "dusKXbexHvv!FhE@98xgnY\$oV5)nYEgN",
//            "^fg4*Ga)v@)vfKB(" to "kHSpH4G$#4b@)*x8Waf@AVNUw\$2mXM@U",
//            "7FxKRnW@(2M48)Wz" to "5FFn%r3ZRLXYBgx6B8LQR\$X*x6JiD^oa",
//            "ot*j3wmN%5N%arA5" to "Y9^q&!4vzERaZ!@FHC%SFGc(Yb3DJ\$np",
//            "LxjNyrz^@gpiM5hV" to "f!jQ2\$Aw6#GyYue*\$z4*Mzt*Xhwdv(8z"
//        )
//        val nonce = UUID.randomUUID().toString()
//        val timestamp = (System.currentTimeMillis() / 1000).toString()
//
//        val appVersion = getAppVersion()
//
//        val caKey = secretMap.keys.random()
//        val secret = secretMap[caKey]
//
//
//        val requestMethod = originalRequest.method()
//        val path = originalRequest.url().encodedPath()
//        var str = "$requestMethod $path?x-ca-key=$caKey&x-ca-nonce=$nonce&x-ca-timestamp=$timestamp&app-version=$appVersion"
//
////        val contentType = originalRequest.body()?.contentType()?.toString()
//        val contentType = originalRequest.body()?.contentType()
//        val requestType = "${contentType?.type()}/${contentType?.subtype()}"
//        when (requestType) {
//            "application/json" -> {
//                val requestBody = originalRequest.body()
//                val buffer = Buffer()
//                requestBody?.writeTo(buffer)
//                val jsonBody = buffer.readUtf8()
//
//                if (jsonBody != null) {
//                    val encodedBody = Base64.getEncoder().encodeToString(jsonBody.toByteArray(StandardCharsets.UTF_8))
//                    str += "&body=$encodedBody"
//                }
//            }
//            "application/x-www-form-urlencoded" -> {
//                val body = originalRequest.body()?.toString()
//                if (body != null) {
//                    // 解析请求体以获取键值对
//                    val pairs = body.split("&").map {
//                        val parts = it.split("=")
//                        Pair(parts[0], parts[1])
//                    }
//                    // 按照键进行升序排序
//                    val sortedPairs = pairs.sortedBy { it.first }
//                    // 拼接
//                    sortedPairs.forEach { pair ->
//                        str += "&${pair.first}=${pair.second}"
//                    }
//                }
//            }
////            "multipart/form-data" -> {
////                // 文件不加入签名加密，只对其他参数排序
////            }
//        }
//        // 获取URL中的查询字符串参数
//        val url = originalRequest.url()
//        val queryParameterNames = url.queryParameterNames()
//        val queryParams = queryParameterNames.flatMap { name ->
//            url.queryParameterValues(name).map { value -> name to value }
//        }
//
//        // 将查询参数按照键进行字典序升序排序
//        val sortedQueryParams = queryParams.sortedBy { it.first }
//
//        // 拼接查询字符串参数
//        sortedQueryParams.forEach { (key, value) ->
//            str += "&$key=$value"
//        }
//
//        val signature = generateSignature(str, secret!!)
//
//        return  mapOf(
//            "x-ca-key" to caKey,
//            "x-ca-nonce" to nonce,
//            "x-ca-timestamp" to timestamp,
//            "x-ca-signature" to signature
//        )
//    }
//
//    private fun generateSignature(data: String, key: String): String {
//        val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
//        val mac = Mac.getInstance("HmacSHA256")
//        mac.init(secretKey)
//        val hmacData = mac.doFinal(data.toByteArray())
//        return hmacData.joinToString("") { "%02x".format(it) }
//    }
//}