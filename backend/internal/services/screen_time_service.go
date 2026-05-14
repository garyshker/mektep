package services

import (
	"context"
	"fmt"

	"github.com/garyshker/mektep-api/internal/models"
	"github.com/garyshker/mektep-api/internal/repository"
)

type ScreenTimeService struct {
	stRepo   *repository.ScreenTimeRepository
	userRepo *repository.UserRepository
}

func NewScreenTimeService(stRepo *repository.ScreenTimeRepository, userRepo *repository.UserRepository) *ScreenTimeService {
	return &ScreenTimeService{stRepo: stRepo, userRepo: userRepo}
}

type BalanceResponse struct {
	BalanceSeconds int `json:"balance_seconds"`
	BalanceMinutes int `json:"balance_minutes"`
}

func (s *ScreenTimeService) GetBalance(ctx context.Context, childID string) (*BalanceResponse, error) {
	profile, err := s.userRepo.GetChildProfile(ctx, childID)
	if err != nil {
		return nil, err
	}

	return &BalanceResponse{
		BalanceSeconds: profile.ScreenTimeBalanceSecs,
		BalanceMinutes: profile.ScreenTimeBalanceSecs / 60,
	}, nil
}

type SpendRequest struct {
	Seconds int    `json:"seconds"`
	AppName string `json:"app_name"`
}

func (s *ScreenTimeService) Spend(ctx context.Context, childID string, req SpendRequest) (*BalanceResponse, error) {
	profile, err := s.userRepo.GetChildProfile(ctx, childID)
	if err != nil {
		return nil, err
	}

	if profile.ScreenTimeBalanceSecs < req.Seconds {
		return nil, fmt.Errorf("insufficient screen time balance")
	}

	txn := &models.ScreenTimeTransaction{
		ChildID:       childID,
		Type:          models.TransactionSpent,
		AmountSeconds: -req.Seconds,
		Source:        req.AppName,
	}
	if err := s.stRepo.AddTransaction(ctx, txn); err != nil {
		return nil, err
	}

	if err := s.userRepo.UpdateScreenTimeBalance(ctx, childID, -req.Seconds); err != nil {
		return nil, err
	}

	return s.GetBalance(ctx, childID)
}

type BonusRequest struct {
	Minutes int    `json:"minutes"`
	Reason  string `json:"reason"`
}

func (s *ScreenTimeService) GrantBonus(ctx context.Context, childID string, req BonusRequest) (*BalanceResponse, error) {
	seconds := req.Minutes * 60

	txn := &models.ScreenTimeTransaction{
		ChildID:       childID,
		Type:          models.TransactionBonus,
		AmountSeconds: seconds,
		Source:        req.Reason,
	}
	if err := s.stRepo.AddTransaction(ctx, txn); err != nil {
		return nil, err
	}

	if err := s.userRepo.UpdateScreenTimeBalance(ctx, childID, seconds); err != nil {
		return nil, err
	}

	return s.GetBalance(ctx, childID)
}

func (s *ScreenTimeService) GetConfig(ctx context.Context, childID string) (*models.ScreenTimeConfig, error) {
	return s.stRepo.GetConfig(ctx, childID)
}

func (s *ScreenTimeService) UpdateConfig(ctx context.Context, cfg *models.ScreenTimeConfig) error {
	return s.stRepo.UpsertConfig(ctx, cfg)
}
