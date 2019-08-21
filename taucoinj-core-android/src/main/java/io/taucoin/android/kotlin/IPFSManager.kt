package io.taucoin.android.kotlin

import android.app.Service
import android.content.Intent
import android.os.Build.CPU_ABI
import android.util.Log

class IPFSManager(private var service: Service) {
    private val tag = "ipfs"

    companion object{
        var daemon: Process? = null
        var logs: MutableList<String> = mutableListOf()
    }

    fun init() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//            NotificationChannel("sweetipfs", "Sweet IPFS", IMPORTANCE_MIN).apply {
//                description = "Sweet IPFS"
//                service.getSystemService(NotificationManager::class.java)
//                    .createNotificationChannel(this)
//        }

        install()
        start()
//        service.startForeground(1, notification.build())
    }

    private fun install() {

        val type = CPU_ABI.let {
            when{
                it.startsWith("arm") -> "arm"
                it.startsWith("x86") -> "386"
                else ->  throw Exception("Unsupported ABI")
            }
        }

        service.bin.apply {
            delete()
            createNewFile()
        }

        val input = service.assets.open(type)
        val output = service.bin.outputStream()

        try {
            input.copyTo(output)
        } finally {
            input.close(); output.close()
        }

        service.bin.setExecutable(true)
        Log.d(tag, "Installed binary")
    }

    fun start() {
        logs.clear()

        service.exec("init").apply {
            read{
                Log.d(tag, "init=$it")
                logs.add(it)
            }
            waitFor()
        }

        service.config {
            obj("API").obj("HTTPHeaders").apply {
                array("Access-Control-Allow-Origin").also { origins ->
                    val webui = json("https://sweetipfswebui.netlify.com")
                    val local = json("http://127.0.0.1:5001")
                    if (webui !in origins) origins.add(webui)
                    if (local !in origins) origins.add(local)
                }

                array("Access-Control-Allow-Methods").also { methods ->
                    val put = json("PUT")
                    val get = json("GET")
                    val post = json("POST")
                    if(put !in methods) methods.add(put)
                    if(get !in methods) methods.add(get)
                    if(post !in methods) methods.add(post)
                }
            }

        }

        service.exec("daemon --enable-pubsub-experiment").apply {
            daemon = this
            read{
                Log.d(tag, "daemon=$it")
                logs.add(it)
            }
        }
    }

    fun stop() {
        daemon?.destroy()
        daemon = null
    }

//    private val notificationBuilder = NotificationCompat.Builder(service, "sweetipfs")

//    private val notification
//        @SuppressLint("RestrictedApi")
//        get() = notificationBuilder.apply {
//            mActions.clear()
//            setOngoing(true)
//            setOnlyAlertOnce(true)
//            color = parseColor("#69c4cd")
//            setSmallIcon(R.mipmap.ic_launcher)
//            setShowWhen(false)
//            setContentTitle("Sweet IPFS")
//
////            val open = pendingActivity<WebActivity>()
////            setContentIntent(open)
////            addAction(R.mipmap.ic_launcher, "Open", open)
//
//            if(daemon == null){
//                setContentText("IPFS is not running")
//
//                val start = service.pendingService(service.intent<TaucoinRemoteService>().action("start"))
//                addAction(R.mipmap.ic_launcher, "start", start)
//            }
//
//            else {
//                setContentText("IPFS is running")
//
//                val restart = service.pendingService(service.intent<TaucoinRemoteService>().action("restart"))
//                addAction(R.mipmap.ic_launcher, "restart", restart)
//
//                val stop = service.pendingService(service.intent<TaucoinRemoteService>().action("stop"))
//                addAction(R.mipmap.ic_launcher, "stop", stop)
//            }
//
//            val exit = service.pendingService(service.intent<TaucoinRemoteService>().action("exit"))
//            addAction(R.mipmap.ic_launcher, "exit", exit)
//        }

    fun onStartCommand(i: Intent?) = Unit.also {
        when (i?.action) {
            "start" -> start()
            "stop" -> stop()
            "restart" -> {
                stop(); start()
            }
            "exit" -> System.exit(0)
        }
//        val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            manager.notify(1, notification.build())
    }
}