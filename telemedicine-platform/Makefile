.PHONY: env up up-all down nuke ps logs verify verify-phase0 build

COMPOSE := docker compose
ENV_FILE := .env

env:
	@if [ ! -f $(ENV_FILE) ]; then cp .env.example $(ENV_FILE) && echo "Created $(ENV_FILE) from .env.example"; else echo "$(ENV_FILE) already exists"; fi

build:
	$(COMPOSE) build gateway external-simulators

up: env
	$(COMPOSE) up -d --build

up-all: env
	COMPOSE_PROFILES=infra,observability,sim,edge,apps $(COMPOSE) up -d --build

down:
	$(COMPOSE) down

nuke:
	$(COMPOSE) down -v --remove-orphans

ps:
	$(COMPOSE) ps

logs:
	$(COMPOSE) logs -f

verify: verify-phase0

verify-phase0:
	@./scripts/verify-phase0.sh
