FROM kbase/condor-worker AS build
# Multistage Build Setup
ARG BUILD_DATE
ARG VCS_REF
ARG BRANCH=develop
COPY . /njs/
RUN echo "About to build $BRANCH" &&  cd /njs && ./gradlew buildAll

FROM kbase/condor-worker
# Copy configs for dockerize
RUN rm -rf /kb/deployment/conf/
COPY --chown=kbase deployment/ /kb/deployment/

# Copy War and Fat Jar into root.war and for distribution to the worker nodes in /kb/deployment/lib
COPY --from=build /njs/dist/NJSWrapper.war /kb/deployment/jettybase/webapps/root.war
COPY --from=build /njs/dist/NJSWrapper-all.jar /kb/deployment/lib/

ENV JETTY_HOME /jetty

RUN mkdir $JETTY_HOME && cd $JETTY_HOME \
&& wget https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/9.4.12.v20180830/jetty-distribution-9.4.12.v20180830.tar.gz \
&& tar -xvf jetty-distribution-9.4.12.v20180830.tar.gz && rm -rf jetty-distribution-9.4.12.v20180830.tar.gz && mv jetty-distribution-9.4.12.v20180830/* $JETTY_HOME

#RUN mkdir $JETTY_HOME && cd $JETTY_HOME && tar -xvf /kb/deployment/jetty.tgz && mv jetty/* .

RUN chmod 777 /etc/condor



#Install Python3 and Libraries
RUN yum install -y centos-release-scl && yum -y update && yum install -y rh-python36

RUN /kb/deployment/bin/install_python_dependencies.sh

ENTRYPOINT [ "/kb/deployment/bin/dockerize" ]
