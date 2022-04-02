package com.admc.jsonproxy

import java.util.logging.Level
import java.lang.reflect.Executable
import java.lang.reflect.Parameter
import java.lang.reflect.Constructor
import com.admc.groovy.GroovyUtil
import java.util.regex.Matcher
import java.util.Map
import java.util.Collection
import java.lang.reflect.Array

/**
 * Major simplification to be had.
 * Save scalar types always as a Class by leveraging Class.forName('[S...;')
 * Can then easily determine if scalar with Class.name.indexOf('.')
 */
@groovy.util.logging.Log(value = 'logger')
class MethodCaller {
    private Class cl
    private Object inst
    private String methodName
    private Object[] paramVals
    private Executable executable

    private MethodCaller(final Class cl, final Object inst,
    final String methodName, final List<Object> params) {
        this.cl = cl
        this.inst = inst
        this.methodName = methodName
        final Map<Executable, Parameter[]> candidates
        paramVals = params.toArray()
        final int paramCount = paramVals.length
        List<Class> pTypes = params.collect() { it.getClass() }
        def initialCandidates = methodName == null ? cl.declaredConstructors : cl.declaredMethods
        if (initialCandidates.size() < 1)
            throw new RuntimeException(
              "${initialCandidates.size()} total ${paramCount}-param $cl.simpleName"
              + ".${methodName == null ? '<CONST>' : methodName}'s")
        candidates = initialCandidates.collectEntries() {
            (methodName == null || it.name == methodName) && it.parameterCount == paramCount ?
              [it, it.parameterTypes] :  [:]
        }
        logger.log Level.WARNING, "{0} {1}.{2} Candidates: {3}",
          candidates.size(), cl.simpleName, methodName, GroovyUtil.pretty(
          candidates.collectEntries() { [it.key.name, it.value.simpleName]})

        // Cull out candidates based on pTypes
        final Set<Executable> matches = candidates.keySet()

        if (matches.size() !== 1) throw new RuntimeException(
          "${matches.size()} match $cl.simpleName"
          + ".${methodName == null ? '<CONST>' : methodName} executables:\n"
          + GroovyUtil.pretty(candidates.collectEntries() { [it.key.name, it.value.simpleName]}))
        executable = matches[0]
    }

    /**
     * Probably makes more sense to work with strings like [I and
     * [Ljava.util.regex.Pattern; , since Class.forName works for all of these.
     * For now, using probably-inferior method of strings like 'int'.
     */
    static Class classForPrimitiveStr(final String pString) {
        [Boolean.class, Character.class, Byte.class, Short.class, Integer.class,
         Long.class, Float.class, Double.class, Void.class].find() {
           it.TYPE.name == pString
         }
    }

    static final Map m0 = [
        int: new int[0],
        byte: new byte[0],
        char: new char[0],
        long: new long[0],
        double: new double[0],
        short: new short[0],
        byte: new byte[0],
        boolean: new boolean[0],
    ]

    static def getArr0(final String typeStr) {
        typeStr.indexOf('.') > 0 ?
            Array.newInstance(Class.forName(typeStr), 0) : m0[typeStr]
    }

    static private def toArray(final Collection c, final String typeStr) {
        if (c == null) return null
        switch(typeStr) {
          case 'int': return c.toArray(getArr0(typeStr)) as int[]
          case 'long': return c.toArray(getArr0(typeStr)) as long[]
          case 'float': return c.toArray(getArr0(typeStr)) as float[]
          case 'double': return c.toArray(getArr0(typeStr)) as double[]
          case 'char': return c.toArray(getArr0(typeStr)) as char[]
          case 'byte': return c.toArray(getArr0(typeStr)) as byte[]
          case 'boolean': return c.toArray(getArr0(typeStr)) as boolean[]
          case 'short': return c.toArray(getArr0(typeStr)) as short[]
          // Some internal Grooyv magic somehow allows only the following
          // toArray() to return a realy array without the 'as' directive:
          default: return c.toArray(getArr0(typeStr))
        }
    }

    static private def toArray(final Collection c, final Map typeMap) {
        if (typeMap.listType == null) switch(typeMap.members) {
          case 'int': return c.toArray(getArr0(typeMap.members))[] as int[][]
          case 'long': return c.toArray(getArr0(typeMap.members))[] as long[][]
          case 'float': return c.toArray(getArr0(typeMap.members))[] as float[][]
          case 'double': return c.toArray(getArr0(typeMap.members))[] as double[][]
          case 'char': return c.toArray(getArr0(typeMap.members))[] as char[][]
          case 'byte': return c.toArray(getArr0(typeMap.members))[] as byte[][]
          case 'boolean': return c.toArray(getArr0(typeMap.members))[] as boolean[][]
          case 'short': return c.toArray(getArr0(typeMap.members))[] as short[][]
          case 'java.lang.String':
            //return c.toArray(new String[0][]) as String[][]
            return c.toArray(getArr0(typeMap.members)[]) as String[][]
          default:
            throw new RuntimeException(
              'You have hit the critical limition of this app.\n'
           + /Can't get groovy to generate a Class[][] for arbitrary object Class."/
              + "\nAs workaround can add a specific case for '$typeMap.members'.");
          //default: return c.toArray(getArr0(typeMap.members))[]
        }
        switch (typeMap.listType) {
          case List.class: return c.toArray(typeMap.listType) as List[]
          // Purposefully no defualt
        }
        throw new RuntimeException(
          'You have hit the critical limition of this app.\n'
        + /Can't get groovy to generate a Class[][] for arbitrary list type."/
          + "\nAs workaround can add a specific case for '$typeMap.listType'.");
    }

    /**
     * I believe that can work entirely with wrapper classes and forget
     * primitives, EXCEPT that need to translate primitive type specs in
     * .get*ParameterTypes from primitives to their wrappers.
     *
     * TODO: Support java.util.Map's similarly to how now support
     *       java.util.Collection's.
     *
     * @returns either
     *  (a) a Map with members 'listType' null-or-Class + 'members' (a or b)  OR
     *  (b) a non-collection/array type name String that is
     *      either a dotted class name or a simple primitive name.
     *
     * The list-type distinction is needed for compatibility testing
     * (because nulls are not allowed in primitive arrays;
     * and to convert to the correct collection/array type.
     */
    static private def toTree(String gSpec) {
        //System.err.println "Trying $gSpec"
        Matcher m
        String c, memberSpec, isArray  // c is Collection type
        if (gSpec.endsWith('>')) {
            // Outermost generified Collection
            m = gSpec =~ /([^<]+)<(.+)>/
            if (!m.matches()) throw new Exception(
              "Failed to parse outer Collection type from: $gSpec")
            c = m.group 1
            memberSpec = m.group 2
        } else if (gSpec.matches(/\w+ \[\[.*/)) {
            // Outermost is an array, common prefix form
            m = gSpec =~ /\w+ \[(\[.*)/
            if (!m.matches()) throw new Exception(
              "Failed to parse outer Collection type from: $gSpec")
            isArray = true
            // Nesting arrays only have (optional) leaf types;
            // If an arrimpleu nests a Collection then the array nesting ends
            // there with leaf type of the Collection.
            // Otherwise 'int[][][][]' means leaf type is int.
            memberSpec = 'type ' + m.group(1)
        } else if (gSpec.endsWith('[]')) {
            // Outermost is an array, suffix form
            isArray = true
            //memberSpec = 'type ' + gSpec.substring(0, gSpec.length()-2)
            memberSpec = gSpec.substring 0, gSpec.length()-2
        } else if (gSpec.contains(' ')) {
            m = gSpec =~ /\w+ (.+)/
            if (!m.matches()) throw new Exception(
                "Got a spec wih space where 2nd token is not [*: $gSpec")
            if (m.group(1).startsWith('[')) {
                isArray = true
                // May be 2nd token of more than a class spec?
                m = m.group(1) =~ /\[([A-Z])(?:(\[?[a-zA-Z_][\w.]*);)?/
                if (!m.matches()) throw new Exception(
                  "Failed to parse single-level array from: $gSpec")
                if (m.group(1) == 'L') {
                    if (m.group(2) == null)
                        throw new Error("[L spec with no class specifier: $gSpec")
                } else {
                    if (m.group(2) != null)
                        throw new Error("[non-S spec with class specifier: $gSpec")
                }
                switch (m.group(1)) {
                  case 'L':
                    memberSpec = 'type ' + m.group(2)
                    break
                  case 'I':
                    memberSpec = 'int'
                    break
                  case 'J':
                    memberSpec = 'long'
                    break
                  case 'F':
                    memberSpec = 'float'
                    break
                  case 'D':
                    memberSpec = 'double'
                    break
                  case 'B':
                    memberSpec = 'byte'
                    break
                  case 'Z':
                    memberSpec = 'boolean'
                    break
                  case 'S':
                    memberSpec = 'short'
                    break
                  case 'C':
                    memberSpec = 'char'
                    break
                  default:
                    throw new Error('Unexpected 1-level primitive array type '
                      + "${m.group(1)}: $gSpec")
                }
            } else {
                if (Map.class.isAssignableFrom(Class.forName(m.group(1))))
                    throw new RuntimeException(
                      "Sorry but Maps not supported yet: ${m.group(1)}")
                if (!Collection.class.isAssignableFrom(Class.forName(m.group(1)))) {
                    return m.group(1)  // Class scalar
                }
                c = m.group 1  // unconstrained Collection
                memberSpec = 'class java.lang.Object'
            }
        } else {
            // Outermost is a scalar, primitive or Object
            if (!gSpec.matches(/[_a-zA-Z][\w.]*/))
                throw new Error("Unrecognized type format: $gSpec")
            return gSpec
        }

        /*
         * We only get to here if we have a list (Collection or array)
         * Generate treespec.  Convention:
         * memberSpec==null:              scalar
         * WHEN memberSpec==null [scalar]
         *   c contains .:     class-type scalar
         *   c contains NO .:  primitive scalar
         * WHEN memberSpec!=null [list]
         *   isArray:          boolean
         *   c null:           unconstrained member types
         *   c non-null:       collection member type
         */
         // if (c) System.err.println "Will resolve class ($c)"
         [
           listType:  isArray ? null : Class.forName(c),
           members:   toTree(memberSpec),
         ]
    }

    /**
     * Attempts .executable executions with variants of paramVals until one succees.
     * Throws if non succeeds.
     */
    private def _exec() {
        executable instanceof Constructor ?
            executable.newInstance(paramVals) :
            executable.invoke(inst, paramVals)
    }

    /**
     * Constructor invocation
     */
    static def exec(final Class cl, final List<Object> params) {
        logger.log Level.INFO, "Constructor exec for {0}-param {1}", params.size(), cl.simpleName
        // Don't yet know if have the same invoke-elicits-JVM-restart issue as the
        // instance method() exec method below.
        new MethodCaller(cl, null, null, params)._exec()
    }

    /**
     * Instance method invocation
     */
    static def exec(final Object inst,
    final String methodName, final List<Object> params) {
        logger.log Level.INFO, "Instance exec for {0}-param {1}.{2}",
          params.size(), inst.getClass().simpleName, methodName
        // Major Groovy defect here, even in 4.0.0.
        // The entire JRE load starts over (with same pid) if invoke done with params
        // has nothing to do with resolving inst or params here.
        //logger.log Level.WARNING, "Invoking {0}.{1} of {2}",
          //cs.keySet()[0].declaringClass.simpleName, methodName, inst.getClass().simpleName
        new MethodCaller(inst.getClass(), inst, methodName, params)._exec()
    }

    /**
     * Static method invocation
     */
    static def exec(final Class cl, final String methodName, final List<Object> params) {
        logger.log Level.INFO, "Static exec for {0}-param {1}.{2}",
          params.size(), cl.simpleName, methodName
        // Don't yet know if have the same invoke-elicits-JVM-restart issue as the
        // instance method() exec method below.
        new MethodCaller(cl, null, methodName, params)._exec()
    }
}
