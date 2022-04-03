package com.admc.jsonproxy

import java.util.logging.Level
import java.lang.reflect.Executable
import java.lang.reflect.Parameter
import java.lang.reflect.Constructor
import java.lang.reflect.Type
import com.admc.groovy.GroovyUtil
import java.util.regex.Matcher
import java.util.Map
import java.util.Collection
import java.lang.reflect.Array

/**
 * This class is responsible for executing java.lang.reflex.Executables.
 * It has nothing to do with the Executors in the java.util.concurrent
 * package.
 *
 * Major simplification to be had.
 * Save scalar types always as a Class by leveraging Class.forName('[S...;')
 * Can then easily determine if scalar with Class.name.indexOf('.')
 */
@groovy.util.logging.Log(value = 'logger')
class Executor {
    private Class cl
    private Object inst
    private String methodName
    private Executable executable
    private List pSummaries
    private List pSpecTrees
    private List<Object> paramVals

    private Executor(final Class cl, final Object inst,
    final String methodName, final List<Object> paramVals) {
        this.cl = cl
        this.inst = inst
        this.methodName = methodName
        this.paramVals = paramVals
        final int paramCount = paramVals.size()
        final Executable[] initialCandidates = (methodName == null ?
          cl.declaredConstructors : cl.declaredMethods).findAll() {
            (methodName == null || it.name == methodName) &&
              it.parameterCount == paramCount
        }
        if (initialCandidates.size() < 1)
            throw new RuntimeException(
          "${initialCandidates.size()} total ${paramCount}-param $cl.simpleName"
              + ".${methodName == null ? '<CONST>' : methodName}'s with "
              + "$paramCount params")
        pSummaries = paramVals.collect() { param -> valSummary param }

        // Cull out candidates based on pTypes
        final Map<Executable, List> candidates
        candidates = initialCandidates.collectEntries() { Executable ex ->
            List pSTs = ex.genericParameterTypes.collect() { Type type ->
                toTree type.toString()
            }
            int i
            pSTs.find() { pSpecTree ->
                !compatible(pSummaries[i++], pSpecTree)
            } ? [:] : [ex, pSTs]
        }
        logger.log Level.WARNING, "{0} {1}.{2} Candidates: {3}",
          candidates.size(), cl.simpleName, methodName,
          candidates.collectEntries() { [it.key.name, it.value.toString()]}

        if (candidates.size() !== 1) throw new RuntimeException(
          "${candidates.size()} match $cl.simpleName"
          + ".${methodName == null ? '<CONST>' : methodName} executables:\n"
          + candidates.collectEntries() { [it.key.name, it.value.toString()]})
        executable = candidates.keySet()[0]
        pSpecTrees = candidates[executable]
        assert paramVals.size() == pSummaries.size()
        assert pSummaries.size() == pSpecTrees.size()
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
          case 'int': return toPrimitiveArray(c, int[].class) as int[]
          case 'long': return toPrimitiveArray(c, long[].class) as long[]
          case 'float': return toPrimitiveArray(c, float[].class) as float[]
          case 'double': return toPrimitiveArray(c, double[].class) as double[]
          case 'char': return toPrimitiveArray(c, char[].class) as char[]
          case 'byte': return toPrimitiveArray(c, byte[].class) as byte[]
          case 'boolean': return toPrimitiveArray(c, boolean[].class) as boolean[]
          case 'short': return toPrimitiveArray(c, short[].class) as short[]
          // Some internal Grooyv magic somehow allows only the following
          // toArray() to return a realy array without the 'as' directive:
          default: return c.toArray(getArr0(typeStr))
        }
    }
    
    /**
     * From https://stackoverflow.com/questions/25149412/how-to-convert-listt-to-array-t-for-primitive-types-using-generic-method
     *
     * Not used now.  Consider using this in place of toArray above, when
     * typeStr1 is a primitive type.
     *
     * Call like:  def ia = toPrimitiveArray [34, 325], int[].class
     */
    public static <P> P toPrimitiveArray(List<?> list, Class<P> arrayType) {
        if (!arrayType.isArray())
            throw new IllegalArgumentException(arrayType.toString());
        Class<?> primitiveType = arrayType.getComponentType();
        if (!primitiveType.isPrimitive())
            throw new IllegalArgumentException(primitiveType.toString());

        P array = arrayType.cast(Array.newInstance(primitiveType, list.size()));
        for (int i = 0; i < list.size(); i++) Array.set(array, i, list.get(i));
        return array;
    }

    /**
     * Original goal was for this to handle arbitrarily deep nesting, but
     * current implementation only works for 2 levels deep.
     * Only clear way forward is to add more levels by explicitly coding
     * type variants at higher levels like "new int[c.size()][][]",
     * "new int[c.size()][][][]", etc.
     * I know of no way to get this to work for arbitrary depth.
     */
    static private def toArray(final Collection c, final Map typeMap) {
        if (typeMap.listType == null) {
            def aa
            Class arrayType
            switch(typeMap.members) {
              //case 'int': return c.toArray(getArr0(typeMap.members))[] as int[][]
              case 'int':
                arrayType = int[].class; aa = new int[c.size()][]; break
              case 'long':
                arrayType = long[].class; aa = new long[c.size()][]; break
              case 'float':
                arrayType = float[].class; aa = new float[c.size()][]; break
              case 'double':
                arrayType = double[].class; aa = new double[c.size()][]; break
              case 'char':
                arrayType = char[].class; aa = new char[c.size()][]; break
              case 'byte':
                arrayType = byte[].class; aa = new byte[c.size()][]; break
              case 'boolean':
                arrayType = boolean[].class; aa = new boolean[c.size()][]; break
              case 'short':
                arrayType = short[].class; aa = new short[c.size()][]; break
              case 'java.lang.String':
                //return c.toArray(new String[0][]) as String[][]
                return c.toArray(getArr0(typeMap.members)[]) as String[][]
              default:
                throw new RuntimeException(
                  "You have hit the critical limition of this app.\nCan't get "
                  + 'groovy to generate a Class[][] for arbitrary object Class.\n'
                  + "As workaround can add a specific case for '$typeMap.members'.")
              //default: return c.toArray(getArr0(typeMap.members))[]
            }
            //c.each() { aa[i++] = toArray(it, 'int') }
            int i
            c.each() {
                aa[i++] = it == null || it.getClass().isArray() ?
                  it : it.toPrimitiveArray(it, arrayType)
            }
            return aa
        }
        switch (typeMap.listType) {
          case List.class: return c.toArray(typeMap.listType) as List[]
          // Purposefully no defualt
        }
        throw new RuntimeException(
          'You have hit the critical limition of this app.\n'
        + /Can't get groovy to generate a Class[][] for arbitrary list type."/
          + "\nAs workaround can add a specific case for '$typeMap.listType'.")
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
                        throw new Error(
                          "[L spec with no class specifier: $gSpec")
                } else {
                    if (m.group(2) != null)
                        throw new Error(
                          "[non-S spec with class specifier: $gSpec")
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
     * If the two classes differ and they are just different precision
     * alternatives, and the first has higher precision than the last
     * then true.
     *
     * As of today, I think that
     *    Integer vs. Long
     *    Float vs. Double
     * are the ony cases to handle, so that's all we're supporting.
     */
    static boolean isHigherPrecision(Class c1, Class c2) {
        if (c1 == c2) return null
        if (c1 == Long.class && c2 == Integer.class) return true
        if (c1 == Double.class && c2 == Float.class) return true
        false
    }

    /**
     * Returns summary of values at level 'level'.
     *
     * Object.class return means must allow any
     *   (as soon as vals must allow 2 unrelated types, it must allow Objects
     *    and therefore null, even with native arrays).
     * Void.class return means no checking at all
     * null.class return means no checking other than must allow null
     *
     * RETURNS:
     *     condition                                          returns
     *     -----------------------------------------------   -----------
     *     Non-collection                                    scalar val (incl. null)
     *     No values (empty list)                            Void.class
     *     All values are nulls                              null.class
     *     Any Object member                                 Object.class
     *     Two non-null members have no common parent class  Object.class
     *     Otherwise + Any non-null value is a scalar        {} w/no child/nest)
     *     Otherwise.  All non-null members are collections  {} w/ child/nest
     *
     * Limitation:  Does not yet support maps
     */
    static private def valSummary(final Object levelMembers) {
        final Class pClass = levelMembers.getClass()
        if (pClass == null || !Collection.class.isAssignableFrom(pClass))
            return levelMembers
        if (levelMembers.size() < 1) return Void.class
        def max, min
        Class itClass, cc  // cc is abbreviation for Common Class
        boolean anyNull, anyScalar
        levelMembers.each() {
            if (it == null) { anyNull = true; return }

            // Update cc:
            itClass = it.getClass()
            if (Map.class.isInstance(it))
                throw new RuntimeException('Sorry but we '
                  + "don't support Maps yet (you provided a ${itClass.name}")
            if (!Collection.class.isInstance(it)) {
                anyScalar = true
                if (it instanceof Long) {
                    if (it > max) max = it
                    if (min == null || it < min) min = it
                }
            }
            if (itClass == cc) {} // nothing to do with cc
            else if (itClass == Object.class) cc = Object.class
            else if (cc == null || itClass.isAssignableFrom(cc)) cc = itClass
            // if cc.isAssignableFrom(itClass), keep more general cc
            else if (!cc.isInstance(it)) {
                // Trickiest case.  2 different non-inter-assignable classes.
                // Need to see if they are precision-alternates or if
                // they have a common ancestor class.
                if (isHigherPrecision(cc, itClass)) {}
                else if (isHigherPrecision(itClass, cc)) cc = itClass
                else while ((itClass = itClass.superclass) != null)
                    // This will always qualify once, Object for totally unrelated:
                    if (itClass.isAssignableFrom(cc)) { cc = itClass; break; }
            }
            assert cc != null
        }
        if (cc == null) return cc.getClass()
        if (cc == Object.class) return cc
        Map retStruct = [
            commonClass: cc,
            anyNull: anyNull,
            max: max,
            min: min,
        ]
        if (!anyScalar) {
            final List glom = []
            levelMembers.each() {
                if (it != null) glom.addAll it
            }
            retStruct.child = valSummary glom
        }
        retStruct
    }

    static private boolean compatible(def vSum, def pSpecTree) {
        logger.fine "vSum isa ${vSum.getClass().name}:\n  $vSum"
        logger.fine "pSpecTree isa ${pSpecTree.getClass().name}:\n  $pSpecTree"
        Class checkSpec
        boolean specPrimitive
        if (Map.class.isInstance(pSpecTree)) {  // pTree Collection
            if (vSum == null) return true  // array or Coll. param may be null
            if (vSum == Void.class) return true  // unrestricted
            if (vSum == null.getClass())
                // Only conflict is if array of primitives
                return pSpecTree.listType != null ||      // non-array
                  pSpecTree.members !instanceof String || // nested coll/array
                  pSpecTree.members.indexOf('.') > 0      // non-prim. scalar
            if (vSum == Object.class) return pSpecTree.members == 'java.lang.Object'
            if (Map.class.isInstance(vSum)) {
                // incompatible if any null member for native array of prim.
                if (vSum.anyNull && pSpecTree.listType == null
                  && pSpecTree.members instanceof String
                  && pSpecTree.members.indexOf('.') < 0) return false
                if (pSpecTree.members instanceof String) {
                    // vSum.commonClass / pS.members compatible?
                    specPrimitive = pSpecTree.members.indexOf('.') < 0
                    checkSpec = specPrimitive ?
                      classForPrimitiveStr(pSpecTree.members) :
                      Class.forName(pSpecTree.members)
                    if (checkSpec.isAssignableFrom(vSum.commonClass))
                        return true
                    // Check for special cases where a commonClass
                    // precision-alternate could work.
                    if (vSum.commonClass == Integer.class
                      && checkSpec == Long.class) return true
                    if (vSum.commonClass == Float.class
                      && checkSpec == Double.class) return true
                    if (vSum.commonClass == Long.class
                      && checkSpec == Integer.class
                      && vSum.max <= Integer.MAX_VALUE
                      && vSum.min >= Integer.MIN_VALUE) return true
                    // I believe for this can convert with:
                    //  Float.valueOf(doubleVal.floatValue())
                    if (vSum.commonClass == Double.class
                      && checkSpec == Float.class) return true
                    return false  // incompatible scalar members
                }
                // pS.members is a Map with .listType and .members
                // I believe that vSum.commonClass and ps.listType here is
                // only useful for conversion, since null is not an issue here.
                return compatible(vSum.child, pSpecTree.members)
            }
            assert !Class.class.isInstance(vSum):
              "Unexpected vSum ${vSum.getClass().name}: $vSum"
            // vSum is a non-null scalar, but pSpecTree demands a coll/array
            return false
        } else {  // pTree scalar
            specPrimitive = pSpecTree.indexOf('.') < 0
            if (vSum == null)
                // null value satisfies all scalar param other than
                // primitive type
                return !specPrimitive
            checkSpec = specPrimitive ?
              classForPrimitiveStr(pSpecTree) : Class.forName(pSpecTree)
            if (checkSpec.isInstance(vSum)) return true
            // Here we can now allow conversion to lower prevision because
            // jsonSlurper would only have made the higher prevision if it
            // would overflow the lower precision.
            return isHigherPrecision(checkSpec, vSum.getClass())
        }
    }

    static private def convertPVal(def val, def vSum, def pSpecTree) {
        assert val == null || vSum != null
        assert pSpecTree != null
        logger.fine "Converting val isa ${val.getClass().name}:\n  $val"
        logger.fine "vSum isa ${vSum.getClass().name}:\n  $vSum"
        logger.fine "pSpecTree isa ${pSpecTree.getClass().name}:\n  $pSpecTree"
        Class checkSpec
        boolean specPrimitive
        if (val == null) return val
        if (Map.class.isInstance(pSpecTree)) {  // pTree Collection
            // A happy (extreme) limitation of Groovy is that it ignores
            // generic <qualifier>s and is able to call any Groovy or Java
            // method of correct collection type with any gen. <qualifier>.
            // So when pSpecTree.listType != null, just return the val.
            if (vSum == Void.class)
                // Return 0-length list of correct member-type
                return pSpecTree.listType == null ?
                  getArr0(pSpecTree.members) : val
            if (vSum == null.getClass() || vSum == Object.class)
                // Return typed list, but no need to convert any values
                // (auto-boxing will take care of primitives).
                return pSpecTree.listType == null ?
                  toArray(val, pSpecTree.members) : val
            if (Map.class.isInstance(vSum)) {
                if (pSpecTree.members instanceof String) {
                    specPrimitive = pSpecTree.members.indexOf('.') < 0
                    checkSpec = specPrimitive ?
                      classForPrimitiveStr(pSpecTree.members) :
                      Class.forName(pSpecTree.members)
                    // Convert all scalar members
                    final Collection convertedScalars = val.collect() {
                        // If null weren't allowed, the compatible function
                        // would have returned false and prevented run of this
                        // function.
                        if (it == null) return it
                        if (checkSpec.isInstance(it)) return it
                        // Check for special cases where a commonClass
                        // precision-alternate could work.
                        if (it instanceof Integer
                          && checkSpec == Long.class) return (Long) it
                        if (it instanceof Float
                          && checkSpec == Double.class) return (Double) it
                        if (it instanceof Long
                          && checkSpec == Integer.class
                          && it <= Integer.MAX_VALUE
                          && it >= Integer.MIN_VALUE) return (Integer) it
                        // I believe for this can convert with:
                        //  Float.valueOf(doubleVal.floatValue())
                        if (it instanceof Double && checkSpec == Float.class)
                            return Float.valueOf(it.floatValue())
                        assert false:
                        "incompatible scalar member ($it) with checkSpec $checkSpec"
                    }
                    return pSpecTree.listType == null ?
                      toArray(convertedScalars, pSpecTree.members) :
                      convertedScalars
                }
                // pS.members is a Map with .listType and .members
                // vSum.commonClass and ps.listType usefulhere
                Collection cContainer = new ArrayList()
                cContainer.addAll(val.collect() {
                    convertPVal it, vSum.child, pSpecTree.members
                })
                return pSpecTree.listType == null ?
                  toArray(cContainer, pSpecTree.members) : cContainer
            }
            assert false: "Got unexpected vSum of ${vSum.getClass().name}: $vSum"
        } else {  // pTree scalar
            assert val == vSum
            specPrimitive = pSpecTree.indexOf('.') < 0
            checkSpec = specPrimitive ?
              classForPrimitiveStr(pSpecTree) : Class.forName(pSpecTree)
            if (checkSpec.isInstance(val)) return val  // let auto-box
            // Only remaining case is we need to increase precision
            final Class vSumClass = val.getClass()
            if (checkSpec == Double.class && vSumClass == Float.class)
                return (Double) val
            if (checkSpec == Long.class && vSumClass == Integer.class)
                return (Long) val
            assert false:
              "Unexpected scalar type '${vSum.getClass().name}' for: $vSum"
        }
    }

    /**
     * Attempts .executable executions with variants of paramVals until one
     * success.
     * Throws if non succeeds.
     */
    private def _exec() {
        int i = -1
        final Object[] convertedPVals = paramVals.collect() { pVal ->
            convertPVal pVal, pSummaries[++i], pSpecTrees[i]
        }
        executable instanceof Constructor ?
            executable.newInstance(convertedPVals) :
            executable.invoke(inst, convertedPVals)
    }

    /**
     * Constructor invocation
     */
    static def exec(final Class cl, final List params) {
        logger.log Level.INFO, "Constructor exec for {0}-param {1}",
          params.size(), cl.simpleName
        // Don't yet know if have the same invoke-elicits-JVM-restart issue as
        // the instance method() exec method below.
        new Executor(cl, null, null, params)._exec()
    }

    /**
     * Instance method invocation
     */
    static def exec(final Object inst,
    final String methodName, final List<Object> params) {
        logger.log Level.INFO, "Instance exec for {0}-param {1}.{2}",
          params.size(), inst.getClass().simpleName, methodName
        // Major Groovy defect here, even in 4.0.0.
        // The entire JRE load starts over (with same pid) if invoke done
        // with params has nothing to do with resolving inst or params here.
        //logger.log Level.WARNING, "Invoking {0}.{1} of {2}",
          //cs.keySet()[0].declaringClass.simpleName, methodName, inst.getClass().simpleName
        new Executor(inst.getClass(), inst, methodName, params)._exec()
    }

    /**
     * Static method invocation
     */
    static def exec(
    final Class cl, final String methodName, final List<Object> params) {
        logger.log Level.INFO, "Static exec for {0}-param {1}.{2}",
          params.size(), cl.simpleName, methodName
        // Don't yet know if have the same invoke-elicits-JVM-restart issue as the
        // instance method() exec method below.
        new Executor(cl, null, methodName, params)._exec()
    }
}
