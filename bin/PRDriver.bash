#!/usr/bin/env bash
PROGNAME="${0##*/}"

set +u
shopt -s xpg_echo

[ $# -gt 1 -a $1 = '-v' ] && {
    GROOVY_PARAMS='-D java.util.logging.config.file=rtfiles/log-finecon.properties'
    shift
}
[ $# -eq 1 ] || {
    echo "SYNTAX: $PROGNAME [-v] SampleMethods_methodName" 1>&2
    exit 3
}
METHOD_NAME="$1"; shift

exec groovy -cp src/test/groovy $GROOVY_PARAMS src/main/groovy/com/admc/jsonproxy/ParamReqs.groovy com.admc.jsonproxy.SampleMethods.$METHOD_NAME
