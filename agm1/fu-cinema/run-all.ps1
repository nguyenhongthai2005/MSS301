# ==============================================================================
# Script khoi chay 4 Microservices FUCinema trong 4 cua so PowerShell rieng biet
# ==============================================================================

Write-Host "Dang khoi chay 4 dich vu FUCinema..." -ForegroundColor Cyan

$services = @(
    @{ Name = "customer-service (Port 8081)"; Path = "customer-service" },
    @{ Name = "movie-service (Port 8082)";    Path = "movie-service" },
    @{ Name = "booking-service (Port 8083)";  Path = "booking-service" },
    @{ Name = "api-gateway (Port 9000)";      Path = "api-gateway" }
)

foreach ($s in $services) {
    Write-Host "-> Mo cua so: $($s.Name)..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle = '$($s.Name)'; cd '$($s.Path)'; .\mvnw spring-boot:run"
    Start-Sleep -Seconds 1
}

Write-Host "`nDa mo xong 4 cua so PowerShell cho 4 service!" -ForegroundColor Green
Write-Host "Doi khoang 10-15 giay de ca 4 service khoi dong xong." -ForegroundColor Cyan
