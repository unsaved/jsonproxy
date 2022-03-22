#!/usr/bin/env groovy

package com.admc.jsonproxy

import com.admc.groovy.GroovyUtil
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Field

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
     * Execute a static class method.
     *
     * @param className
     * @param methodName
     * @param methodParams
     * @returns method return value.  Null for void methods.
     */
    private def staticCall(final Object... args) {
        final List params = args
        if (params.size() < 2)
            throw new IllegalArgumentException('Service.staticCall '
              + 'requires at least 2 string param, className, methodName')
        _call(Class.forName(params.remove(0)), null, params.remove(0), params)
    }

    /**
     * Execute an instance method.
     *
     * @param instanceKey
     * @param methodName
     * @param methodParams
     * @returns method return value.  Null for void methods.
     */
    private def call(final Object... args) {
        final List params = args
        if (params.size() < 2)
            throw new IllegalArgumentException('Service.call '
              + 'requires at least 2 string param, instanceKey, methodName')
        String key = params.remove 0
        final Object inst = get key
        if (inst == null)
            throw new IllegalArgumentException(
              "We have no instance with key '$key'")
        _call(inst.getClass(), inst, params.remove(0), params)
    }

    /**
     * Execute a method, static or instance.
     *
     * @param className
     * @param methodName
     * @param methodParams
     * @returns method return value.  Null for void methods.
     */
    private def _call(final Class cl, final Object inst,
    final String methodName, final List<Object> params) {
        System.err.println "Got class $cl.name"
        List<Class> pTypes = params.collect() { it.getClass() }
        Method meth
        try {
            meth = cl.getDeclaredMethod(methodName, pTypes as Class[])
        } catch(NoSuchMethodException nsme) {
             //Purposefully empty
        }
        if (meth == null) {
            System.err.println 'Trying fallback meth signatures'
            boolean anyChanged
            pTypes = pTypes.collect() {
                Field f
                try {
                    f = it.getField 'TYPE'
                } catch(NoSuchFieldException nsfe) {
                    return it
                }
                //Modifier.isStatic(f.modifiers) ? it.TYPE : it
                if (!Modifier.isStatic(f.modifiers)) return it
                anyChanged = true
                return it.TYPE
            }
            if (!anyChanged)
                throw new NoSuchMethodException(
                  "Specified meth signature not found for $cl.name")
            meth = cl.getDeclaredMethod(methodName, pTypes as Class[])
        }
        System.err.println "Got method ${cl.name}.$meth.name"
        if (inst == null && !Modifier.isStatic(meth.modifiers))
            throw new IllegalArgumentException(
              "Method is not static: meth.name")
        meth.invoke(inst, params as Object[])
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
        List<Class> pTypes = params.collect() { it.getClass() }
        Constructor cons
        try {
            cons = cl.getDeclaredConstructor(pTypes as Class[])
        } catch(NoSuchMethodException nsme) {
             //Purposefully empty
        }
        if (cons == null) {
            System.err.println 'Trying fallback cons signatures'
            boolean anyChanged
            pTypes = pTypes.collect() {
                Field f
                try {
                    f = it.getField 'TYPE'
                } catch(NoSuchFieldException nsfe) {
                    return it
                }
                //Modifier.isStatic(f.modifiers) ? it.TYPE : it
                if (!Modifier.isStatic(f.modifiers)) return it
                anyChanged = true
                return it.TYPE
            }
            if (!anyChanged)
                throw new NoSuchMethodException(
                  "Specified cons signature not found for $clName")
            cons = cl.getDeclaredConstructor(pTypes as Class[])
        }
        System.err.println "Got class $cl.name"
        put(key, cons.newInstance(params as Object[]))
    }

    /**
     * Overwrite HashMap.remove to return null.
     * For one thing we don't want to encourage caller from holding a
     * reference and possibly causing memory leak.
     * For another, our intended client can't use our map values.
     * ... hm, should override other 'value' methods too.
     */
    Object remove(Object key) { super.remove key; null }
}
