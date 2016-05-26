#!/bin/bash

set -e

mvnprojects=(\
"lt.utilities" \
"lt.seg" \
"lt.lm" \
"lt.ltbot" \
"lt.lm.webapp" \
)

for p in ${mvnprojects[@]}; do
  echo "building ${p}..."
  mvn clean install javadoc:jar source:jar -Dadditionalparam='-Xdoclint:none' -DskipTests -f ${p}
  mvn eclipse:clean eclipse:eclipse -f ${p}
done
