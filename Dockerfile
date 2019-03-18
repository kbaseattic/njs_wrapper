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
&& wget https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/8.1.21.v20160908/jetty-distribution-8.1.21.v20160908.tar.gz \
&& tar -xvf jetty-distribution-8.1.21.v20160908 && rm -rf jetty-distribution-8.1.21.v20160908

ENTRYPOINT [ "/kb/deployment/bin/dockerize" ]
