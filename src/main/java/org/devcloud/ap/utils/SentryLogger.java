package org.devcloud.ap.utils;

import io.sentry.Sentry;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class SentryLogger {
    private static Config config = new Config();
    private SentryLogger() {
        // only CodeSmell Remove
    }

    public static void startSentry() {
        config.initConfig();
        Sentry.init(options -> {
            options.setDsn("https://79dd0ad84c08424ca57f89c81578de3a@o1145510.ingest.sentry.io/4503984812130304");
            options.setEnableExternalConfiguration(true);
            options.setTracesSampleRate(1.0);
            options.setDebug(true);     //! Auf false setzen, wenn konsole zu gebombt wird.
        });
        configSentry();
    }

    private static void configSentry() {
        SystemInfo sysinfo = new SystemInfo();
        HardwareAbstractionLayer hal = sysinfo.getHardware();
        CentralProcessor cpu = hal.getProcessor();
        GlobalMemory mem = hal.getMemory();
        ComputerSystem pc = hal.getComputerSystem();

        Sentry.configureScope(scope -> {
            scope.setTag("Operating System", System.getProperty("os.name"));
            scope.setTag("DevMode", config.debugmode);  // TODO: Replace Devmode Toggler with DevMode out of Properties File
            scope.setTag("CPU", cpu.getName());
            scope.setTag("PC-Manufacturer", pc.getManufacturer());
            scope.setTag("PC-Serial", pc.getSerialNumber());
            scope.setTag("PC-Model", pc.getModel());
            scope.setTag("PC-Motherboard", pc.getBaseboard().getModel());
            scope.setTag("RAM Total", "" + ((mem.getTotal() / 1024) / 1024) + " MB");
            scope.setTag("RAM Free", "" + ((mem.getAvailable() / 1024) / 1024) + " MB");
            scope.setTag("RAM Used", "" + (((mem.getTotal() - mem.getAvailable()) / 1024) / 1024) + "MB");
        });
    }


}
