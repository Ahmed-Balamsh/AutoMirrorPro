package com.automirror.pro

import android.content.Intent
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.validation.HostValidator

class AutoMirrorCarService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        // السماح لجميع مضيفي Android Auto لعرض شاشة الجوال بدون حظر
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                return MirrorSurfaceScreen(carContext)
            }
        }
    }
}

class MirrorSurfaceScreen(carContext: CarContext) : Screen(carContext), SurfaceCallback {
    private var activeSurface: Surface? = null

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        activeSurface = surfaceContainer.surface
        // ربط سطح شاشة السيارة مباشرة بمستقبل الفيديو (H264) على المنفذ 8989
        MirrorProjectionBridge.attachCarSurface(
            surface = activeSurface,
            width = surfaceContainer.width,
            height = surfaceContainer.height,
            dpi = surfaceContainer.dpi
        )
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        MirrorProjectionBridge.detachCarSurface()
        activeSurface = null
    }

    override fun onClick(x: Float, y: Float) {
        // إرسال نقرات شاشة السيارة إلى خدمة AccessibilityService في الجوال
        MirrorProjectionBridge.dispatchTap(x, y)
    }

    override fun onGetTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("تدوير الشاشة")
                    .setOnClickListener { MirrorProjectionBridge.toggleOrientation() }
                    .build()
            )
            .addAction(Action.Builder.PAN)
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .build()
    }
}
