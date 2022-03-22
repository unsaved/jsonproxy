package com.admc.jsonproxy

import spock.lang.Specification

class CoreTest extends Specification {
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

    def "sanity2"() {
        setup:
        System.setIn(new ByteArrayInputStream('"another string"'.getBytes('UTF-8')));
        // can't inst. unil after setIn + setOut
        final Service service = new Service()

        when:
        service.run()
        System.err.println baos.toString('UTF-8')

        then:
        service.size() == 1
        baos.toString().startsWith('Got 16 bytes')
    }

    def cleanupSpec() {
        println 'You should see this stdout, proving that System.out is restored'
    }
}
