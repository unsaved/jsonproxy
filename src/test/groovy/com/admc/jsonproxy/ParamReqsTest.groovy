package com.admc.jsonproxy

import spock.lang.Specification

class ParamReqsTest extends Specification {
    def "generation"(final String methName, final String prString) {
        expect:
        prString == new ParamReqs(ParamReqs.staticSingleParams(
          'com.admc.jsonproxy.SampleMethods', methName)).toString()
        where:
        methName | prString
        'S' | 'java.lang.String'
    }
}
