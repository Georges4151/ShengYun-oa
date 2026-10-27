package com.ceams.service.impl;

import com.ceams.service.UsbDetectionService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UsbDetectionServiceImpl implements UsbDetectionService {

    private static final String SEAL_STORAGE_DIR = ".seal_storage";
    private static final String SEAL_STORAGE_NAME = "电子章安全存储";

    @Override
    public List<File> detectUsbDrives() {
        List<File> usbDrives = new ArrayList<>();

        if (isWindows()) {
            Set<String> removableDrives = getRemovableDrivesWindows();
            for (String driveLetter : removableDrives) {
                File drive = new File(driveLetter);
                if (drive.exists() && drive.canRead()) {
                    usbDrives.add(drive);
                }
            }
        } else {
            File[] roots = File.listRoots();
            if (roots != null) {
                for (File root : roots) {
                    if (isUsbDrive(root)) {
                        usbDrives.add(root);
                    }
                }
            }
        }

        return usbDrives;
    }

    private Set<String> getRemovableDrivesWindows() {
        Set<String> removableDrives = new HashSet<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("wmic", "logicaldisk", "where", "drivetype=2", "get", "deviceid");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.matches("[A-Z]:")) {
                    removableDrives.add(line + "\\");
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("WMIC检测失败，使用备用方法: " + e.getMessage());
            return getRemovableDrivesFallback();
        }
        return removableDrives;
    }

    private Set<String> getRemovableDrivesFallback() {
        Set<String> removableDrives = new HashSet<>();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                String path = root.getAbsolutePath();
                if (path.matches("[A-Z]:\\\\") &&
                        !isSystemDrive(root) &&
                        root.canWrite() &&
                        isLikelyUsbDrive(root)) {
                    removableDrives.add(path);
                }
            }
        }
        return removableDrives;
    }

    private boolean isLikelyUsbDrive(File drive) {
        try {
            long totalSpace = drive.getTotalSpace();
            return totalSpace > 0 && totalSpace < 500L * 1024 * 1024 * 1024;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isUsbDrive(File drive) {
        if (drive == null || !drive.exists() || !drive.isDirectory()) {
            return false;
        }

        String path = drive.getAbsolutePath();

        if (isWindows()) {
            return getRemovableDrivesWindows().contains(path);
        } else {
            String mountPoint = "/media/" + System.getProperty("user.name");
            return path.startsWith(mountPoint) ||
                    path.startsWith("/mnt/") ||
                    path.startsWith("/run/media/");
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private boolean isSystemDrive(File drive) {
        String systemDrive = System.getenv("SystemDrive");
        if (systemDrive != null) {
            return drive.getAbsolutePath().equalsIgnoreCase(systemDrive + "\\");
        }
        String path = drive.getAbsolutePath();
        return path.equalsIgnoreCase("C:\\");
    }

    @Override
    public File findSealStorageDrive() {
        List<File> usbDrives = detectUsbDrives();
        for (File drive : usbDrives) {
            if (hasSealStorage(drive)) {
                return drive;
            }
        }
        return null;
    }

    @Override
    public String getDriveInfo(File drive) {
        if (drive == null) {
            return "未知设备";
        }

        StringBuilder info = new StringBuilder();
        info.append(drive.getAbsolutePath());

        try {
            long totalSpace = drive.getTotalSpace();
            long freeSpace = drive.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            info.append(" (");
            info.append(formatSize(usedSpace));
            info.append(" / ");
            info.append(formatSize(totalSpace));
            info.append(", 可用: ");
            info.append(formatSize(freeSpace));
            info.append(")");
        } catch (SecurityException e) {
            info.append(" (无法获取空间信息)");
        }

        return info.toString();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    @Override
    public boolean hasSealStorage(File drive) {
        if (drive == null || !drive.exists()) {
            return false;
        }

        File sealStorage = getSealStoragePath(drive);
        return sealStorage.exists() && sealStorage.isDirectory();
    }

    @Override
    public File getSealStoragePath(File drive) {
        if (drive == null) {
            return null;
        }
        return new File(drive, SEAL_STORAGE_DIR);
    }

    @Override
    public void createSealStorageDirectory(File drive) {
        if (drive == null || !drive.exists()) {
            throw new IllegalArgumentException("无效的驱动器");
        }

        File sealStorage = getSealStoragePath(drive);
        if (!sealStorage.exists()) {
            boolean created = sealStorage.mkdirs();
            if (!created) {
                throw new RuntimeException("无法创建电子章存储目录");
            }

            if (isWindows()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("attrib", "+h", sealStorage.getAbsolutePath());
                    pb.start();
                } catch (Exception e) {
                    System.err.println("无法隐藏目录: " + e.getMessage());
                }
            }
        }
    }
}
