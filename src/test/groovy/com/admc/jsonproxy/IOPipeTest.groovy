package com.admc.jsonproxy

import spock.lang.Specification
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * These tests manipulate stdin and stdout to we can test the JSON I/O pipe
 */
class IOPipe extends Specification {
    @spock.lang.Shared PrintStream origSystemOut = System.out
    @spock.lang.Shared InputStream origSystemIn = System.in
    @spock.lang.Shared ByteArrayOutputStream baos = new ByteArrayOutputStream()
    @spock.lang.Shared JsonSlurper slurper = new JsonSlurper()

    def setup() {
        baos.reset()
        System.setOut(new PrintStream(baos, true, 'UTF-8'))
    }

    def cleanup() {
        System.setOut origSystemOut
        System.setIn origSystemIn
    }

    def cleanupSpec() {
        println 'You should see this stdout, proving that System.out is restored'
    }

    def "instantiate"() {
        setup:
        System.setIn(new ByteArrayInputStream(
          JsonOutput.toJson([
              op: 'instantiate',
              key: 'key1',
              'class': 'java.util.Date',
              params: [123456789]
          ]).getBytes('UTF-8')));
        // can't inst. unil after setIn + setOut
        final Service service = new Service()

        when:
        service.run()
        def obj = slurper.parseText baos.toString('UTF-8')

        then:
        service.size() == 1
        obj == null
        service.contains 'key1', 'java.util.Date'
    }

    def "remove"() {
        setup:
        System.setIn(new ByteArrayInputStream(
          JsonOutput.toJson([
              op: 'instantiate',
              key: 'key1',
              'class': 'java.util.Date',
              params: [123456789]
          ]).getBytes('UTF-8')));
        // can't inst. unil after setIn + setOut
        final Service service = new Service()

        when:
        service.run()
        def obj = slurper.parseText baos.toString('UTF-8')

        then:
        service.size() == 1
        obj == null
        service.contains 'key1', 'java.util.Date'

        when:
        Object rmVal = service.remove 'key1', 'java.util.Date'

        then:
        rmVal == null
        service.size() == 0
    }

    def "staticCall"() {
        setup:
        System.setIn(new ByteArrayInputStream(
          JsonOutput.toJson([
              op: 'staticCall',
              'class': 'java.lang.String',
              params: ['format', '<%s> (%d)', ['werd', 345]]
          ]).getBytes('UTF-8')));
        // can't inst. unil after setIn + setOut
        final Service service = new Service()

        when:
        service.run()
        def obj = slurper.parseText baos.toString('UTF-8')

        then:
        service.size() == 0
        obj == '<werd> (345)'
    }

    def "call"() {
        setup:
        System.setIn(new ByteArrayInputStream(
          JsonOutput.toJson([
              op: 'call',
              'class': 'java.lang.String',
              params: ['format', '<%s> (%d)', ['werd', 345]]
          ]).getBytes('UTF-8')));
        // can't inst. unil after setIn + setOut
        final Service service = new Service()

        when:
        service.run()
        def obj = slurper.parseText baos.toString('UTF-8')

        then:
        service.size() == 0
        obj == '<werd> (345)'
    }
}
