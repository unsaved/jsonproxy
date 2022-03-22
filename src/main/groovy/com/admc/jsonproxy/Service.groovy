#!/usr/bin/env groovy

package com.admc.jsonproxy

class StringDriver {
    static void main(String[] args) {
        List<String> argList = args
        println "Helo.  $argList.size args: $args"
        System.exit 0
    }
}
