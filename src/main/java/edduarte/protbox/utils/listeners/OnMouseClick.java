/*
 * Copyright 2014 University of Aveiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edduarte.protbox.utils.listeners;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * The listener interface for receiving "interesting" mouse events
 * (press, release, click, enter, and exit) on a component.
 * (To track mouse moves and mouse drags, use the
 * <code>MouseMotionListener</code>.)
 * <p/>
 * The class that is interested in processing a mouse event
 * either implements this interface (and all the methods it
 * contains) or extends the abstract <code>MouseAdapter</code> class
 * (overriding only the methods of interest).
 * <p/>
 * The listener object created from that class is then registered with a
 * component using the component's <code>addMouseListener</code>
 * method. A mouse event is generated when the mouse is pressed, released
 * clicked (pressed and released). A mouse event is also generated when
 * the mouse cursor enters or leaves a component. When a mouse event
 * occurs, the relevant method in the listener object is invoked, and
 * the <code>MouseEvent</code> is passed to it.
 *
 * @author Eduardo Duarte (<a href="mailto:eduardo.miguel.duarte@gmail.com">eduardo.miguel.duarte@gmail.com</a>)
 * @see java.awt.event.MouseAdapter
 * @see MouseEvent
 * @see <a href="http://docs.oracle.com/javase/tutorial/uiswing/events/mouselistener.html">Tutorial: Writing a Mouse Listener</a>
 */
@FunctionalInterface
public interface OnMouseClick extends MouseListener {

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    @Override
    public void mouseClicked(MouseEvent e);

    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    @Override
    default public void mousePressed(MouseEvent e) {
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    @Override
    default public void mouseReleased(MouseEvent e) {
    }

    /**
     * Invoked when the mouse enters a component.
     */
    @Override
    default public void mouseEntered(MouseEvent e) {
    }

    /**
     * Invoked when the mouse exits a component.
     */
    @Override
    default public void mouseExited(MouseEvent e) {
    }
}
