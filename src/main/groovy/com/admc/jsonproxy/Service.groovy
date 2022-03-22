#!/usr/bin/env groovy

package com.admc.jsonproxy

import com.admc.groovy.GroovyUtil
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class Service extends HashMap {
    private BufferedReader reader
    private OutputStreamWriter writer
    private char[] buffer
    static final CHAR_BUFFER_SIZE = 10240
    private JsonSlurper slurper

    static void main(String[] args) {
        /* Can't think of any execution option options, for now...
        List<String> argList = args
        println "Helo.  $argList.size args: $args" */
        new Service().run()
        System.exit 0
    }

    /**
     * System.in and System.out must be ready before this constructor
     * executes
     */
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
            System.err.println 'Service wrote to stdout'
            obj = slurper.parseText(String.valueOf(buffer, 0, i))
            println "Reconstituted to a ${obj.getClass().name}"
            println GroovyUtil.pretty(obj)
            put '1ststr', obj
        }
    }

    /**
     * Execute a static class method
     *
     * @param className
     * @param methodParams
     * @returns method return value.  Null for void methods.
     */
    def staticCall(final Object... args) {
        final List<String> params = args
        if (params.size() < 1)
            throw new IllegalArgumentException('Service.instantiate '
              + 'requires at least 2 string param, className, methodName')
        final String clName = params.remove 0
        final String methodName = params.remove 0
        final Class cl = Class.forName clName
        System.err.println "Got class $cl.name"
        final List<Class> pTypes = params.collect() { it.getClass().name }
        final Method meth = cl.getDeclaredMethod(methodName, pTypes as Class[])
//final Method meth = cl.getMethod(methodName, [String.class, Object.class, Object.class] as Class[])
        System.err.println "Got method ${cl.name}.$meth.name"
        if (!Modifier.isStatic(meth.modifiers))
            throw new IllegalArgumentException(
              "Method is not static: meth.name")
        return meth.invoke(null, params as Object[])
        //put(key, cons.newInstance(params as Object[]))
    }

    /**
     * Instantiate an object
     *
     * @param key  String identifier for the new object in the repository
     * @param className
     * @param constructorParams
     * @returns nothing
     */
    void instantiate(final Object... args) {
        final List<String> params = args
        if (params.size() < 2)
            throw new IllegalArgumentException('Service.instantiate '
              + 'requires at least 2 string params, key + className')
        final String key = params.remove 0
        final String clName = params.remove 0
        final Class cl = Class.forName clName
        final List<Class> pTypes = params.collect() { it.getClass().name }
        final Constructor cons = cl.getDeclaredConstructor(pTypes as Class[])
        System.err.println "Got class $cl.name"
        put(key, cons.newInstance(params as Object[]))
    }

    //public int size() { keySet().size() }
}
