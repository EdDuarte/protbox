package edduarte.protbox.ui.windows;

import edduarte.protbox.core.Constants;
import edduarte.protbox.ui.listeners.OnKeyReleased;
import edduarte.protbox.ui.listeners.OnMouseClick;
import edduarte.protbox.utils.Utils;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class ProviderListWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(ProviderListWindow.class);

    private Consumer<String> selectedProviderConsumer;
    private JList<String> jList;
    private JLabel action;
    private boolean providerWasSelected = false;

    private ProviderListWindow(final Vector<String> providerNames, final Consumer<String> selectedProviderConsumer) {
        super("Providers - Protbox");
        setIconImage(Constants.getAsset("box.png"));
        setLayout(null);

        this.selectedProviderConsumer = selectedProviderConsumer;


        JLabel close = new JLabel(new ImageIcon(Constants.getAsset("close.png")));
        close.setLayout(null);
        close.setBounds(472, 7, 18, 18);
        close.setFont(Constants.FONT);
        close.setForeground(Color.gray);
        close.addMouseListener((OnMouseClick) e -> dispose());
        add(close);

        action = new JLabel(new ImageIcon(Constants.getAsset("ok.png")));
        action.setLayout(null);
        action.setBounds(90, 465, 122, 39);
        action.setBackground(Color.black);
        action.addMouseListener((OnMouseClick) e -> onOkPressed());
        add(action);

        final JLabel cancel = new JLabel(new ImageIcon(Constants.getAsset("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(290, 465, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener((OnMouseClick) e -> dispose());
        add(cancel);

        jList = new JList<>();
        jList.setListData(providerNames);

        final int[] over = new int[1];
        jList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (jList.getSelectedValue() != null) {
                    action.setEnabled(true);
                } else {
                    action.setEnabled(false);
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

//                String providerName = ((String) value);
//                setFont(new Font(Constants.FONT, Font.PLAIN, 13));
//
//                JLabel machineName = new JLabel();
//                machineName.setText("Machine Name: " + user.getMachineName());
//                machineName.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
//                machineName.setBounds(106, 90, 370, 50);
//                add(machineName);

                return label;
            }
        });
        jList.addKeyListener((OnKeyReleased) e -> {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                onOkPressed();
            }
        });
        jList.setSelectedIndex(0);
        jList.requestFocus();


        final JScrollPane scroll = new JScrollPane(jList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(new DropShadowBorder());
        scroll.setBorder(new LineBorder(Color.lightGray));
        scroll.setBounds(2, 50, 494, 360);
        add(scroll);

        JXLabel info = new JXLabel("Choose which PKCS11 provider to use to load your eID token:");
        info.setLineWrap(true);
        info.setFont(Constants.FONT);
        info.setBounds(10, 10, 470, 40);
        add(info);

        setSize(500, 512);
        setUndecorated(true);
        getContentPane().setBackground(Color.white);
        setBackground(Color.white);
        setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setVisible(true);
    }


    public static ProviderListWindow showWindow(final Set<String> providerNames,
                                                final Consumer<String> selectedProviderConsumer) {
        return new ProviderListWindow(new Vector<>(providerNames), selectedProviderConsumer);
    }


    private void expandTree(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }


    @Override
    public void dispose() {
        if (!providerWasSelected) {
            if (JOptionPane.showConfirmDialog(
                    ProviderListWindow.this, "You need to choose a PKCS11 provider in order to use the application. " +
                            "Are you sure you want to cancel and close the application?\n",
                    "Confirm Close",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                super.dispose();
                System.exit(1);
            }
        } else {
            super.dispose();
        }
    }


    public void onOkPressed() {
        if (action.isEnabled()) {
            String selectedProviderName = jList.getSelectedValue();
            selectedProviderConsumer.accept(selectedProviderName);
            providerWasSelected = true;
            dispose();
        }
    }
}
