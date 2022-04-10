package com.admc.jsonproxy

import spock.lang.Specification

class ParamReqsTest extends Specification {
    def "generation"(final String methName, final String prString) {
        expect:
        prString == new ParamReqs(ParamReqs.staticSingleParams(
          'com.admc.jsonproxy.SampleMethods', methName)).toString()
        where:
        methName | prString
        'o' | 'java.lang.Object'
        'S' | 'java.lang.String'
        'R' | 'java.lang.Runnable'
        'T' | 'java.util.List\n  java.lang.Object'
        'i' | 'int'
        'l' | 'long'
        'I' | 'java.lang.Integer'
        'L' | 'java.lang.Long'
        'aS' | 'java.lang.String[1]'
        'ai' | 'int[1]'
        'al' | 'long[1]'
        'af' | 'float[1]'
        'ad' | 'double[1]'
        'ay' | 'byte[1]'
        'ab' | 'boolean[1]'
        'ah' | 'short[1]'
        'ac' | 'char[1]'
        'aR' | 'java.lang.Runnable[1]'
        'TTS' | 'java.util.List\n  java.util.List\n    java.lang.String'
        'TaS' | 'java.util.List\n  java.lang.String[1]'
        'TS' | 'java.util.List\n  java.lang.String'
        'TR' | 'java.util.List\n  java.lang.Runnable'
        'Tai' | 'java.util.List\n  int[1]'
        'aTS' | '<ARRAY>[1]\n  java.util.List\n    java.lang.String'
        'aaS' | 'java.lang.String[2]'
        'aai' | 'int[2]'
        'aTI' | '<ARRAY>[1]\n  java.util.List\n    java.lang.Integer'
        'TTTaaaTTI' | 'java.util.List\n  java.util.List\n    java.util.List\n      <ARRAY>[3]\n        java.util.List\n          java.util.List\n            java.lang.Integer'
        'TTTaaaTTaai' | 'java.util.List\n  java.util.List\n    java.util.List\n      <ARRAY>[3]\n        java.util.List\n          java.util.List\n            int[2]'
    }
}
