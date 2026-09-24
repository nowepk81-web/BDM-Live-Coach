# Creates a text-first knowledge file for a ChatGPT Project / managed-workspace GPT.
# It reads all supplied transcripts, including DOCX word/document.xml, and writes no audio.
param(
    [string]$SourceDirectory = (Split-Path -Parent $PSScriptRoot),
    [string]$OutputFile = (Join-Path $PSScriptRoot "BDM_REFERENCE_TRANSCRIPTS.md")
)

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Get-DocxText([string]$Path) {
    $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $entry = $archive.GetEntry('word/document.xml')
        if ($null -eq $entry) { return '' }
        $reader = [System.IO.StreamReader]::new($entry.Open())
        try {
            [xml]$xml = $reader.ReadToEnd()
            $namespaces = [System.Xml.XmlNamespaceManager]::new($xml.NameTable)
            $namespaces.AddNamespace('w', 'http://schemas.openxmlformats.org/wordprocessingml/2006/main')
            return (($xml.SelectNodes('//w:t', $namespaces) | ForEach-Object { $_.'#text' }) -join ' ')
        } finally { $reader.Dispose() }
    } finally { $archive.Dispose() }
}

$sources = Get-ChildItem -LiteralPath $SourceDirectory -File |
    Where-Object { $_.Name -notlike '~$*' -and $_.Extension -in '.txt', '.docx' } |
    Sort-Object Name

$builder = [System.Text.StringBuilder]::new()
[void]$builder.AppendLine('# Biblioteka referencyjna BDM Live Coach')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('Materiał źródłowy do rozpoznawania wzorców rozmów. Nie stanowi sam w sobie potwierdzenia funkcji produktu ani danych o konkretnym kliencie.')

foreach ($source in $sources) {
    try {
        $text = if ($source.Extension -eq '.docx') { Get-DocxText $source.FullName } else { Get-Content -LiteralPath $source.FullName -Raw -Encoding UTF8 }
        if (![string]::IsNullOrWhiteSpace($text)) {
            [void]$builder.AppendLine("`n---`n## Źródło: $($source.Name)`n")
            [void]$builder.AppendLine($text.Trim())
        }
    } catch { Write-Warning "Pominięto $($source.Name): $($_.Exception.Message)" }
}

[System.IO.File]::WriteAllText($OutputFile, $builder.ToString(), [System.Text.UTF8Encoding]::new($false))
Write-Host "Gotowe: $OutputFile"
