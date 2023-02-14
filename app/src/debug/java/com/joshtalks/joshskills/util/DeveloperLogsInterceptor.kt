package com.joshtalks.joshskills.util

import android.util.Log
import com.google.gson.JsonParser
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.JoshDevDatabase
import com.joshtalks.joshskills.repository.entity.ApiRequest
import com.joshtalks.joshskills.repository.service.DIR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

class DebugLogsInterceptor : Interceptor {
    val dao by lazy { JoshDevDatabase.getDatabase(AppObjectController.joshApplication)?.apiRequestDao() }
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        try {
            val requestBody = request.body?.toJsonString()
            val responseBody = response.peekBody(10485760L).string()
            val responseFormatted: String = try {
                val jsonElement = JsonParser.parseString(responseBody)
                AppObjectController.gsonMapper.toJson(jsonElement)
            } catch (e: Exception) {
                responseBody
            }

            val headers = request.headers.toString()
            CoroutineScope(Dispatchers.IO).launch {
                val api = ApiRequest(
                    status = response.code,
                    message = response.message,
                    method = request.method,
                    url = request.url.encodedPath.replace("/$DIR", ""),
                    request = requestBody,
                    response = responseFormatted,
                    time = response.receivedResponseAtMillis,
                    duration = response.sentRequestAtMillis.minus(response.receivedResponseAtMillis),
                    headers = headers,
                    curl = request.getCurlCommand()
                )
                dao?.insert(api)
                dao?.getLatest()?.let {
                    //Log.d("DeveloperLogsInter", "YASH => intercept:48 $it")
                    ApiRequestNotification.createNotification(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return response
    }
}

private fun Request.getCurlCommand(): String {
    var command = StringBuffer("curl -X ${this.method}")

    this.headers.names().forEach { element ->
        command.append("-H", "'$element: ${this.header(element)}' ")
    }

    val body = this.body
    if (body != null) {
        if (body.contentType() != null) {
            command.append("-H", "'Content-Type: ${body.contentType()}'")
        }
        if (body.contentLength() != (-1).toLong()) {
            command.append("-H", "'Content-Length: ${body.contentLength()}'")
        }
        command.append("-d" to "'${body.toJsonString()}'")
    }
    command = command.append("--compressed" to "'${this.url}'")

    return command.toString()
}


private fun RequestBody.toJsonString(): String {
    val buffer = Buffer()
    this.writeTo(buffer)
    return buffer.readUtf8()
}