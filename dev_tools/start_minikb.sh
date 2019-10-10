git clone https://github.com/bio-boris/mini_kb.git && cd mini_kb && git checkout justExecutionEngine
time docker-compose -f execution-engine.yml pull
time docker-compose -f execution-engine.yml up -d
timeout_seconds = 240
echo "Waiting for mini_kb to start"
sleep $timeout_seconds


# Logs
docker-compose -f execution-engine.yml logs njs
docker-compose -f execution-engine.yml logs auth
docker-compose -f execution-engine.yml logs catalog
docker-compose -f execution-engine.yml logs narrative_method_store
docker-compose -f execution-engine.yml logs 

# Curl
curl nginx/services/njs 
curl nginx/services/auth 
curl nginx/services/catalog 
curl -m5 -d '{"method": "NarrativeJobService.status", "version": "1.1", "id": 1, "params": []}' http://nginx/services/njs |python -mjson.tool
