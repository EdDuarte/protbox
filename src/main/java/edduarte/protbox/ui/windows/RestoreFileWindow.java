package edduarte.protbox.ui.windows;

import com.google.common.collect.Lists;
import edduarte.protbox.core.Constants;
import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.core.registry.PbxEntry;
import edduarte.protbox.core.registry.PbxFile;
import edduarte.protbox.core.registry.PbxFolder;
import edduarte.protbox.ui.listeners.OnKeyReleased;
import edduarte.protbox.ui.listeners.OnMouseClick;
import edduarte.protbox.utils.Utils;
import org.apache.commons.lang3.SystemUtils;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.jdesktop.xswingx.PromptSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class RestoreFileWindow extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(RestoreFileWindow.class);

    private static final Map<PReg, RestoreFileWindow> instances = new HashMap<>();

    private RestoreFileWindow(final PReg registry) {
        super();
        setLayout(null);

        final JTextField field = new JTextField();
        field.setLayout(null);
        PromptSupport.setPrompt("  Search for a file to restore...", field);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.SHOW_PROMPT, field);
        field.setBounds(2, 2, 301, 26);
        field.setBorder(new LineBorder(Color.lightGray));
        field.setFont(Constants.FONT);
        add(field);


        final JLabel noDeleted = new JLabel("<html>No deleted files were found!<br><br>" +
                "<font color=\"gray\">If you think some file from the registry was<br>" +
                "deleted and it's not here, please create an issue here:<br>" +
                "<a href=\"#\">https://github.com/edduarte/protbox/issues</a></font></html>");
        noDeleted.setLayout(null);
        noDeleted.setBounds(20, 50, 300, 300);
        noDeleted.setFont(Constants.FONT.deriveFont(14f));
        noDeleted.addMouseListener((OnMouseClick) e -> {
            String urlPath = "https://github.com/edduarte/protbox/issues";

            try {

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(urlPath));

                } else {
                    if (SystemUtils.IS_OS_WINDOWS) {
                        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + urlPath);

                    } else {
                        java.util.List<String> browsers =
                                Lists.newArrayList("firefox", "opera", "safari", "mozilla", "chrome");

                        for (String browser : browsers) {
                            if (Runtime.getRuntime().exec(new String[]{"which", browser}).waitFor() == 0) {

                                Runtime.getRuntime().exec(new String[]{browser, urlPath});
                                break;
                            }
                        }
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        DefaultMutableTreeNode rootTreeNode = registry.buildDeletedTree();
        final JTree tree = new JTree(rootTreeNode);
        if (rootTreeNode.getChildCount() == 0) {
            field.setEnabled(false);
            add(noDeleted);
        }
        expandTree(tree);
        tree.setLayout(null);
//        tree.setBounds(10, 30, 340, 350);
        tree.setRootVisible(false);
        tree.setEditable(false);
        tree.setCellRenderer(new RestoreCellRenderer(field));
        field.addKeyListener((OnKeyReleased) e -> {
            // update and expand tree
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.nodeStructureChanged((TreeNode) model.getRoot());
            expandTree(tree);
        });
        final JScrollPane scroll = new JScrollPane(tree,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(new DropShadowBorder());
        scroll.setBorder(new LineBorder(Color.lightGray));
        scroll.setBounds(2, 30, 334, 360);
        add(scroll);


        JLabel close = new JLabel(new ImageIcon(Constants.getAsset("close.png")));
        close.setLayout(null);
        close.setBounds(312, 7, 18, 18);
        close.setFont(Constants.FONT);
        close.setForeground(Color.gray);
        close.addMouseListener((OnMouseClick) e -> dispose());
        add(close);


        final JLabel permanent = new JLabel(new ImageIcon(Constants.getAsset("permanent.png")));
        permanent.setLayout(null);
        permanent.setBounds(91, 390, 154, 39);
        permanent.setBackground(Color.black);
        permanent.setEnabled(false);
        permanent.addMouseListener((OnMouseClick) e -> {
            if (permanent.isEnabled()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                PbxEntry entry = (PbxEntry) node.getUserObject();

                registry.permanentDelete(entry);
                dispose();
                RestoreFileWindow.getInstance(registry);
            }
        });
        add(permanent);


        final JLabel action = new JLabel(new ImageIcon(Constants.getAsset("restore.png")));
        action.setLayout(null);
        action.setBounds(3, 390, 85, 39);
        action.setBackground(Color.black);
        action.setEnabled(false);
        action.addMouseListener((OnMouseClick) e -> {
            if (action.isEnabled()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                PbxEntry entry = (PbxEntry) node.getUserObject();
                registry.showPair(entry);
                dispose();
            }
        });
        add(action);


        tree.addMouseListener((OnMouseClick) e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node != null) {
                PbxEntry entry = (PbxEntry) node.getUserObject();
                if (entry.isHidden()) {
                    permanent.setEnabled(true);
                    action.setEnabled(true);
                } else {
                    permanent.setEnabled(false);
                    action.setEnabled(false);
                }
            }
        });


        final JLabel cancel = new JLabel(new ImageIcon(Constants.getAsset("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(229, 390, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener((OnMouseClick) e -> dispose());
        add(cancel);

        addWindowFocusListener(new WindowFocusListener() {
            private boolean gained = false;

            @Override
            public void windowGainedFocus(WindowEvent e) {
                gained = true;
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (gained) {
                    dispose();
                }
            }
        });

        setSize(340, 432);
        setUndecorated(true);
        getContentPane().setBackground(Color.white);
        setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        setVisible(true);
    }


    public static RestoreFileWindow getInstance(final PReg registry) {
        RestoreFileWindow newInstance;
//                = instances.get(registry);
//        if (newInstance == null) {
        newInstance = new RestoreFileWindow(registry);
//            instances.put(registry, newInstance);
//        } else {
        newInstance.setVisible(true);
        newInstance.toFront();
//        }
        return newInstance;
    }


    private void expandTree(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);
    }


    private class RestoreCellRenderer extends DefaultTreeCellRenderer {

        private final JTextField field;

        public RestoreCellRenderer(JTextField field) {
            this.field = field;
        }

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            // processing search functionality getFirst and rendering later
            String search = field.getText();
            String query = value.toString();
            StringBuffer html = new StringBuffer("<html>");
            Matcher m = Pattern.compile(Pattern.quote(search)).matcher(query);
            while (m.find()) {
                m.appendReplacement(html, "<b>" + m.group() + "</b>");
                m.appendTail(html).append("</html>");
            }
            super.getTreeCellRendererComponent(tree, html.toString(), sel, expanded, leaf, row, hasFocus);
            tree.setRowHeight(32);

            // rendering font and ASSETS
            PbxEntry entry = (PbxEntry) ((DefaultMutableTreeNode) value).getUserObject();
            setFont(Constants.FONT.deriveFont(13f));


            if (entry instanceof PbxFile) {
                if (entry.isHidden()) {
                    setIcon(new ImageIcon(Constants.getAsset("file.png"))); // image of deleted file
                    setForeground(Color.gray);
                } else {
                    setIcon(new ImageIcon(Constants.getAsset("file.png"))); // image of normal file
                }
            } else if (entry instanceof PbxFolder) {
                if (entry.isHidden()) {
                    setIcon(new ImageIcon(Constants.getAsset("folder.png"))); // image of deleted file
                    setForeground(Color.gray);
                } else {
                    setIcon(new ImageIcon(Constants.getAsset("folder.png"))); // image of normal folder
                }
            }

            return this;
        }
    }

}
