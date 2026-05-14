package services

import (
	"context"
	"errors"
	"time"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/garyshker/mektep-api/internal/repository"
	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

var (
	ErrInvalidCredentials = errors.New("invalid credentials")
	ErrEmailTaken         = errors.New("email already registered")
	ErrInvalidToken       = errors.New("invalid token")
)

type AuthService struct {
	userRepo  *repository.UserRepository
	jwtSecret string
}

func NewAuthService(userRepo *repository.UserRepository, jwtSecret string) *AuthService {
	return &AuthService{userRepo: userRepo, jwtSecret: jwtSecret}
}

type RegisterRequest struct {
	Email              string      `json:"email"`
	Password           string      `json:"password"`
	Role               models.Role `json:"role"`
	LanguagePreference string      `json:"language_preference"`
	// Child-specific fields
	DisplayName string `json:"display_name,omitempty"`
	GradeLevel  int    `json:"grade_level,omitempty"`
}

type AuthResponse struct {
	AccessToken  string      `json:"access_token"`
	RefreshToken string      `json:"refresh_token"`
	User         models.User `json:"user"`
}

func (s *AuthService) Register(ctx context.Context, req RegisterRequest) (*AuthResponse, error) {
	// Check if email exists
	existing, _ := s.userRepo.GetByEmail(ctx, req.Email)
	if existing != nil {
		return nil, ErrEmailTaken
	}

	// Hash password
	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		return nil, err
	}

	lang := req.LanguagePreference
	if lang == "" {
		lang = "en"
	}

	user := &models.User{
		Email:              req.Email,
		PasswordHash:       string(hash),
		Role:               req.Role,
		LanguagePreference: lang,
		Timezone:           "UTC",
	}

	if err := s.userRepo.Create(ctx, user); err != nil {
		return nil, err
	}

	// If child, create child profile
	if req.Role == models.RoleChild {
		displayName := req.DisplayName
		if displayName == "" {
			displayName = req.Email
		}
		gradeLevel := req.GradeLevel
		if gradeLevel == 0 {
			gradeLevel = 1
		}
		profile := &models.ChildProfile{
			UserID:      user.ID,
			DisplayName: displayName,
			GradeLevel:  gradeLevel,
		}
		if err := s.userRepo.CreateChildProfile(ctx, profile); err != nil {
			return nil, err
		}
	}

	return s.generateTokens(user)
}

type LoginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

func (s *AuthService) Login(ctx context.Context, req LoginRequest) (*AuthResponse, error) {
	user, err := s.userRepo.GetByEmail(ctx, req.Email)
	if err != nil {
		return nil, ErrInvalidCredentials
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)); err != nil {
		return nil, ErrInvalidCredentials
	}

	return s.generateTokens(user)
}

type RefreshRequest struct {
	RefreshToken string `json:"refresh_token"`
}

func (s *AuthService) Refresh(ctx context.Context, req RefreshRequest) (*AuthResponse, error) {
	token, err := jwt.Parse(req.RefreshToken, func(t *jwt.Token) (interface{}, error) {
		return []byte(s.jwtSecret), nil
	})
	if err != nil || !token.Valid {
		return nil, ErrInvalidToken
	}

	claims, ok := token.Claims.(jwt.MapClaims)
	if !ok {
		return nil, ErrInvalidToken
	}

	tokenType, _ := claims["type"].(string)
	if tokenType != "refresh" {
		return nil, ErrInvalidToken
	}

	userID, _ := claims["sub"].(string)
	user, err := s.userRepo.GetByID(ctx, userID)
	if err != nil {
		return nil, ErrInvalidToken
	}

	return s.generateTokens(user)
}

func (s *AuthService) generateTokens(user *models.User) (*AuthResponse, error) {
	// Access token (15 min)
	accessClaims := jwt.MapClaims{
		"sub":  user.ID,
		"role": string(user.Role),
		"type": "access",
		"exp":  time.Now().Add(15 * time.Minute).Unix(),
		"iat":  time.Now().Unix(),
	}
	accessToken := jwt.NewWithClaims(jwt.SigningMethodHS256, accessClaims)
	accessStr, err := accessToken.SignedString([]byte(s.jwtSecret))
	if err != nil {
		return nil, err
	}

	// Refresh token (7 days)
	refreshClaims := jwt.MapClaims{
		"sub":  user.ID,
		"type": "refresh",
		"exp":  time.Now().Add(7 * 24 * time.Hour).Unix(),
		"iat":  time.Now().Unix(),
	}
	refreshToken := jwt.NewWithClaims(jwt.SigningMethodHS256, refreshClaims)
	refreshStr, err := refreshToken.SignedString([]byte(s.jwtSecret))
	if err != nil {
		return nil, err
	}

	return &AuthResponse{
		AccessToken:  accessStr,
		RefreshToken: refreshStr,
		User:         *user,
	}, nil
}
