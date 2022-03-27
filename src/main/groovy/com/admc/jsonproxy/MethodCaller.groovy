package com.admc.jsonproxy

import java.util.logging.Level
import java.lang.reflect.Type
import com.admc.groovy.GroovyUtil

@groovy.util.logging.Log(value='logger')
class MethodCaller {
    private Class cl
    private Object inst
    private String methodName
    private paramVals
    def candidates

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
        new MethodCaller(cl, null, null, params)
    }

    static def call(final Object inst,
    final String methodName, final List<Object> params) {
        def cs = new MethodCaller(inst.getClass(), int, methodName, params).candidates
        if (cs.size() != 1) throw new Error("Resolved to ${cs.size()}  methods")
        // Major Groovy defect here, even in 4.0.0.
        // The entire JRE load starts over (with same pid) if invoke done with params
        // has nothign to do with resolving inst or params here.
        cs.keySet()[0].invoke(inst, params as Object[])
    }
}
