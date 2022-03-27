package com.admc.jsonproxy

import spock.lang.Specification
import java.sql.Connection
import java.sql.DriverManager

class MethodCallerTest extends Specification {
    private Service service

    def "cons/no-param"() {
        when:
        Object obj = MethodCaller.call(ArrayList.class, [])

        then:
        obj instanceof ArrayList
    }

    def "method/no-param"() {
        when:
        def res = MethodCaller.call([1, 'two', 'three'], 'size', [])

        then:
        res == 3
    }
}
