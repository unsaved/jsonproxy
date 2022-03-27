package com.admc.jsonproxy

import java.util.logging.Level
import java.lang.reflect.Type
import com.admc.groovy.GroovyUtil

@groovy.util.logging.Log(value='logger')
class MethodCaller {
    private Class cl
    private Object inst
    private String methodName
    private List<Object> paramVals
    private def candidates

    private MethodCaller(final Class cl, final Object inst,
    final String methodName, final List<Object> params) {
        this.cl = cl
        this.inst = inst
        this.methodName = methodName
        paramVals = params
        final int paramCount = paramVals.size()
        List<Class> pTypes = params.collect() { it.getClass() }
        //def candidates = inst == null ?
        candidates = inst == null ?
          cl.declaredConstructors.collectEntries() {
              it.parameterCount == paramCount ?
                [(it): it.typeParameters] :  [:]
          } :
          cl.declaredMethods.collectEntries() {
              it.name == methodName && it.parameterCount == paramCount ?
                //[it, it.parameterTypes.length] :  [:]
                [it, it.parameterTypes] :  [:]
          }
        logger.log Level.WARNING, "{0} {1}.{2} Candidates: {3}",
          candidates.size(), cl.simpleName, methodName, GroovyUtil.pretty(
          candidates.collectEntries() { [it.key.name, it.value.simpleName]})
    }

    static def call(final Class cl, final List<Object> params) {
        logger.log Level.INFO, "Constructor call for {0}-param {1}", params.size(), cl.simpleName
        new MethodCaller(cl, null, null, params)
        def cs = new MethodCaller(cl, null, null, params).candidates
        if (cs.size() != 1) throw new Error("Resolved to ${cs.size()}  constructors")
        // Don't yet know if have the same invoke-elicits-JVM-restart issue as the
        // method() call method below.
        cs.keySet()[0].newInstance(params as Object[])
    }

    static def call(final Object inst,
    final String methodName, final List<Object> params) {
        logger.log Level.INFO, "Instance call for {0}-param {1}.{2}",
          params.size(), inst.getClass().simpleName, methodName
        def cs = new MethodCaller(inst.getClass(), int, methodName, params).candidates
        if (cs.size() != 1) throw new Error("Resolved to ${cs.size()}  methods")
        // Major Groovy defect here, even in 4.0.0.
        // The entire JRE load starts over (with same pid) if invoke done with params
        // has nothing to do with resolving inst or params here.
        //logger.log Level.WARNING, "Invoking {0}.{1} of {2}",
          //cs.keySet()[0].declaringClass.simpleName, methodName, inst.getClass().simpleName
        cs.keySet()[0].invoke(inst, params as Object[])
    }
}
