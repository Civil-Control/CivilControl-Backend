try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/transactional-documents?page=0&size=2" -Method GET -Headers @{"Accept"="application/json"}
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "StatusCode: $($_.Exception.Response.StatusCode.value__)"
}

