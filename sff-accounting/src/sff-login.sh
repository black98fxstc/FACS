#!/bin/bash
/usr/bin/touch /var/sff/login/did-login
/usr/bin/java -cp /var/sff/login/sff-logins.jar sff.accounting.MacLogin /var/sff/login Login $1
