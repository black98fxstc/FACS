#!/bin/bash
perl ../calweb/cw_utilities/reportall.pl
echo "Content-Type: text/html"
/usr/java/j2sdk1.4.2_03/bin/java -cp /var/www/data/accounting/sff-logins.jar sff.accounting.ScheduleSummary machines.txt
