#!/usr/bin/env groovy

package com.admc.jsonproxy

import groovy.json.JsonSlurper
import java.util.logging.Level
import java.util.Collection

/**
 * This holds a collection parameter value with metadata.  Supports nesting.
 *
 * Whenever an AnnotatedPV has memberClass==Object:
 *   childAPV==null
 *   no validation can be done on any descendant members.
 *   members get no param-val conversion, data members go directly to params
 */
@groovy.util.logging.Log(value = 'logger')
class AnnotatedPV {
    private boolean anyNull
    private int size
    private AnnotatedPV childAPV
    private Class memberClass
    private boolean isMap

    String toString() {
        String.format '%s [%d] %s  %s nulls -> %s',
          memberClass, size, isMap ? 'map' : 'coll', anyNull?'T':'F', childAPV
    }

    static void main(String[] sa) {
        if (sa.size() !== 1)
            throw new IllegalArgumentException(
              'SYNTAX: java com.admc.jsonproxy.AnnotatedPV file.json')
        final Object obj = new JsonSlurper().parseText(new File(sa[0]).text)
        println "Input Object: $obj"
        println "Output AnnotatedPV: ${new AnnotatedPV(obj)}"
    }

    /**
     * @param dummy is only here to distinguish this constructor, since the
     *        other one has param Object which would also match AnnotatedPV.
     */
    private AnnotatedPV(AnnotatedPV src, boolean dummy) {
        anyNull = src.anyNull
        size = src.size
        memberClass = src.memberClass
        isMap = src.isMap
    }

    AnnotatedPV(Object inData) {
        Class datumClass
        List<AnnotatedPV> childrenAPVs = new ArrayList<>()
        Collection data

        if (AbstractMap.isInstance(inData)) {
            isMap = true
            if (null != inData.keySet().find() {
                it != null && it !instanceof String
            }) throw new IllegalArgumentException(
              'AnnotatedPV constructor called with non-String/null map key: '
              + it)
            data = inData.values()
        } else if (Collection.isInstance(inData)) {
            data = inData
        } else {
            throw new IllegalArgumentException(
              "AnnotatedPV constructor called with ${inData.getClass().name} "
              + '(neither a Collection nor a Map)')
        }
        size = data.size()
        data.each() { datum ->
            if (datum == null) { anyNull = true; return }
            datumClass = datum.getClass()
            if (memberClass == datumClass) {} // nothing to do
            else if (datumClass == Object.class) memberClass = Object.class
            else if (memberClass == null
              || datumClass.isAssignableFrom(memberClass))
                memberClass = datumClass
            else if (!memberClass.isInstance(datum)) {
                // Trickiest case.  2 different non-inter-assignable classes.
                // Need to see if they are precision-alternates or if
                // they have a common ancestor class.
                if (isHigherPrecision(memberClass, datumClass)) {}
                else if (isHigherPrecision(datumClass, memberClass))
                    memberClass = datumClass
                else while ((datumClass = datumClass.superclass) != null)
                    // This will always qualify once, Obj for totally unrelated:
                    if (datumClass.isAssignableFrom(memberClass)) {
                        memberClass = datumClass
                        break
                    }
            }
            assert memberClass != null
            if (AbstractMap.isInstance(datum) || Collection.isInstance(datum))
                childrenAPVs << new AnnotatedPV(datum)
        }
        logger.fine "Before childAPV gen., ${childrenAPVs.size()} children"
        // N.b. if memberClass is Object then we discard all childrenAPVs:
        if (childrenAPVs.size() < 1 || memberClass == Object.class) return
        childAPV = _mergeAPVs childrenAPVs
    }

    /**
     * First merges the .childAPV's for the given peers, then merges
     * the remaining attribures.
     *
     * @returns null if the peers are incompatible
     */
    static private AnnotatedPV _mergeAPVs(List<AnnotatedPV> peerAPVs) {
        // No damned reason why this should not work!!:
        List<AnnotatedPV> childAPVs = peerAPVs.findResults() { it.childAPV }
        AnnotatedPV newAPV = new AnnotatedPV(peerAPVs.remove(0), false)
        newAPV.childAPV = childAPVs.size() == 0 ? null : _mergeAPVs(childAPVs)
        (null == peerAPVs.find() {  otherAPV ->
            // return true to quit because incompatible
            newAPV.size += otherAPV.size
            newAPV.anyNull = newAPV.anyNull || otherAPV.anyNull
            if (otherAPV.size == 0) return false  // 0 elements
            if (newAPV.isMap != otherAPV.isMap) return true
            if (newAPV.memberClass == otherAPV.memberClass) {} // nothing todo
            else if (otherAPV.memberClass == Object.class)
                newAPV.memberClass = Object.class
            else if (newAPV.memberClass == null
              || otherAPV.memberClass.isAssignableFrom(newAPV.memberClass))
                newAPV.memberClass = otherAPV.memberClass
            else if (!newAPV.memberClass.isInstance(datum)) {
                // Trickiest case.  2 different non-inter-assignable classes.
                // Need to see if they are precision-alternates or if
                // they have a common ancestor class.
                if (isHigherPrecision(
                  newAPV.memberClass, otherAPV.memberClass)) {}
                else if (isHigherPrecision(
                  otherAPV.memberClass, newAPV.memberClass))
                    newAPV.memberClass = otherAPV.memberClass
                else while ((otherAPV.memberClass =
                  otherAPV.memberClass.superclass) != null)
                    // This will always qualify once, Obj for totally unrelated:
                    if (otherAPV.memberClass.isAssignableFrom(
                      newAPV.memberClass)) {
                        newAPV.memberClass = otherAPV.memberClass
                        break
                    }
            }
            false
        }) ? newAPV : null
    }

    /**
     * If the two classes differ and they are just different precision
     * alternatives, and the first has higher precision than the last
     * then true.
     *
     * As of today, I think that
     *    Integer vs. Long
     *    Float vs. Double
     * are the ony cases to handle, so that's all we're supporting.
     */
    static boolean isHigherPrecision(Class c1, Class c2) {
        if (c1 == c2) return null
        if (c1 == Long.class && c2 == Integer.class) return true
        if (c1 == Double.class && c2 == Float.class) return true
        false
    }
}
