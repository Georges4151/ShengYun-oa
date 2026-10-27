package com.ceams.ui.shortcut;

public interface Saveable {
    void save();

    String getModuleName();

    boolean hasUnsavedChanges();
}
