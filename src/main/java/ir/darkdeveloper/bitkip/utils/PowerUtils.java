package ir.darkdeveloper.bitkip.utils;

import java.io.IOException;

public class PowerUtils {

    public static boolean turnOff() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"shutdown /s /t 0"});
            int exitValue = process.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean sleep() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"rundll32.exe powrprof.dll,SetSuspendState 0,1,0"});
            int exitValue = process.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hibernate() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"shutdown /h"});
            int exitValue = process.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

}
