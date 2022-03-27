#!/usr/bin/env groovy

import java.sql.Connection
import java.sql.DriverManager
import groovy.json.JsonOutput

// Workaround for Groovy defect hitting MethodCaller
@groovy.transform.Field static int ctr = 0
if (ctr > 2) {
    System.err.println 'Imploing Groovy JRE-reinit workaround'
} else {
    def c = DriverManager.getConnection('jdbc:hsqldb:mem:name', 'SA', '')

    // Work up test data outside of jsonproxy environment
    c.createStatement().execute 'CREATE TABLE tbl(i INT, s VARCHAR(20))'
    c.createStatement().execute(/INSERT INTO tbl VALUES(1, 'one')/)

    def s = com.admc.jsonproxy.MethodCaller.call(c, 'createStatement', [])
    def rs = com.admc.jsonproxy.MethodCaller.call(s, 'executeQuery',
      ['SELECT * FROM tbl'])
    println 'graceful completion'
}
