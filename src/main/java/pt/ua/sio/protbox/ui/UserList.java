package pt.ua.sio.protbox.ui;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.slf4j.LoggerFactory;
import pt.ua.sio.protbox.core.Constants;
import pt.ua.sio.protbox.core.User;
import pt.ua.sio.protbox.util.AWTUtils;
import pt.ua.sio.protbox.util.Uno;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class UserList extends JFrame {
    private transient static org.slf4j.Logger logger = LoggerFactory.getLogger(UserList.class);

//    private Directory directory;
//    private static Map<Directory, UserList> instances = new HashMap<>();

    Uno<User> result;
    private final boolean askingPermission;

    public static UserList getInstance(final String sharedFolderName, final List<User> userList, final boolean askingPermission) {
        return new UserList(sharedFolderName, userList, askingPermission);
    }

    private UserList(final String sharedFolderName, final List<User> userList, boolean askingPermission) {
        super(sharedFolderName+"'s users - Protbox");
        this.setIconImage(Constants.ASSETS.get("box.png"));
        this.askingPermission = askingPermission;
        this.setLayout(null);

        JLabel close = new JLabel(new ImageIcon(Constants.ASSETS.get("close.png")));
        close.setLayout(null);
        close.setBounds(472, 7, 18, 18);
        close.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        close.setForeground(Color.gray);
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });
        this.add(close);


        final JLabel action = new JLabel(new ImageIcon(Constants.ASSETS.get("ask.png")));


        final JList<User> jList = new JList<>();
        jList.setListData(userList.toArray(new User[0]));

        final int[] over = new int[1];
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(jList.getSelectedValue()!=null){
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
                setFont(new Font("Segoe UI", Font.PLAIN, 13));
                ImageIcon imageIcon = user.getPhoto();
                setIcon(imageIcon);

                JLabel machineName = new JLabel();
                machineName.setText("Machine Name: "+user.getMachineName());
                machineName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                machineName.setBounds(106, 90, 370, 50);
                add(machineName);


                JLabel availableIcon = new JLabel();
                availableIcon.setBounds(450, 44, 50, 50);
                if(user.isAvailable()){
                    availableIcon.setIcon(new ImageIcon(Constants.ASSETS.get("status_online.png")));
                    availableIcon.setToolTipText("Available");
                }
                else{
                    availableIcon.setIcon(new ImageIcon(Constants.ASSETS.get("status_offline.png")));
                    availableIcon.setToolTipText("Unavailable");
                }
                add(availableIcon);

                return label;
            }
        });

        if(!askingPermission){
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
        if(askingPermission)
            scroll.setBounds(2, 100, 494, 360);
        else
            scroll.setBounds(2, 30, 494, 360);
        this.add(scroll);

        if(askingPermission){
            JXLabel info = new JXLabel("<html><b>You will need to ask for permission in order to access this folder's contents.</b><br>" +
                    "Please choose from the list below which user from this folder do you wish to ask permission for access:</html>");
            info.setLineWrap(true);
            info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            info.setBounds(10, 25, 470, 70);
            add(info);

            action.setLayout(null);
            action.setBounds(120, 465, 180, 39);
            action.setBackground(Color.black);
            action.setEnabled(false);
            action.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if(action.isEnabled()){
                        User selectedUser = jList.getSelectedValue();
                        if(!selectedUser.isAvailable()) { // Not available
                            JOptionPane.showMessageDialog(UserList.this, "The requested user is currently " +
                                    "not available. Please try again later or choose another user.");
                        } else {
                            result = new Uno<>(selectedUser);
                            dispose();
                        }
                    }
                }
            });
            this.add(action);

            final JLabel cancel = new JLabel(new ImageIcon(Constants.ASSETS.get("cancel.png")));
            cancel.setLayout(null);
            cancel.setBounds(290, 465, 122, 39);
            cancel.setBackground(Color.black);
            cancel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    dispose();
                }
            });
            this.add(cancel);
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

        if(askingPermission)
            this.setSize(500, 512);
        else
            this.setSize(500, 400);

        this.setUndecorated(true);
        this.getContentPane().setBackground(Color.white);
        this.setBackground(Color.white);
        this.setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        AWTUtils.setComponentLocationOnCenter(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);
    }

    private void expandTree(JTree tree) {

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    @Override
    public void dispose(){
        if(askingPermission && result==null){
            if (JOptionPane.showConfirmDialog(
                    UserList.this, "Are you sure you wish to cancel? You need " +
                    "to ask for permission in order to access this folder!\n",
                    "Confirm Cancel",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                result = new Uno<>(null);
                super.dispose();
            }
        } else{
            super.dispose();
        }
    }
}
