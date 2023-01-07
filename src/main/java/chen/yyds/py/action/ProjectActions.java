package chen.yyds.py.action;

import chen.yyds.py.Notifyer;
import chen.yyds.py.impl.ProjectServer;
import chen.yyds.py.impl.ProjectServerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.editor.Editor;
import java.util.Objects;

public class ProjectActions extends AnAction {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER = com.intellij.openapi.diagnostic.Logger.getInstance(ProjectActions.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() == null) {
            return;
        }

        ActionManagerEx ex = ActionManagerEx.getInstanceEx();
        String thisActionId = ex.getId(this);
        LOGGER.warn("--> ActionPerformed id: " + thisActionId);
        if (thisActionId == null) return;

        String projectPath = e.getProject().getBasePath();
        ProjectServerImpl projectServer = (ProjectServerImpl)e.getProject().getService(ProjectServer.class);

        if (projectPath == null) {
            Notifyer.notifyError("获取工程路径失败");
            return;
        }

        // 注意有一个action不用连接设备，其它全部都需要
        if (projectServer.isClientConnectOk() && !Objects.requireNonNull(thisActionId).equals("id_zip_project")) {
            Notifyer.notifyError("连接调试设备失败!");
            return;
        }

        switch (thisActionId) {
            case "id_device_reconnect":
                projectServer.reConnect();
                break;
            case "id_push_run_project":
            case "id_yyds_project_push_run":
                projectServer.sendProject();
                projectServer.startProject();
                break;
            case "id_stop_project":
                projectServer.stopProject();
                break;
            case "id_push_project":
                projectServer.sendProject();
                break;
            case "id_zip_project":
                projectServer.zipProject();
                break;
            case "id_run_project":
                projectServer.startProject();
                break;
            case "id_start_code_snippet":
                Editor editor = e.getData(PlatformDataKeys.EDITOR);
                if (editor != null) {
                    String selectedText = editor.getSelectionModel().getSelectedText();
                    if (selectedText != null) {
                        projectServer.runCodeSnippet(selectedText);
                        LOGGER.warn("Select Code:" + selectedText);
                    } else {
                        Notifyer.notifyWarn("运行选中代码失败，未选中任何代码!");
                    }
                }
                break;
            case "id_start_code_file":
                String code = e.getData(PlatformDataKeys.FILE_TEXT);
                projectServer.runCodeSnippet(code);
                break;
        }
    }
}
