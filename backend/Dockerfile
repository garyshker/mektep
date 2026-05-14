FROM golang:1.23-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /mektep-api ./cmd/api
RUN CGO_ENABLED=0 GOOS=linux go build -o /mektep-cli ./cmd/cli

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
COPY --from=builder /mektep-api /usr/local/bin/mektep-api
COPY --from=builder /mektep-cli /usr/local/bin/mektep-cli

EXPOSE 8080
CMD ["mektep-api"]
