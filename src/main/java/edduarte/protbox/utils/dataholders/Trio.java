package edduarte.protbox.utils.dataholders;

import java.io.Serializable;

/**
 * Container to ease passing around a tuple of three objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class Trio<T> extends Triple<T, T, T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for a triple of elements of the same type.
     *
     * @param first  the first object in the trio
     * @param second the second object in the trio
     * @param third  the second object in the trio
     */
    public Trio(T first, T second, T third) {
        super(first, second, third);
    }
}
