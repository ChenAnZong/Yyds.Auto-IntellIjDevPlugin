package chen.yyds.py.status;


import chen.yyds.py.impl.ProjectServer;
import chen.yyds.py.impl.ProjectServerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class YyStatusPresentation implements StatusBarWidget.MultipleTextValuesPresentation {
    private static final com.intellij.openapi.diagnostic.Logger LOGGER =
            com.intellij.openapi.diagnostic.Logger.getInstance(YyStatusPresentation.class);
    ProjectServerImpl mProjectServer;

    public YyStatusPresentation(Project project) {
        super();
        mProjectServer = (ProjectServerImpl) project.getService(ProjectServer .class);
        mProjectServer.reConnect();
        LOGGER.warn("Open log window, call mProjectServer.reConnect()");
    }

    private final ListPopupStep popupStep = new ListPopupStep() {

        ArrayList<String> l  = new ArrayList<>();

        @Override
        public @NotNull List getValues() {
            l.add("<待开发功能1>");
            l.add("<待开发功能2>");
            return l;
        }

        @Override
        public boolean isSelectable(Object value) {
            return true;
        }

        @Override
        public @Nullable Icon getIconFor(Object value) {
            return null;
        }

        @Override
        public @NlsContexts.ListItem @NotNull String getTextFor(Object value) {
            return value.toString();
        }

        @Override
        public @Nullable ListSeparator getSeparatorAbove(Object value) {
            return null;
        }

        @Override
        public int getDefaultOptionIndex() {
            return 0;
        }

        @Override
        public @NlsContexts.PopupTitle @Nullable String getTitle() {
            return "引擎连接状态";
        }

        @Override
        public @Nullable PopupStep<?> onChosen(Object selectedValue, boolean finalChoice) {
            LOGGER.warn("onChosen" + "  :"  + selectedValue + "  :" + finalChoice);
            return null;
        }

        @Override
        public boolean hasSubstep(Object selectedValue) {
            return true;
        }

        @Override
        public void canceled() {

        }

        @Override
        public boolean isMnemonicsNavigationEnabled() {
            return false;
        }

        @Override
        public @Nullable MnemonicNavigationFilter getMnemonicNavigationFilter() {
            return null;
        }

        @Override
        public boolean isSpeedSearchEnabled() {
            return false;
        }

        @Override
        public @Nullable SpeedSearchFilter getSpeedSearchFilter() {
            return null;
        }

        @Override
        public boolean isAutoSelectionEnabled() {
            return true;
        }

        @Override
        public @Nullable Runnable getFinalRunnable() {
            return null;
        }
    };

    @Nullable("null means the widget is unable to show the popup")
    @Override
    public ListPopup getPopupStep() {
        ListPopup listPopup = JBPopupFactory.getInstance().createListPopup(popupStep);
        listPopup.setShowSubmenuOnHover(true);
        listPopup.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                LOGGER.warn("1111111111222" + e.toString());
            }
        });
        listPopup.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(@NotNull LightweightWindowEvent event) {
                LOGGER.warn("bbbb");
                JBPopupListener.super.beforeShown(event);
            }
        });
        return listPopup;
    }

    @Nullable
    @Override
    public String getSelectedValue() {
        return mProjectServer.getConnectDescStatus();
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return "调试设备连接状态";
    }

    @Nullable
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        LOGGER.warn("getClickConsumer------------");
        return null;
    }
}