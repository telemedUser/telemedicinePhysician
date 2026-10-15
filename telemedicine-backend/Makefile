.PHONY: build build-java build-python up down logs scaffold-java scaffold-python test-python

COMPOSE_FILE := deploy/docker-compose.dev.yml

build: build-java

build-java:
	docker compose -f $(COMPOSE_FILE) build gateway template-java template-python

build-python:
	pip install -r shared/python-common/requirements.txt
	pip install -r services/_template-python/requirements.txt
	pip install -r services/_template-python/requirements-dev.txt

up:
	docker compose -f $(COMPOSE_FILE) up -d --build

down:
	docker compose -f $(COMPOSE_FILE) down

logs:
	docker compose -f $(COMPOSE_FILE) logs -f

test-python:
	cd services/_template-python && PYTHONPATH=src pytest tests -v

scaffold-java:
	@test -n "$(NAME)" || (echo "Usage: make scaffold-java NAME=my-service" && exit 1)
	./tools/scaffold-service.sh java $(NAME)

scaffold-python:
	@test -n "$(NAME)" || (echo "Usage: make scaffold-python NAME=my-service" && exit 1)
	./tools/scaffold-service.sh python $(NAME)

# Windows: powershell -File tools/scaffold-service.ps1 -Lang java -Name my-service
