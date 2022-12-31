package chen.yyds.py.impl;

import com.intellij.notification.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import impl.EngineConnector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Predicate;

public class ProjectServerImpl {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER =
            com.intellij.openapi.diagnostic.Logger.getInstance(ProjectServerImpl.class);

    private final Project project;

    public static void showNotification(String text) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Yyds.Py.Noti")
                .createNotification(text, NotificationType.INFORMATION)
                .notify(null);
    }

    public ProjectServerImpl(Project project) {
        this.project = project;
    }

    public Properties loadProjectProperties() {
        try {
            File configFile = Paths.get(project.getBasePath(), "project.config").toFile();
            FileInputStream fs = new FileInputStream(configFile);
            Properties properties = new Properties();
            properties.load(fs);
            return properties;
        } catch (FileNotFoundException e) {
            showNotification("错误:配置文件不存在");
            throw new RuntimeException("配置文件不存在");
        } catch (IOException e) {
            showNotification("读取项目配置文件失败");
            throw new RuntimeException("读取项目配置文件失败");
        }
    }

    public String getProjectProperties(String key) {
        return loadProjectProperties().getProperty(key);
    }

    public String getScreenShot() {
        Properties properties = loadProjectProperties();
        String debugDeviceIp = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
        EngineConnector.INSTANCE.setDeviceIp(debugDeviceIp);
        return EngineConnector.INSTANCE.getScreenShot();
    }

    public String getUiaDump() {
        Properties properties = loadProjectProperties();
        String debugDeviceIp = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
        EngineConnector.INSTANCE.setDeviceIp(debugDeviceIp);
        return EngineConnector.INSTANCE.getUiaDump();
    }

    public String getForeground() {
        Properties properties = loadProjectProperties();
        String debugDeviceIp = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
        EngineConnector.INSTANCE.setDeviceIp(debugDeviceIp);
        return EngineConnector.INSTANCE.getForeground();
    }

    public void runCodeSnippet(String code) {
        Properties properties = loadProjectProperties();
        String debugDeviceIp = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
        EngineConnector.INSTANCE.setDeviceIp(debugDeviceIp);
        EngineConnector.INSTANCE.runCodeSnippet(code);
    }

    public void startProject() {
        Properties properties = loadProjectProperties();
        String debugDeviceIp = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
        String projectName = properties.getProperty(Const.PROP_KEY_PROJECT_NAME);
        showNotification(debugDeviceIp + " | StartProject:" + projectName);
        EngineConnector.INSTANCE.setDeviceIp(debugDeviceIp);
        EngineConnector.INSTANCE.notifyStartProject(projectName);
    }

    public void stopProject() {
        Properties properties = loadProjectProperties();
        String debugDeviceIp = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
        String projectName = properties.getProperty(Const.PROP_KEY_PROJECT_NAME);
        showNotification(debugDeviceIp + " | StopProject");
        EngineConnector.INSTANCE.setDeviceIp(debugDeviceIp);
        EngineConnector.INSTANCE.notifyStopProject();
    }

    public File[] getProjectFile() {
        return Arrays.stream(new File(project.getBasePath()).listFiles()).filter(new Predicate<File>() {
            @Override
            public boolean test(File file) {
                if (file.getName().startsWith(".") || file.getName().contains("yyp.zip")) {
                    return false;
                } else {
                    return true;
                }
            }
        }).toArray(File[]::new);
    }

    public void zipProject() {
        Properties properties = loadProjectProperties();
        String projectName = properties.getProperty(Const.PROP_KEY_PROJECT_NAME);
        String projectVersion = properties.getProperty(Const.PROP_KEY_PROJECT_VERSION);
        try {
            String zipFileName = String.format("/%s_%s.yyp.zip", projectName, projectVersion);
            ZipUtility.zip(Arrays.asList(getProjectFile().clone()), project.getBasePath().concat(zipFileName));
            LOGGER.warn("Zip Ok: $tempZip");
            showNotification("打包成功 " + zipFileName);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public void sendProject() {
        try {
            Properties properties = loadProjectProperties();
            String debugDeviceIp = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
            String projectName = properties.getProperty(Const.PROP_KEY_PROJECT_NAME);
            showNotification(debugDeviceIp + " | 推送工程:" + projectName);
            LOGGER.warn("全部工程文件:" +  Arrays.toString(new File(project.getBasePath()).list()));
            EngineConnector.INSTANCE.setDeviceIp(debugDeviceIp);
            EngineConnector.INSTANCE.sendEntireProject(project.getBasePath().concat("/.local.zip"), projectName, getProjectFile());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public boolean isClientConnectOk() {
        Properties properties = loadProjectProperties();
        String debugDeviceIp = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
        try {
            InetAddress address = InetAddress.getByName(debugDeviceIp);
            boolean reachable = address.isReachable(1500);
            if (!reachable) {
                return false;
            }
            EngineConnector.INSTANCE.setDeviceIp(debugDeviceIp);
            return EngineConnector.INSTANCE.checkClientSocketOk();
        } catch (Exception e) {
            return false;
        }
    }
}
