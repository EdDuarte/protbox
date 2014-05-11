
package pt.ua.sio.protbox.exception;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class ProtException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor with message.
     * @param m Associated message.
     */
    public ProtException(final String m) {
        super(m);
    }

    /**
     * Constructor with pt.ua.sio.protbox.exception.
     * @param e Associated pt.ua.sio.protbox.exception.
     */
    public ProtException(final Exception e) {
        super(e);
    }

    /**
     * Constructor with message and throwable pt.ua.sio.protbox.exception.
     * @param m Associated message.
     * @param t Associated throwable pt.ua.sio.protbox.exception.
     */
    public ProtException(final String m, final Throwable t) {
        super(m, t);
    }
}
