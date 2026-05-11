package com.parkinggame

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

/**
 * UIHelper.kt — reusable drawing functions for buttons, panels, text
 *
 * IMPORTANT: drawCentredText does NOT call batch.begin/end itself.
 * The caller must wrap it in batch.begin() ... batch.end().
 */
object UIHelper {

    private val layout = GlyphLayout()

    fun drawPanel(sr: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, color: Color) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = color
        sr.rect(x, y, w, h)
        sr.end()
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(1f, 1f, 1f, 0.06f)
        sr.rect(x, y + h - 4f, w, 4f)
        sr.end()
    }

    /**
     * Draws a button and returns its Rectangle for hit testing.
     * Handles its own batch.begin/end internally.
     */
    fun drawButton(
        sr: ShapeRenderer,
        batch: SpriteBatch,
        font: BitmapFont,
        label: String,
        x: Float, y: Float, w: Float, h: Float,
        bgColor: Color = UITheme.BTN_PRIMARY,
        textColor: Color = UITheme.BG_DARK
    ): Rectangle {
        // Shadow
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0f, 0f, 0f, 0.35f)
        sr.rect(x + 3f, y - 3f, w, h)
        sr.end()

        // Button body
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = bgColor
        sr.rect(x, y, w, h)
        sr.end()

        // Top shine
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(1f, 1f, 1f, 0.10f)
        sr.rect(x, y + h - 5f, w, 5f)
        sr.end()

        // Label — owns its own begin/end
        batch.begin()
        font.color = textColor
        layout.setText(font, label)
        font.draw(batch, label, x + (w - layout.width) / 2f, y + (h + layout.height) / 2f)
        batch.end()

        return Rectangle(x, y, w, h)
    }

    /**
     * Draw centred text. Caller must have already called batch.begin().
     */
    fun drawCentredText(
        batch: SpriteBatch,
        font: BitmapFont,
        text: String,
        centreX: Float,
        y: Float,
        color: Color = UITheme.TEXT_WHITE
    ) {
        layout.setText(font, text)
        font.color = color
        font.draw(batch, text, centreX - layout.width / 2f, y)
    }

    /** Check if a screen-space touch hits a rectangle (flips Y from top-origin to bottom-origin) */
    fun hits(rect: Rectangle, touchX: Float, touchY: Float, screenH: Float): Boolean {
        return rect.contains(touchX, screenH - touchY)
    }
}