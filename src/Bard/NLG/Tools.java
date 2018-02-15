/**
 * This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 *
 * @author Dr. Matthieu Herrmann
 * Miscellaneous tools
 */

package Bard.NLG;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Tools {

    // --- --- --- Debug flag
    public static final boolean DEBUG = true;

    // --- --- --- Logger

    /**
     * Output Stream for the logger
     */
    public static PrintStream outStream = System.out;

    /**
     * Logger flag.
     */
    public static boolean loggerFlag = false;

    /**
     * Turn on the logger on the standard output stream
     */
    public static void loggerOn() {
        outStream = System.out;
        loggerFlag = true;
    }

    /**
     * Turn on the logger on a Specific output stream
     */
    public static void loggerOn(PrintStream os) {
        outStream = os;
        loggerFlag = true;
    }

    /**
     * Turn off the logger.
     */
    public static void loggerOff() {
        loggerFlag = false;
    }

    /**
     * Log a message/object.
     */
    public static void log(Object o) {
        if (loggerFlag) {
            if (o != null) {
                outStream.println(o);
            } else {
                outStream.println("**log(): called with a null object*");
            }
            outStream.flush();
        }
    }

    /**
     * Log a message/object as an error.
     */
    public static void logError(Object o) {
        if (loggerFlag) {
            if (o != null) {
                outStream.println("Error: " + o);
            } else {
                outStream.println("**logError(): called a null object!**");
            }
            System.exit(1);
        }
    }

    /**
     * To be put in cases we think should not happen.
     * Print a message, the stack trace and exit the program.
     */
    public static void shouldNotHappen(Object o) {
        if (o != null) {
            outStream.println("SHOULD NOT HAPPEN: " + o);
        } else {
            outStream.println("SHOULD NOT HAPPEN: called with null object");
        }
        outStream.println("A situation that was believed not to happen... happened. There is a bug in our program, please contact us!");
        (new RuntimeException()).printStackTrace();
        System.exit(2);
    }


    // --- --- --- Convenience Freezing methods

    /**
     * Obtain a frozen copy of a map.
     */
    public static <K, V> Map<K, V> freeze(Map<K, V> map) {
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

    /**
     * Obtain a frozen copy of a map mapping to Set values. Set values are also frozen.
     */
    public static <K, V> Map<K, Set<V>> freezeMapSet(Map<K, Set<V>> map) {
        return Collections.unmodifiableMap(map.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, kv -> Collections.unmodifiableSet(kv.getValue()))
        ));
    }

    /**
     * Obtain a frozen copy of a set.
     */
    public static <T> Set<T> freeze(Set<T> set) {
        return Collections.unmodifiableSet(new HashSet<>(set));
    }

    /**
     * Obtain a frozen copy of a list.
     */

    public static <T> List<T> freeze(List<T> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }


    // --- --- --- Either Type
    public static class Either<A, B> {

        private A left = null;
        private B right = null;

        private Either(A a, B b) {
            left = a;
            right = b;
        }

        public static <A, B> Either<A, B> left(A a) {
            return new Either<A, B>(a, null);
        }

        public static <A, B> Either<A, B> right(B b) {
            return new Either<A, B>(null, b);
        }

        /** Working with consumer: i.e. by side effects */
        public void accept(Consumer<A> ifLeft, Consumer<B> ifRight) {
            if (right == null) {
                ifLeft.accept(left);
            } else {
                ifRight.accept(right);
            }
        }

        /** Mapping */
        public <T> T map(Function<A, T> ifLeft, Function<B, T> ifRight) {
            if (right == null) {
                return ifLeft.apply(left);
            } else {
                return ifRight.apply(right);
            }
        }

    }


}
