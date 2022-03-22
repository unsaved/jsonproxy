package com.admc.jsonproxy

import spock.lang.Specification

/**
 * These tests manipulate stdin and stdout to we can test the JSON I/O pipe
 */
class JsonTest extends Specification {
    @spock.lang.Shared PrintStream origSystemOut = System.out
    @spock.lang.Shared InputStream origSystemIn = System.in
    @spock.lang.Shared ByteArrayOutputStream baos = new ByteArrayOutputStream()

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

    def "sanity"() {
        setup:
        System.setIn(new ByteArrayInputStream('"a string"'.getBytes('UTF-8')));
        // can't inst. unil after setIn + setOut
        final Service service = new Service()

        when:
        service.run()
        System.err.println baos.toString('UTF-8')

        then:
        service.size() == 1
        baos.toString().startsWith('Got 10 bytes')
    }
}
