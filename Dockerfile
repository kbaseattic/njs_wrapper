FROM kbase/kb_jre AS build
# Multistage Build Setup
RUN apt-get -y update && apt-get -y install ant git openjdk-8-jdk make
RUN cd / && git clone https://github.com/kbase/njs_wrapper && cd /njs_wrapper/ && ./gradlew buildAll 

FROM kbase/kb_jre
# These ARGs values are passed in via the docker build command
ARG BUILD_DATE
ARG VCS_REF
ARG BRANCH=develop

#COPY ROOT WAR AND FAT JAR
COPY --from=build /njs_wrapper/dist/NJSWrapper.war /kb/deployment/jettybase/webapps/root.war
COPY --from=build /njs_wrapper/dist/NJSWrapper-all.jar /kb/deployment/lib/

# The htcondor package tries an interactive config, set DEBIAN_FRONTEND to
# noninteractive in order to prevent that
RUN apt-get update && \
    export DEBIAN_FRONTEND=noninteractive && \
    apt-get install -y htcondor zile vim libgomp1 && \
    chown -R kbase:kbase /etc/condor && \
    mkdir /scratch && \
    cd /tmp && \
    wget http://submit-3.batlab.org/nmi-runs/condorauto/2018/03/condorauto_submit-3.batlab.org_1520871025_1539075/userdir/nmi:x86_64_Debian9/results.tar.gz && \
    tar xvzf results.tar.gz && \
    cd public && \
    tar xvzf condor-8.6.10-x86_64_Debian9-stripped.tar.gz && \
    cd condor-8.6.10-x86_64_Debian9-stripped && \
    ./condor_install --prefix=/usr --type=submit --local-dir=/scratch/condor --owner=kbase --overwrite && \
    cd /tmp && \
    rm -rf results.tar.gz public && \
    mkdir /var/run/condor && \
    touch /var/log/condor/StartLog /var/log/condor/ProcLog && \
    chown kbase /run/condor /var/lock/condor /var/log/condor /var/lib/condor/execute /var/log/condor/*

# Install docker binaries based on
# https://docs.docker.com/install/linux/docker-ce/debian/#install-docker-ce
# Also add the user to the groups that map to "docker" on Linux and "daemon" on
# MacOS
RUN apt-get install -y apt-transport-https software-properties-common && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - && \
    add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable" && \
    apt-get update && \
    apt-get install -y docker-ce=18.03.0~ce-0~debian && \
    usermod -a -G 0 kbase && \
    usermod -a -G 999 kbase

USER kbase:999
COPY --chown=kbase deployment/ /kb/deployment/

# Extra all of the jars for NJS so that the scripts can use them in classpath
#RUN cd /kb/deployment/lib && unzip /kb/deployment/jettybase/webapps/root.war

ENV KB_DEPLOYMENT_CONFIG /kb/deployment/conf/deployment.cfg

# The BUILD_DATE value seem to bust the docker cache when the timestamp changes, move to
# the end
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.vcs-url="https://github.com/kbase/njs_wrapper.git" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.schema-version="1.0.0-rc1" \
      us.kbase.vcs-branch=$BRANCH \
      maintainer="Steve Chan sychan@lbl.gov"

EXPOSE 7058
ENTRYPOINT [ "/kb/deployment/bin/dockerize" ]
CMD [ "-template", "/kb/deployment/conf/.templates/deployment.cfg.templ:/kb/deployment/conf/deployment.cfg", \
      "-template", "/kb/deployment/conf/.templates/http.ini.templ:/kb/deployment/jettybase/start.d/http.ini", \
      "-template", "/kb/deployment/conf/.templates/server.ini.templ:/kb/deployment/jettybase/start.d/server.ini", \
      "-template", "/kb/deployment/conf/.templates/start_server.sh.templ:/kb/deployment/bin/start_server.sh", \
      "-template", "/kb/deployment/conf/.templates/condor_config.templ:/etc/condor/condor_config.local", \
      "-stdout", "/kb/deployment/jettybase/logs/request.log", \
      "/kb/deployment/bin/start_server.sh" ]

WORKDIR /kb/deployment/jettybase

# for a NJS worker node use the following CMD in the docker-compose file
#CMD [ "-template", "/kb/deployment/conf/.templates/condor_config.templ:/etc/condor/condor_config.local", \
#      "-stdout", "/var/log/condor/ProcLog", \
#      "-stdout", "/var/log/condor/StartLog", \
#      "/kb/deployment/bin/start-condor.sh" ]
