package edduarte.protbox.ui.window;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.User;
import edduarte.protbox.ui.listeners.OnMouseClick;
import edduarte.protbox.utils.Ref;
import edduarte.protbox.utils.Utils;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class UserListWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(UserListWindow.class);

    private final boolean askingPermission;
    Ref.Single<User> result;

    private UserListWindow(final String sharedFolderName, final List<User> userList, boolean askingPermission) {
        super(sharedFolderName + "'s users - Protbox");
        this.setIconImage(Constants.getAsset("box.png"));
        this.askingPermission = askingPermission;
        this.setLayout(null);

        JLabel close = new JLabel(new ImageIcon(Constants.getAsset("close.png")));
        close.setLayout(null);
        close.setBounds(472, 7, 18, 18);
        close.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
        close.setForeground(Color.gray);
        close.addMouseListener((OnMouseClick) e -> dispose());
        this.add(close);


        final JLabel action = new JLabel(new ImageIcon(Constants.getAsset("ask.png")));


        final JList<User> jList = new JList<>();
        jList.setListData(userList.toArray(new User[0]));

        final int[] over = new int[1];
        jList.addMouseListener(new OnMouseClick() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (jList.getSelectedValue() != null) {
                    action.setEnabled(true);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                over[0] = jList.locationToIndex(e.getPoint());
                jList.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                over[0] = -1;
                jList.repaint();
            }
        });
        jList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                over[0] = jList.locationToIndex(e.getPoint());
                jList.repaint();
            }
        });
        jList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
                    final JList<?> list,
                    Object value,
                    final int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (over[0] == index) {
                    if (index != list.getSelectedIndex())
                        label.setBackground(new Color(239, 240, 241));
                }

                list.setFixedCellHeight(32);

                User user = ((User) value);
                setFont(new Font(Constants.FONT, Font.PLAIN, 13));

                JLabel machineName = new JLabel();
                machineName.setText("Machine Name: " + user.getMachineName());
                machineName.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
                machineName.setBounds(106, 90, 370, 50);
                add(machineName);

                return label;
            }
        });

        if (!askingPermission) {
            jList.setSelectionModel(new DefaultListSelectionModel() {
                @Override
                public void setSelectionInterval(int index0, int index1) {
                    super.setSelectionInterval(-1, -1);
                }
            });
        }


        final JScrollPane scroll = new JScrollPane(jList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(new DropShadowBorder());
        scroll.setBorder(new LineBorder(Color.lightGray));
        if (askingPermission)
            scroll.setBounds(2, 100, 494, 360);
        else
            scroll.setBounds(2, 30, 494, 360);
        add(scroll);

        if (askingPermission) {
            JXLabel info = new JXLabel("<html><b>You will need to ask for permission in order to access this folder's contents.</b><br>" +
                    "Please choose from the list below which user from this folder do you wish to ask permission for access:</html>");
            info.setLineWrap(true);
            info.setFont(new Font(Constants.FONT, Font.PLAIN, 13));
            info.setBounds(10, 25, 470, 70);
            add(info);

            action.setLayout(null);
            action.setBounds(120, 465, 180, 39);
            action.setBackground(Color.black);
            action.setEnabled(false);
            action.addMouseListener((OnMouseClick) e -> {
                if (action.isEnabled()) {
                    User selectedUser = jList.getSelectedValue();
                        result = Ref.of1(selectedUser);
                        dispose();
                }
            });
            add(action);

            final JLabel cancel = new JLabel(new ImageIcon(Constants.getAsset("cancel.png")));
            cancel.setLayout(null);
            cancel.setBounds(290, 465, 122, 39);
            cancel.setBackground(Color.black);
            cancel.addMouseListener((OnMouseClick) e -> dispose());
            add(cancel);
        }


//        this.addWindowFocusListener(new WindowFocusListener() {
//            private boolean gained = false;
//
//            @Override
//            public void windowGainedFocus(WindowEvent e) {
//                gained = true;
//            }
//
//            @Override
//            public void windowLostFocus(WindowEvent e) {
//                if (gained) {
//                    dispose();
//                }
////                if (SwingUtilities.isDescendingFrom(e.getOppositeWindow(), ClickAwayDialog.this)) {
////                    return;
////                }
////                ClickAwayDialog.this.setVisible(false);
//            }
//        });

        if (askingPermission) {
            setSize(500, 512);
        } else {
            setSize(500, 400);
        }

        setUndecorated(true);
        getContentPane().setBackground(Color.white);
        setBackground(Color.white);
        setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setVisible(true);
    }


    public static UserListWindow getInstance(final String sharedFolderName, final List<User> userList, final boolean askingPermission) {
        return new UserListWindow(sharedFolderName, userList, askingPermission);
    }


    private void expandTree(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }


    @Override
    public void dispose() {
        if (askingPermission && result == null) {
            if (JOptionPane.showConfirmDialog(
                    UserListWindow.this, "Are you sure you wish to cancel? You need " +
                            "to ask for permission in order to access this folder!\n",
                    "Confirm Cancel",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                result = Ref.of1(null);
                super.dispose();
            }
        } else {
            super.dispose();
        }
    }
}
