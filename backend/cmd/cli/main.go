package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

const defaultBaseURL = "http://localhost:8080/api/v1/admin"

func main() {
	if len(os.Args) < 2 {
		printUsage()
		os.Exit(1)
	}

	baseURL := os.Getenv("MEKTEP_API_URL")
	if baseURL == "" {
		baseURL = defaultBaseURL
	}

	apiKey := os.Getenv("MEKTEP_API_KEY")
	if apiKey == "" {
		fmt.Fprintln(os.Stderr, "Error: MEKTEP_API_KEY environment variable is required")
		os.Exit(1)
	}

	command := os.Args[1]
	switch command {
	case "lesson":
		if len(os.Args) < 3 {
			fmt.Fprintln(os.Stderr, "Usage: mektep-cli lesson <upload|validate|list|delete|export>")
			os.Exit(1)
		}
		handleLesson(baseURL, apiKey, os.Args[2], os.Args[3:])
	case "seed":
		if len(os.Args) < 3 {
			fmt.Fprintln(os.Stderr, "Usage: mektep-cli seed <file-or-directory>")
			os.Exit(1)
		}
		handleSeed(baseURL, apiKey, os.Args[2])
	default:
		fmt.Fprintf(os.Stderr, "Unknown command: %s\n", command)
		printUsage()
		os.Exit(1)
	}
}

func printUsage() {
	fmt.Println(`mektep-cli - Mektep content management tool

Usage:
  mektep-cli lesson upload <file.json>       Upload a single lesson
  mektep-cli lesson upload <directory/>       Upload all JSON files in directory
  mektep-cli lesson validate <file.json>      Validate JSON without uploading
  mektep-cli lesson delete --id=<lesson-id>   Delete a lesson
  mektep-cli lesson export --subject=<id>     Export lessons for a subject
  mektep-cli seed <file-or-directory>          Bulk upload curriculum

Environment:
  MEKTEP_API_URL   API base URL (default: http://localhost:8080/api/v1/admin)
  MEKTEP_API_KEY   Admin API key (required)`)
}

func handleLesson(baseURL, apiKey, action string, args []string) {
	switch action {
	case "upload":
		if len(args) == 0 {
			fmt.Fprintln(os.Stderr, "Usage: mektep-cli lesson upload <file-or-directory>")
			os.Exit(1)
		}
		uploadPath(baseURL, apiKey, args[0])

	case "validate":
		if len(args) == 0 {
			fmt.Fprintln(os.Stderr, "Usage: mektep-cli lesson validate <file.json>")
			os.Exit(1)
		}
		validateFile(baseURL, apiKey, args[0])

	case "delete":
		id := ""
		for _, arg := range args {
			if strings.HasPrefix(arg, "--id=") {
				id = strings.TrimPrefix(arg, "--id=")
			}
		}
		if id == "" {
			fmt.Fprintln(os.Stderr, "Usage: mektep-cli lesson delete --id=<lesson-id>")
			os.Exit(1)
		}
		deleteLesson(baseURL, apiKey, id)

	case "export":
		subject := ""
		grade := ""
		for _, arg := range args {
			if strings.HasPrefix(arg, "--subject=") {
				subject = strings.TrimPrefix(arg, "--subject=")
			}
			if strings.HasPrefix(arg, "--grade=") {
				grade = strings.TrimPrefix(arg, "--grade=")
			}
		}
		if subject == "" {
			fmt.Fprintln(os.Stderr, "Usage: mektep-cli lesson export --subject=<id> [--grade=<n>]")
			os.Exit(1)
		}
		exportLessons(baseURL, apiKey, subject, grade)

	default:
		fmt.Fprintf(os.Stderr, "Unknown lesson action: %s\n", action)
		os.Exit(1)
	}
}

func uploadPath(baseURL, apiKey, path string) {
	info, err := os.Stat(path)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	if info.IsDir() {
		entries, err := os.ReadDir(path)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error reading directory: %v\n", err)
			os.Exit(1)
		}

		success, failed := 0, 0
		for _, entry := range entries {
			if entry.IsDir() || !strings.HasSuffix(entry.Name(), ".json") {
				continue
			}
			fullPath := filepath.Join(path, entry.Name())
			if uploadFile(baseURL, apiKey, fullPath) {
				success++
			} else {
				failed++
			}
		}
		fmt.Printf("\nDone: %d uploaded, %d failed\n", success, failed)
	} else {
		uploadFile(baseURL, apiKey, path)
	}
}

func uploadFile(baseURL, apiKey, path string) bool {
	data, err := os.ReadFile(path)
	if err != nil {
		fmt.Fprintf(os.Stderr, "[FAIL] %s: %v\n", path, err)
		return false
	}

	// Validate JSON
	var lesson interface{}
	if err := json.Unmarshal(data, &lesson); err != nil {
		fmt.Fprintf(os.Stderr, "[FAIL] %s: invalid JSON: %v\n", path, err)
		return false
	}

	resp, err := doRequest("POST", baseURL+"/lessons/upload", apiKey, data)
	if err != nil {
		fmt.Fprintf(os.Stderr, "[FAIL] %s: %v\n", path, err)
		return false
	}

	if resp.StatusCode == http.StatusCreated {
		fmt.Printf("[OK]   %s\n", path)
		return true
	}

	body, _ := io.ReadAll(resp.Body)
	fmt.Fprintf(os.Stderr, "[FAIL] %s: %s\n", path, string(body))
	return false
}

func validateFile(baseURL, apiKey, path string) {
	data, err := os.ReadFile(path)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	resp, err := doRequest("POST", baseURL+"/lessons/validate", apiKey, data)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	body, _ := io.ReadAll(resp.Body)
	var result map[string]interface{}
	json.Unmarshal(body, &result)

	if valid, ok := result["valid"].(bool); ok && valid {
		fmt.Println("Valid!")
	} else {
		fmt.Println("Validation errors:")
		prettyPrint(body)
		os.Exit(1)
	}
}

func deleteLesson(baseURL, apiKey, id string) {
	resp, err := doRequest("DELETE", baseURL+"/lessons/"+id, apiKey, nil)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	if resp.StatusCode == http.StatusOK {
		fmt.Println("Lesson deleted successfully")
	} else {
		body, _ := io.ReadAll(resp.Body)
		fmt.Fprintf(os.Stderr, "Error: %s\n", string(body))
		os.Exit(1)
	}
}

func exportLessons(baseURL, apiKey, subject, grade string) {
	url := baseURL + "/lessons/export?subject=" + subject
	if grade != "" {
		url += "&grade=" + grade
	}

	resp, err := doRequest("GET", url, apiKey, nil)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	body, _ := io.ReadAll(resp.Body)
	prettyPrint(body)
}

func handleSeed(baseURL, apiKey, path string) {
	fmt.Println("Seeding curriculum from:", path)
	uploadPath(baseURL, apiKey, path)
}

func doRequest(method, url, apiKey string, body []byte) (*http.Response, error) {
	var bodyReader io.Reader
	if body != nil {
		bodyReader = bytes.NewReader(body)
	}

	req, err := http.NewRequest(method, url, bodyReader)
	if err != nil {
		return nil, err
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-API-Key", apiKey)

	return http.DefaultClient.Do(req)
}

func prettyPrint(data []byte) {
	var out bytes.Buffer
	if err := json.Indent(&out, data, "", "  "); err != nil {
		fmt.Println(string(data))
		return
	}
	fmt.Println(out.String())
}
