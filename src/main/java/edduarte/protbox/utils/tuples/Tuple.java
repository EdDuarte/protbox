package edduarte.protbox.utils.tuples;

/**
 * @author Ed Duarte (<a href="mailto:eduarte@ubiwhere.com">eduarte@ubiwhere.com</a>)
 * @version 1.0
 */
public abstract class Tuple {

    protected static boolean areAllSameType(Object... objects) {
        Class<?> firstClass = null;

        for (Object o : objects) {
            if (o != null) {
                if (firstClass == null) {
                    firstClass = o.getClass();

                } else if (firstClass != o.getClass()) {
                    return false;
                }
            }
        }
        return true;
    }

}
