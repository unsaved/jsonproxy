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
          service.instantiate('key1', 'java.util.Date', 123456789L)

        then:
        service.size() == 1
        constRet == null
        service.get('key1').time == 123456789L
    }

    def "staticCall"() {
        when:
        final String callRet =
          service.staticCall('java.lang.String',
            'format', '<%s> (%d)', ['werd', 345] as Object[])

        then:
        service.size() == 0
        callRet == '<werd> (345)'
    }

    def "call"() {
        when:
        final String constRet =
          service.instantiate('key1', 'java.lang.String', 'input str')

        then:
        service.size() == 1
        constRet == null
        service.call('key1', 'substring', 3, 7) == 'ut s'
    }

    def "remove"() {
        when:
        final String constRet =
          service.instantiate('key1', 'java.lang.String', 'input str')

        then:
        service.size() == 1
        constRet == null
        service.get('key1') == 'input str'

        when:
        Object rmVal = service.remove 'key1', 'java.lang.String'

        then:
        rmVal == null
        service.size() == 0
    }
}
