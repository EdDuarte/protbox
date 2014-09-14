package edduarte.protbox.ui.listeners;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * The listener interface for receiving keyboard events (keystrokes).
 * The class that is interested in processing a keyboard event
 * either implements this interface (and all the methods it
 * contains) or extends the abstract <code>KeyAdapter</code> class
 * (overriding only the methods of interest).
 * <P>
 * The listener object created from that class is then registered with a
 * component using the component's <code>addKeyListener</code>
 * method. A keyboard event is generated when a key is pressed, released,
 * or typed. The relevant method in the listener
 * object is then invoked, and the <code>KeyEvent</code> is passed to it.
 *
 * @see java.awt.event.KeyAdapter
 * @see java.awt.event.KeyEvent
 * @see <a href="http://java.sun.com/docs/books/tutorial/post1.0/ui/keylistener.html">Tutorial: Writing a Key Listener</a>
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
@FunctionalInterface
public interface OnKeyReleased extends KeyListener {

    /**
     * Invoked when a key has been typed.
     * See the class description for {@link java.awt.event.KeyEvent} for a definition of
     * a key typed event.
     */
    default public void keyTyped(KeyEvent e){
    }

    /**
     * Invoked when a key has been pressed.
     * See the class description for {@link KeyEvent} for a definition of
     * a key pressed event.
     */
    default public void keyPressed(KeyEvent e){
    }

    /**
     * Invoked when a key has been released.
     * See the class description for {@link KeyEvent} for a definition of
     * a key released event.
     */
    public void keyReleased(KeyEvent e);
}
