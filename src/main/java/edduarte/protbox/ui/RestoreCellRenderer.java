package edduarte.protbox.ui;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.directory.Pair;
import edduarte.protbox.core.directory.PairFile;
import edduarte.protbox.core.directory.PairFolder;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class RestoreCellRenderer extends DefaultTreeCellRenderer {

    private final JTextField field;

    public RestoreCellRenderer(JTextField field){
        this.field = field;
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus){

        // processing search functionality getFirst and rendering later
        String search = field.getText();
        String query = value.toString();
        StringBuffer html = new StringBuffer("<html>");
        Matcher m = Pattern.compile(Pattern.quote(search)).matcher(query);
        while (m.find()){
            m.appendReplacement(html, "<b>" + m.group() + "</b>");
            m.appendTail(html).append("</html>");
        }
        super.getTreeCellRendererComponent(tree, html.toString(), sel, expanded, leaf, row, hasFocus);
        tree.setRowHeight(32);

        // rendering font and ASSETS
        Pair entry = (Pair)((DefaultMutableTreeNode)value).getUserObject();
        setFont(new Font("Segoe UI", Font.PLAIN, 13));


        if(entry instanceof PairFile){
            if(entry.isHidden()) {
                setIcon(new ImageIcon(Constants.getAsset("file.png"))); // image of deleted file
                setForeground(Color.gray);
            } else{
                setIcon(new ImageIcon(Constants.getAsset("file.png"))); // image of normal file
            }
        } else if(entry instanceof PairFolder){
            if(entry.isHidden()) {
                setIcon(new ImageIcon(Constants.getAsset("folder.png"))); // image of deleted file
                setForeground(Color.gray);
            } else{
                setIcon(new ImageIcon(Constants.getAsset("folder.png"))); // image of normal folder
            }
        }

        return this;
    }
}
