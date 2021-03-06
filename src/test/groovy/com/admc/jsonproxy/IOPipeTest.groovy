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
        int i = reader.read buffer
        // N.b. reader.ready is useless and always returns false
        if (i < 1)
            throw new RuntimeException("Read $i bytes off of input pipe")
        if (i == buffer.length)
            throw new RuntimeException(
              'Input too large.  Increase IOPipeTest.MAX_INPUT size.')
        slurper.parseText String.valueOf(buffer, 0, i)
    }

    def setup() {
        service = new Service()

        PipedInputStream pInputStream
        PipedOutputStream pOutputStream

        // Set up pipe service->test
        pInputStream = new PipedInputStream(MAX_INPUT)
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

    def "instantiate+get"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'instantiate',
            newKey: 'key1',
            'class': 'java.util.Date',
            params: [123456789]
        ])
        writer.flush()
        obj = readObj()

        then:
        service.size() == 1
        obj == null
        service.contains 'key1', 'java.util.Date'

        when:
        writer.write JsonOutput.toJson([
            op: 'get',
            key: 'key1',
            'class': 'java.util.Date',
        ])
        writer.flush()
        obj = readObj()

        then:
        obj == '1970-01-02T10:17:36+0000'
    }

    def "remove+size"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'instantiate',
            newKey: 'key1',
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
            methodName: 'format',
            params: ['<%s> (%d)', ['werd', 345]]
        ]);
        writer.flush()
        def obj = readObj()

        then:
        service.size() == 0
        obj == '<werd> (345)'
    }

    def "staticCallPut"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'staticCallPut',
            'class': 'java.lang.String',
            methodName: 'format',
            params: ['<%s> (%d)', ['werd', 345]],
            newKey: 'output'
        ]);
        writer.flush()
        obj = readObj()

        then:
        service.size() == 1
        obj == null

        when:
        writer.write JsonOutput.toJson([
            op: 'get',
            'class': 'java.lang.String',
            key: 'output'
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == '<werd> (345)'
    }

    def "@reference"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'instantiate',
            newKey: 'date',
            'class': 'java.util.Date',
            params: [1000000000000]
        ])
        writer.flush()
        obj = readObj()

        then:
        service.size() == 1
        obj == null
        service.contains 'date', 'java.util.Date'

        when:
        writer.write JsonOutput.toJson([
            op: 'staticCall',
            'class': 'java.lang.String',
            methodName: 'format',
            params: ['Today is %tB %<te', ['@date']]
        ]);
        writer.flush()
        obj = readObj()

        then:
        service.size() == 1
        obj == 'Today is September 8'
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
            newKey: 'key1',
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
            methodName: 'substring',
            params: [3, 7],
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

    def "callPut+contains"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'instantiate',
            newKey: 'key1',
            'class': 'java.lang.String',
            params: ['input str']
        ])
        writer.flush()
        obj = readObj()

        then:
        service.size() == 1
        obj == null
        service.contains 'key1', 'java.lang.String'

        when:
        writer.write JsonOutput.toJson([
            op: 'callPut',
            key: 'key1',
            newKey: 'out',
            methodName: 'substring',
            params: [3, 7],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null

        when:
        writer.write JsonOutput.toJson([
            op: 'contains',
            key: 'out',
            'class': 'java.lang.String',
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == true
    }

    def "no-such-op"() {
        when:
        writer.write JsonOutput.toJson([ op: 'no-such', ]);
        writer.flush()
        def obj = readObj()

        then:
        obj instanceof Map
        obj.type == 'error'
    }
}
