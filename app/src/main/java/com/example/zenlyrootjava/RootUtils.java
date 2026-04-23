package com.example.zenlyrootjava;

import java.io.DataOutputStream;

public class RootUtils {
    public static void hideForegroundNotification() {
        try {
            int pid = android.os.Process.myPid();
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            // Lệnh shell Android để hủy thông báo foreground service
            os.writeBytes("cmd notification cancel --user 0 com.example.zenlyrootjava 1\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
