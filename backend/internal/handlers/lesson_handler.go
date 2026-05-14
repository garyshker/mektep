package handlers

import (
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"

	"github.com/garyshker/mektep-api/internal/middleware"
	"github.com/garyshker/mektep-api/internal/services"
)

type LessonHandler struct {
	lessonService *services.LessonService
}

func NewLessonHandler(lessonService *services.LessonService) *LessonHandler {
	return &LessonHandler{lessonService: lessonService}
}

func (h *LessonHandler) ListSubjects(w http.ResponseWriter, r *http.Request) {
	subjects, err := h.lessonService.ListSubjects(r.Context())
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, "failed to list subjects")
		return
	}

	middleware.JSON(w, http.StatusOK, subjects)
}

func (h *LessonHandler) ListLessons(w http.ResponseWriter, r *http.Request) {
	subjectID := chi.URLParam(r, "subjectID")
	gradeLevel, _ := strconv.Atoi(r.URL.Query().Get("grade"))

	lessons, err := h.lessonService.ListLessons(r.Context(), subjectID, gradeLevel)
	if err != nil {
		middleware.JSONError(w, http.StatusInternalServerError, "failed to list lessons")
		return
	}

	middleware.JSON(w, http.StatusOK, lessons)
}

func (h *LessonHandler) GetLesson(w http.ResponseWriter, r *http.Request) {
	lessonID := chi.URLParam(r, "lessonID")

	lesson, err := h.lessonService.GetLesson(r.Context(), lessonID)
	if err != nil {
		middleware.JSONError(w, http.StatusNotFound, "lesson not found")
		return
	}

	middleware.JSON(w, http.StatusOK, lesson)
}
