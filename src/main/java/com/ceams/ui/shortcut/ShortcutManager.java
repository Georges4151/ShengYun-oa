package com.ceams.ui.shortcut;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class ShortcutManager {

    private static ShortcutManager instance;

    private Map<KeyCombination, Consumer<KeyEvent>> globalShortcuts;
    private Map<String, Map<KeyCombination, Consumer<KeyEvent>>> moduleShortcuts;
    private String currentModule;
    private Node currentFocusOwner;

    private ShortcutManager() {
        globalShortcuts = new HashMap<>();
        moduleShortcuts = new HashMap<>();
        currentModule = "global";
        initializeDefaultShortcuts();
    }

    public static synchronized ShortcutManager getInstance() {
        if (instance == null) {
            instance = new ShortcutManager();
        }
        return instance;
    }

    private void initializeDefaultShortcuts() {
        // Ctrl+S 全局保存快捷键
        KeyCombination ctrlS = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        registerGlobalShortcut(ctrlS, event -> {
            System.out.println("Ctrl+S 快捷键触发 - 触发全局保存");
            SaveControllerManager.getInstance().performSave();
        });
    }

    public void registerGlobalShortcut(KeyCombination keyCombination, Consumer<KeyEvent> action) {
        globalShortcuts.put(keyCombination, action);
    }

    public void registerModuleShortcut(String module, KeyCombination keyCombination, Consumer<KeyEvent> action) {
        moduleShortcuts.computeIfAbsent(module, k -> new HashMap<>()).put(keyCombination, action);
    }

    public void setCurrentModule(String module) {
        this.currentModule = module;
        System.out.println("切换到模块: " + module);
    }

    public void setCurrentFocusOwner(Node node) {
        this.currentFocusOwner = node;
    }

    public boolean handleKeyEvent(KeyEvent event) {
        // 根据事件动态构建快捷键组合
        KeyCombination.Modifier[] modifiers = new KeyCombination.Modifier[4];
        int index = 0;
        
        if (event.isShiftDown()) {
            modifiers[index++] = KeyCombination.SHIFT_DOWN;
        }
        if (event.isControlDown()) {
            modifiers[index++] = KeyCombination.CONTROL_DOWN;
        }
        if (event.isAltDown()) {
            modifiers[index++] = KeyCombination.ALT_DOWN;
        }
        if (event.isMetaDown()) {
            modifiers[index++] = KeyCombination.META_DOWN;
        }
        
        // 创建只包含实际按下的修饰符的数组
        KeyCombination.Modifier[] actualModifiers = new KeyCombination.Modifier[index];
        System.arraycopy(modifiers, 0, actualModifiers, 0, index);
        
        KeyCombination keyCombination = new KeyCodeCombination(event.getCode(), actualModifiers);

        // 检查全局快捷键
        for (Map.Entry<KeyCombination, Consumer<KeyEvent>> entry : globalShortcuts.entrySet()) {
            if (entry.getKey().match(event)) {
                entry.getValue().accept(event);
                event.consume();
                return true;
            }
        }

        // 检查当前模块的快捷键
        Map<KeyCombination, Consumer<KeyEvent>> moduleShortcutsMap = moduleShortcuts.get(currentModule);
        if (moduleShortcutsMap != null) {
            for (Map.Entry<KeyCombination, Consumer<KeyEvent>> entry : moduleShortcutsMap.entrySet()) {
                if (entry.getKey().match(event)) {
                    entry.getValue().accept(event);
                    event.consume();
                    return true;
                }
            }
        }

        return false;
    }

    public EventHandler<KeyEvent> getGlobalKeyHandler() {
        return event -> {
            if (handleKeyEvent(event)) {
                event.consume();
            }
        };
    }

    public void clearModuleShortcuts(String module) {
        moduleShortcuts.remove(module);
    }

    public void clearAllModuleShortcuts() {
        moduleShortcuts.clear();
    }

    public Map<KeyCombination, Consumer<KeyEvent>> getGlobalShortcuts() {
        return globalShortcuts;
    }

    public Map<String, Map<KeyCombination, Consumer<KeyEvent>>> getModuleShortcuts() {
        return moduleShortcuts;
    }
}
