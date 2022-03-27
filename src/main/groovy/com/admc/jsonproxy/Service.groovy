#!/usr/bin/env groovy

package com.admc.jsonproxy

import java.util.logging.Level
import com.admc.groovy.GroovyUtil
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonException
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Field

@groovy.util.logging.Log(value='logger')
class Service extends HashMap implements Runnable {
    private BufferedReader reader
    private OutputStreamWriter writer
    static final CHAR_BUFFER_SIZE = 10240
    private JsonSlurper slurper = new JsonSlurper()

    static void main(String[] args) {
        // Can't think of any execution option options, for now...
        new Service().serve()
    }

    /**
     * System.in and System.out must be ready before this method
     * executes
     */
    void serve() {
        new Thread(this).start()
        logger.info 'started'
    }

    void run() {
        logger.info 'serve start'
        final char[] buffer = new char[CHAR_BUFFER_SIZE]
        int i
        def obj
        Object issueReport
        Set inputKeys, requiredKeys
        List deNestedListParams
        reader = new BufferedReader(new InputStreamReader(System.in, 'UTF-8'))
        writer = new OutputStreamWriter(System.out)
        try { // just to close the writer
        while ((i = reader.read(buffer, 0, buffer.length)) > 0) try {
            try {
                obj = slurper.parseText(String.valueOf(buffer, 0, i))
            } catch(JsonException je) {
                logger.log Level.SEVERE, "Input not JSON: $je.message"
                continue
            }
            if (obj !instanceof Map)
                throw new RuntimeException(
                    "Input JSON reconstituted to a ${obj.getClass().simpleName}")
            if (!('op' in obj.keySet()))
                throw new RuntimeException(
                    'Input JSON contains no .op (Operation) value')
            //logger.fine GroovyUtil.pretty(obj)
            switch (obj.op) {
              case 'instantiate':
                requiredKeys = ['op', 'newKey', 'class', 'params'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj.newKey !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string newKey: $obj.newKey")
                if (obj['class'] !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string class: ${obj['class']}")
                if (obj.params !instanceof List)
                    throw new RuntimeException(
                      "Input JSON contains non-List params: $obj.params")
                if (containsKey(obj.newKey))
                    throw new RuntimeException('Repository already contains '
                      + "an instance with key $obj.newKey")
                obj.params.add 0, obj['class']
                obj.params.add 0, obj.newKey
                instantiate(obj.params as Object[])
                writer.write JsonOutput.toJson(null)
                break
              case 'staticCall':
                requiredKeys = ['op', 'class', 'params', 'methodName'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj['class'] !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string class: ${obj['class']}")
                if (obj.methodName !instanceof String)
                    throw new RuntimeException(
                      'Input JSON contains non-string methodName: '
                      + obj.methodName)
                if (obj.params !instanceof List)
                    throw new RuntimeException(
                      "Input JSON contains non-List params: $obj.params")
                obj.params.add 0, obj.methodName
                obj.params.add 0, obj['class']
                logger.warning 'Need to find element type here'
                deNestedListParams = obj.params.collect() {
                    it instanceof List ? it as Object[] : it
                }
                writer.write JsonOutput.toJson(
                  staticCall(deNestedListParams as Object[]))
                break
              case 'staticCallPut':
                requiredKeys =
                  ['op', 'class', 'params', 'newKey', 'methodName'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj['class'] !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string class: ${obj['class']}")
                if (obj.methodName !instanceof String)
                    throw new RuntimeException(
                      'Input JSON contains non-string methodName: '
                      + obj.methodName)
                if (obj.params !instanceof List)
                    throw new RuntimeException(
                      "Input JSON contains non-List params: $obj.params")
                if (obj.newKey !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string newKey: $obj.newKey")
                obj.params.add 0, obj.methodName
                obj.params.add 0, obj['class']
                logger.warning 'Need to find element type here'
                deNestedListParams = obj.params.collect() {
                    it instanceof List ? it as Object[] : it
                }
                put obj.newKey, staticCall(deNestedListParams as Object[])
                writer.write JsonOutput.toJson(null)
                break
              case 'callPut':
                requiredKeys =
                  ['op', 'key', 'params', 'newKey', 'methodName'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj.key !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string key: $obj.key")
                if (obj.methodName !instanceof String)
                    throw new RuntimeException(
                      'Input JSON contains non-string methodName: '
                      + obj.methodName)
                if (obj.params !instanceof List)
                    throw new RuntimeException(
                      "Input JSON contains non-List params: $obj.params")
                if (obj.newKey !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string newKey: $obj.newKey")
                obj.params.add 0, obj.methodName
                obj.params.add 0, obj.key
                logger.warning 'Need to find element type here'
                deNestedListParams = obj.params.collect() {
                    it instanceof List ? it as Object[] : it
                }
                put obj.newKey, call(deNestedListParams as Object[])
                writer.write JsonOutput.toJson(null)
                break
              case 'call':
                requiredKeys = ['op', 'key', 'params', 'methodName'] as Set
                inputKeys = obj.keySet()
                if (requiredKeys != inputKeys)
                    throw new RuntimeException(
                      "Input '$obj.op' JSON has keys $inputKeys "
                      + "instead of $requiredKeys")
                if (obj.key !instanceof String)
                    throw new RuntimeException(
                      "Input JSON contains non-string key: $obj.key")
                if (obj.methodName !instanceof String)
                    throw new RuntimeException(
                      'Input JSON contains non-string methodName: '
                      + obj.methodName)
                if (obj.params !instanceof List)
                    throw new RuntimeException(
                      "Input JSON contains non-List params: $obj.params")
                obj.params.add 0, obj.methodName
                obj.params.add 0, obj.key
                logger.warning 'Need to find element type here'
                deNestedListParams = obj.params.collect() {
                    it instanceof List ? it as Object[] : it
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
              case 'get':
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
                writer.write JsonOutput.toJson(get(obj.key, obj['class']))
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
        } catch(RuntimeException re) {
            logger.log Level.SEVERE, "Handling ${re.getClass().simpleName}: $re.message"
            issueReport = [
              type: 'error',
              summary: "Service threw a ${re.getClass().simpleName}: $re.message",
            ]
            issueReport.detail = re.cause ?
              ("Service threw a ${re.getClass().simpleName}: $re.message\n"
               + (re.cause.stackTrace.collect() { it.toString() }).join('\n')) :
              (re.stackTrace.collect() { it.toString() }).join('\n')
            writer.write JsonOutput.toJson(issueReport)
        } catch(Throwable t) {
            logger.log Level.SEVERE,
              "Service aborting due to ${t.getClass().simpleName}"
            throw t
        } finally {
            writer.flush()
            logger.fine 'server flushed'
        } // Close request-handler loop

        } finally {  // close writer
            writer.close()
            logger.info 'serve end'
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
        final List<Object> params = args
        if (params.size() < 1 || params[0] !instanceof String)
            throw new IllegalArgumentException(
              "Service.staticCall param 'className' not a String: ${args[0]}")
        if (params.size() < 2 || params[1] !instanceof String)
            throw new IllegalArgumentException(
              "Service.staticCall param 'methodName' not a String: ${args[1]}")
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
        if (params.size() < 1 || params[0] !instanceof String)
            throw new IllegalArgumentException(
              "Service.call param 'instanceKey' not a String: ${args[0]}")
        if (params.size() < 2 || params[1] !instanceof String)
            throw new IllegalArgumentException(
              "Service.call 'methodName' not a String: ${args[1]}")
        String key = params.remove 0
        final Object inst = get key
        if (inst == null)
            throw new IllegalArgumentException(
              "We have no instance with key '$key'")
        _call(inst.getClass(), inst, params.remove(0), params)
    }

    private List<Object> dereference(final List<Object> inParams) {
        // Only going 1 level deep for now
        logger.warning 'Do a true recursive dereferencing'
        Object inst
        inParams.collect() {
            if (it instanceof Map) return it.collectEntries() {
                if (it.value !instanceof String || !it.value.startsWith('@'))
                    return [it.key, it.value]
                inst = get it.value.substring(1)
                if (inst == null)
                    throw new RuntimeException(
                      "Unsatisfied reference '$it.value' in param list")
                [it.key, inst]
            }
            if (it instanceof List) return it.collect() {
                if (it !instanceof String || !it.startsWith('@')) return it
                inst = get it.substring(1)
                if (inst == null)
                    throw new RuntimeException(
                      "Unsatisfied reference '$it' in param list")
                inst
            }
            if (it.getClass().isArray()) {
                for (int i in 0..it.length-1) {
                    if (it[i] !instanceof String
                      || !it[i].startsWith('@')) continue
                    inst = get it[i].substring(1)
                    if (inst == null)
                        throw new RuntimeException(
                          "Unsatisfied reference '${it[i]}' in param list")
                    it[i] = inst
                }
                return it
            }
            if (it !instanceof String || !it.startsWith('@'))
                return it
            inst = get it.substring(1)
            if (inst == null)
                throw new RuntimeException(
                  "Unsatisfied reference '$it' in param list")
            inst
        }
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
    final String methodName, final List<Object> inParams) {
        final List<Object> params = dereference inParams
        logger.log Level.FINE, "Got class $cl.simpleName"
        List<Class> pTypes = params.collect() { it.getClass() }
        Method meth
        try {
            meth = cl.getDeclaredMethod(methodName, pTypes as Class[])
        } catch(NoSuchMethodException nsme) {
             //Purposefully empty
        }
        if (meth == null) {
            logger.warning 'Copy the fallback signature type checks to here'
            logger.info 'Trying fallback meth signatures'
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
                it.TYPE
            }
            if (!anyChanged) {
                logger.info 'Trying Obj[]->Str[]'
                pTypes = pTypes.collect() {
                    if (it != Object[].getClass()) return it
                    anyChanged = true
                    String[].getClass()
                }
            }
            /*
            if (!anyChanged)
                throw new NoSuchMethodException('Specified meth signature '
                  + "not found for ${cl.simpleName}.$methodName: $pTypes")
            */
            try {
                meth = cl.getDeclaredMethod(methodName, pTypes as Class[])
            } catch (NoSuchMethodException) {
                logger.log(Level.WARNING,
                  "Trying last ditch effort since failing to find ${cl.simpleName}.$methodName: $pTypes")
                final matchingMethods = cl.getDeclaredMethods().findAll() {
                    it.name == methodName && it.parameterCount == params.size()
                }
                if (matchingMethods.size() != 1)
                    throw new NoSuchMethodException(matchingMethods.size()
                      + " matches for meth signature ${cl.simpleName}.$methodName: $pTypes")
                meth = matchingMethods[0]
                logger.warning 'Employing severe hack'
// When get IllegalArgument Exception with text "argument type mismatch",
// need to auto cast array and lists to the declaration array/list types.
params[3] = (String[]) params[3]
            }
        }
        logger.log Level.FINE, "Got method ${cl.simpleName}.$meth.name"
        if (inst == null && !Modifier.isStatic(meth.modifiers))
            throw new IllegalArgumentException(
              "Method is not static: meth.name")
        meth.invoke(inst, params as Object[])
    }

    /**
     * Instantiate an object
     *
     * @param newKey  String identifier for the new object in the repository
     * @param className
     * @param constructorParams
     * @returns nothing
     */
    void instantiate(final Object... args) {
        final List<String> params = args
        if (params.size() < 1 || params[0] !instanceof String)
            throw new IllegalArgumentException(
              "Service.instantiate param 'newKey' not a String: ${args[0]}")
        if (params.size() < 2 || params[1] !instanceof String)
            throw new IllegalArgumentException(
              "Service.instantiate param 'className' not a String: ${args[1]}")
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
            logger.fine 'Trying primitivized cons signatures'
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
                it.TYPE
            }
            if (anyChanged) try {
                cons = cl.getDeclaredConstructor(pTypes as Class[])
            } catch(NoSuchMethodException nsme) {
                 //Purposefully empty
            }
        }
        if (cons == null) {
            logger.fine 'Trying larger type cons signatures'
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
            logger.fine 'Trying primitivized larger type cons signatures'
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
                it.TYPE
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

    Object get(String key, String cName) {
        final def inst = get key
        if (inst == null) throw new RuntimeException("No such key '$key'")
        if (!Class.forName(cName).isInstance(inst))
            throw new RuntimeException("Instance '$key' is not a $cName")
        inst
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
