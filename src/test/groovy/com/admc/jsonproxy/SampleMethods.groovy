package com.admc.jsonproxy

import java.util.logging.Level

/**
 * N.b. Groovy, unlike Java, does not enforce agreement between
 * calling param's Generic spec and the method param's Generic spec.
 * (Even with Groovy v.4).
 * E.g. Groovy does not catch conflict in:
 *    static void meth(List<Integer> li) {..
 *    ...
 *    List<Object> lo = new ArrayList<>();
 *    ...
 *    meth lo
 *
 * Abbreviations:
 *   S string
 *   i int
 *   l long
 *   I Integer
 *   L Long
 *   a array
 *   T list
 *
 * We need to test primitive and non-primitive on the Method param side,
 * but we don't need to test any primitive invocation-side param values,
 * since our JsonSlurper will never provide a primitive param value.
 *
 * For now there are only samples for single-parameter methods, static
 * methods, and Methods (no Constructors) since this allows us to most
 * conveniently test everything that needs to be tested.
 *
 * (A few multi-param method samples are commented out.  They can be
 * enabled and expanded if there turns out to be a need).
 */
 @groovy.util.logging.Log(value = 'logger')
class SampleMethods {
    // N.b. can't have a Tx where x is a primitive type like i or y,
    // because Collections can't have primitives as members.
    /*  
    static void iI(int i, Integer I) {}
    static void LLL(Long L1, Long L2, Long L3) {}
    // No need for varargs since equivalent to array unless you call
    // Parameter/Method/Constructor.isVarArgs.
    //static void IvS(Integer, String... strings) {}
    static void LaS(Long L, String[] strings) {}
    static void lTS(long l, List<String> strings) {}
    */

    // MONSTER Nestings
    static void TTTaaaTTI(List<List<List<List<List<Integer>>[][][]>>> stuf) {
        logger.fine(stuf == null ? '<NULL>'
          : "${stuf.size()} List<List>: $stuf")
    }
    static void TTTaaaTTaai(List<List<List<List<List<int[][]>>[][][]>>> stuf) {
        logger.fine(stuf == null ? '<NULL>'
          : "${stuf.size()} List<List>: $stuf")
    }

    static void TTS(List<List<String>> stuf) {
        logger.fine(stuf == null ? '<NULL>'
          : "${stuf.size()} List<List>: $stuf")
    }
    static void TaS(List<String[]> stuf) {
        logger.fine(stuf == null ? '<NULL>' : "${stuf.size()} List[]: $stuf")
    }
    static void TS(List<String> strings) {
        logger.fine(strings == null ? '<NULL>'
          : "${strings.size()} List: $strings")
    }
    static void Tai(List<int[]> stuf) {
        logger.fine(stuf == null ? '<NULL>'
          : "${stuf.size()} List<int[]>: $stuf")
    }

    static void aTS(List<String>[] stuf) {
        logger.fine(stuf == null ? '<NULL>' : "${stuf.size()} List[]: $stuf")
    }
    static void aaS(String[][] stuf) {
        logger.fine(stuf == null ? '<NULL>'
          : "${stuf.size()} String[][]: $stuf")
    }
    static void aai(int[][] stuf) {
        logger.fine(stuf == null ? '<NULL>' : "${stuf.size()} init[][]: $stuf")
    }
    static void aTI(List<Integer>[] stuf) {
        logger.fine(stuf == null ? '<NULL>' : "${stuf.size()} List[]: $stuf")
    }
    static void aS(String[] strings) {
        logger.fine(strings == null ? '<NULL>'
          : "${strings.size()} array: $strings")
    }
    static void ai(int[] ints) {
        logger.fine(ints == null ? '<NULL>' : "${ints.size()} array: $ints")
    }
    static void al(long[] longs) {
        logger.fine(longs == null ? '<NULL>' : "${longs.size()} array: $longs")
    }
    static void af(float[] floats) {
        logger.fine(floats == null ? '<NULL>'
          : "${floats.size()} array: $floats")
    }
    static void ad(double[] doubles) {
        logger.fine(doubles == null ? '<NULL>'
          : "${doubles.size()} array: $doubles")
    }
    static void ay(byte[] bytes) {
        logger.fine(bytes == null ? '<NULL>' : "${bytes.size()} array: $bytes")
    }
    static void ab(boolean[] booleans) {
        logger.fine(booleans == null ? '<NULL>'
          : "${booleans.size()} array: $booleans")
    }
    static void ah(short[] shorts) {
        logger.fine(shorts == null ? '<NULL>'
          : "${shorts.size()} array: $shorts")
    }
    static void ac(char[] chars) {
        logger.fine(chars == null ? '<NULL>' : "${chars.size()} array: $chars")
    }

    // wrappers
    static void I(Integer i) {
        logger.fine(i == null ? '<NULL>' : i.toString())
    }
    static void L(Long l) {
        logger.fine(l == null ? '<NULL>' : l.toString())
    }

    // primitives
    // Though primitive-param function bodies will always get a wrapper
    // value instead of a primitive value, Groovy does enforce that it
    // can't be called with null, and therefore body will not get a
    // null primitive value
    static void i(int i) { logger.fine i.toString() }
    static void l(long l) { logger.fine l.toString() }

    static void o(Object o) { logger.fine "isa ${o.getClass().name}: $o" }
    static void S(String S) { logger.fine S }
    static void T(List list) {
        logger.fine(list == null ? '<NULL>' : "${list.size()} List: $list")
    }
}
