# Shortly Frontend

Angular frontend for the Shortly URL shortener. The app uses a generated OpenAPI client and is intended as an exercise project.

## What is it?

- Angular 21 App
- API client is generated from `shortly_api.yaml`
- Only really usable with a running backend

## Prerequisites

- Node.js 20+ and npm (package.json uses npm@11.6.2)
- Docker (optional, for a ready-made image)

## Run locally

```bash
cd shortly-frontend
npm install
npm start
```

After that the app runs at `http://localhost:4200`.

## Build Docker image

```bash
cd shortly-frontend
docker build -t shortly-frontend:local .
docker run --rm -p 80:80 shortly-frontend:local
```

## Update OpenAPI client (optional)

```bash
cd shortly-frontend
npx openapi-generator-cli generate -i ../shortly_api.yaml -g typescript-angular -o src/app/core/modules/openapi --additional-properties fileNaming=kebab-case,withInterfaces=true --generate-alias-as-model
```
