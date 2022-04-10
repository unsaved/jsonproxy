#!/usr/bin/env groovy

package com.admc.jsonproxy

import java.util.logging.Level
import java.lang.reflect.Type
import java.util.regex.Matcher
import java.util.Map
import java.util.Collection
import java.lang.reflect.Method

@groovy.util.logging.Log(value = 'logger')
class ParamReqs {
    private Class clazz
    private ParamReqs leafPR
    private Integer arrayDims
    static {
        // Groovy doesn't honor JUL customizations, including
        // java.util.logging.config.file sysprop, unless you run this:
        java.util.logging.LogManager.logManager.updateConfiguration null
    }


    static void main(String[] sa) {
        if (sa.size() !== 1)
            throw new IllegalArgumentException(
              'SYNTAX: java com.admc.jsonproxy.ParamReqs pkg.Class.scalarMeth')
        Class cl
        Matcher m = sa[0] =~ /([\w.]+)[.](\w+)/
        if (!m)
            throw new IllegalArgumentException('Malformatted method spec.\n' +
              'SYNTAX: java com.admc.jsonproxy.ParamReqs pkg.Class.scalarMeth')
        try {
            cl = Class.forName(m.group(1))
        } catch (Exception e) {
            System.err.println "No such class: ${m.group 1}"
            System.exit 1
        }
        Method[] methods = cl.methods.findAll() { it.name==m.group(2) }
        if (methods.length != 1) {
            System.err.println "${methods.size()} method smatch ${sa[0]}"
            System.exit 1
        }
        Type[] types = methods[0].genericParameterTypes
        if (types.size() != 1) {
            System.err.println "Method ${sa[0]} takes ${types.size()} params"
            System.exit 1
        }
        println(new ParamReqs(types[0]))
    }

    String toString(final int level = 0) {
        '  '*level +
          (clazz==null ? '<ARRAY>': clazz.name) +
          (arrayDims==null ? '' : "[$arrayDims]") +
          (leafPR == null ? '' : ('\n' + leafPR.toString(level+1)))
    }

    ParamReqs(Type paramType) {
        this(paramType.toString())
    }

    private ParamReqs(String gSpec) {
        /* In this parse block, we always set memberSpec, + one case of:
         *    colType set.   Collection (capital 'C')
         *    arrayDims set. array
         *    neither set.   Scalar  */
        logger.fine "Trying $gSpec"
        Matcher m
        String colType, memberSpec
        if (gSpec.endsWith('>')) {
            // Outermost generified Collection
            m = gSpec =~ /([^<]+)<(.+)>/
            if (!m.matches()) throw new Exception(
              "Failed to parse outer Collection type from: $gSpec")
            colType = m.group 1
            memberSpec = m.group 2
        } else if (gSpec.endsWith('[]')) {
            m = gSpec =~ /(.+?)((?:\[\])+)/
            if (!m.matches())
              throw new Exception("Malfomatted []-suffix spec: $gSpec")
            // Outermost is an array, suffix form
            arrayDims = m.group(2).length()/2
            //memberSpec = 'TYPE ' + gSpec.substring(0, gSpec.length()-2)
            memberSpec = m.group 1
        } else if (gSpec.contains(' ')) {
            //m = gSpec =~ /(?:interface|class|TYPE) (.+)/
            m = gSpec =~ /(?:interface|class) (.+)/
            if (!m.matches()) throw new Exception('Got a spec wih space '
              + "where 1st token is not 'interface' or 'class': $gSpec")
            if (m.group(1).startsWith('[')) {
                m = m.group(1) =~ /(\[+)([A-Z])(?:(\[?[a-zA-Z_][\w.]*);)?/
                if (!m.matches()) throw new Exception(
                  "Failed to parse single-level array from: $gSpec")
                arrayDims = m.group(1).length()
                if (m.group(2) == 'L') {
                    if (m.group(3) == null)
                        throw new Error(
                          "[L spec with no class specifier: $gSpec")
                    //memberSpec = 'TYPE ' + m.group(3)
                    memberSpec = m.group 3
                } else {
                    if (m.group(3) != null)
                        throw new Error(
                          "[non-S spec with class specifier: $gSpec")
                    switch (m.group(2)) {
                      //case 'L': memberSpec = 'TYPE ' + m.group(2); break
                      case 'I': memberSpec = 'int'; break
                      case 'J': memberSpec = 'long'; break
                      case 'F': memberSpec = 'float'; break
                      case 'D': memberSpec = 'double'; break
                      case 'B': memberSpec = 'byte'; break
                      case 'Z': memberSpec = 'boolean'; break
                      case 'S': memberSpec = 'short'; break
                      case 'C': memberSpec = 'char'; break
                      default:
                        throw new Error(
                          'Unexpected 1-level primitive array type '
                          + "${m.group(2)}: $gSpec")
                    }
                }
            } else {
                // /^TYPE|class|interface x.y$/ is simple scalar object spec
                if (Map.class.isAssignableFrom(Class.forName(m.group(1))))
                    throw new RuntimeException(
                      "Sorry but Maps not supported yet: ${m.group(1)}")
                if (Collection.class.isAssignableFrom(Class.forName(m.group(1)))) {
                    colType = m.group 1  // unconstrained Collection
                    memberSpec = 'java.lang.Object'
                } else {
                    memberSpec = m.group(1)  // Class scalar
                }
            }
        } else {
            // Single token
            // Outermost is a scalar, primitive or Object
            if (!gSpec.matches(/[_a-zA-Z][\w.]*/))
                throw new Error("Unrecognized type format: $gSpec")
            memberSpec = gSpec
        }

        if (colType != null) {
            clazz = Class.forName(colType)
            leafPR = new ParamReqs(memberSpec)
            return
        }
        if (memberSpec.matches(/[a-z]+$/)) switch(memberSpec) {
          case 'int': clazz = int.class; break
          case 'long': clazz = long.class; break
          case 'float': clazz = float.class; break
          case 'double': clazz = double.class; break
          case 'byte': clazz = byte.class; break
          case 'boolean': clazz = boolean.class; break
          case 'short': clazz = short.class; break
          case 'char': clazz = char.class; break
          default: assert false:
            "Unexpected plain-word memberSpec: $memberSpec"
        } else if (memberSpec.matches(/[\w.]+$/))
            clazz = Class.forName memberSpec
        else
            leafPR = new ParamReqs(memberSpec)
    }
}
