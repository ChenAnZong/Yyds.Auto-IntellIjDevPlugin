package chen.yyds.py.impl;

import chen.yyds.py.Notifyer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
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
            FileInputStream fs = new FileInputStream(configFile);
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
        new Thread(() -> {
                Properties properties = loadProjectProperties();
                String projectName = properties.getProperty(Const.PROP_KEY_PROJECT_NAME);
                EngineImplement.INSTANCE.notifyStartProject(projectName);
        }).start();
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
        try {
            String zipFileName = String.format("/%s_%s.yyp.zip", projectName, projectVersion);
            ZipUtility.zip(Arrays.asList(getProjectFile().clone()), project.getBasePath().concat(zipFileName));
            LOGGER.warn("Zip Finish:$tempZip");
            Notifyer.notifyInfo("打包成功 " + zipFileName);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public void sendProject() {
        try {
            new Thread(()-> {
                Properties properties = loadProjectProperties();
                String projectName = properties.getProperty(Const.PROP_KEY_PROJECT_NAME);
                LOGGER.warn("All Project files:" +  Arrays.toString(new File(Objects.requireNonNull(project.getBasePath())).list()));
                EngineImplement.INSTANCE.sendEntireProject(project.getBasePath().concat("/.local.zip"), projectName, getProjectFile());
            }).start();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void reConnect() {
        Properties properties = loadProjectProperties();
        String ip = properties.getProperty(Const.PROP_KEY_DEBUG_DEVICE_IP);
        EngineImplement.INSTANCE.reConnect(ip);
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

    public boolean isClientConnectOk() {
        try {
            InetAddress address = InetAddress.getByName(EngineImplement.INSTANCE.getDeviceIp());
            return !address.isReachable(1500);
        } catch (Exception e) {
            return true;
        }
    }
    @Override
    public void dispose() {
        EngineImplement.INSTANCE.disConnectAll();
    }
}
