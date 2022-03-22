#!/usr/bin/env groovy

package com.admc.jsonproxy

class Service {
    private BufferedReader reader
    private OutputStreamWriter writer
    char[] buffer
    static final CHAR_BUFFER_SIZE = 10240;

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
    }

    void run() {
        int i
        while ((i = reader.read(buffer, 0, buffer.length)) > 0)
            println "Got $i bytes: <${String.valueOf(buffer, 0, i)}>"
    }
}
