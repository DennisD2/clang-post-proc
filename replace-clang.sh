#!/bin/bash
# call sed. '>' will not work in Maven, so we use a script as wrapper.
echo "sed -E -f $2 $1 >$3"
sed -E -f $2 $1 >$3
