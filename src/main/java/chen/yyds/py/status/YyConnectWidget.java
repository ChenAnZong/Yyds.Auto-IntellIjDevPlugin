package chen.yyds.py.status;

import chen.yyds.py.impl.ProjectServerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.openapi.wm.impl.status.StatusBarUI;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;
import java.util.Objects;


public class YyConnectWidget extends EditorBasedWidget {

    private static final com.intellij.openapi.diagnostic.Logger LOGGER =
            com.intellij.openapi.diagnostic.Logger.getInstance(YyConnectWidget.class);
    Project mProject;
    ProjectServerImpl mProjectServer;

    public static final String WIDGET_ID = "YYConnectWidget";

    public YyConnectWidget(@NotNull Project project, ProjectServerImpl projectServer) {
        super(project);
        mProject = project;
        mProjectServer = projectServer;
    }

    @NotNull
    @Override
    public String ID() {
        return WIDGET_ID;
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        new Thread(()-> {
            String lastStatus = "";
            while (true) {
                try {
                    Thread.sleep(3000);
                    String status = mProjectServer.getConnectDescStatus();
                    // LOGGER.warn(status);
                    if (!Objects.equals(status, lastStatus)) {
                        WindowManager.getInstance().getStatusBar(mProject).updateWidget(WIDGET_ID);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
        return new YyStatusPresentation(mProject);
    }
}