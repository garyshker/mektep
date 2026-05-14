package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"

	"github.com/garyshker/mektep-api/internal/middleware"
	"github.com/garyshker/mektep-api/internal/models"
	"github.com/garyshker/mektep-api/internal/services"
)

type ScreenTimeHandler struct {
	stService *services.ScreenTimeService
}

func NewScreenTimeHandler(stService *services.ScreenTimeService) *ScreenTimeHandler {
	return &ScreenTimeHandler{stService: stService}
}

func (h *ScreenTimeHandler) GetBalance(w http.ResponseWriter, r *http.Request) {
	childID := chi.URLParam(r, "childID")

	balance, err := h.stService.GetBalance(r.Context(), childID)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, balance)
}

func (h *ScreenTimeHandler) Spend(w http.ResponseWriter, r *http.Request) {
	childID := chi.URLParam(r, "childID")

	var req services.SpendRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	balance, err := h.stService.Spend(r.Context(), childID, req)
	if err != nil {
		middleware.JSONError(w, http.StatusBadRequest, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, balance)
}

func (h *ScreenTimeHandler) GrantBonus(w http.ResponseWriter, r *http.Request) {
	childID := chi.URLParam(r, "childID")

	var req services.BonusRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	balance, err := h.stService.GrantBonus(r.Context(), childID, req)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, balance)
}

func (h *ScreenTimeHandler) GetConfig(w http.ResponseWriter, r *http.Request) {
	childID := chi.URLParam(r, "childID")

	cfg, err := h.stService.GetConfig(r.Context(), childID)
	if err != nil {
		middleware.JSONError(w, http.StatusNotFound, "config not found")
		return
	}

	middleware.JSON(w, http.StatusOK, cfg)
}

func (h *ScreenTimeHandler) UpdateConfig(w http.ResponseWriter, r *http.Request) {
	childID := chi.URLParam(r, "childID")

	var cfg models.ScreenTimeConfig
	if err := json.NewDecoder(r.Body).Decode(&cfg); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}
	cfg.ChildID = childID

	if err := h.stService.UpdateConfig(r.Context(), &cfg); err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, cfg)
}
