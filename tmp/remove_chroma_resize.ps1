param(
    [Parameter(Mandatory = $true)][string]$InputPath,
    [Parameter(Mandatory = $true)][string]$OutputPath
)

Add-Type -AssemblyName System.Drawing

$source = [System.Drawing.Bitmap]::new((Resolve-Path -LiteralPath $InputPath).Path)
$cutout = [System.Drawing.Bitmap]::new($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

for ($y = 0; $y -lt $source.Height; $y++) {
    for ($x = 0; $x -lt $source.Width; $x++) {
        $pixel = $source.GetPixel($x, $y)
        $greenExcess = $pixel.G - [Math]::Max($pixel.R, $pixel.B)
        $alpha = if ($greenExcess -ge 95 -and $pixel.G -ge 150) {
            0
        } elseif ($greenExcess -gt 25 -and $pixel.G -ge 110) {
            [Math]::Max(0, [Math]::Min(255, [int](255 * (95 - $greenExcess) / 70)))
        } else {
            255
        }

        if ($alpha -lt 255) {
            $newGreen = [Math]::Min($pixel.G, [Math]::Max($pixel.R, $pixel.B))
            $cutout.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $pixel.R, $newGreen, $pixel.B))
        } else {
            $cutout.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $pixel.R, $pixel.G, $pixel.B))
        }
    }
}

$final = [System.Drawing.Bitmap]::new(256, 256, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($final)
$graphics.Clear([System.Drawing.Color]::Transparent)
$graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
$graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$graphics.DrawImage($cutout, 0, 0, 256, 256)

$destination = Join-Path (Get-Location) $OutputPath
$final.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)

$graphics.Dispose()
$final.Dispose()
$cutout.Dispose()
$source.Dispose()
