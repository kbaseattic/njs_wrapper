KB_TOP ?= /kb/dev_container
KB_RUNTIME ?= /kb/runtime
DEPLOY_RUNTIME ?= $(KB_RUNTIME)
TARGET ?= /kb/deployment
CURR_DIR = $(shell pwd)
SERVICE_NAME = njs_wrapper
SERVICE_CAPS = NarrativeJobService
SERVICE_SPEC = NJSWrapper
SERVICE_DIR = $(TARGET)/services/$(SERVICE_NAME)
WAR_FILE = NJSWrapper.war
URL = https://kbase.us/services/njs_wrapper/
ANT = ant
BIN = $(TARGET)/bin
JAVA_HOME ?= $(KB_RUNTIME)/java

ASADMIN = $(GLASSFISH_HOME)/glassfish/bin/asadmin

SERVICE_PORT = 8200
THREADPOOL_SIZE = 50

default: compile

compile-specs: compile-typespec compile-typespec-java

compile-typespec:
	kb-sdk compile \
		--out lib \
		--jsclname javascript/$(SERVICE_NAME)/Client \
		--plclname Bio::KBase::$(SERVICE_NAME)::Client \
		--pyclname biokbase.$(SERVICE_NAME).client \
		--url $(URL) \
		$(SERVICE_SPEC).spec

compile-typespec-java:
	kb-sdk compile  --java --javasrc src --javasrv --out . \
		--url $(URL) $(SERVICE_SPEC).spec

deploy-all: deploy

deploy: deploy-client deploy-service deploy-scripts deploy-docs

test: test-client test-service test-scripts

test-client:
	@echo "No tests for client"

test-service:
	@echo "No tests for service"

test-scripts:
	$(ANT) test

compile: src
	$(ANT) war

deploy-client: deploy-scripts

deploy-service: deploy-scripts
	@echo "Service folder: $(SERVICE_DIR)"
	mkdir -p $(SERVICE_DIR)
	cp -f ./deploy.cfg $(SERVICE_DIR)
	cp -f ./dist/$(WAR_FILE) $(SERVICE_DIR)
	mkdir $(SERVICE_DIR)/webapps
	cp ./dist/$(WAR_FILE) $(SERVICE_DIR)/webapps/root.war
	cp server_scripts/jetty.xml $(SERVICE_DIR)
	#server_scripts/build_server_control_scripts.py $(SERVICE_DIR) $(WAR_FILE)\
		$(TARGET) $(JAVA_HOME) deploy.cfg $(ASADMIN) $(SERVICE_CAPS)\
		$(SERVICE_PORT)

	#chmod +x $(SERVICE_DIR)/start_service
	#chmod +x $(SERVICE_DIR)/stop_service

deploy-scripts:
	$(ANT) script -Djardir=$(TARGET)/lib/jars -Djarsdir=$(TARGET)/lib/jars -Dbindir=$(BIN) -Djava.home=$(JAVA_HOME)

create-shock-to-mongo-script:
	$(ANT) shockmigscript

deploy-docs:
	@echo "No documentation"

clean:
	$(ANT) clean
