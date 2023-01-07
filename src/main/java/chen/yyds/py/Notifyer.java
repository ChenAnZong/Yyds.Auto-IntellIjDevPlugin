package chen.yyds.py;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;

public class Notifyer {
    private static final String GROUP_ID = "Yyds.Py.Noti";
    private static final String DEFAULT_TITLE = "Yyds.Auto";

    public static void notifyInfo(String text) {
        notifyInfo(DEFAULT_TITLE, text);
    }
    public static void notifyError(String text) {
        notifyError(DEFAULT_TITLE, text);
    }

    public static void notifyWarn(String text) {
        notifyError(DEFAULT_TITLE, text);
    }



    public static void notifyInfo(String title, String text) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(text, NotificationType.INFORMATION)
                .setTitle(title)
                .notify(null);
    }
    public static void notifyError(String title, String text) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(text, NotificationType.ERROR)
                .setTitle(title)
                .setImportant(true)
                .notify(null);
    }

    public static void notifyWarn(String title, String text) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_ID)
                .createNotification(text, NotificationType.WARNING)
                .setTitle(title)
                .notify(null);
    }
}
