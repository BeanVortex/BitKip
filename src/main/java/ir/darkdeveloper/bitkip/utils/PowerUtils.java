package ir.darkdeveloper.bitkip.utils;

import com.sun.jna.Native;
import ir.darkdeveloper.bitkip.models.TurnOffMode;

import java.io.IOException;

import static com.sun.jna.Platform.*;

public class PowerUtils {

    public static void turnOff(TurnOffMode turnOffMode) {
        switch (turnOffMode) {
            case SLEEP -> sleep();
            case TURN_OFF -> shutDown();
        }
    }

    private static void shutDown() {
        try {
            if (isWindows())
                Runtime.getRuntime().exec(new String[]{"shutdown", "-s"});
            else if (isLinux() || isSolaris() || isFreeBSD() || isOpenBSD())
                Runtime.getRuntime().exec(new String[]{"sudo", "shutdown", "now"});
            else if (isMac())
                Runtime.getRuntime().exec(new String[]{"sudo", "shutdown"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sleep() {
        try {
            if (isWindows())
                SetSuspendState(false, false, false);
            else if (isLinux() || isSolaris() || isFreeBSD() || isOpenBSD())
                Runtime.getRuntime().exec(new String[]{"sudo", "systemctl", "suspend"});
            else if (isMac())
                Runtime.getRuntime().exec(new String[]{"sudo", "shutdown", "-s"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static native boolean SetSuspendState(boolean hibernate, boolean forceCritical, boolean disableWakeEvent);

    static {
        if (isWindows())
            Native.register("powrprof");
    }


}
