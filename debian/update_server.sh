#!/bin/bash

service bugs stop &&
cp /var/bugs/bugs.jar /var/bugs/bugs.jar.old &&
cp /var/bugs/bugs.jar.new /var/bugs/bugs.jar &&
service bugs start