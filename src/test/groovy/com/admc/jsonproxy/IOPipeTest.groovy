package com.admc.jsonproxy

import spock.lang.Specification
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * These tests manipulate stdin and stdout to we can test the JSON I/O pipe
 */
class IOPipe extends Specification {
    static final int MAX_INPUT = 10240

    PrintStream origSystemOut = System.out
    InputStream origSystemIn = System.in
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    JsonSlurper slurper = new JsonSlurper()
    OutputStreamWriter writer
    InputStreamReader reader
    Service service
    char[] buffer = new char[MAX_INPUT];  // obviously only for synch. usage

    def readObj() {
        /* slurper.parse() sucks!  Blocks when there is good input
         * but writing end not closed; yet throws if writing end
         * is closed.
        def obj = slurper.parse reader */
        slurper.parseText String.valueOf(
          buffer, 0, reader.read(buffer))
    }

    def setup() {
        service = new Service()

        PipedInputStream pInputStream
        PipedOutputStream pOutputStream

        // Set up pipe service->test
        pInputStream = new PipedInputStream()
        pOutputStream = new PipedOutputStream(pInputStream)
        System.setOut new PrintStream(pOutputStream, true, 'UTF-8') //svc.output
        reader = new InputStreamReader(pInputStream, 'UTF-8')

        // Set up pipe test->service
        pInputStream = new PipedInputStream()
        pOutputStream = new PipedOutputStream(pInputStream)
        System.setIn pInputStream  // sets input for Service, BEFORE .serve()
        writer = new OutputStreamWriter(pOutputStream, 'UTF-8')

        service.serve()  // serve, with System.in + System.out set above
    }

    def cleanup() {
        writer.close()
        System.setOut origSystemOut
        System.setIn origSystemIn
        println 'stdin/stdout restored'
        writer = null
        reader = null
        service = null
    }

    def cleanupSpec() {
        println 'You should see this stdout, proving that System.out is restored'
    }

    def "instantiate"() {
        when:
        writer.write JsonOutput.toJson([
            op: 'instantiate',
            key: 'key1',
            'class': 'java.util.Date',
            params: [123456789]
        ])
        writer.flush()
        def obj = readObj()

        then:
        service.size() == 1
        obj == null
        service.contains 'key1', 'java.util.Date'
    }

    def "remove+size"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'instantiate',
            key: 'key1',
            'class': 'java.util.Date',
            params: [123456789]
        ]);
        writer.flush()
        obj = readObj()

        then:
        service.size() == 1
        obj == null
        service.contains 'key1', 'java.util.Date'

        when:
        writer.write JsonOutput.toJson([ op: 'size' ]);
        writer.flush()
        obj = readObj()

        then:
        obj == 1

        when:
        writer.write JsonOutput.toJson([
            op: 'remove',
            key: 'key1',
            'class': 'java.util.Date',
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null
        service.size() == 0

        when:
        writer.write JsonOutput.toJson([ op: 'size' ]);
        writer.flush()
        obj = readObj()

        then:
        obj == 0
    }

    def "staticCall"() {
        when:
        writer.write JsonOutput.toJson([
            op: 'staticCall',
            'class': 'java.lang.String',
            params: ['format', '<%s> (%d)', ['werd', 345]]
        ]);
        writer.flush()
        def obj = readObj()

        then:
        service.size() == 0
        obj == '<werd> (345)'
    }

    def "call+contains"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'contains',
            key: 'key1',
            'class': 'java.lang.String',
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == false

        then:
        writer.write JsonOutput.toJson([
            op: 'instantiate',
            key: 'key1',
            'class': 'java.lang.String',
            params: ['input str']
        ])
        writer.flush()

        when:
        obj = readObj()

        then:
        service.size() == 1
        obj == null
        service.contains 'key1', 'java.lang.String'

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'key1',
            params: ['substring', 3, 7],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == 'ut s'

        when:
        writer.write JsonOutput.toJson([
            op: 'contains',
            key: 'key1',
            'class': 'java.lang.String',
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == true
    }
}
