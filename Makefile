.PHONY: build run dev css css-watch test vet sqlc migrate-up migrate-down seed clean tidy

BINARY  := bin/server
MAIN    := cmd/server/main.go

# Assemble a golang-migrate / mysql DSN from .env
DB_HOST ?= $(shell grep -s '^DB_HOST=' .env | cut -d= -f2)
DB_PORT ?= $(shell grep -s '^DB_PORT=' .env | cut -d= -f2)
DB_USER ?= $(shell grep -s '^DB_USER=' .env | cut -d= -f2)
DB_PASS ?= $(shell grep -s '^DB_PASSWORD=' .env | cut -d= -f2)
DB_NAME ?= $(shell grep -s '^DB_NAME=' .env | cut -d= -f2)
MIGRATE_DSN := mysql://$(DB_USER):$(DB_PASS)@tcp($(DB_HOST):$(DB_PORT))/$(DB_NAME)

css:
	npx @tailwindcss/cli -i static/css/admin.src.css -o static/css/admin.css --minify

css-watch:
	npx @tailwindcss/cli -i static/css/admin.src.css -o static/css/admin.css --watch

build: css
	go build -ldflags "-s -w" -o $(BINARY) $(MAIN)

run: build
	./$(BINARY)

dev:
	air

test:
	go test ./...

vet:
	go vet ./...

tidy:
	go mod tidy

sqlc:
	sqlc generate

migrate-up:
	migrate -path internal/database/migration -database "$(MIGRATE_DSN)" up

migrate-down:
	migrate -path internal/database/migration -database "$(MIGRATE_DSN)" down 1

seed:
	mysql -h$(DB_HOST) -P$(DB_PORT) -u$(DB_USER) $(if $(DB_PASS),-p$(DB_PASS),) $(DB_NAME) < internal/database/migration/seed.sql

clean:
	rm -f $(BINARY)
	rm -rf node_modules
