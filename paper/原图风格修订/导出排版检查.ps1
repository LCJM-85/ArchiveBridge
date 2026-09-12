# 只读打开本轮论文副本，导出检查PDF，不保存或更新论文。
$ErrorActionPreference = 'Stop'
$paperPath = Join-Path $PSScriptRoot '论文_原图风格修订_实验待实测.docx'
$reviewFolder = Join-Path $PSScriptRoot '排版检查'
New-Item -ItemType Directory -Path $reviewFolder -Force | Out-Null
$paperWord = $null
$paperDocument = $null
try {
    $paperWord = New-Object -ComObject Word.Application
    $paperWord.Visible = $false
    $paperWord.DisplayAlerts = 0
    $paperDocument = $paperWord.Documents.Open($paperPath, $false, $true)
    $paperDocument.Repaginate()
    $paperDocument.ExportAsFixedFormat((Join-Path $reviewFolder '论文排版检查.pdf'), 17)
    Write-Output ('Pages: ' + $paperDocument.ComputeStatistics(2))
}
finally {
    if ($null -ne $paperDocument) { $paperDocument.Close(0) }
    if ($null -ne $paperWord) { $paperWord.Quit() }
}
