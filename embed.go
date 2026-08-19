package assets

import "embed"

// TemplateFS holds the server-rendered admin panel templates.
//
//go:embed template
var TemplateFS embed.FS

// StaticFS holds embedded static assets (CSS/JS) shipped with the binary.
//
//go:embed static/css static/js
var StaticFS embed.FS
