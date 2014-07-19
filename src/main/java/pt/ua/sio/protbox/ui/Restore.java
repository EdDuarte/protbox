package pt.ua.sio.protbox.ui;

import org.jdesktop.swingx.border.DropShadowBorder;
import org.jdesktop.xswingx.PromptSupport;
import org.slf4j.LoggerFactory;
import pt.ua.sio.protbox.core.Constants;
import pt.ua.sio.protbox.core.directory.Registry;
import pt.ua.sio.protbox.core.directory.Pair;
import pt.ua.sio.protbox.util.AWTUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class Restore extends JDialog {
    private transient static org.slf4j.Logger logger = LoggerFactory.getLogger(Restore.class);

    private static Restore instance;

    public static Restore getInstance(final Registry directory) {
//        if(instance==null){
//            instance = new Restore(directory);
//        } else {
//            instance.toFront();
//        }
//        return instance;
        return new Restore(directory);
    }

    private Restore(final Registry directory) {
        super();
        this.setLayout(null);


        final JTextField field = new JTextField();
        field.setLayout(null);
        PromptSupport.setPrompt("  Search for a file to restore...", field);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.SHOW_PROMPT, field);
        field.setBounds(2, 2, 301, 26);
        field.setBorder(new LineBorder(Color.lightGray));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.add(field);





        final JLabel noDeleted = new JLabel("<html><b>No deleted files were found!</b><br><br>" +
                "<font color=\"gray\">If you think some file from the directory was<br>" +
                "deleted and it's not here, contact us at<br>" +
                "<a href=\"#\">www.protbox.com/support</a></font></html>");
        noDeleted.setLayout(null);
        noDeleted.setBounds(20, 50, 300, 300);
        noDeleted.setFont(new Font("Segoe UI", Font.PLAIN, 15));






        DefaultMutableTreeNode rootTreeNode = directory.buildDeletedTree();
        final JTree tree = new JTree(rootTreeNode);
        if(rootTreeNode.getChildCount()==0){
            field.setEnabled(false);
            this.add(noDeleted);
        }
        this.expandTree(tree);
        tree.setLayout(null);
//        tree.setBounds(10, 30, 340, 350);
        tree.setRootVisible(false);
        tree.setEditable(false);
        tree.setCellRenderer(new RestoreCellRenderer(field));
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // update and expand tree
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.nodeStructureChanged((TreeNode)model.getRoot());
                expandTree(tree);
            }
        });
        final JScrollPane scroll = new JScrollPane(tree,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(new DropShadowBorder());
        scroll.setBorder(new LineBorder(Color.lightGray));
        scroll.setBounds(2, 30, 334, 360);
        this.add(scroll);

        JLabel close = new JLabel(new ImageIcon(Constants.ASSETS.get("close.png")));
        close.setLayout(null);
        close.setBounds(312, 7, 18, 18);
        close.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        close.setForeground(Color.gray);
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });
        this.add(close);


        final JLabel permanent = new JLabel(new ImageIcon(Constants.ASSETS.get("permanent.png")));
        permanent.setLayout(null);
        permanent.setBounds(91, 390, 154, 39);
        permanent.setBackground(Color.black);
        permanent.setEnabled(false);
        permanent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (permanent.isEnabled()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    Pair entry = (Pair) node.getUserObject();

                    directory.permanentDelete(entry);
                    dispose();
                    Restore.getInstance(directory);
                }
            }
        });
        this.add(permanent);



        final JLabel action = new JLabel(new ImageIcon(Constants.ASSETS.get("restore.png")));
        action.setLayout(null);
        action.setBounds(3, 390, 85, 39);
        action.setBackground(Color.black);
        action.setEnabled(false);
        action.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (action.isEnabled()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    Pair entry = (Pair) node.getUserObject();
                    try {
                        directory.showEntry(entry);
                        dispose();
                    } catch (IOException ex) {
                        logger.error(ex.toString());
                    }
                }
            }
        });
        this.add(action);


        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node != null) {
                    Pair entry = (Pair) node.getUserObject();
                    if (entry.isHidden()){
                        permanent.setEnabled(true);
                        action.setEnabled(true);
                    }
                    else{
                        permanent.setEnabled(false);
                        action.setEnabled(false);
                    }
                }
            }
        });


        final JLabel cancel = new JLabel(new ImageIcon(Constants.ASSETS.get("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(229, 390, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });
        this.add(cancel);

        this.addWindowFocusListener(new WindowFocusListener() {
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
//                if (SwingUtilities.isDescendingFrom(e.getOppositeWindow(), ClickAwayDialog.this)) {
//                    return;
//                }
//                ClickAwayDialog.this.setVisible(false);
            }
        });

        this.setSize(340, 432);
        this.setUndecorated(true);
//        this.getContentPane().setBackground(new Color(239, 240, 241));
        this.getContentPane().setBackground(Color.white);
        this.setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        AWTUtils.setComponentLocationOnCenter(this);
        this.setVisible(true);
    }

    private void expandTree(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);
    }

    @Override
    public void dispose(){
//        instances.put(directory, null);
        super.dispose();
    }

}
