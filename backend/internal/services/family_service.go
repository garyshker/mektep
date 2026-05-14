package services

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"errors"
	"time"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/garyshker/mektep-api/internal/repository"
)

var (
	ErrInviteExpired = errors.New("invite code expired or invalid")
	ErrNotFound      = errors.New("not found")
)

type FamilyService struct {
	familyRepo *repository.FamilyRepository
	userRepo   *repository.UserRepository
}

func NewFamilyService(familyRepo *repository.FamilyRepository, userRepo *repository.UserRepository) *FamilyService {
	return &FamilyService{familyRepo: familyRepo, userRepo: userRepo}
}

type CreateFamilyRequest struct {
	Name string `json:"name"`
}

func (s *FamilyService) Create(ctx context.Context, userID string, req CreateFamilyRequest) (*models.Family, error) {
	family := &models.Family{Name: req.Name}
	if err := s.familyRepo.Create(ctx, family); err != nil {
		return nil, err
	}

	// Add creator as parent member
	member := &models.FamilyMember{
		FamilyID: family.ID,
		UserID:   userID,
		Role:     models.RoleParent,
	}
	if err := s.familyRepo.AddMember(ctx, member); err != nil {
		return nil, err
	}

	return family, nil
}

func (s *FamilyService) Get(ctx context.Context, familyID string) (*models.Family, error) {
	return s.familyRepo.GetByID(ctx, familyID)
}

func (s *FamilyService) GenerateInvite(ctx context.Context, familyID string) (*models.InviteCode, error) {
	code, err := generateCode(6)
	if err != nil {
		return nil, err
	}

	invite := &models.InviteCode{
		FamilyID:  familyID,
		Code:      code,
		ExpiresAt: time.Now().Add(48 * time.Hour),
	}
	if err := s.familyRepo.CreateInviteCode(ctx, invite); err != nil {
		return nil, err
	}
	return invite, nil
}

type JoinFamilyRequest struct {
	Code string `json:"code"`
}

func (s *FamilyService) Join(ctx context.Context, userID string, req JoinFamilyRequest) (*models.Family, error) {
	invite, err := s.familyRepo.GetInviteByCode(ctx, req.Code)
	if err != nil {
		return nil, ErrInviteExpired
	}

	user, err := s.userRepo.GetByID(ctx, userID)
	if err != nil {
		return nil, err
	}

	member := &models.FamilyMember{
		FamilyID: invite.FamilyID,
		UserID:   userID,
		Role:     user.Role,
	}
	if err := s.familyRepo.AddMember(ctx, member); err != nil {
		return nil, err
	}

	if err := s.familyRepo.MarkInviteUsed(ctx, invite.ID); err != nil {
		return nil, err
	}

	return s.familyRepo.GetByID(ctx, invite.FamilyID)
}

func (s *FamilyService) ListMembers(ctx context.Context, familyID string) ([]models.FamilyMember, error) {
	return s.familyRepo.ListMembers(ctx, familyID)
}

func generateCode(length int) (string, error) {
	b := make([]byte, length)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return hex.EncodeToString(b)[:length], nil
}
