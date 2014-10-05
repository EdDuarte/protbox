package edduarte.protbox.utils;

/**
 * A simple asynchronous object callback that provides an easy bridge between
 * one class and another.
 * <p>
 * If one object T at class B is to be returned to a class A, then the class A
 * must send an implementation of the Callback interface into the class B so that
 * when B calls the Callback method with the object T, A will receive
 * it and have complete access. As long as B does not call the Callback method,
 * class A will continue to run its tasks while waiting for T.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
@FunctionalInterface
public interface Callback<T> {

    /**
     * This method must be implemented to contain instructions of what must
     * be done to the object returned by the class that received this Callback.
     *
     * @param result the returned object
     */
    void onResult(final T result);

}
