host=mysite.com

# ЛОКАЛЬНО:

scp service/bugs-start $host:/usr/local/bin/bugs-start &&
scp service/bugs-stop $host:/usr/local/bin/bugs-stop &&
scp service/bugs $host:/etc/init.d/bugs &&

# НА СЕРВЕРЕ:

apt-get update &&

apt-get install mysql-server -y &&

wget --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u121-b13/e9e7ea248e2c4826b92b3f075a80e441/jdk-8u121-linux-x64.tar.gz &&

mkdir /opt/jdk &&

tar -zxf jdk-8u121-linux-x64.tar.gz -C /opt/jdk/ &&

rm jdk-8u121-linux-x64.tar.gz &&

update-alternatives --install /usr/bin/java java /opt/jdk/jdk1.8.0_121/bin/java 100 &&

update-alternatives --install /usr/bin/javac javac /opt/jdk/jdk1.8.0_121/bin/javac 100 &&

mkdir /var/bugs &&

chmod +x /usr/local/bin/bugs-start &&
chmod +x /usr/local/bin/bugs-stop &&
chmod +x /etc/init.d/bugs &&
update-rc.d bugs defaults

mysql -u root -p
# CREATE DATABASE bugs;
# apply schema.sql
# CREATE USER 'bugs_user'@'%' IDENTIFIED BY 'password';
# GRANT ALL PRIVILEGES ON bugs TO 'bugs_user'@'%';
# FLUSH PRIVILEGES;

nano /etc/mysql/my.cnf
# change line to: bind-address = $host

# ЛОКАЛЬНО:

scp update_server.sh $host:/var/bugs &&
scp rollback_server.sh $host:/var/bugs