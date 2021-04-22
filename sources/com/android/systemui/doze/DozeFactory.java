package com.android.systemui.doze;

import android.app.AlarmManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Handler;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.dock.DockManager;
import com.android.systemui.doze.DozeMachine;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.AsyncSensorManager;
import com.android.systemui.util.wakelock.DelayedWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
import com.sonymobile.systemui.doze.SomcDozeScreenBrightness;

public class DozeFactory {
    public DozeMachine assembleMachine(DozeService dozeService) {
        SensorManager sensorManager = (SensorManager) Dependency.get(AsyncSensorManager.class);
        AlarmManager alarmManager = (AlarmManager) dozeService.getSystemService(AlarmManager.class);
        DockManager dockManager = (DockManager) Dependency.get(DockManager.class);
        DozeHost host = getHost(dozeService);
        AmbientDisplayConfiguration ambientDisplayConfiguration = new AmbientDisplayConfiguration(dozeService);
        DozeParameters instance = DozeParameters.getInstance(dozeService);
        Handler handler = new Handler();
        DelayedWakeLock delayedWakeLock = new DelayedWakeLock(handler, WakeLock.createPartial(dozeService, "Doze"));
        DozeMachine.Service wrapIfNeeded = DozeSuspendScreenStatePreventingAdapter.wrapIfNeeded(DozeScreenStatePreventingAdapter.wrapIfNeeded(new DozeBrightnessHostForwarder(dozeService, host), instance), instance);
        DozeMachine dozeMachine = new DozeMachine(wrapIfNeeded, ambientDisplayConfiguration, delayedWakeLock);
        dozeMachine.setParts(new DozeMachine.Part[]{new DozePauser(handler, dozeMachine, alarmManager, instance.getPolicy()), new DozeFalsingManagerAdapter(FalsingManagerFactory.getInstance(dozeService)), createDozeTriggers(dozeService, sensorManager, host, alarmManager, ambientDisplayConfiguration, instance, handler, delayedWakeLock, dozeMachine, dockManager), createDozeUi(dozeService, host, delayedWakeLock, dozeMachine, handler, alarmManager, instance), new DozeScreenState(wrapIfNeeded, handler, instance, delayedWakeLock), createDozeScreenBrightness(dozeService, wrapIfNeeded, sensorManager, host, instance, handler), new DozeWallpaperState(dozeService, getBiometricUnlockController(dozeService)), new DozeDockHandler(dozeService, dozeMachine, host, ambientDisplayConfiguration, handler, dockManager)});
        return dozeMachine;
    }

    private DozeMachine.Part createDozeScreenBrightness(Context context, DozeMachine.Service service, SensorManager sensorManager, DozeHost dozeHost, DozeParameters dozeParameters, Handler handler) {
        Sensor findSensorWithType = DozeSensors.findSensorWithType(sensorManager, context.getString(C0014R$string.doze_brightness_sensor_type));
        if (context.getResources().getBoolean(C0003R$bool.config_useSomcDozeScreenBrightness)) {
            return new SomcDozeScreenBrightness(context, service, sensorManager, findSensorWithType, dozeHost, handler, dozeParameters);
        }
        return new DozeScreenBrightness(context, service, sensorManager, findSensorWithType, dozeHost, handler, dozeParameters.getPolicy());
    }

    private DozeTriggers createDozeTriggers(Context context, SensorManager sensorManager, DozeHost dozeHost, AlarmManager alarmManager, AmbientDisplayConfiguration ambientDisplayConfiguration, DozeParameters dozeParameters, Handler handler, WakeLock wakeLock, DozeMachine dozeMachine, DockManager dockManager) {
        return new DozeTriggers(context, dozeMachine, dozeHost, alarmManager, ambientDisplayConfiguration, dozeParameters, sensorManager, handler, wakeLock, true, dockManager);
    }

    private DozeMachine.Part createDozeUi(Context context, DozeHost dozeHost, WakeLock wakeLock, DozeMachine dozeMachine, Handler handler, AlarmManager alarmManager, DozeParameters dozeParameters) {
        return new DozeUi(context, alarmManager, dozeMachine, wakeLock, dozeHost, handler, dozeParameters, KeyguardUpdateMonitor.getInstance(context));
    }

    public static DozeHost getHost(DozeService dozeService) {
        return (DozeHost) ((SystemUIApplication) dozeService.getApplication()).getComponent(DozeHost.class);
    }

    public static BiometricUnlockController getBiometricUnlockController(DozeService dozeService) {
        return (BiometricUnlockController) ((SystemUIApplication) dozeService.getApplication()).getComponent(BiometricUnlockController.class);
    }
}
