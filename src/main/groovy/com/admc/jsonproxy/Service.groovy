#!/usr/bin/env groovy

package com.admc.jsonproxy

import com.admc.groovy.GroovyUtil
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class Service {
    private BufferedReader reader
    private OutputStreamWriter writer
    private char[] buffer
    static final CHAR_BUFFER_SIZE = 10240;
    private JsonSlurper slurper

    static void main(String[] args) {
        /* Can't think of any execution option options, for now...
        List<String> argList = args
        println "Helo.  $argList.size args: $args" */
        new Service().run()
        System.exit 0
    }

    Service() {
        reader = new BufferedReader(new InputStreamReader(System.in, 'UTF-8'))
        writer = new OutputStreamWriter(System.out)
        buffer = new char[CHAR_BUFFER_SIZE]
        slurper = new JsonSlurper()
    }

    void run() {
        int i
        def obj
        while ((i = reader.read(buffer, 0, buffer.length)) > 0) {
            println "Got $i bytes: <${String.valueOf(buffer, 0, i)}>"
            obj = slurper.parseText(String.valueOf(buffer, 0, i))
            println "Reconstituted to a ${obj.getClass().name}"
            println GroovyUtil.pretty(obj)
        }
    }
}
