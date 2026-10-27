package com.ceams.service;

import java.io.File;
import java.util.List;

public interface UsbDetectionService {

    List<File> detectUsbDrives();

    boolean isUsbDrive(File drive);

    File findSealStorageDrive();

    String getDriveInfo(File drive);

    boolean hasSealStorage(File drive);

    File getSealStoragePath(File drive);

    void createSealStorageDirectory(File drive);
}
