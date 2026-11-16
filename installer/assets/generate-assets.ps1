# generate-assets.ps1
# Genera los recursos graficos del asistente de instalacion de Farmac:
#   - wizard-image.bmp  (164 x 314)  Banner lateral del asistente
#   - wizard-small.bmp  (55 x 55)    Icono pequeno de las paginas interiores
#   - farmac.ico                     Icono de la aplicacion
#
# Identidad visual: fondo azul marino (#12294F), cruz verde (#2BB58A),
# texto "FARMAC" y lema "Cada lote, bajo control.".
#
# Uso:  powershell -ExecutionPolicy Bypass -File installer\assets\generate-assets.ps1

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'
$assets = Split-Path -Parent $MyInvocation.MyCommand.Definition

$navy  = [System.Drawing.Color]::FromArgb(0x12, 0x29, 0x4F)
$green = [System.Drawing.Color]::FromArgb(0x2B, 0xB5, 0x8A)
$white = [System.Drawing.Color]::White

function New-Canvas {
    param([int]$Width, [int]$Height)
    $bmp = New-Object System.Drawing.Bitmap($Width, $Height)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear($navy)
    return @{ Bitmap = $bmp; Graphics = $g }
}

function Draw-Cross {
    param($Graphics, [double]$CenterX, [double]$CenterY, [double]$Arm, [double]$Thickness, $Color)
    $brush = New-Object System.Drawing.SolidBrush($Color)
    $half = $Arm / 2.0
    $Graphics.FillRectangle($brush, [single]($CenterX - $half), [single]($CenterY - $Thickness / 2.0), [single]$Arm, [single]$Thickness)
    $Graphics.FillRectangle($brush, [single]($CenterX - $Thickness / 2.0), [single]($CenterY - $half), [single]$Thickness, [single]$Arm)
    $brush.Dispose()
}

# ---- Banner lateral 164 x 314 ------------------------------------------------
$wizard = New-Canvas -Width 164 -Height 314
$bmp = $wizard.Bitmap; $g = $wizard.Graphics
Draw-Cross -Graphics $g -CenterX 82 -CenterY 92 -Arm 74 -Thickness 26 -Color $green
$fontBig = New-Object System.Drawing.Font('Segoe UI', 26, [System.Drawing.FontStyle]::Bold)
$fontSmall = New-Object System.Drawing.Font('Segoe UI', 10, [System.Drawing.FontStyle]::Italic)
$sfCenter = New-Object System.Drawing.StringFormat
$sfCenter.Alignment = [System.Drawing.StringAlignment]::Center
$brushWhite = New-Object System.Drawing.SolidBrush($white)
$g.DrawString('FARMAC', $fontBig, $brushWhite, (New-Object System.Drawing.RectangleF(0, 165, 164, 46)), $sfCenter)
$g.DrawString('Cada lote, bajo control.', $fontSmall, $brushWhite, (New-Object System.Drawing.RectangleF(0, 214, 164, 30)), $sfCenter)
$wizard.Bitmap.Save((Join-Path $assets 'wizard-image.bmp'), [System.Drawing.Imaging.ImageFormat]::Bmp)
$fontBig.Dispose(); $fontSmall.Dispose(); $brushWhite.Dispose(); $sfCenter.Dispose()
$g.Dispose(); $wizard.Bitmap.Dispose()

# ---- Icono pequeno 55 x 55 ---------------------------------------------------
$small = New-Canvas -Width 55 -Height 55
$g = $small.Graphics
Draw-Cross -Graphics $g -CenterX 27.5 -CenterY 27.5 -Arm 34 -Thickness 12 -Color $green
$small.Bitmap.Save((Join-Path $assets 'wizard-small.bmp'), [System.Drawing.Imaging.ImageFormat]::Bmp)
$g.Dispose(); $small.Bitmap.Dispose()

# ---- Icono de la aplicacion (.ico) ------------------------------------------
$icon = New-Canvas -Width 256 -Height 256
$g = $icon.Graphics
Draw-Cross -Graphics $g -CenterX 128 -CenterY 118 -Arm 168 -Thickness 58 -Color $green
$fontIcon = New-Object System.Drawing.Font('Segoe UI', 34, [System.Drawing.FontStyle]::Bold)
$sfIcon = New-Object System.Drawing.StringFormat
$sfIcon.Alignment = [System.Drawing.StringAlignment]::Center
$brushWhite2 = New-Object System.Drawing.SolidBrush($white)
$g.DrawString('FARMAC', $fontIcon, $brushWhite2, (New-Object System.Drawing.RectangleF(0, 196, 256, 50)), $sfIcon)
$hicon = $icon.Bitmap.GetHicon()
$ico = [System.Drawing.Icon]::FromHandle($hicon)
$stream = [System.IO.File]::Create((Join-Path $assets 'farmac.ico'))
$ico.Save($stream)
$stream.Close()
$fontIcon.Dispose(); $brushWhite2.Dispose(); $sfIcon.Dispose()
$g.Dispose(); $icon.Bitmap.Dispose()

Write-Host "Assets generados en: $assets"
Get-ChildItem $assets -File | Where-Object { $_.Name -ne 'generate-assets.ps1' } | Select-Object Name, Length
