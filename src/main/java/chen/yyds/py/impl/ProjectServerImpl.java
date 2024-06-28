package chen.yyds.py.impl;

import chen.yyds.py.Notifyer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Predicate;

public class ProjectServerImpl implements Disposable {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER =
            com.intellij.openapi.diagnostic.Logger.getInstance(ProjectServerImpl.class);


    private final Project project;

    public ProjectServerImpl(Project project) {
        this.project = project;
        Properties properties = loadProjectProperties();
        String debugDeviceIp = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
        EngineImplement.INSTANCE.setDeviceIp(debugDeviceIp);
    }

    public Properties loadProjectProperties() {
        try {
            File configFile = Paths.get(Objects.requireNonNull(project.getBasePath()), "project.config").toFile();
            InputStreamReader fs = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8);
            Properties properties = new Properties();
            properties.load(fs);
            return properties;
        } catch (FileNotFoundException e) {
            Notifyer.notifyError("错误:配置文件不存在");
            throw new RuntimeException("配置文件不存在");
        } catch (IOException e) {
            Notifyer.notifyError("读取项目配置文件失败");
            throw new RuntimeException("读取项目配置文件失败");
        }
    }

    public String getProjectProperties(String key) {
        return loadProjectProperties().getProperty(key);
    }

    public String getScreenShot() {
        return EngineImplement.INSTANCE.getScreenShot();
    }

    public String getUiaDump() {
        return EngineImplement.INSTANCE.getUiaDump();
    }

    public String getForeground() {
        return EngineImplement.INSTANCE.getForeground();
    }

    public void runCodeSnippet(String code) {
        new Thread(() -> {
            EngineImplement.INSTANCE.runCodeSnippet(code);
        }).start();
    }

    public void startProject() {
        Properties properties = loadProjectProperties();
        String projectName = properties.getProperty(Const.PROP_KEY_PROJECT_NAME);
        EngineImplement.INSTANCE.notifyStartProject(projectName);
    }

    public void stopProject() {
        EngineImplement.INSTANCE.notifyStopProject();
    }

    public File[] getProjectFile() {
        return Arrays.stream(new File(project.getBasePath()).listFiles()).filter(new Predicate<File>() {
            @Override
            public boolean test(File file) {
                return !file.getName().startsWith(".") && !file.getName().contains("yyp.zip");
            }
        }).toArray(File[]::new);
    }

    public void zipProject() {
        Properties properties = loadProjectProperties();
        String projectName = properties.getProperty(Const.PROP_KEY_PROJECT_NAME);
        String projectVersion = properties.getProperty(Const.PROP_KEY_PROJECT_VERSION);
        boolean isWithVersion = Objects.equals(properties.getProperty(Const.PROP_KEY_WITH_VERSION), "true");
        try {
            String zipFileName;
            if (isWithVersion) {
                zipFileName = String.format("/%s_%s.yyp.zip", projectName, projectVersion);
            } else {
                zipFileName = String.format("/%s.yyp.zip", projectName);
            }
            ZipUtility.zip(Arrays.asList(getProjectFile().clone()), project.getBasePath().concat(zipFileName));
            LOGGER.warn("Zip Finish:$tempZip");
            Notifyer.notifyInfo("打包成功 " + zipFileName);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public void sendProject() {
        try {
            Properties properties = loadProjectProperties();
            String projectName = properties.getProperty(Const.PROP_KEY_PROJECT_NAME);
            LOGGER.warn("All Project files:" +  Arrays.toString(new File(Objects.requireNonNull(project.getBasePath())).list()));
            EngineImplement.INSTANCE.sendEntireProject(project.getBasePath().concat("/.local.zip"), projectName, getProjectFile());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void reConnect() {
        new Thread(()-> {
            try {
                LOGGER.warn("reConnect()");
                Properties properties = loadProjectProperties();
                String ip = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
                EngineImplement.INSTANCE.setDeviceIp(ip);
                LOGGER.warn("Refresh Ip:" + ip);
                EngineImplement.INSTANCE.reConnect(ip);
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }).start();
    }

    public void disConnect() {
        EngineImplement.INSTANCE.disConnect();
    }

    public boolean logHasNext() {
        return EngineImplement.INSTANCE.logHasNext();
    }
    public String nextLog() {
        return EngineImplement.INSTANCE.nextLog();
    }

    public boolean click(int x, int y) {
        return EngineImplement.INSTANCE.click(x, y);
    }

    public boolean isClientConnectFailed() {
        try {
            InetAddress address = InetAddress.getByName(EngineImplement.INSTANCE.getDeviceIp());
            return !address.isReachable(1500);
        } catch (Exception e) {
            LOGGER.error(e);
            return true;
        }
    }

    public String getCurDeviceIp() {
        return EngineImplement.INSTANCE.getDeviceIp();
    }

    public String getConnectDescStatus() {
        String status;
        if (EngineImplement.INSTANCE.isApiConnecting.get()) {
            status = " 控制通讯:连接";
        } else {
            status = " 控制通讯:断开";
        }
        if (EngineImplement.INSTANCE.isLogConnecting.get()) {
            status += " 日志通讯:连接";
        } else {
            status += " 日志通讯:断开";
        }
        return status + " " + EngineImplement.INSTANCE.getDeviceIp();
    }

    @Override
    public void dispose() {
        EngineImplement.INSTANCE.disConnect();
    }
}
