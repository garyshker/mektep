package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"

	"github.com/garyshker/mektep-api/internal/middleware"
	"github.com/garyshker/mektep-api/internal/services"
)

type AdminHandler struct {
	lessonService *services.LessonService
}

func NewAdminHandler(lessonService *services.LessonService) *AdminHandler {
	return &AdminHandler{lessonService: lessonService}
}

func (h *AdminHandler) UploadLesson(w http.ResponseWriter, r *http.Request) {
	var upload services.LessonUpload
	if err := json.NewDecoder(r.Body).Decode(&upload); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid JSON: "+err.Error())
		return
	}

	lesson, err := h.lessonService.UploadLesson(r.Context(), &upload)
	if err != nil {
		middleware.JSONError(w, http.StatusBadRequest, err.Error())
		return
	}

	middleware.JSON(w, http.StatusCreated, lesson)
}

func (h *AdminHandler) UploadBulk(w http.ResponseWriter, r *http.Request) {
	var uploads []services.LessonUpload
	if err := json.NewDecoder(r.Body).Decode(&uploads); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid JSON: "+err.Error())
		return
	}

	type result struct {
		Index  int         `json:"index"`
		Lesson interface{} `json:"lesson,omitempty"`
		Error  string      `json:"error,omitempty"`
	}

	var results []result
	for i, upload := range uploads {
		lesson, err := h.lessonService.UploadLesson(r.Context(), &upload)
		if err != nil {
			results = append(results, result{Index: i, Error: err.Error()})
		} else {
			results = append(results, result{Index: i, Lesson: lesson})
		}
	}

	middleware.JSON(w, http.StatusOK, results)
}

func (h *AdminHandler) ValidateLesson(w http.ResponseWriter, r *http.Request) {
	var upload services.LessonUpload
	if err := json.NewDecoder(r.Body).Decode(&upload); err != nil {
		middleware.JSONError(w, http.StatusBadRequest, "invalid JSON: "+err.Error())
		return
	}

	errs := h.lessonService.ValidateUpload(&upload)
	if len(errs) > 0 {
		middleware.JSON(w, http.StatusUnprocessableEntity, map[string]interface{}{
			"valid":  false,
			"errors": errs,
		})
		return
	}

	middleware.JSON(w, http.StatusOK, map[string]interface{}{
		"valid":  true,
		"errors": []string{},
	})
}

func (h *AdminHandler) DeleteLesson(w http.ResponseWriter, r *http.Request) {
	lessonID := chi.URLParam(r, "lessonID")

	if err := h.lessonService.DeleteLesson(r.Context(), lessonID); err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, map[string]string{"status": "deleted"})
}

func (h *AdminHandler) ExportLessons(w http.ResponseWriter, r *http.Request) {
	subjectID := r.URL.Query().Get("subject")
	gradeLevel, _ := strconv.Atoi(r.URL.Query().Get("grade"))

	if subjectID == "" {
		middleware.JSONError(w, http.StatusBadRequest, "subject query parameter is required")
		return
	}

	lessons, err := h.lessonService.ExportLessons(r.Context(), subjectID, gradeLevel)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	middleware.JSON(w, http.StatusOK, lessons)
}
