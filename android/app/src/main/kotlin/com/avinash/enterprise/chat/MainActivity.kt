package com.avinash.enterprise.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.NonNull
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity : FlutterFragmentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CHANNEL_BIOMETRIC = "com.avinash.enterprise.chat/biometric"
        private const val CHANNEL_DEEP_LINK = "com.avinash.enterprise.chat/deeplink"
        private const val CHANNEL_NOTIFICATIONS = "com.avinash.enterprise.chat/notifications"
    }

    private var pendingDeepLink: String? = null
    private var biometricChannel: MethodChannel? = null
    private var deepLinkChannel: MethodChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
    }

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        setupBiometricChannel(flutterEngine)
        setupDeepLinkChannel(flutterEngine)
        setupNotificationChannel(flutterEngine)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    // ==================== BIOMETRIC CHANNEL ====================
    private fun setupBiometricChannel(flutterEngine: FlutterEngine) {
        biometricChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL_BIOMETRIC
        ).apply {
            setMethodCallHandler { call, result ->
                when (call.method) {
                    "checkBiometricAvailability" -> checkBiometricAvailability(result)
                    "authenticateWithBiometric" -> {
                        val title = call.argument<String>("title") ?: "Authenticate"
                        val subtitle = call.argument<String>("subtitle") ?: ""
                        val description = call.argument<String>("description") ?: ""
                        authenticateWithBiometric(title, subtitle, description, result)
                    }
                    else -> result.notImplemented()
                }
            }
        }
    }

    private fun checkBiometricAvailability(result: MethodChannel.Result) {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        val response = mapOf(
            "available" to (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS),
            "status" to when (canAuthenticate) {
                BiometricManager.BIOMETRIC_SUCCESS -> "success"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "no_hardware"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "hw_unavailable"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "none_enrolled"
                else -> "unknown"
            }
        )
        result.success(response)
    }

    private fun authenticateWithBiometric(
        title: String,
        subtitle: String,
        description: String,
        result: MethodChannel.Result
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(authResult: BiometricPrompt.AuthenticationResult) {
                    result.success(mapOf("success" to true, "message" to "Authentication successful"))
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    result.success(mapOf(
                        "success" to false,
                        "errorCode" to errorCode,
                        "message" to errString.toString()
                    ))
                }

                override fun onAuthenticationFailed() {
                    // Don't call result here — more attempts may follow
                    Log.w(TAG, "Biometric authentication attempt failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // ==================== DEEP LINK CHANNEL ====================
    private fun setupDeepLinkChannel(flutterEngine: FlutterEngine) {
        deepLinkChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL_DEEP_LINK
        ).apply {
            setMethodCallHandler { call, result ->
                when (call.method) {
                    "getInitialLink" -> result.success(pendingDeepLink)
                    else -> result.notImplemented()
                }
            }
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val deepLink = uri.toString()
        Log.d(TAG, "Deep link received: $deepLink")
        
        if (deepLinkChannel != null) {
            deepLinkChannel?.invokeMethod("onDeepLink", deepLink)
        } else {
            pendingDeepLink = deepLink
        }
    }

    // ==================== NOTIFICATION CHANNEL ====================
    private fun setupNotificationChannel(flutterEngine: FlutterEngine) {
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL_NOTIFICATIONS
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "createNotificationChannels" -> {
                    createNotificationChannels()
                    result.success(null)
                }
                "clearBadge" -> {
                    // Clear app badge count
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            
            // Messages channel
            android.app.NotificationChannel(
                "messages",
                "Messages",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New chat messages"
                enableVibration(true)
                setShowBadge(true)
                notificationManager.createNotificationChannel(this)
            }

            // Group calls channel
            android.app.NotificationChannel(
                "calls",
                "Calls",
                android.app.NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Incoming calls"
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }

            // Background sync channel (silent)
            android.app.NotificationChannel(
                "sync",
                "Background Sync",
                android.app.NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background message sync"
                setShowBadge(false)
                notificationManager.createNotificationChannel(this)
            }
        }
    }
}
