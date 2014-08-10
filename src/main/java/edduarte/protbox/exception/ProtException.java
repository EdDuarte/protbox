
package edduarte.protbox.exception;

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
     * Constructor with protbox.exception.
     * @param e Associated protbox.exception.
     */
    public ProtException(final Exception e) {
        super(e);
    }

    /**
     * Constructor with message and throwable protbox.exception.
     * @param m Associated message.
     * @param t Associated throwable protbox.exception.
     */
    public ProtException(final String m, final Throwable t) {
        super(m, t);
    }
}
