package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"

	"github.com/garyshker/mektep-api/internal/middleware"
	"github.com/garyshker/mektep-api/internal/services"
)

type AttemptHandler struct {
	attemptService *services.AttemptService
}

func NewAttemptHandler(attemptService *services.AttemptService) *AttemptHandler {
	return &AttemptHandler{attemptService: attemptService}
}

func (h *AttemptHandler) Start(w http.ResponseWriter, r *http.Request) {
	lessonID := chi.URLParam(r, "lessonID")

	// Get child profile ID from request body or derive from user
	var req struct {
		ChildID string `json:"child_id"`
	}
	json.NewDecoder(r.Body).Decode(&req)

	if req.ChildID == "" {
		middleware.JSONError(w, http.StatusBadRequest, "child_id is required")
		return
	}

	attempt, err := h.attemptService.Start(r.Context(), req.ChildID, lessonID)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	middleware.JSON(w, http.StatusCreated, attempt)
}

func (h *AttemptHandler) SubmitAnswer(w http.ResponseWriter, r *http.Request) {
	attemptID := chi.URLParam(r, "attemptID")

	var req services.SubmitAnswerRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	resp, err := h.attemptService.SubmitAnswer(r.Context(), attemptID, req)
	if err != nil {
		middleware.JSONError(w, http.StatusBadRequest, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, resp)
}

func (h *AttemptHandler) Complete(w http.ResponseWriter, r *http.Request) {
	attemptID := chi.URLParam(r, "attemptID")

	resp, err := h.attemptService.Complete(r.Context(), attemptID)
	if err != nil {
		middleware.JSONError(w, http.StatusBadRequest, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, resp)
}

func (h *AttemptHandler) Dashboard(w http.ResponseWriter, r *http.Request) {
	childID := chi.URLParam(r, "childID")

	resp, err := h.attemptService.Dashboard(r.Context(), childID)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, resp)
}

func (h *AttemptHandler) Progress(w http.ResponseWriter, r *http.Request) {
	childID := chi.URLParam(r, "childID")

	progress, err := h.attemptService.Progress(r.Context(), childID)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, progress)
}
