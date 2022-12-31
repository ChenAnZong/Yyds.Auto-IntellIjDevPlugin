package chen.yyds.py.action;

import chen.yyds.py.impl.ProjectServer;
import chen.yyds.py.impl.ProjectServerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;



public class ActionPushProject extends AnAction {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER = com.intellij.openapi.diagnostic.Logger.getInstance(ActionStartTestProject.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() == null) {
            return;
        }

        String projectPath = e.getProject().getBasePath();

        if (projectPath == null) {
            return;
        }

        ProjectServerImpl projectServer = (ProjectServerImpl)e.getProject().getService(ProjectServer.class);
        if (!projectServer.isClientConnectOk()) {
            ProjectServerImpl.showNotification("连接调试设备失败!");
            return;
        }
        projectServer.sendProject();
    }
}
