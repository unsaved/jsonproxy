#!/usr/bin/env bash

# To make this easy to write, it's only intended to invoke from the
# root jsonproxy project directory.

[ -f build/libs/jsonproxy.jar ] || {
    echo "Build 'jsonproxy.jar' before using this script" 1>&2
    exit 2
}
[ -f bin/driver.groovy ] || {
    echo "Where did 'bin/driver.groovy' go to?" 1>&2
    exit 2
}

groovy -cp build/libs/jsonproxy.jar bin/driver.groovy
