# Kubernetes Setup Verification Script
# Run this to verify your K8s environment is ready

Write-Host "=== Kubernetes Setup Verification ===" -ForegroundColor Cyan
Write-Host ""

# Check if kubectl is available
Write-Host "1. Checking kubectl..." -ForegroundColor Yellow
try {
    $kubectlVersion = kubectl version --client --short 2>$null
    Write-Host "   ✓ kubectl installed: $kubectlVersion" -ForegroundColor Green
} catch {
    Write-Host "   ✗ kubectl not found. Install Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check if Kubernetes cluster is running
Write-Host ""
Write-Host "2. Checking Kubernetes cluster..." -ForegroundColor Yellow
try {
    $clusterInfo = kubectl cluster-info 2>&1
    if ($clusterInfo -match "Kubernetes control plane") {
        Write-Host "   ✓ Kubernetes is running" -ForegroundColor Green
        Write-Host "     $($clusterInfo | Select-String 'Kubernetes control plane')" -ForegroundColor Gray
    } else {
        throw "Cluster not running"
    }
} catch {
    Write-Host "   ✗ Kubernetes cluster is NOT running" -ForegroundColor Red
    Write-Host ""
    Write-Host "   To fix:" -ForegroundColor Yellow
    Write-Host "   1. Open Docker Desktop" -ForegroundColor White
    Write-Host "   2. Go to Settings → Kubernetes" -ForegroundColor White
    Write-Host "   3. Check 'Enable Kubernetes'" -ForegroundColor White
    Write-Host "   4. Click 'Apply & Restart'" -ForegroundColor White
    Write-Host "   5. Wait 2-5 minutes" -ForegroundColor White
    exit 1
}

# Check nodes
Write-Host ""
Write-Host "3. Checking nodes..." -ForegroundColor Yellow
try {
    $nodes = kubectl get nodes --no-headers 2>&1
    if ($nodes -match "Ready") {
        Write-Host "   ✓ Node ready: $($nodes -split '\s+' | Select-Object -First 1)" -ForegroundColor Green
    } else {
        throw "No ready nodes"
    }
} catch {
    Write-Host "   ✗ No nodes ready" -ForegroundColor Red
    exit 1
}

# Check metrics-server
Write-Host ""
Write-Host "4. Checking metrics-server (required for HPA)..." -ForegroundColor Yellow
try {
    $metricsServer = kubectl get deployment metrics-server -n kube-system --no-headers 2>&1
    if ($metricsServer -match "metrics-server") {
        Write-Host "   ✓ metrics-server installed" -ForegroundColor Green
        
        # Try to get node metrics
        $nodeMetrics = kubectl top nodes 2>&1
        if ($nodeMetrics -match "NAME") {
            Write-Host "   ✓ metrics-server is working" -ForegroundColor Green
        } else {
            Write-Host "   ⚠ metrics-server installed but not ready yet (wait 30s)" -ForegroundColor Yellow
        }
    } else {
        throw "Not installed"
    }
} catch {
    Write-Host "   ✗ metrics-server not installed" -ForegroundColor Red
    Write-Host ""
    Write-Host "   Installing metrics-server..." -ForegroundColor Yellow
    kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
    Start-Sleep -Seconds 5
    
    Write-Host "   Patching for Docker Desktop..." -ForegroundColor Yellow
    kubectl patch deployment metrics-server -n kube-system --type='json' -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}]'
    
    Write-Host "   ✓ metrics-server installed (wait 30s for it to start)" -ForegroundColor Green
}

# Check Docker Compose
Write-Host ""
Write-Host "5. Checking Docker Compose..." -ForegroundColor Yellow
try {
    $composeVersion = docker compose version 2>&1
    if ($composeVersion -match "version") {
        Write-Host "   ✓ Docker Compose available: $($composeVersion)" -ForegroundColor Green
    } else {
        throw "Not found"
    }
} catch {
    Write-Host "   ✗ Docker Compose not found" -ForegroundColor Red
}

# Check if namespace exists
Write-Host ""
Write-Host "6. Checking trading-system namespace..." -ForegroundColor Yellow
try {
    $namespace = kubectl get namespace trading-system --no-headers 2>&1
    if ($namespace -match "trading-system") {
        Write-Host "   ✓ Namespace exists" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ Namespace not created yet" -ForegroundColor Yellow
        Write-Host "     Run: kubectl apply -f k8s/namespace.yaml" -ForegroundColor Gray
    }
} catch {
    Write-Host "   ⚠ Namespace not created yet" -ForegroundColor Yellow
    Write-Host "     Run: kubectl apply -f k8s/namespace.yaml" -ForegroundColor Gray
}

# Summary
Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Your Kubernetes environment is ready!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Start infrastructure with Docker Compose:" -ForegroundColor White
Write-Host "     docker compose up -d" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. Deploy to Kubernetes:" -ForegroundColor White
Write-Host "     kubectl apply -f k8s/namespace.yaml" -ForegroundColor Gray
Write-Host "     kubectl apply -f k8s/deployments/price-service-deployment.yaml" -ForegroundColor Gray
Write-Host "     kubectl apply -f k8s/services/price-service-service.yaml" -ForegroundColor Gray
Write-Host "     kubectl apply -f k8s/autoscaling/price-service-hpa.yaml" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. Monitor deployment:" -ForegroundColor White
Write-Host "     kubectl get pods -n trading-system" -ForegroundColor Gray
Write-Host "     kubectl get hpa -n trading-system" -ForegroundColor Gray
Write-Host ""
