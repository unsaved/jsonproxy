package com.admc.jsonproxy

import spock.lang.Specification

import groovy.json.JsonSlurper

class AnnotatedPVTest extends Specification {
    static private JsonSlurper slurper = new JsonSlurper()
    
    static private String jsonFileToAPVString(final String baseName) {
        new AnnotatedPV(
          slurper.parseText(new File("src/test/data/${baseName}").text)).
          toString()
    }

    def "generation"(final String expectAPV, final String inputFile) {
        expect:
        jsonFileToAPVString(inputFile) == expectAPV
        where:
        expectAPV | inputFile
        'class org.apache.groovy.json.internal.LazyMap [2] coll  F nulls -> class java.lang.Integer [5] map  T nulls -> null' | '2listMaps.json'
        'class java.util.ArrayList [2] map  F nulls -> class java.lang.Object [8] coll  F nulls -> null' | '2mapLists.json'
        'class java.util.ArrayList [3] map  T nulls -> class java.lang.Object [9] coll  T nulls -> null' | '2mapListsNull.json'
        'class org.apache.groovy.json.internal.LazyMap [2] map  F nulls -> class java.lang.Integer [5] map  T nulls -> null' | '2mapMaps.json'
        'class java.util.ArrayList [2] map  F nulls -> class java.util.ArrayList [4] coll  F nulls -> class java.lang.Integer [12] coll  F nulls -> null' | '2x3ImapLists.json'
        'class java.util.ArrayList [2] map  F nulls -> class java.util.ArrayList [4] coll  F nulls -> class java.lang.Object [12] coll  F nulls -> null' | '2x3mapLists.json'
        'class java.util.ArrayList [2] map  F nulls -> class java.util.ArrayList [6] coll  F nulls -> class java.util.ArrayList [12] coll  F nulls -> class java.lang.Integer [24] coll  F nulls -> null' | '3x2ImapLists.json'
        'class java.util.ArrayList [2] map  F nulls -> class java.util.ArrayList [6] coll  F nulls -> class java.util.ArrayList [12] coll  F nulls -> class java.lang.Integer [22] coll  F nulls -> null' | '3x2ImapListsMixed.json'
        'class java.lang.Object [4] map  T nulls -> null' | 'topMapObjNull.json'
    }
}
