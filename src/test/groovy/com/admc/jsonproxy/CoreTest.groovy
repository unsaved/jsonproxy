package com.admc.jsonproxy

import spock.lang.Specification

class CoreTest extends Specification {
    @spock.lang.Shared PrintStream origSystemOut = System.out
    @spock.lang.Shared InputStream origSystemIn = System.in

    def cleanup() {
        System.setOut origSystemOut
        System.setIn origSystemIn
    }

    def "sanity"() {
        setup:
        System.setIn(new ByteArrayInputStream('"a string"'.getBytes("UTF-8")));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream()
        System.setOut(new PrintStream(baos, true, "UTF-8"))
        def service = new Service()

        when:
        service.run()
        System.err.println baos.toString("UTF-8")
        true

        then:
        service.size() == 1
    }

    def "io restoration check"() {
        when:
        println 'You should see this stdout'
        true

        then:
        true
    }
}
