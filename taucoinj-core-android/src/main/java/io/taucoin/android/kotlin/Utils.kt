package io.taucoin.android.kotlin

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.gson.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileReader
import java.io.InterruptedIOException
import java.lang.Runtime.getRuntime

val Context.store get() = getExternalFilesDir(null)!!["ipfs"]
val Context.bin get() = filesDir["goipfs"]
val Context.config get() = JsonParser().parse(FileReader(store["config"])).asJsonObject

fun Context.config(consumer: JsonObject.() -> Unit) {
    val config = config.apply(consumer)
    val data = GsonBuilder().setPrettyPrinting().create().toJson(config)
    store["config"].writeBytes(data.toByteArray())
}

fun Context.exec(cmd: String) = getRuntime().exec(
    "${bin.absolutePath} $cmd",
    arrayOf("IPFS_PATH=${store.absolutePath}")
)

operator fun File.get(path: String) = File(this, path)

inline fun <reified T> Context.intent(builder: Intent.() -> Unit = {}) = Intent(this, T::class.java).apply(builder)
fun Intent.action(value: String) = apply { action = value }

inline fun <reified T : Activity> Context.startActivity() = startActivity(intent<T>())
inline fun <reified T : Service> Context.startService() = startService(intent<T>())

inline fun <reified T : Service> Context.pendingService() = pendingService(intent<T>())
fun Context.pendingService(intent: Intent) = PendingIntent.getService(this, 0, intent, 0)

inline fun <reified T : Activity> Context.pendingActivity() = pendingActivity(intent<T>())
fun Context.pendingActivity(intent: Intent) = PendingIntent.getActivity(this, 0, intent, 0)

fun <T> Context.catcher(action: () -> T) =
    try {
        action()
    } catch (ex: Exception) {
        Toast.makeText(this, ex.localizedMessage, Toast.LENGTH_LONG).show()
    }

fun json(value: Boolean) = JsonPrimitive(value)
fun json(value: Int) = JsonPrimitive(value)
fun json(value: String) = JsonPrimitive(value)
fun json(value: List<String>) = JsonArray().apply { value.forEach(::add) }

fun JsonObject.set(key: String, value: JsonElement) = add(key, value)
fun JsonObject.boolean(key: String) = getAsJsonPrimitive(key)?.asBoolean
fun JsonObject.string(key: String) = getAsJsonPrimitive(key)?.asString
fun JsonObject.int(key: String) = getAsJsonPrimitive(key)?.asInt

fun JsonObject.array(key: String): JsonArray {
    if (key !in keySet()) set(key, JsonArray())
    return getAsJsonArray(key)
}

fun JsonObject.obj(key: String): JsonObject {
    if (key !in keySet()) set(key, JsonObject())
    return getAsJsonObject(key)
}

fun Process.read(consumer: (String) -> Unit) {
    listOf(inputStream, errorStream).forEach {
        stream -> GlobalScope.launch {
            try{
                stream.bufferedReader().forEachLine { consumer(it) }
            } catch(ex: InterruptedIOException){}
        }
    }
}