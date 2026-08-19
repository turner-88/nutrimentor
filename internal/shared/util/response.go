package util

import (
	"encoding/json"
	"net/http"
)

// JSONResponse is the standard API response envelope.
type JSONResponse struct {
	Success bool        `json:"success"`
	Message string      `json:"message,omitempty"`
	Data    interface{} `json:"data,omitempty"`
	Error   string      `json:"error,omitempty"`
}

func WriteJSON(w http.ResponseWriter, status int, data interface{}) error {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	return json.NewEncoder(w).Encode(data)
}

func WriteSuccess(w http.ResponseWriter, message string, data interface{}) error {
	return WriteJSON(w, http.StatusOK, JSONResponse{Success: true, Message: message, Data: data})
}

func WriteError(w http.ResponseWriter, status int, message string) error {
	return WriteJSON(w, status, JSONResponse{Success: false, Error: message})
}

func WriteBadRequest(w http.ResponseWriter, m string) error   { return WriteError(w, http.StatusBadRequest, m) }
func WriteUnauthorized(w http.ResponseWriter, m string) error { return WriteError(w, http.StatusUnauthorized, m) }
func WriteForbidden(w http.ResponseWriter, m string) error    { return WriteError(w, http.StatusForbidden, m) }
func WriteNotFound(w http.ResponseWriter, m string) error     { return WriteError(w, http.StatusNotFound, m) }
func WriteInternalError(w http.ResponseWriter, m string) error {
	return WriteError(w, http.StatusInternalServerError, m)
}

// DecodeJSON decodes a JSON request body into dst, rejecting unknown fields.
func DecodeJSON(r *http.Request, dst interface{}) error {
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	return dec.Decode(dst)
}
