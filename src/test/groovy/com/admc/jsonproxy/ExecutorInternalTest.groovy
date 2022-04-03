package com.admc.jsonproxy

import spock.lang.Specification
import java.lang.reflect.Executable
import static Executor.*
import java.lang.reflect.Modifier

class ExecutorInternalTest extends Specification {
    static private Map<String, Executable> testMethods =
      SampleMethods.class.declaredMethods.collectEntries() {
        (it.name.startsWith('$') || !Modifier.isStatic(it.modifiers)) ? [:] :
        [it.name, it]
    }

    static private String getP1PTree(final String methodKey) {
        final Executable exable = testMethods[methodKey]
        assert exable != null: "No such method: $methodKey"
        toTree(exable.genericParameterTypes[0].toString()).toString()
    }

    static private void invokeP1(
    final String methodKey, final boolean compat, final Object inVal) {
        final Executable exable = testMethods[methodKey]
        assert exable != null: "No such method: $methodKey"
        final def vSu = valSummary inVal
        final def pST = toTree(exable.genericParameterTypes[0].toString())
        assert compatible(vSu, pST) == compat
        if (!compat) return
        final def pVl = convertPVal inVal, vSu, pST
        exable.invoke(null, [pVl] as Object[])
    }

    def "toTree"(final String methodName, final String expectTreeStr) {
        expect:
        getP1PTree(methodName) == expectTreeStr
        where:
        methodName | expectTreeStr
        'o' | 'java.lang.Object'
        'S' | 'java.lang.String'
        'T' | '[listType:interface java.util.List, members:java.lang.Object]'
        'i' | 'int'
        'l' | 'long'
        'I' | 'java.lang.Integer'
        'L' | 'java.lang.Long'
        'ac' | '[listType:null, members:char]'
        'ah' | '[listType:null, members:short]'
        'ab' | '[listType:null, members:boolean]'
        'ay' | '[listType:null, members:byte]'
        'ad' | '[listType:null, members:double]'
        'af' | '[listType:null, members:float]'
        'al' | '[listType:null, members:long]'
        'ai' | '[listType:null, members:int]'
        'aS' | '[listType:null, members:java.lang.String]'
        'aai' | '[listType:null, members:[listType:null, members:int]]'
        'aTI' | '[listType:null, members:[listType:interface java.util.List, members:java.lang.Integer]]'
        'aaS' | '[listType:null, members:[listType:null, members:java.lang.String]]'
        'TS' | '[listType:interface java.util.List, members:java.lang.String]'
        'aTS' | '[listType:null, members:[listType:interface java.util.List, members:java.lang.String]]'
        'TTS' | '[listType:interface java.util.List, members:[listType:interface java.util.List, members:java.lang.String]]'
        'TaS' | '[listType:interface java.util.List, members:[listType:null, members:java.lang.String]]'
        'Tai' | '[listType:interface java.util.List, members:[listType:null, members:int]]'
    }

    /**
     * JsonSlurper will never give us a value with a high-precision data type
     * that can be demoted without data corruption, but if we change parser
     * or something, could add tests like this one for safe vs. corrupting
     * datatype decrease (and same for 'I', 'i', 'Ia', etc):
     *   'iaa' | [[1073741824L,-2L], [5L, 1]]  // valid (high) int
     *   'iaa' | [[4294967296L,-2L], [5L, 1]]  // int overflow
     */
    def "invokeP1"(
    final String methodName, final boolean isCompatible, final def invocPVals) {
        expect:
        invokeP1 methodName, isCompatible, invocPVals
        where:
        methodName | isCompatible | invocPVals
        'i' | true | 8
        'I' | true | 8
        'i' | false | 8L  // prohibit precision decrease
        'I' | false | 8L  // prohibit precision decrease
        'i' | false | null
        'I' | true | null
        'l' | true | 8  // escalate precision
        'L' | true | 8  // escalate precision
        'l' | true | 8L
        'L' | true | 8L
        'l' | false | null
        'L' | true | null
        'o' | true | 8
        'o' | true | 'str'
        'o' | true | null
        'S' | false | 8
        'S' | true | 'str'
        'S' | true | null
        'T' | true | null
        'T' | true | [null]
        'T' | true | [null, null, null]
        'T' | true | [2, 3L, 'str', null]
        'aS' | true | null
        'aS' | true | [null]
        'aS' | true | [null, null, null]
        'aS' | false | 'str'
        'aS' | false | 3
        'aS' | false | [3]
        'aS' | true | []
        'aS' | true | ['str']
        'aS' | true | ['str1', 'str2', 'str3']
        'aS' | true | ['str1', null, 'str3']
        'TS' | true | null
        'TS' | true | [null]
        'TS' | true | [null, null, null]
        'TS' | false | 'str'
        'TS' | false | 3
        'TS' | false | [3]
        'TS' | true | []
        'TS' | true | ['str']
        'TS' | true | ['str1', 'str2', 'str3']
        'TS' | true | ['str1', null, 'str3']
        'ai' | true | null
        'ai' | false | [null]
        'ai' | false | [null, null, null]
        'ai' | false | 'str'
        'ai' | false | 3
        'ai' | true | [3]
        'ai' | true | []
        'ai' | true | [3]
        'ai' | true | [3, 4, 5]
        'ai' | false | [3, null, 5]
        'ai' | false | [3, 4D, 5]
        'al' | true | null
        'al' | false | [null]
        'al' | false | [null, null, null]
        'al' | false | 'str'
        'al' | false | 3
        'al' | false | ['str']
        'al' | true | []
        'al' | true | [3]
        'al' | true | [3L]
        'al' | true | [3, 4, 5]
        'al' | false | [3, null, 5]
        'al' | true | [3, 4L, 5]
        'aaS' | true | [null, null]
        'aaS' | true | [null, null, ['str']]
        'aaS' | true | [[null], [null,  null]]
        'aaS' | true | [['str11', 'str12', 'str13'], ['str21', null, 'str23'], null]
        'aaS' | true | [['str11', 'str12', 'str13'], ['str21', null, 'str23'], ['str3']]
        'aaS' | false | [['str11', 4, 'str13'], ['str21', null, 'str23'], ['str3']]
        'TTS' | true | [null, null]
        'TTS' | true | [null, null, ['str']]
        'TTS' | true | [[null], [null,  null]]
        'TTS' | true | [['str11', 'str12', 'str13'], ['str21', null, 'str23'], null]
        'TTS' | true | [['str11', 'str12', 'str13'], ['str21', null, 'str23'], ['str3']]
        'TTS' | false | [['str11', 4, 'str13'], ['str21', null, 'str23'], ['str3']]
        'TaS' | true | [null, null]
        'TaS' | true | [null, null, ['str']]
        'TaS' | true | [[null], [null,  null]]
        'TaS' | true | [['str11', 'str12', 'str13'], ['str21', null, 'str23'], null]
        'TaS' | true | [['str11', 'str12', 'str13'], ['str21', null, 'str23'], ['str3']]
        'TaS' | false | [['str11', 4, 'str13'], ['str21', null, 'str23'], ['str3']]
        'aTS' | true | [null, null]
        'aTS' | true | [null, null, ['str']]
        'aTS' | true | [[null], [null,  null]]
        'aTS' | true | [['str11', 'str12', 'str13'], ['str21', null, 'str23'], null]
        'aTS' | true | [['str11', 'str12', 'str13'], ['str21', null, 'str23'], ['str3']]
        'aTS' | false | [['str11', 4, 'str13'], ['str21', null, 'str23'], ['str3']]
        'aai' | true | [null, null]
        'aai' | true | [null, null, [3]]
        'aai' | false | [[null], [null,  null]]
        'aai' | false | [[null]]
        'aai' | false | [[null,  null]]
        'aai' | true | [[11, 12, 13], [21, 22, 23], null]
        'aai' | false | [[11, 12, 13], [21, null, 23], null]
        'aai' | false | [[11, 12, 13], [21, null, 23], [3]]
        'aai' | true | [[11, 4, 13], [21, 22, 23]]
        'aai' | true | [[3]]
        'Tai' | true | [null, null]
        'Tai' | true | [null, null, [3]]
        'Tai' | false | [[null], [null,  null]]
        'Tai' | false | [[null]]
        'Tai' | false | [[null,  null]]
        'Tai' | true | [[11, 12, 13], [21, 22, 23], null]
        'Tai' | false | [[11, 12, 13], [21, null, 23], null]
        'Tai' | false | [[11, 12, 13], [21, null, 23], [3]]
        'Tai' | true | [[11, 4, 13], [21, 22, 23]]
        'Tai' | true | [[3]]
        'aTI' | true | [null, null]
        'aTI' | true | [null, null, [3]]
        'aTI' | true | [[null], [null,  null]]
        'aTI' | true | [[null]]
        'aTI' | true | [[null,  null]]
        'aTI' | true | [[11, 12, 13], [21, 22, 23], null]
        'aTI' | true | [[11, 12, 13], [21, null, 23], null]
        'aTI' | true | [[11, 12, 13], [21, null, 23], [3]]
        'aTI' | true | [[11, 4, 13], [21, 22, 23]]
        'aTI' | true | [[3]]
    }
}
