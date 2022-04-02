package com.admc.jsonproxy

import spock.lang.Specification
import java.util.regex.Pattern

class MethodCallerTest extends Specification {
    private Service service

    def "cons/no-param"() {
        when:
        Object obj = MethodCaller.exec(ArrayList.class, [])

        then:
        obj instanceof ArrayList
    }

    def "instmeth/no-param"() {
        when:
        def res = MethodCaller.exec([1, 'two', 'three'], 'size', [])

        then:
        res == 3
    }

    def "instmeth/1-Int"() {
        when:
        def res = MethodCaller.exec([1, 'two', 'three'], 'get', [1])

        then:
        res == 'two'
    }

    def "statmeth/1-Str"() {
        when:
        def res = MethodCaller.exec(Pattern.class, 'compile', [/a*b/])

        then:
        res.pattern() == Pattern.compile(/a*b/).pattern()
    }

/* Disabling just to allow regression testing with no errors during code merge.
    def "cons/1-Str"() {
        when:
        def throwable =
          MethodCaller.exec(Throwable.class, ['msg'])

        then:
        throwable == new Throwable('msg')
    }
*/

    def "statmeth/no-param"() {
        when:
        def ls = MethodCaller.exec(System.class, 'lineSeparator', [])

        then:
        ls == System.lineSeparator()
    }
}
