# Create Kubernetes Secrets from .env file
# This script reads .env and creates K8s secrets for the trading-system namespace

param(
    [string]$EnvFile = ".env",
    [string]$Namespace = "trading-system"
)

Write-Host "Creating Kubernetes secrets from $EnvFile..." -ForegroundColor Cyan

# Check if .env exists
if (!(Test-Path $EnvFile)) {
    Write-Host "Error: $EnvFile not found. Copy from .env.example first." -ForegroundColor Red
    exit 1
}

# Load environment variables
$envVars = @{}
Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^([^=#]+)=(.*)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        $envVars[$key] = $value
    }
}

# Extract required variables
$mongoUser = $envVars['MONGO_INITDB_ROOT_USERNAME']
$mongoPass = $envVars['MONGO_INITDB_ROOT_PASSWORD']
$postgresDb = $envVars['POSTGRES_DB']
$postgresUser = $envVars['POSTGRES_USER']
$postgresPass = $envVars['POSTGRES_PASSWORD']
$rabbitmqUser = $envVars['RABBITMQ_DEFAULT_USER']
$rabbitmqPass = $envVars['RABBITMQ_DEFAULT_PASS']
$geminiKey = $envVars['GEMINI_API_KEY']
$openrouterKey = $envVars['OPENROUTER_API_KEY']

# Build connection strings (use host.docker.internal for Docker Compose services)
$mongodbUri = "mongodb://${mongoUser}:${mongoPass}@host.docker.internal:27017/?authSource=admin"
$postgresUrl = "jdbc:postgresql://host.docker.internal:5432/${postgresDb}"
$rabbitmqUrl = "amqp://${rabbitmqUser}:${rabbitmqPass}@host.docker.internal:5672"

Write-Host "Creating secret with the following connections:" -ForegroundColor Yellow
Write-Host "  MongoDB: $mongodbUri" -ForegroundColor Gray
Write-Host "  PostgreSQL: $postgresUrl" -ForegroundColor Gray
Write-Host "  RabbitMQ: $rabbitmqUrl" -ForegroundColor Gray

# Create secret
kubectl create secret generic trading-secrets `
    --from-literal=MONGODB_URI="$mongodbUri" `
    --from-literal=POSTGRES_URL="$postgresUrl" `
    --from-literal=POSTGRES_USER="$postgresUser" `
    --from-literal=POSTGRES_PASSWORD="$postgresPass" `
    --from-literal=RABBITMQ_URL="$rabbitmqUrl" `
    --from-literal=GEMINI_API_KEY="$geminiKey" `
    --from-literal=OPENROUTER_API_KEY="$openrouterKey" `
    -n $Namespace `
    --dry-run=client -o yaml | kubectl apply -f -

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Secrets created successfully in namespace: $Namespace" -ForegroundColor Green
} else {
    Write-Host "`n❌ Failed to create secrets" -ForegroundColor Red
    exit 1
}

# Verify
Write-Host "`nVerifying secret..." -ForegroundColor Cyan
kubectl get secret trading-secrets -n $Namespace

Write-Host "`n✅ Done! You can now deploy services to Kubernetes." -ForegroundColor Green
