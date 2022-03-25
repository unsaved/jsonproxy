package com.admc.jsonproxy

import spock.lang.Specification
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * These tests manipulate stdin and stdout to we can test the JSON I/O pipe
 */
class JdbcPipeTest extends Specification {
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
        writer = null
        reader = null
        service = null
    }

    def "connect"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'staticCallPut',
            newKey: 'conn',
            'class': 'java.sql.DriverManager',
            params: ['getConnection', 'jdbc:hsqldb:mem:name', 'SA', '']
        ]);
        writer.flush()
        obj = readObj()

        then:
        service.size() == 1
        service.contains('conn', 'java.sql.Connection')

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'conn',
            params: ['close'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null
    }

    def "createTable+metadata"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'staticCallPut',
            newKey: 'conn',
            'class': 'java.sql.DriverManager',
            params: ['getConnection', 'jdbc:hsqldb:mem:name', 'SA', '']
        ]);
        writer.flush()
        obj = readObj()

        then:
        service.size() == 1
        service.contains('conn', 'java.sql.Connection')

        when:
        writer.write JsonOutput.toJson([
            op: 'callPut',
            key: 'conn',
            newKey: 'statement',
            params: ['createStatement'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'statement',
            params: ['execute', 'CREATE TABLE tbl(i INT, s VARCHAR(20))'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == false

        when:
        writer.write JsonOutput.toJson([
            op: 'callPut',
            key: 'conn',
            newKey: 'metadata',
            params: ['getMetaData'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null

        when:
        writer.write JsonOutput.toJson([
            op: 'callPut',
            key: 'metadata',
            newKey: 'rs',
            params: ['getTables', null, 'PUBLIC', null, ['TABLE']],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'rs',
            params: ['next'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == true

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'rs',
            params: ['getString', 'TABLE_NAME'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == 'TBL'

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'conn',
            params: ['close'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null
    }

    def "tableWrite+tableRead"() {
        def obj

        when:
        writer.write JsonOutput.toJson([
            op: 'staticCallPut',
            newKey: 'conn',
            'class': 'java.sql.DriverManager',
            params: ['getConnection', 'jdbc:hsqldb:mem:name', 'SA', '']
        ]);
        writer.flush()
        obj = readObj()

        then:
        service.size() == 1
        service.contains('conn', 'java.sql.Connection')

        when:
        writer.write JsonOutput.toJson([
            op: 'callPut',
            key: 'conn',
            newKey: 'statement',
            params: ['createStatement'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'statement',
            params: ['execute', "INSERT INTO tbl VALUES(1, 'one')"],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == false

        when:
        writer.write JsonOutput.toJson([
            op: 'callPut',
            key: 'statement',
            newKey: 'rs',
            params: ['executeQuery', 'SELECT * FROM tbl'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'rs',
            params: ['next'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == true

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'rs',
            params: ['getString', 'S'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == 'one'

        when:
        writer.write JsonOutput.toJson([
            op: 'call',
            key: 'conn',
            params: ['close'],
        ]);
        writer.flush()
        obj = readObj()

        then:
        obj == null
    }
}
