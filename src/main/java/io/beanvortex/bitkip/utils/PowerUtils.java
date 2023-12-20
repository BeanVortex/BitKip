package io.beanvortex.bitkip.utils;

import com.sun.jna.Native;
import io.beanvortex.bitkip.models.TurnOffMode;

import java.io.IOException;

import static com.sun.jna.Platform.*;
import static io.beanvortex.bitkip.config.AppConfigs.log;
import static io.beanvortex.bitkip.config.AppConfigs.userPassword;

public class PowerUtils {

    public static void turnOff(TurnOffMode turnOffMode) {
        switch (turnOffMode) {
            case SLEEP -> sleep();
            case TURN_OFF -> shutDown();
        }
    }

    private static void shutDown() {
        try {
            log.info("Shutting down");
            if (isWindows())
                Runtime.getRuntime().exec(new String[]{"shutdown", "-s"});
            else {
                // isMac()
                var command = "sudo,-S,shutdown";
                if (isLinux() || isSolaris() || isFreeBSD() || isOpenBSD())
                    command += ",now";
                var pb = new ProcessBuilder(command.split(","));
                var process = pb.start();
                process.getOutputStream().write((userPassword + "\n").getBytes());
                process.getOutputStream().flush();
            }

        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    private static void sleep() {
        try {
            log.info("Sleeping");
            if (isWindows())
                SetSuspendState(false, false, false);
            else {
                // isLinux() || isSolaris() || isFreeBSD() || isOpenBSD()
                var command = "sudo,-S,systemctl,suspend";
                if (isMac())
                    command = "sudo,-S,shutdown,-s";
                var pb = new ProcessBuilder(command.split(","));
                var process = pb.start();
                process.getOutputStream().write((userPassword + "\n").getBytes());
                process.getOutputStream().flush();
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    public static native boolean SetSuspendState(boolean hibernate, boolean forceCritical, boolean disableWakeEvent);

    static {
        if (isWindows())
            Native.register("powrprof");
    }


}
