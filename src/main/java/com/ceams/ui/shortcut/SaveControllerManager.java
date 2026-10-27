package com.ceams.ui.shortcut;

import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SaveControllerManager {

    private static SaveControllerManager instance;
    private Map<String, Saveable> registeredControllers;
    private String currentActiveModule;

    private SaveControllerManager() {
        registeredControllers = new HashMap<>();
    }

    public static synchronized SaveControllerManager getInstance() {
        if (instance == null) {
            instance = new SaveControllerManager();
        }
        return instance;
    }

    public void registerController(String moduleName, Saveable controller) {
        registeredControllers.put(moduleName, controller);
        System.out.println("注册保存控制器: " + moduleName);
    }

    public void unregisterController(String moduleName) {
        registeredControllers.remove(moduleName);
        System.out.println("注销保存控制器: " + moduleName);
    }

    public void setCurrentActiveModule(String moduleName) {
        this.currentActiveModule = moduleName;
        System.out.println("当前活动模块设置为: " + moduleName);
    }

    public void performSave() {
        System.out.println("执行保存操作，当前活动模块: " + currentActiveModule);

        if (currentActiveModule != null && registeredControllers.containsKey(currentActiveModule)) {
            Saveable controller = registeredControllers.get(currentActiveModule);
            try {
                controller.save();
                System.out.println("模块 " + currentActiveModule + " 保存成功");
            } catch (Exception e) {
                System.err.println("模块 " + currentActiveModule + " 保存失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (registeredControllers.containsKey("global")) {
            Saveable controller = registeredControllers.get("global");
            try {
                controller.save();
                System.out.println("全局保存成功");
            } catch (Exception e) {
                System.err.println("全局保存失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("没有找到活动的保存控制器");
        }
    }

    public boolean hasActiveSaveable() {
        return currentActiveModule != null && registeredControllers.containsKey(currentActiveModule) ||
               registeredControllers.containsKey("global");
    }

    public String getCurrentActiveModule() {
        return currentActiveModule;
    }

    public Map<String, Saveable> getRegisteredControllers() {
        return registeredControllers;
    }

    public void clearAll() {
        registeredControllers.clear();
        currentActiveModule = null;
    }
}
