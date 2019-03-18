FROM kbase/condor-worker AS build
# Multistage Build Setup
ARG BRANCH=NoBranchSet
COPY . /njs/
RUN echo "About to build $BRANCH" &&  cd /njs && ./gradlew buildAll

FROM kbase/condor-worker
# These ARGs values are passed in via build_docker_image.sh
ARG BUILD_DATE
ARG VCS_REF
ARG BRANCH=NoBranchSet

# Copy War and Fat Jar into root.war and for distribution to the worker nodes in /kb/deployment/lib
COPY --from=build /njs/dist/NJSWrapper.war /kb/deployment/jettybase/webapps/root.war
COPY --from=build /njs/dist/NJSWrapper-all.jar /kb/deployment/lib/
