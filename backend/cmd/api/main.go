package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/garyshker/mektep-api/internal/config"
	"github.com/garyshker/mektep-api/internal/database"
	"github.com/garyshker/mektep-api/internal/handlers"
	"github.com/garyshker/mektep-api/internal/middleware"
	"github.com/garyshker/mektep-api/internal/repository"
	"github.com/garyshker/mektep-api/internal/services"
	"github.com/go-chi/chi/v5"
	chimw "github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("failed to load config: %v", err)
	}

	db, err := database.Connect(cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}
	defer db.Close()

	if err := database.RunMigrations(db); err != nil {
		log.Fatalf("failed to run migrations: %v", err)
	}

	// Repositories
	userRepo := repository.NewUserRepository(db)
	familyRepo := repository.NewFamilyRepository(db)
	subjectRepo := repository.NewSubjectRepository(db)
	lessonRepo := repository.NewLessonRepository(db)
	attemptRepo := repository.NewAttemptRepository(db)
	screenTimeRepo := repository.NewScreenTimeRepository(db)

	// Services
	authService := services.NewAuthService(userRepo, cfg.JWTSecret)
	familyService := services.NewFamilyService(familyRepo, userRepo)
	lessonService := services.NewLessonService(lessonRepo, subjectRepo)
	attemptService := services.NewAttemptService(attemptRepo, lessonRepo, userRepo)
	screenTimeService := services.NewScreenTimeService(screenTimeRepo, userRepo)

	// Handlers
	authHandler := handlers.NewAuthHandler(authService)
	familyHandler := handlers.NewFamilyHandler(familyService)
	lessonHandler := handlers.NewLessonHandler(lessonService)
	attemptHandler := handlers.NewAttemptHandler(attemptService)
	screenTimeHandler := handlers.NewScreenTimeHandler(screenTimeService)
	adminHandler := handlers.NewAdminHandler(lessonService)

	// Router
	r := chi.NewRouter()

	r.Use(chimw.Logger)
	r.Use(chimw.Recoverer)
	r.Use(chimw.RequestID)
	r.Use(chimw.RealIP)
	r.Use(chimw.Timeout(30 * time.Second))
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type"},
		ExposedHeaders:   []string{"Link"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	// Health check
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte(`{"status":"ok"}`))
	})

	// Public routes
	r.Route("/api/v1", func(r chi.Router) {
		// Auth
		r.Post("/auth/register", authHandler.Register)
		r.Post("/auth/login", authHandler.Login)
		r.Post("/auth/refresh", authHandler.Refresh)

		// Protected routes
		r.Group(func(r chi.Router) {
			r.Use(middleware.Auth(cfg.JWTSecret))

			// Family
			r.Post("/families", familyHandler.Create)
			r.Get("/families/{familyID}", familyHandler.Get)
			r.Post("/families/{familyID}/invite", familyHandler.GenerateInvite)
			r.Post("/families/join", familyHandler.Join)
			r.Get("/families/{familyID}/members", familyHandler.ListMembers)

			// Subjects & Lessons (read)
			r.Get("/subjects", lessonHandler.ListSubjects)
			r.Get("/subjects/{subjectID}/lessons", lessonHandler.ListLessons)
			r.Get("/lessons/{lessonID}", lessonHandler.GetLesson)

			// Lesson attempts
			r.Post("/lessons/{lessonID}/start", attemptHandler.Start)
			r.Post("/lesson-attempts/{attemptID}/answer", attemptHandler.SubmitAnswer)
			r.Post("/lesson-attempts/{attemptID}/complete", attemptHandler.Complete)

			// Child dashboard
			r.Get("/children/{childID}/dashboard", attemptHandler.Dashboard)
			r.Get("/children/{childID}/progress", attemptHandler.Progress)

			// Screen time
			r.Get("/children/{childID}/screen-time/balance", screenTimeHandler.GetBalance)
			r.Post("/children/{childID}/screen-time/spend", screenTimeHandler.Spend)
			r.Post("/children/{childID}/screen-time/bonus", screenTimeHandler.GrantBonus)
			r.Get("/children/{childID}/config", screenTimeHandler.GetConfig)
			r.Put("/children/{childID}/config", screenTimeHandler.UpdateConfig)
		})

		// Admin routes (API key auth)
		r.Group(func(r chi.Router) {
			r.Use(middleware.AdminAPIKey(cfg.AdminAPIKey))

			r.Post("/admin/lessons/upload", adminHandler.UploadLesson)
			r.Post("/admin/lessons/upload/bulk", adminHandler.UploadBulk)
			r.Post("/admin/lessons/validate", adminHandler.ValidateLesson)
			r.Delete("/admin/lessons/{lessonID}", adminHandler.DeleteLesson)
			r.Get("/admin/lessons/export", adminHandler.ExportLessons)
		})
	})

	// Server
	srv := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      r,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Graceful shutdown
	go func() {
		log.Printf("server starting on :%s", cfg.Port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("server error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("shutting down server...")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("server forced to shutdown: %v", err)
	}
	log.Println("server stopped")
}
