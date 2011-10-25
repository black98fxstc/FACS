CFLAGS=-g

whtaped : whtaped.o


whtaped.o : whtaped.c


clean :
	-rm whtaped.o
	-rm whtaped

install : whtaped whouse.conf whouse
	cp whtaped /usr/sbin/whtaped
	cp whouse.conf /etc/whouse.conf
	cp whouse /etc/rc.d/init.d/whouse
	/sbin/chkconfig  --add  whouse

uninstall :
	-/sbin/service whouse stop
	-/sbin/chkconfig --del whouse
	-rm /usr/sbin/whtaped
	-rm /etc/whouse.conf
	-rm /etc/rc.d/init.d/whouse
