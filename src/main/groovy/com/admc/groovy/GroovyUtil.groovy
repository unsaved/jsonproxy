package com.admc.groovy

import java.util.regex.Pattern
import java.util.regex.Matcher
import static groovy.json.JsonOutput.*

class GroovyUtil {
    static private final Pattern INDENT_STR_PAT =
      Pattern.compile '^  +', Pattern.MULTILINE

    static String pretty(final def obj) {
        final Matcher m = INDENT_STR_PAT.matcher prettyPrint(toJson(obj))
        final StringBuffer sb = new StringBuffer()
        while (m.find())
          m.appendReplacement sb, m.group().substring(
            0, ((m.group().length()/2).toInteger()))
        m.appendTail sb
        return sb.toString().replaceAll('[{]\\s+[}]', '{}').
          replaceAll('\\[\\s+\\]', '[]')
    }
}
