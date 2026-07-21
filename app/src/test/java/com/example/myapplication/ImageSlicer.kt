package com.example.myapplication

import org.junit.Test
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.sqrt

class ImageSlicer {

    private val baseDir = "C:/Users/User/AndroidStudioProjects/MyApplication4"
    private val drawableDir = "$baseDir/app/src/main/res/drawable"

    @Test
    fun sliceAssets() {
        slicePlanets()
        sliceFleet()
    }

    private fun slicePlanets() {
        val input = File("$drawableDir/planet_sprite_sheet.png")
        if (!input.exists()) return
        val sheet = ImageIO.read(input)
        
        val cols = 4
        val rows = 3
        val cellW = sheet.width / cols
        val cellH = sheet.height / rows

        val names = listOf(
            "planet_01_forest", "planet_02_earth", "planet_03_bands", "planet_04_swirl",
            "planet_05_ice", "planet_06_tech", "planet_07_lava", "planet_08_desert",
            "planet_09_ocean", "planet_10_void", "planet_11_storm", "planet_12_moss"
        )

        for (i in names.indices) {
            val r = i / cols
            val c = i % cols
            
            val cell = sheet.getSubimage(c * cellW, r * cellH, cellW, cellH)
            
            // Pixel manipulation to remove the "pentagon" glint from the 12th planet
            val cleanCell = BufferedImage(cellW, cellH, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until cellH) {
                for (x in 0 until cellW) {
                    val rgb: Int = cell.getRGB(x, y)
                    val rv: Int = (rgb shr 16) and 0xff
                    val gv: Int = (rgb shr 8) and 0xff
                    
                    var finalAlpha = 255
                    // Kill the sparkle on the last planet (it's white/very bright)
                    if (i == 11 && x > cellW * 0.7 && y > cellH * 0.7 && rv > 200 && gv > 200) {
                        finalAlpha = 0
                    }
                    
                    cleanCell.setRGB(x, y, (finalAlpha shl 24) or (rgb and 0xffffff))
                }
            }
            
            val size = 512
            val polished = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val g = polished.createGraphics()
            
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            
            // Mask to keep it circular
            g.color = Color.WHITE
            g.fillOval(2, 2, size - 4, size - 4)
            g.composite = AlphaComposite.SrcIn
            
            val scale = size.toFloat() / Math.max(cellW, cellH).toFloat()
            val sw = (cellW * scale).toInt()
            val sh = (cellH * scale).toInt()
            
            g.drawImage(cleanCell, (size - sw) / 2, (size - sh) / 2, sw, sh, null)
            g.dispose()

            ImageIO.write(polished, "PNG", File("$drawableDir/${names[i]}.png"))
            println("Exported Planet: ${names[i]}.png")
        }
    }

    private fun sliceFleet() {
        val input = File("$drawableDir/ui_preview.png")
        if (!input.exists()) return
        val sheet = ImageIO.read(input)
        
        val cols = 6
        val rows = 5
        val cellW = sheet.width / cols
        val cellH = sheet.height / rows

        val names = listOf(
            "fleet_asteroid", "fleet_spider", "fleet_walker", "fleet_rocket_01",
            "fleet_rocket_02", "fleet_crate", "fleet_shuttle", "fleet_rover",
            "fleet_capsule", "fleet_ufo_net", "fleet_ufo_ring", "fleet_ufo_big",
            "fleet_ufo_alien", "fleet_station", "fleet_asteroid_cluster", "fleet_portal",
            "upgrade_weld_torch", "upgrade_magnet", "upgrade_signal_beacon",
            "upgrade_quantum_wrench", "upgrade_debris_harvester", "fleet_probe",
            "fleet_hauler", "fleet_sentinel", "fleet_comet", "fleet_railgun"
        )

        val bgColor = sheet.getRGB(0, 0)

        for (i in names.indices) {
            val r = i / cols
            val c = i % cols
            
            val startX = c * cellW
            val startY = r * cellH
            
            val cell = sheet.getSubimage(startX, startY, cellW, (cellH * 0.75).toInt())
            val cleanCell = removeBackground(cell, bgColor, 55.0)
            val trimmed = trimImage(cleanCell, 40)
            
            saveToCanvas(trimmed, names[i], 256, 0.85f)
        }
    }

    private fun removeBackground(img: BufferedImage, bgRGB: Int, threshold: Double): BufferedImage {
        val res = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
        val br = (bgRGB shr 16) and 0xff
        val bg = (bgRGB shr 8) and 0xff
        val bb = bgRGB and 0xff

        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val rgb = img.getRGB(x, y)
                val r = (rgb shr 16) and 0xff
                val g = (rgb shr 8) and 0xff
                val b = rgb and 0xff
                
                val dist = sqrt(((r - br) * (r - br) + (g - bg) * (g - bg) + (b - bb) * (b - bb)).toDouble())
                
                if (dist < threshold) {
                    res.setRGB(x, y, 0x00000000)
                } else {
                    res.setRGB(x, y, rgb or (0xff shl 24))
                }
            }
        }
        return res
    }

    private fun trimImage(img: BufferedImage, threshold: Int): BufferedImage {
        var minX = img.width; var minY = img.height
        var maxX = -1; var maxY = -1

        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val alpha = (img.getRGB(x, y) shr 24) and 0xff
                if (alpha > threshold) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX == -1) return img
        
        val w = (maxX - minX + 1)
        val h = (maxY - minY + 1)
        return img.getSubimage(minX, minY, w, h)
    }

    private fun saveToCanvas(img: BufferedImage, name: String, canvasSize: Int, padding: Float) {
        val finalImg = BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB)
        val g = finalImg.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        val scale = (canvasSize * padding) / max(img.width, img.height).toFloat()
        val sw = (img.width * scale).toInt()
        val sh = (img.height * scale).toInt()
        
        g.drawImage(img, (canvasSize - sw) / 2, (canvasSize - sh) / 2, sw, sh, null)
        g.dispose()
        
        ImageIO.write(finalImg, "PNG", File("$drawableDir/$name.png"))
        println("Re-cut Clean: $name.png")
    }
}
