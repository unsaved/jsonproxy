#!/usr/bin/env bash

# To make this easy to write, it's only intended to invoke from the
# root jsonproxy project directory.

GROOVY_SCRIPT="${0%.bash}.groovy"
[ -f  "$GROOVY_SCRIPT" ] || {
    echo "Where did '$GROOVY_SCRIPT' go to?" 1>&2
    exit 2
}
#[ -f build/libs/jsonproxy.jar ] || {
    #echo "Build 'jsonproxy.jar' before using this script" 1>&2
    #exit 2
#}

groovy -cp src/main/groovy:build/classes/java/ "$GROOVY_SCRIPT" "$@"
