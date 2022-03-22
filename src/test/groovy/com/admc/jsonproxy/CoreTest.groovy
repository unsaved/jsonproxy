package com.admc.jsonproxy

import spock.lang.Specification

/**
 * These are tests without using Service.run or the JSON I/O pipe
 */
class CoreTest extends Specification {
    private Service service

    def setup() {
        service = new Service()
    }

    def cleanup() {
        service = null
    }

    def "instantiate"() {
        when:
        final String constRet =
          service.instantiate('key1', 'java.lang.String', 'input str')
        final int newSize = service.size()

        then:
        newSize == 1
        service.get('key1') == 'input str'
    }

    def "staticCall"() {
        when:
        final String callRet =
          service.staticCall('java.lang.String',
            'format', '<%s> (%d)', ['werd', 345] as Object[])
        final int newSize = service.size()

        then:
        newSize == 0
        callRet == '<werd> (345)'
    }
}
