package chen.yyds.py.impl;

import chen.yyds.py.status.YyConnectWidget;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;

public interface ProjectServer {
    static ProjectServer getInstance(@NotNull Project project) {
        System.out.println("-------------");
        return ServiceManager.getService(project, ProjectServer.class);
    }
}
