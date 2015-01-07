KB_TOP ?= /kb/dev_container
KB_RUNTIME ?= /kb/runtime
DEPLOY_RUNTIME ?= $(KB_RUNTIME)
TARGET ?= /kb/deployment
CURR_DIR = $(shell pwd)
SERVICE_NAME = $(shell basename $(CURR_DIR))
SERVICE_DIR = $(TARGET)/services/$(SERVICE_NAME)
LIB_JARS_DIR = $(KB_TOP)/modules/jars/lib/jars
WAR_FILE = NJSWrapper.war

TARGET_PORT = 8200
THREADPOOL_SIZE = 50

default: compile

deploy-all: deploy

deploy: deploy-client deploy-service deploy-scripts deploy-docs

test: test-client test-service test-scripts

test-client:
	@echo "No tests for client"

test-service:
	@echo "No tests for service"

test-scripts:
	@echo "No tests for scripts"

compile: src
	ant war

deploy-client:
	@echo "No deployment for client"

deploy-service:
	@echo "Service folder: $(SERVICE_DIR)"
	mkdir -p $(SERVICE_DIR)
	cp -f ./deploy.cfg $(SERVICE_DIR)
	cp -f ./dist/$(WAR_FILE) $(SERVICE_DIR)
	cp -f ./service/glassfish_start_service.sh $(SERVICE_DIR)
	cp -f ./service/glassfish_stop_service.sh $(SERVICE_DIR)
	echo 'if [ -z "$$KB_DEPLOYMENT_CONFIG" ]' > $(SERVICE_DIR)/start_service
	echo 'then' >> $(SERVICE_DIR)/start_service
	echo '    export KB_DEPLOYMENT_CONFIG=$$KB_TOP/deployment.cfg' >> $(SERVICE_DIR)/start_service
	echo 'fi' >> $(SERVICE_DIR)/start_service
	echo "./glassfish_start_service.sh $(SERVICE_DIR)/$(WAR_FILE) $(TARGET_PORT) $(THREADPOOL_SIZE)" >> $(SERVICE_DIR)/start_service
	chmod +x $(SERVICE_DIR)/start_service
	echo "./glassfish_stop_service.sh $(TARGET_PORT)" > $(SERVICE_DIR)/stop_service
	chmod +x $(SERVICE_DIR)/stop_service

deploy-scripts:
	@echo "No deployment for scripts"

deploy-docs:
	@echo "No documentation"

clean:
	ant clean
