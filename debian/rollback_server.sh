#!/bin/bash

service bugs stop &&
cp /var/bugs/bugs.jar.old /var/bugs/bugs.jar &&
service bugs start