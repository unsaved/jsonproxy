#!/usr/bin/env groovy

package com.admc.jsonproxy

import com.admc.groovy.GroovyUtil
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonException
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Field

class Service extends HashMap implements Runnable {
    private BufferedReader reader
    private OutputStreamWriter writer
    static final CHAR_BUFFER_SIZE = 10240
    private JsonSlurper slurper = new JsonSlurper()

    static void main(String[] args) {
        /* Can't think of any execution option options, for now...
        List<String> argList = args
        println "Helo.  $argList.size args: $args" */
        new Service().serve()
    }

    /**
     * System.in and System.out must be ready before this method
     * executes
     */
    void serve() {
        reader = new BufferedReader(new InputStreamReader(System.in, 'UTF-8'))
        writer = new OutputStreamWriter(System.out)
        new Thread(this).start()
        System.err.println 'started'
    }

    void run() {
        System.err.println 'serve start'
        final char[] buffer = new char[CHAR_BUFFER_SIZE]
        int i
        def obj
        Set inputKeys, requiredKeys
        List deNestedListParams
        while ((i = reader.read(buffer, 0, buffer.length)) > 0) {
            try {
                obj = slurper.parseText(String.valueOf(buffer, 0, i))
            } catch(JsonException je) {
                System.err.println "Input not JSON: $je.message"
                continue
            }
            if (obj !instanceof Map)
                throw new RuntimeException(
                    "Input JSON reconstituted to a ${obj.getClass().name}")
            if (!('op' in obj.keySet()))
                throw new RuntimeException(
                    'Input JSON contains no .op (Operation) value')
            //println GroovyUtil.pretty(obj)
            switch (obj.op) {
              case 'instantiate':
                requiredKeys = ['op', 'key', 'class', 'params'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj.key !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string key: $obj.key")
                if (obj['class'] !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string class: ${obj['class']}")
                if (obj.params !instanceof List)
                    throw new RuntimeException(
                      "Input JSON contains non-List params: $obj.params")
                obj.params.add 0, obj['class']
                obj.params.add 0, obj.key
                instantiate(obj.params as Object[])
                writer.write JsonOutput.toJson(null)
                break
              case 'staticCall':
                requiredKeys = ['op', 'class', 'params'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj['class'] !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string class: ${obj['class']}")
                if (obj.params !instanceof List)
                    throw new RuntimeException(
                      "Input JSON contains non-List params: $obj.params")
                obj.params.add 0, obj['class']
                System.err.println 'Need to find element type here'
                deNestedListParams = obj.params.collect() {
                    (it instanceof List) ? (it as Object[]) : it
                }
                writer.write JsonOutput.toJson(
                  staticCall(deNestedListParams as Object[]))
                break
              case 'call':
                requiredKeys = ['op', 'key', 'params'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj.key !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string key: $obj.key")
                if (obj.params !instanceof List)
                    throw new RuntimeException(
                      "Input JSON contains non-List params: $obj.params")
                obj.params.add 0, obj.key
                System.err.println 'Need to find element type here'
                deNestedListParams = obj.params.collect() {
                    (it instanceof List) ? (it as Object[]) : it
                }
                writer.write JsonOutput.toJson(
                  call(deNestedListParams as Object[]))
                break
              case 'contains':
                requiredKeys = ['op', 'key', 'class'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj.key !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string key: $obj.key")
                if (obj['class'] !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string class: ${obj['class']}")
                writer.write JsonOutput.toJson(contains(obj.key, obj['class']))
                break
              case 'remove':
                requiredKeys = ['op', 'key', 'class'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj.key !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string key: $obj.key")
                if (obj['class'] !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string class: ${obj['class']}")
                remove obj.key, obj['class']
                writer.write JsonOutput.toJson(null)
                break
              case 'size':
                requiredKeys = ['op'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                writer.write JsonOutput.toJson(size())
                break
              default:
                throw new RuntimeException("Unexpected operation: $obj.op")
            }
            writer.flush()
            System.err.println 'server flushed'
        }
        writer.close()
        System.err.println 'serve end'
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
System.err.println 'Copy the fallback signature tpe checks to here'
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
                throw new NoSuchMethodException('Specified meth signature '
                  + "not found for $cl.name: $pTypes")
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
        final List<Class> origPTypes = params.collect() { it.getClass() }
        List<Class> pTypes
        Constructor cons
        boolean anyChanged
        try {
            cons = cl.getDeclaredConstructor(origPTypes as Class[])
        } catch(NoSuchMethodException nsme) {
             //Purposefully empty
        }
        if (cons == null) {
            System.err.println 'Trying primitivized cons signatures'
            anyChanged = false
            pTypes = origPTypes.collect() {
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
            if (anyChanged) try {
                cons = cl.getDeclaredConstructor(pTypes as Class[])
            } catch(NoSuchMethodException nsme) {
                 //Purposefully empty
            }
        }
        if (cons == null) {
            System.err.println 'Trying larger type cons signatures'
            anyChanged = false
            pTypes = origPTypes.collect() {
                if (it == Integer.class) {
                    anyChanged = true
                    return Long.class
                }
                if (it == Float.class) {
                    anyChanged = true
                    return Double.class
                }
                it
            }
            if (anyChanged) try {
                cons = cl.getDeclaredConstructor(pTypes as Class[])
            } catch(NoSuchMethodException nsme) {
                 //Purposefully empty
            }
        }
        if (cons == null) {
            System.err.println 'Trying primitivized larger type cons signatures'
            anyChanged = false
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

    boolean contains(String key, String cName) {
        final def inst = get key
        inst != null && Class.forName(cName).isInstance(inst)
    }

    /**
     * Remove with type-validation
     */
    void remove(final String key, final String cName) {
        if (!contains(key, cName))
            throw new RuntimeException('No such instance in repository')
        this.remove key
    }
}
