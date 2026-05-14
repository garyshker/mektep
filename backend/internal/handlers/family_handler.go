package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"

	"github.com/garyshker/mektep-api/internal/middleware"
	"github.com/garyshker/mektep-api/internal/services"
)

type FamilyHandler struct {
	familyService *services.FamilyService
}

func NewFamilyHandler(familyService *services.FamilyService) *FamilyHandler {
	return &FamilyHandler{familyService: familyService}
}

func (h *FamilyHandler) Create(w http.ResponseWriter, r *http.Request) {
	var req services.CreateFamilyRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	userID := middleware.UserIDFromContext(r.Context())
	family, err := h.familyService.Create(r.Context(), userID, req)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, "failed to create family")
		return
	}

	middleware.JSON(w, http.StatusCreated, family)
}

func (h *FamilyHandler) Get(w http.ResponseWriter, r *http.Request) {
	familyID := chi.URLParam(r, "familyID")
	family, err := h.familyService.Get(r.Context(), familyID)
	if err != nil {
		middleware.JSONError(w, http.StatusNotFound, "family not found")
		return
	}

	middleware.JSON(w, http.StatusOK, family)
}

func (h *FamilyHandler) GenerateInvite(w http.ResponseWriter, r *http.Request) {
	familyID := chi.URLParam(r, "familyID")
	invite, err := h.familyService.GenerateInvite(r.Context(), familyID)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, "failed to generate invite")
		return
	}

	middleware.JSON(w, http.StatusCreated, invite)
}

func (h *FamilyHandler) Join(w http.ResponseWriter, r *http.Request) {
	var req services.JoinFamilyRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	userID := middleware.UserIDFromContext(r.Context())
	family, err := h.familyService.Join(r.Context(), userID, req)
	if err != nil {
		middleware.JSONError(w, http.StatusBadRequest, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, family)
}

func (h *FamilyHandler) ListMembers(w http.ResponseWriter, r *http.Request) {
	familyID := chi.URLParam(r, "familyID")
	members, err := h.familyService.ListMembers(r.Context(), familyID)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, "failed to list members")
		return
	}

	middleware.JSON(w, http.StatusOK, members)
}
