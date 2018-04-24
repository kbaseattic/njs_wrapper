docker-compose -f execution-engine.yml down
docker-compose -f execution-engine.yml pull
docker-compose -f execution-engine.yml up -d
docker-compose -f execution-engine.yml exec -u 0 njs bash