package chen.yyds.py.impl;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface ProjectServer {
    static ProjectServer getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, ProjectServer.class);
    }
}
