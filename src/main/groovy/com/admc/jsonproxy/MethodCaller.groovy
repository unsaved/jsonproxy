package com.admc.jsonproxy

import java.util.logging.Level
import java.lang.reflect.Executable
import java.lang.reflect.Parameter
import java.lang.reflect.Constructor
import com.admc.groovy.GroovyUtil

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

        // Cull out candidates based on pTypes

        if (candidates.size() !== 1) throw new RuntimeException(
          "${candidates.size()} matching $cl.simpleName"
          + ".${methodName == null ? '<CONST>' : methodName} executables:\n"
          + GroovyUtil.pretty(candidates.collectEntries() { [it.key.name, it.value.simpleName]}))
        logger.log Level.WARNING, "{0} {1}.{2} Candidates: {3}",
          candidates.size(), cl.simpleName, methodName, GroovyUtil.pretty(
          candidates.collectEntries() { [it.key.name, it.value.simpleName]})
        executable = candidates.keySet()[0]
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
