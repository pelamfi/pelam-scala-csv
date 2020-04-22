#!/bin/bash
. ~/.sdkman/bin/sdkman-init.sh


# 2020-04 there seems to be some incompatibility with this java version and sbt
# sdk use sbt 1.3.10 || exit

sdk use scala 2.13.1 || exit
sdk use gradle 6.3 || exit
sdk use java 14.0.0.hs-adpt || exit

set -euo pipefail
IFS=$'\n\t'

gradle clean install || exit

