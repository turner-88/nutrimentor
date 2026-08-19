package config

import (
	"fmt"
	"log"
	"os"

	"github.com/joho/godotenv"
)

// Config holds all application configuration.
type Config struct {
	Server   ServerConfig
	JWT      JWTConfig
	DB       DBConfig
	Env      string
	AppURL   string
	AppTheme string
	PageSize int
}

type ServerConfig struct {
	Host string
	Port string
}

type JWTConfig struct {
	SecretKey       string
	ExpirationHours int
}

type DBConfig struct {
	Host     string
	Port     string
	Name     string
	User     string
	Password string
}

// DSN returns the MySQL/MariaDB Data Source Name.
func (c *DBConfig) DSN() string {
	return fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci&loc=Local",
		c.User, c.Password, c.Host, c.Port, c.Name)
}

// Load reads configuration from environment variables (and .env if present).
func Load() *Config {
	_ = godotenv.Load()

	cfg := &Config{
		Server: ServerConfig{
			Host: getEnv("SERVER_HOST", "localhost"),
			Port: getEnv("SERVER_PORT", "8080"),
		},
		JWT: JWTConfig{
			SecretKey:       getEnv("JWT_SECRET", "change-me-in-production"),
			ExpirationHours: getEnvInt("JWT_EXPIRATION_HOURS", 72),
		},
		DB: DBConfig{
			Host:     getEnv("DB_HOST", "127.0.0.1"),
			Port:     getEnv("DB_PORT", "3306"),
			Name:     getEnv("DB_NAME", "sebaya"),
			User:     getEnv("DB_USER", "root"),
			Password: getEnv("DB_PASSWORD", ""),
		},
		Env:      getEnv("ENV", "development"),
		AppURL:   getEnv("APP_URL", "http://localhost:"+getEnv("SERVER_PORT", "8080")),
		AppTheme: getEnv("APP_THEME", "emerald"),
		PageSize: getEnvInt("APP_PAGESIZE", 15),
	}

	if cfg.IsProduction() {
		if cfg.JWT.SecretKey == "change-me-in-production" {
			log.Fatal("FATAL: JWT_SECRET must be changed from default in production")
		}
		if cfg.DB.Password == "" {
			log.Fatal("FATAL: DB_PASSWORD must be set in production")
		}
	}

	log.Printf("Configuration loaded: %s:%s (env: %s)", cfg.Server.Host, cfg.Server.Port, cfg.Env)
	return cfg
}

func (c *Config) ServerAddr() string  { return c.Server.Host + ":" + c.Server.Port }
func (c *Config) IsProduction() bool  { return c.Env == "production" }

func getEnv(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func getEnvInt(key string, def int) int {
	if v := os.Getenv(key); v != "" {
		var n int
		if _, err := fmt.Sscanf(v, "%d", &n); err == nil {
			return n
		}
	}
	return def
}
