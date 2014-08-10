package edduarte.protbox.ui;


import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 *  Convenient listener to request focus on a component when a window
 *  of components is fully realized.
 *
 *  When a component is added to a realized Window, that will request focus by
 *  default, since the ancestorAdded event is fired immediately according to
 *  Java native architecture.
 *
 *  Setting this class as an AncestorListener for a certain JComponent will
 *  set that component to be forced focused once the AncestorEvent is generated,
 *  overriding other default focus from other generated components.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
class FocusOnDefaultListener implements AncestorListener {

    @Override
    public void ancestorAdded(AncestorEvent e){
        JComponent component = e.getComponent();
        component.requestFocusInWindow();
    }

    @Override
    public void ancestorMoved(AncestorEvent e){
        // Nothing here, not needed
    }

    @Override
    public void ancestorRemoved(AncestorEvent e){
        // Nothing here, not needed
    }
}
