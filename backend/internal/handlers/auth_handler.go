package handlers

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/garyshker/mektep-api/internal/middleware"
	"github.com/garyshker/mektep-api/internal/services"
)

type AuthHandler struct {
	authService *services.AuthService
}

func NewAuthHandler(authService *services.AuthService) *AuthHandler {
	return &AuthHandler{authService: authService}
}

func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	var req services.RegisterRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	resp, err := h.authService.Register(r.Context(), req)
	if err != nil {
		if errors.Is(err, services.ErrEmailTaken) {
			middleware.JSONError(w, http.StatusConflict, "email already registered")
			return
		}
		middleware.JSONError(w, http.StatusInternalServerError, "registration failed")
		return
	}

	middleware.JSON(w, http.StatusCreated, resp)
}

func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	var req services.LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	resp, err := h.authService.Login(r.Context(), req)
	if err != nil {
		if errors.Is(err, services.ErrInvalidCredentials) {
			middleware.JSONError(w, http.StatusUnauthorized, "invalid email or password")
			return
		}
		middleware.JSONError(w, http.StatusInternalServerError, "login failed")
		return
	}

	middleware.JSON(w, http.StatusOK, resp)
}

func (h *AuthHandler) Refresh(w http.ResponseWriter, r *http.Request) {
	var req services.RefreshRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	resp, err := h.authService.Refresh(r.Context(), req)
	if err != nil {
		middleware.JSONError(w, http.StatusUnauthorized, "invalid refresh token")
		return
	}

	middleware.JSON(w, http.StatusOK, resp)
}
