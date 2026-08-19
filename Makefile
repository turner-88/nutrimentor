.PHONY: build run dev css css-watch test vet sqlc migrate-up migrate-down seed clean tidy

BINARY  := bin/server
MAIN    := cmd/server/main.go

# Standalone Tailwind CLI — no Node/npm required. Downloaded on demand into bin/.
TAILWIND_VERSION ?= v4.3.3
TAILWIND_BIN     := bin/tailwindcss

UNAME_S := $(shell uname -s)
UNAME_M := $(shell uname -m)
ifeq ($(UNAME_S),Darwin)
  TW_OS := macos
else
  TW_OS := linux
endif
ifeq ($(filter arm64 aarch64,$(UNAME_M)),)
  TW_ARCH := x64
else
  TW_ARCH := arm64
endif
TW_ASSET := tailwindcss-$(TW_OS)-$(TW_ARCH)

# Assemble a golang-migrate / mysql DSN from .env
DB_HOST ?= $(shell grep -s '^DB_HOST=' .env | cut -d= -f2)
DB_PORT ?= $(shell grep -s '^DB_PORT=' .env | cut -d= -f2)
DB_USER ?= $(shell grep -s '^DB_USER=' .env | cut -d= -f2)
DB_PASS ?= $(shell grep -s '^DB_PASSWORD=' .env | cut -d= -f2)
DB_NAME ?= $(shell grep -s '^DB_NAME=' .env | cut -d= -f2)
MIGRATE_DSN := mysql://$(DB_USER):$(DB_PASS)@tcp($(DB_HOST):$(DB_PORT))/$(DB_NAME)

$(TAILWIND_BIN):
	@mkdir -p bin
	curl -fsSL "https://github.com/tailwindlabs/tailwindcss/releases/download/$(TAILWIND_VERSION)/$(TW_ASSET)" -o $(TAILWIND_BIN)
	chmod +x $(TAILWIND_BIN)

css: $(TAILWIND_BIN)
	$(TAILWIND_BIN) -i static/css/admin.src.css -o static/css/admin.css --minify

css-watch: $(TAILWIND_BIN)
	$(TAILWIND_BIN) -i static/css/admin.src.css -o static/css/admin.css --watch

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
	rm -f $(TAILWIND_BIN)
