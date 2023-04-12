package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.TurnOffMode;

import java.io.IOException;

public class PowerUtils {

    public static void turnOff(TurnOffMode turnOffMode) {
        switch (turnOffMode) {
            case SLEEP -> sleep();
            case TURN_OFF -> shutDown();
            case HIBERNATE -> hibernate();
        }
    }

    private static void shutDown() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"shutdown /s /t 0"});
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void sleep() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"rundll32.exe powrprof.dll,SetSuspendState 0,1,0"});
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void hibernate() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"shutdown /h"});
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
