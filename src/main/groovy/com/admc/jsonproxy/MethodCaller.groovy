package com.admc.jsonproxy

import java.util.logging.Level
import java.lang.reflect.Executable
import java.lang.reflect.Parameter
import com.admc.groovy.GroovyUtil

@groovy.util.logging.Log(value = 'logger')
class MethodCaller {
    private Class cl
    private Object inst
    private String methodName
    private List<Object> paramVals
    private Executable executable

    private MethodCaller(final Class cl, final Object inst,
    final String methodName, final List<Object> params) {
        this.cl = cl
        this.inst = inst
        this.methodName = methodName
        paramVals = params
        final int paramCount = paramVals.size()
        List<Class> pTypes = params.collect() { it.getClass() }
        def initialCandidates = methodName == null ? cl.declaredConstructors : cl.declaredMethods
        if (initialCandidates.size() < 1)
            throw new RuntimeException(
              "${initialCandidates.size()} total ${paramCount}-param $cl.simpleName"
              + ".${methodName == null ? '<CONST>' : methodName}'s")
        final Map<Executable, Parameter[]> candidates = methodName == null ?
          initialCandidates.collectEntries() {
              it.parameterCount == paramCount ?
                [(it): it.typeParameters] :  [:]
          } :
          initialCandidates.collectEntries() {
              it.name == methodName && it.parameterCount == paramCount ?
                [it, it.parameterTypes] :  [:]
          }
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
     * Constructor invocation
     */
    static def call(final Class cl, final List<Object> params) {
        logger.log Level.INFO, "Constructor call for {0}-param {1}", params.size(), cl.simpleName
        // Don't yet know if have the same invoke-elicits-JVM-restart issue as the
        // instance method() call method below.
        new MethodCaller(cl, null, null, params).executable.newInstance(params as Object[])
    }

    /**
     * Instance method invocation
     */
    static def call(final Object inst,
    final String methodName, final List<Object> params) {
        logger.log Level.INFO, "Instance call for {0}-param {1}.{2}",
          params.size(), inst.getClass().simpleName, methodName
        // Major Groovy defect here, even in 4.0.0.
        // The entire JRE load starts over (with same pid) if invoke done with params
        // has nothing to do with resolving inst or params here.
        //logger.log Level.WARNING, "Invoking {0}.{1} of {2}",
          //cs.keySet()[0].declaringClass.simpleName, methodName, inst.getClass().simpleName
        new MethodCaller(inst.getClass(), inst, methodName, params).executable.
          invoke(inst, params as Object[])
    }

    /**
     * Static method invocation
     */
    static def call(final Class cl, final String methodName, final List<Object> params) {
        logger.log Level.INFO, "Static call for {0}-param {1}.{2}",
          params.size(), cl.simpleName, methodName
        // Don't yet know if have the same invoke-elicits-JVM-restart issue as the
        // instance method() call method below.
        new MethodCaller(cl, null, methodName, params).executable.invoke(null, params as Object[])
    }
}
